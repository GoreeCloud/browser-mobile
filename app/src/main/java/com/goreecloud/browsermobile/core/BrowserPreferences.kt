package com.goreecloud.browsermobile.core

import android.content.Context
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

class BrowserPreferences(context: Context) {
    private val storage = context.getSharedPreferences("goreecloud_browser_preferences", Context.MODE_PRIVATE)

    var blockAds by mutableStateOf(storage.getBoolean("block_ads", true))
        private set
    @Volatile var blockAdsForRequests = blockAds
        private set
    var cleanUrls by mutableStateOf(storage.getBoolean("clean_urls", true))
        private set
    var acceptCookies by mutableStateOf(storage.getBoolean("accept_cookies", true))
        private set
    var thirdPartyCookies by mutableStateOf(storage.getBoolean("third_party_cookies", false))
        private set
    var savedSites by mutableStateOf(loadSites())
        private set

    // Immutable snapshots allow safe reads from WebView's background request threads.
    @Volatile private var exceptionSnapshot = loadSiteExceptions()
    var siteExceptionHosts by mutableStateOf(exceptionSnapshot)
        private set

    init { CookieManager.getInstance().setAcceptCookie(acceptCookies) }

    fun updateBlockAds(enabled: Boolean) {
        blockAds = enabled
        blockAdsForRequests = enabled
        storage.edit().putBoolean("block_ads", enabled).apply()
    }

    fun updateCleanUrls(enabled: Boolean) {
        cleanUrls = enabled
        storage.edit().putBoolean("clean_urls", enabled).apply()
    }

    fun updateAcceptCookies(enabled: Boolean) {
        acceptCookies = enabled
        CookieManager.getInstance().setAcceptCookie(enabled)
        storage.edit().putBoolean("accept_cookies", enabled).apply()
    }

    fun updateThirdPartyCookies(enabled: Boolean) {
        thirdPartyCookies = enabled
        storage.edit().putBoolean("third_party_cookies", enabled).apply()
    }

    fun isSiteExempt(pageUrl: String): Boolean =
        SiteExceptions.isAllowed(exceptionSnapshot, pageUrl)

    fun setSiteExempt(pageUrl: String, exempt: Boolean) {
        val host = SiteExceptions.hostForPage(pageUrl) ?: return
        val next = if (exempt) {
            if (host !in exceptionSnapshot && exceptionSnapshot.size >= 100) return
            exceptionSnapshot + host
        } else {
            exceptionSnapshot - host
        }
        exceptionSnapshot = next
        siteExceptionHosts = next
        val array = JSONArray()
        next.sorted().forEach(array::put)
        storage.edit().putString("site_exceptions", array.toString()).apply()
    }

    fun toggleBookmark(url: String) {
        if (!NavigationPolicy.isHttpUrl(url)) return
        savedSites = if (url in savedSites) savedSites - url else (listOf(url) + savedSites).take(30)
        val array = JSONArray()
        savedSites.forEach(array::put)
        storage.edit().putString("bookmarks", array.toString()).apply()
    }

    private fun loadSiteExceptions(): Set<String> = try {
        val array = JSONArray(storage.getString("site_exceptions", "[]"))
        (0 until minOf(array.length(), 100)).mapNotNull { index ->
            val candidate = array.getString(index)
            SiteExceptions.hostForPage("https://" + candidate)
        }.toSet()
    } catch (_: Exception) {
        emptySet()
    }

    private fun loadSites(): List<String> = try {
        val array = JSONArray(storage.getString("bookmarks", "[]"))
        (0 until array.length()).map { array.getString(it) }
            .filter { NavigationPolicy.isHttpUrl(it) }
            .distinct().take(30)
    } catch (_: Exception) {
        emptyList()
    }
}
