package com.goreecloud.browsermobile.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** First-phase Webspaces: tab grouping and URL rules; NOT isolated WebView cookie stores. */
data class WebspaceEntry(val id: String, val name: String)

object WebspacePolicy {
    val builtIns = listOf(
        WebspaceEntry("standard", "Standard"),
        WebspaceEntry("goreecloud", "GoreeCloud"),
        WebspaceEntry("google", "Google"),
        WebspaceEntry("microsoft", "Microsoft"),
        WebspaceEntry("meta", "Meta"),
        WebspaceEntry("proton", "Proton")
    )

    fun validName(name: String): Boolean {
        val text = name.trim()
        return text.length in 2..30 && text.none { it.isISOControl() } &&
            text.any { it.isLetterOrDigit() }
    }

    private fun matches(host: String, domain: String) =
        host == domain || host.endsWith(".$domain")

    fun route(url: String, assignments: Map<String, String> = emptyMap()): String {
        val host = VisitPolicy.host(url) ?: return "standard"
        assignments[host]?.let { return it }
        return when {
            listOf("goreecloud.com").any { matches(host, it) } -> "goreecloud"
            listOf("google.com", "gmail.com", "youtube.com").any { matches(host, it) } -> "google"
            listOf("microsoft.com", "microsoftonline.com", "outlook.com", "live.com", "office.com")
                .any { matches(host, it) } -> "microsoft"
            listOf("facebook.com", "instagram.com", "whatsapp.com", "threads.net")
                .any { matches(host, it) } -> "meta"
            listOf("proton.me", "protonmail.com", "protonvpn.com")
                .any { matches(host, it) } -> "proton"
            else -> "standard"
        }
    }
}

/** Configuration stored locally; never exports cookies, auth sessions or secrets. */
class WebspacesManager(context: Context) {
    private val storage = context.getSharedPreferences("goreecloud_webspaces", Context.MODE_PRIVATE)

    var custom by mutableStateOf(loadCustom())
        private set
    var assignments by mutableStateOf(loadAssignments())
        private set

    val entries: List<WebspaceEntry> get() = WebspacePolicy.builtIns + custom

    fun isKnown(id: String): Boolean = entries.any { it.id == id }

    fun destination(url: String): String = WebspacePolicy.route(url,
        assignments.filterValues { isKnown(it) })

    fun create(name: String): WebspaceEntry? {
        val trimmed = name.trim()
        if (!WebspacePolicy.validName(trimmed) || custom.size >= 10 ||
            entries.any { it.name.equals(trimmed, ignoreCase = true) }) return null
        val entry = WebspaceEntry("custom-" + UUID.randomUUID(), trimmed)
        custom = custom + entry
        saveCustom()
        return entry
    }

    fun remove(id: String) {
        if (custom.none { it.id == id }) return
        custom = custom.filter { it.id != id }
        assignments = assignments.filterValues { it != id }
        // One preference transaction: a deleted Webspace must not retain its
        // exact-host routing assignments if the app is stopped between writes.
        storage.edit().putString("custom", customJson())
            .putString("assignments", assignmentsJson()).apply()
    }

    fun assign(url: String, id: String) {
        val host = VisitPolicy.host(url) ?: return
        if (!isKnown(id) || (host !in assignments && assignments.size >= 100)) return
        assignments = assignments + (host to id)
        saveAssignments()
    }

    fun removeAssignment(url: String) {
        val host = VisitPolicy.host(url) ?: return
        assignments = assignments - host
        saveAssignments()
    }

    private fun loadCustom(): List<WebspaceEntry> = try {
        val a = JSONArray(storage.getString("custom", "[]"))
        val out = ArrayList<WebspaceEntry>()
        for (i in 0 until minOf(a.length(), 10)) {
            val obj = a.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            val name = obj.optString("name", "")
            if (id.startsWith("custom-") && id.length <= 64 && WebspacePolicy.validName(name) &&
                out.none { it.id == id } && WebspacePolicy.builtIns.none {
                    it.name.equals(name, ignoreCase = true)
                }) out.add(WebspaceEntry(id, name))
        }
        out
    } catch (_: Exception) { emptyList() }

    private fun loadAssignments(): Map<String, String> = try {
        val obj = JSONObject(storage.getString("assignments", "{}"))
        val result = LinkedHashMap<String, String>()
        for (key in obj.keys()) {
            if (result.size >= 100) break
            if (VisitPolicy.host("https://$key/") != key) continue
            val id = obj.optString(key, "")
            if (id in (WebspacePolicy.builtIns + custom).map { it.id }) result[key] = id
        }
        result
    } catch (_: Exception) { emptyMap() }

    private fun customJson(): String {
        val a = JSONArray()
        custom.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
        return a.toString()
    }

    private fun assignmentsJson(): String {
        val obj = JSONObject()
        assignments.forEach { (host, id) -> obj.put(host, id) }
        return obj.toString()
    }

    private fun saveCustom() {
        storage.edit().putString("custom", customJson()).apply()
    }

    private fun saveAssignments() {
        storage.edit().putString("assignments", assignmentsJson()).apply()
    }
}
