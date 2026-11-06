package com.goreecloud.browsermobile.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

data class FilterListStatus(
    val enabled: Boolean,
    val domains: Int = 0,
    val updating: Boolean = false,
    val hasCache: Boolean = false,
    val lastUpdated: Long = 0L,
    val updateFailed: Boolean = false
)

/**
 * Downloads reviewed lists to private storage on a background thread.
 * Updates are atomic and never replace last-good rules with an invalid response.
 * This does not provide cosmetic rules or a systemwide DNS blocker.
 */
class FilterListEngine(context: Context, blockEnabled: Boolean = true) : AutoCloseable {
    private val files = File(context.filesDir, "filter-lists").apply { mkdirs() }
    private val preferences = context.getSharedPreferences("goreecloud_filter_subscriptions", Context.MODE_PRIVATE)
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val loaded = ConcurrentHashMap<FilterSource, Set<String>>()

    val states = mutableStateMapOf<FilterSource, FilterListStatus>()
    var activeRuleCount by mutableIntStateOf(0)
        private set

    @Volatile private var matcher = DomainRuleMatcher(emptySet())
    @Volatile private var hasReadyRules = false

    init {
        FilterSource.entries.forEach { source ->
            states[source] = FilterListStatus(enabled = isEnabled(source))
        }
        if (blockEnabled) refresh()
    }

    private fun isEnabled(source: FilterSource): Boolean =
        preferences.getBoolean("enabled_" + source.name, source.enabledByDefault)

    fun setEnabled(source: FilterSource, enabled: Boolean) {
        preferences.edit().putBoolean("enabled_" + source.name, enabled).apply()
        states[source] = states.getValue(source).copy(enabled = enabled, updateFailed = false)

        // OISD Big includes Small. Switching between them does not disable StevenBlack.
        val opposite = when (source) {
            FilterSource.OISD_BIG -> FilterSource.OISD_SMALL
            FilterSource.OISD_SMALL -> FilterSource.OISD_BIG
            else -> null
        }
        if (enabled && opposite != null) {
            preferences.edit().putBoolean("enabled_" + opposite.name, false).apply()
            states[opposite] = states.getValue(opposite).copy(enabled = false)
        }
        rebuildMatcher()
        if (enabled) enqueueLoad(source, false)
    }

    fun refresh(force: Boolean = false) {
        FilterSource.entries.filter(::isEnabled).forEach { enqueueLoad(it, force) }
    }

    private fun enqueueLoad(source: FilterSource, force: Boolean) {
        worker.execute {
            if (!isEnabled(source)) return@execute
            publish(source) { it.copy(updating = true, updateFailed = false) }
            try {
                val cache = File(files, source.name.lowercase() + ".txt")
                if (!loaded.containsKey(source) && cache.isFile) {
                    try {
                        val saved = cache.reader(Charsets.UTF_8).use {
                            DomainFilterParser.parse(it, source.format)
                        }
                        if (saved.size >= MIN_DOMAINS) {
                            loaded[source] = saved
                            rebuildMatcher()
                            publish(source) { it.copy(domains = saved.size, hasCache = true, lastUpdated = cache.lastModified()) }
                        }
                    } catch (_: Exception) {
                        // A malformed cache cannot prevent a clean network update.
                    }
                }
                val expired = System.currentTimeMillis() - cache.lastModified() >= REFRESH_INTERVAL_MS
                if (force || !cache.isFile || expired || !loaded.containsKey(source)) {
                    val fresh = download(source)
                    loaded[source] = fresh
                    rebuildMatcher()
                    publish(source) {
                        it.copy(domains = fresh.size, hasCache = true, lastUpdated = cache.lastModified())
                    }
                }
                publish(source) { it.copy(updating = false) }
            } catch (_: Exception) {
                publish(source) { it.copy(updating = false, updateFailed = true) }
            }
        }
    }

    private fun download(source: FilterSource): Set<String> {
        val url = URL(source.url)
        check(url.protocol == "https")
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 18_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "text/plain")
            setRequestProperty("Accept-Encoding", "identity")
        }
        val temporary = File.createTempFile("filter-", ".partial", files)
        try {
            check(connection.responseCode == 200) { "Filter source did not return HTTP 200" }
            val declared = connection.contentLengthLong
            check(declared <= MAX_BYTES || declared < 0L) { "Oversized list" }
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { out ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= MAX_BYTES) { "Oversized list" }
                        out.write(buffer, 0, count)
                    }
                    out.fd.sync()
                }
            }
            val rules = temporary.reader(Charsets.UTF_8).use {
                DomainFilterParser.parse(it, source.format)
            }
            check(rules.size >= MIN_DOMAINS) { "List did not contain enough valid rules" }
            val destination = File(files, source.name.lowercase() + ".txt")
            check(temporary.renameTo(destination)) { "Could not replace cached filter list" }
            return rules
        } finally {
            connection.disconnect()
            temporary.delete()
        }
    }

    /** Called on WebView request threads; no disk or network access. */
    fun shouldBlock(url: String): Boolean {
        if (matcher.matchesUrl(url)) return true
        // Limited fallback while all requested remote lists are unavailable.
        return !hasReadyRules && FilterSource.entries.any(::isEnabled) &&
            RequestBlocker.shouldBlock(url)
    }

    private fun rebuildMatcher() {
        val all = HashSet<String>()
        FilterSource.entries.filter(::isEnabled).forEach { loaded[it]?.let(all::addAll) }
        matcher = DomainRuleMatcher(all)
        hasReadyRules = all.isNotEmpty()
        main.post { activeRuleCount = all.size }
    }

    private fun publish(source: FilterSource, change: (FilterListStatus) -> FilterListStatus) {
        main.post {
            val state = states[source] ?: return@post
            states[source] = change(state).copy(enabled = isEnabled(source))
        }
    }

    override fun close() {
        worker.shutdownNow()
    }

    companion object {
        private const val MIN_DOMAINS = 100
        private const val MAX_BYTES = 25L * 1024 * 1024
        private const val REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
