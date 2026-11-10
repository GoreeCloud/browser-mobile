package com.goreecloud.browsermobile.core

import org.junit.Assert.*
import org.junit.Test

class NavigationPolicyTest {
    @Test fun defaultSearchIsStartpage() {
        assertEquals("https://www.startpage.com/do/search?query=privacy+first",
            NavigationPolicy.resolve("privacy first"))
        assertEquals("Startpage", SearchPolicy.ACTIVE_ENGINE)
    }

    @Test fun resolvesBareDomainToHttps() {
        assertEquals("https://example.com/path", NavigationPolicy.resolve("example.com/path"))
    }

    @Test fun upgradesExplicitHttpWithoutDowngradeFallback() {
        assertEquals("https://example.com/path", NavigationPolicy.resolve("http://example.com/path"))
        assertEquals(
            "https://example.com/p?q=a#top",
            NavigationPolicy.resolve("HTTP://example.com/p?utm_source=x&q=a#top")
        )
        assertEquals("https://example.com/p", NavigationPolicy.resolve("http://example.com/p", cleanLinks = false))
        assertEquals(
            "https://example.com:8080/p", NavigationPolicy.secureHttpUrl("http://example.com:8080/p")
        )
    }

    @Test fun urlCleanerDoesNotRestoreHttpScheme() {
        val result = NavigationPolicy.resolve("http://example.org/?fbclid=tracking&q=privacy")
        assertEquals("https://example.org/?q=privacy", result)
        assertEquals("https://example.org/", NavigationPolicy.secureHttpUrl("https://example.org/"))
    }

    @Test fun interceptedMainFrameNavigationIsHttpsOnlyAndCleaned() {
        val result = NavigationPolicy.navigationAction(
            "http://example.com/?utm_source=mail&q=cat", "GET", cleanLinks = true
        )
        assertTrue(result.intercept)
        assertEquals("https://example.com/?q=cat", result.destination)
        assertEquals(1, result.removedParameters)
        val direct = NavigationPolicy.navigationAction("https://example.com", "GET", true)
        assertFalse(direct.intercept)
        assertNull(direct.destination)
    }

    @Test fun neverReplaysNonGetOrNonWebSchemes() {
        val post = NavigationPolicy.navigationAction(
            "https://example.com/?fbclid=campaign", "POST", true
        )
        assertTrue(post.intercept)
        assertNull(post.destination)
        assertEquals(0, post.removedParameters)
        val cleartextPost = NavigationPolicy.navigationAction(
            "http://example.org/form", "POST", false
        )
        assertTrue(cleartextPost.intercept)
        assertNull(cleartextPost.destination)
        val javascript = NavigationPolicy.navigationAction("javascript:alert(1)", "GET", true)
        assertTrue(javascript.intercept)
        assertNull(javascript.destination)
    }

    @Test fun keepsUsefulParamsAndFragments() {
        val result = UrlCleaner.clean("https://example.com/?utm_source=mail&q=cat&fbclid=abc#part")
        assertEquals("https://example.com/?q=cat#part", result.url)
        assertEquals(2, result.removedParameters)
    }

    @Test fun doesNotAlterUntargetedLinks() {
        val url = "https://example.com/?q=cat&ref=help"
        assertEquals(url, UrlCleaner.clean(url).url)
    }

    @Test fun malformedExplicitWebAddressesNeverBecomeExternalSearchQueries() {
        for (input in listOf(
            "https://person:private-password@example.com/private",
            "https://example.com/private path",
            "http://",
            "HTTPS://person@example.com/account"
        )) {
            assertEquals("Malformed explicit URL should not enter Startpage search", "",
                NavigationPolicy.resolve(input))
            assertEquals("", NavigationPolicy.resolve(input, cleanLinks = false))
        }
        // Ordinary user-entered search queries remain supported.
        assertEquals("https://www.startpage.com/do/search?query=privacy+first",
            NavigationPolicy.resolve("privacy first"))
        assertEquals("https://example.com/path",
            NavigationPolicy.resolve("https://example.com/path"))
    }

    @Test fun disallowsWebviewDangerousSchemes() {
        assertFalse(NavigationPolicy.isHttpUrl("javascript:alert(1)"))
        assertFalse(NavigationPolicy.isHttpUrl("file:///etc/passwd"))
        assertFalse(NavigationPolicy.isHttpUrl("https://person@example.com"))
    }

    @Test fun blockerUsesHostBoundaries() {
        assertTrue(RequestBlocker.shouldBlock("https://ads.doubleclick.net/script.js"))
        assertFalse(RequestBlocker.shouldBlock("https://doubleclick.net.evil.test/pixel"))
        assertFalse(RequestBlocker.shouldBlock("https://example.org/"))
    }
}
