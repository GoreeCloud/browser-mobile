package com.goreecloud.browsermobile.core

import java.net.URI
import java.util.Locale

/**
 * A user choice to exempt a specific top-level website from browser subresource
 * filtering. Other sites and the browser-wide adblock setting are unaffected.
 */
object SiteExceptions {
    fun hostForPage(url: String): String? {
        val uri = try { URI(url) } catch (_: Exception) { return null }
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) return null
        if (uri.rawUserInfo != null) return null
        val host = uri.host ?: return null
        val canonical = host.lowercase(Locale.ROOT).trimEnd('.')
        if (canonical == "localhost" || canonical.indexOf('.') < 0) return null
        if (canonical.length !in 4..253) return null
        return canonical
    }

    fun isAllowed(allowedHosts: Set<String>, pageUrl: String): Boolean {
        val host = hostForPage(pageUrl) ?: return false
        return host in allowedHosts
    }
}
