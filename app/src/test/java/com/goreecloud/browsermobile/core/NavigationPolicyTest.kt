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

    @Test fun keepsUsefulParamsAndFragments() {
        val result = UrlCleaner.clean("https://example.com/?utm_source=mail&q=cat&fbclid=abc#part")
        assertEquals("https://example.com/?q=cat#part", result.url)
        assertEquals(2, result.removedParameters)
    }

    @Test fun doesNotAlterUntargetedLinks() {
        val url = "https://example.com/?q=cat&ref=help"
        assertEquals(url, UrlCleaner.clean(url).url)
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
