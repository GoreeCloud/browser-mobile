package com.goreecloud.browsermobile.core

import java.net.URI
import java.net.URLEncoder
import java.util.Locale

/** Policy is deliberately fixed to Startpage until a verified GoreeCloud Search release is approved. */
object SearchPolicy {
    const val ACTIVE_ENGINE = "Startpage"
    const val HOME_URL = "https://www.startpage.com"
    private const val SEARCH_URL = "https://www.startpage.com/do/search?query="

    fun search(query: String): String = SEARCH_URL + URLEncoder.encode(query, "UTF-8")
}

data class CleanedUrl(val url: String, val removedParameters: Int)

/** Removes known tracking query fields without rewriting unrelated parameters or fragments. */
object UrlCleaner {
    private val trackingKeys = setOf(
        "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid",
        "mc_cid", "mc_eid", "igshid", "yclid", "_hsenc", "_hsmi",
        "vero_id", "oly_anon_id", "oly_enc_id", "twclid", "ttclid",
        "srsltid", "ref_src", "spm", "scid"
    )

    fun clean(input: String): CleanedUrl {
        val uri = try { URI(input) } catch (_: Exception) { return CleanedUrl(input, 0) }
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) {
            return CleanedUrl(input, 0)
        }
        val query = uri.rawQuery ?: return CleanedUrl(input, 0)
        val fragment = input.indexOf('#').let { if (it >= 0) input.substring(it) else "" }
        val beforeFragment = if (fragment.isNotEmpty()) input.dropLast(fragment.length) else input
        val questionIndex = beforeFragment.indexOf('?')
        if (questionIndex == -1) return CleanedUrl(input, 0)

        var removed = 0
        val kept = query.split("&").filter { param ->
            val rawName = param.substringBefore("=")
            val name = try {
                java.net.URLDecoder.decode(rawName, "UTF-8").lowercase(Locale.ROOT)
            } catch (_: Exception) {
                rawName.lowercase(Locale.ROOT)
            }
            val tracking = name.startsWith("utm_") || name in trackingKeys
            if (tracking) removed++
            !tracking
        }
        if (removed == 0) return CleanedUrl(input, 0)
        val base = beforeFragment.substring(0, questionIndex)
        val rewritten = base + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + fragment
        return CleanedUrl(rewritten, removed)
    }
}

data class WebNavigationAction(
    val intercept: Boolean,
    val destination: String? = null,
    val removedParameters: Int = 0
)

object NavigationPolicy {
    /**
     * Safe top-level WebView navigation decision. A rewritten non-GET request
     * must be blocked rather than silently reissued as an unauthenticated GET.
     */
    fun navigationAction(url: String, method: String, cleanLinks: Boolean): WebNavigationAction {
        if (!isHttpUrl(url)) return WebNavigationAction(intercept = true)
        val secured = secureHttpUrl(url)
        val cleaned = if (cleanLinks) UrlCleaner.clean(secured) else null
        val destination = cleaned?.url ?: secured
        if (destination == url) return WebNavigationAction(intercept = false)
        if (!method.equals("GET", ignoreCase = true)) return WebNavigationAction(intercept = true)
        return WebNavigationAction(
            intercept = true,
            destination = destination,
            removedParameters = cleaned?.removedParameters ?: 0
        )
    }

    private val hostPattern = Regex(
        """^(localhost|(?:[\p{L}\p{N}-]+\.)+[\p{L}\p{N}-]{2,})(?::\d{1,5})?(?:[/?#].*)?$""",
        RegexOption.IGNORE_CASE
    )
    private val ipv4Pattern = Regex("""^\d{1,3}(?:\.\d{1,3}){3}(?::\d{1,5})?(?:[/?#].*)?$""")
    // Pasted URI schemes are not search queries. In particular, credentials,
    // content paths and intent payloads must never be sent to Startpage.
    private val explicitUriScheme = Regex("""^[a-z][a-z0-9+.-]*:(?://|\S)""", RegexOption.IGNORE_CASE)

    fun resolve(text: String, cleanLinks: Boolean = true): String {
        val input = text.trim()
        if (input.isBlank()) return ""
        val looksLikeUrl = input.startsWith("https://", true) || input.startsWith("http://", true)
        // Explicit web addresses may contain credentials or private paths.
        // Never forward malformed explicit URLs to an external search provider.
        if (looksLikeUrl && !isHttpUrl(input)) return ""
        // Host:port address bars are URLs, not unknown protocol schemes.
        val bareWebAddress = !input.any { it.isWhitespace() } &&
            (hostPattern.matches(input) || ipv4Pattern.matches(input))
        if (!looksLikeUrl && !bareWebAddress && explicitUriScheme.containsMatchIn(input)) return ""
        val target = when {
            looksLikeUrl -> input
            bareWebAddress -> "https://$input"
            else -> return SearchPolicy.search(input)
        }
        return if (isHttpUrl(target)) {
            val secured = secureHttpUrl(target)
            if (cleanLinks) UrlCleaner.clean(secured).url else secured
        } else {
            SearchPolicy.search(input)
        }
    }

    /**
     * Never load cleartext pages in this browser. Upgrade explicit HTTP to HTTPS,
     * without a silent HTTP fallback if TLS fails. Only call for validated web URLs.
     */
    fun secureHttpUrl(value: String): String {
        if (!isHttpUrl(value)) return value
        return if (value.startsWith("http://", ignoreCase = true)) {
            "https://" + value.substring(7)
        } else value
    }

    fun isHttpUrl(value: String): Boolean {
        return try {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host
            (scheme == "https" || scheme == "http") &&
                !host.isNullOrBlank() &&
                uri.rawUserInfo == null &&
                !value.any { it == '\n' || it == '\r' || it == '\t' }
        } catch (_: Exception) {
            false
        }
    }
}

/** Conservative built-in request-domain list; not equivalent to full cosmetic network filtering. */
object RequestBlocker {
    private val blockedDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adnxs.com", "adsrvr.org", "taboola.com", "outbrain.com",
        "scorecardresearch.com", "criteo.com", "criteo.net", "pubmatic.com",
        "rubiconproject.com", "openx.net", "casalemedia.com", "quantserve.com",
        "moatads.com", "hotjar.com", "segment.io", "branch.io", "adjust.com"
    )

    fun shouldBlock(url: String): Boolean {
        val host = try { URI(url).host?.lowercase(Locale.ROOT) } catch (_: Exception) { null }
            ?: return false
        return blockedDomains.any { host == it || host.endsWith(".$it") }
    }
}
