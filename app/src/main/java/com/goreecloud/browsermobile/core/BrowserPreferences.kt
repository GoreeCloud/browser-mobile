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
    var cleanUrls by mutableStateOf(storage.getBoolean("clean_urls", true))
        private set
    var acceptCookies by mutableStateOf(storage.getBoolean("accept_cookies", true))
        private set
    var thirdPartyCookies by mutableStateOf(storage.getBoolean("third_party_cookies", false))
        private set
    var savedSites by mutableStateOf(loadSites())
        private set

    init { CookieManager.getInstance().setAcceptCookie(acceptCookies) }

    fun setBlockAds(enabled: Boolean) {
        blockAds = enabled
        storage.edit().putBoolean("block_ads", enabled).apply()
    }

    fun setCleanUrls(enabled: Boolean) {
        cleanUrls = enabled
        storage.edit().putBoolean("clean_urls", enabled).apply()
    }

    fun setAcceptCookies(enabled: Boolean) {
        acceptCookies = enabled
        CookieManager.getInstance().setAcceptCookie(enabled)
        storage.edit().putBoolean("accept_cookies", enabled).apply()
    }

    fun setThirdPartyCookies(enabled: Boolean) {
        thirdPartyCookies = enabled
        storage.edit().putBoolean("third_party_cookies", enabled).apply()
    }

    fun toggleBookmark(url: String) {
        if (!NavigationPolicy.isHttpUrl(url)) return
        savedSites = if (url in savedSites) savedSites - url else (listOf(url) + savedSites).take(30)
        val array = JSONArray()
        savedSites.forEach(array::put)
        storage.edit().putString("bookmarks", array.toString()).apply()
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
