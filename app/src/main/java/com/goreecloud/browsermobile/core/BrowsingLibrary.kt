package com.goreecloud.browsermobile.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/** A bounded, local-only website-level history. Never stores paths, queries or fragments. */
data class VisitedSite(
    val host: String,
    val visits: Int,
    val lastVisitedMs: Long
) {
    val url: String get() = "https://$host/"
}

object VisitPolicy {
    fun host(url: String): String? {
        if (!NavigationPolicy.isHttpUrl(url)) return null
        val uri = try { URI(url) } catch (_: Exception) { return null }
        return uri.host?.lowercase()?.takeIf { it.length in 1..253 }
    }

    fun record(
        previous: List<VisitedSite>, url: String, now: Long
    ): List<VisitedSite> {
        val host = host(url) ?: return previous
        val old = previous.firstOrNull { it.host == host }
        val changed = VisitedSite(
            host, ((old?.visits ?: 0) + 1).coerceAtMost(1_000_000), now
        )
        return (listOf(changed) + previous.filter { it.host != host })
            .sortedByDescending { it.lastVisitedMs }.take(100)
    }

    fun recent(sites: List<VisitedSite>): List<VisitedSite> =
        sites.sortedByDescending { it.lastVisitedMs }.take(8)

    fun mostVisited(sites: List<VisitedSite>): List<VisitedSite> =
        sites.sortedWith(compareByDescending<VisitedSite> { it.visits }
            .thenByDescending { it.lastVisitedMs }).take(8)
}

/** App-private, explicit-clear, max-100 website index. No cross-device sync. */
class BrowsingLibrary(context: Context) {
    private val storage = context.getSharedPreferences("goreecloud_site_library", Context.MODE_PRIVATE)

    var sites by mutableStateOf(load())
        private set
    var historyEnabled by mutableStateOf(storage.getBoolean("history_enabled", true))
        private set

    fun updateHistoryEnabled(enabled: Boolean) {
        historyEnabled = enabled
        storage.edit().putBoolean("history_enabled", enabled).apply()
    }

    fun record(url: String) {
        if (!historyEnabled) return
        val next = VisitPolicy.record(sites, url, System.currentTimeMillis())
        if (next != sites) {
            sites = next
            persist(next)
        }
    }

    fun clear() {
        sites = emptyList()
        storage.edit().remove("sites").apply()
    }

    private fun load(): List<VisitedSite> = try {
        val array = JSONArray(storage.getString("sites", "[]"))
        val result = ArrayList<VisitedSite>()
        for (i in 0 until minOf(array.length(), 100)) {
            val obj = array.optJSONObject(i) ?: continue
            val host = obj.optString("host", "")
            if (VisitPolicy.host("https://$host") != host) continue
            val count = obj.optInt("visits", 0)
            if (count !in 1..1_000_000) continue
            result.add(VisitedSite(host, count, obj.optLong("lastVisited", 0L)))
        }
        result.distinctBy { it.host }.take(100)
    } catch (_: Exception) { emptyList() }

    private fun persist(value: List<VisitedSite>) {
        val array = JSONArray()
        value.forEach { site ->
            array.put(JSONObject().put("host", site.host).put("visits", site.visits).put("lastVisited", site.lastVisitedMs))
        }
        storage.edit().putString("sites", array.toString()).apply()
    }
}
