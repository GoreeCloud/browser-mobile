package com.goreecloud.browsermobile.core

/**
 * Explicit, nonpersistent actions on the current webpage.
 *
 * The desktop user agent is a best-effort request, not a claim that Android
 * WebView has become a desktop browser. No OS user-agent override is stored.
 */
object PageActionPolicy {
    private val chromiumVersion = Regex(
        """(?:^|\s)(?:Chrome|Chromium)/(\d+(?:\.\d+){0,3})(?=\s|$)"""
    )

    /**
     * Builds a bounded, standard desktop Chrome user agent using only the
     * Chrome version advertised by the installed Android WebView. If the
     * version cannot be identified, refuse to offer synthetic desktop mode.
     */
    fun desktopUserAgent(original: String): String? {
        val version = chromiumVersion.find(original)?.groupValues?.getOrNull(1)
            ?: return null
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/$version Safari/537.36"
    }

    /**
     * Only explicitly valid HTTP(S) page URLs are eligible for OS clipboard
     * writes. Remove the *known* advertising query keys, preserving all
     * other query parameters, path and fragment without following redirects.
     */
    fun cleanCopyableLink(url: String): String? =
        url.takeIf(NavigationPolicy::isHttpUrl)?.let { UrlCleaner.clean(it).url }
}
