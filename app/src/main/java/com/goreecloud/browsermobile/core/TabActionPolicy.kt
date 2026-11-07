package com.goreecloud.browsermobile.core

/**
 * Privacy-bounded, session-only actions for existing Android browser tabs.
 * No WebView state, thumbnail, POST body, cookie or browser history is stored.
 */
internal data class RecentlyClosedPage(
    val url: String,
    val title: String,
    val webspaceId: String,
    val pinnedWebspace: Boolean,
    val closedAtElapsedMs: Long
)

internal object TabActionPolicy {
    const val MAX_OPEN_TABS = 12
    const val REOPEN_WINDOW_MS = 30_000L
    private const val MAX_URL_LENGTH = 2048
    private const val MAX_TITLE_LENGTH = 100
    private const val MAX_WEBSPACE_ID_LENGTH = 64

    fun isEligiblePage(url: String): Boolean =
        url.length <= MAX_URL_LENGTH &&
            url.startsWith("https://", ignoreCase = true) &&
            NavigationPolicy.isHttpUrl(url)

    fun canDuplicate(url: String, openTabCount: Int): Boolean =
        openTabCount in 1 until MAX_OPEN_TABS && isEligiblePage(url)

    fun snapshot(
        url: String,
        title: String,
        webspaceId: String,
        pinnedWebspace: Boolean,
        closedAtElapsedMs: Long
    ): RecentlyClosedPage? {
        if (!isEligiblePage(url) || closedAtElapsedMs < 0L) return null
        return RecentlyClosedPage(
            url = url,
            title = title.take(MAX_TITLE_LENGTH).ifBlank { "Website" },
            webspaceId = webspaceId.take(MAX_WEBSPACE_ID_LENGTH).ifBlank { "standard" },
            pinnedWebspace = pinnedWebspace,
            closedAtElapsedMs = closedAtElapsedMs
        )
    }

    fun canReopen(
        snapshot: RecentlyClosedPage?,
        nowElapsedMs: Long,
        openTabCount: Int
    ): Boolean {
        if (snapshot == null || openTabCount !in 1 until MAX_OPEN_TABS) return false
        val elapsed = nowElapsedMs - snapshot.closedAtElapsedMs
        return nowElapsedMs >= snapshot.closedAtElapsedMs &&
            elapsed in 0L..REOPEN_WINDOW_MS && isEligiblePage(snapshot.url)
    }
}
