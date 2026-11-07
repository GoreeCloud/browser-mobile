package com.goreecloud.browsermobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageActionPolicyTest {
    private val androidUa =
        "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/132.0.6834.80 Mobile Safari/537.36"

    @Test fun `desktop user agent uses installed WebView browser version`() {
        assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/132.0.6834.80 Safari/537.36",
            PageActionPolicy.desktopUserAgent(androidUa)
        )
    }

    @Test fun `unsupported user agent refuses arbitrary desktop substitution`() {
        assertNull(PageActionPolicy.desktopUserAgent(""))
        assertNull(PageActionPolicy.desktopUserAgent("SomeBrowser/1.0"))
        assertNull(PageActionPolicy.desktopUserAgent("Chrome/not-a-version"))
        assertNull(PageActionPolicy.desktopUserAgent("Chrome/123.0.0.0extra"))
    }

    @Test fun `desktop user agent does not reuse Android model and Mobile token`() {
        val userAgent = PageActionPolicy.desktopUserAgent(androidUa)!!
        assertFalse(userAgent.contains("Pixel"))
        assertFalse(userAgent.contains("Android"))
        assertFalse(userAgent.contains(" Mobile "))
        assertTrue(userAgent.contains("Chrome/132.0.6834.80"))
    }

    @Test fun `clean copy removes only known tracking parameters`() {
        val input = "https://example.test/articles/7?id=42&utm_source=mail&fbclid=trace&view=full#anchor"
        assertEquals(
            "https://example.test/articles/7?id=42&view=full#anchor",
            PageActionPolicy.cleanCopyableLink(input)
        )
    }

    @Test fun `clean copy preserves valid links with no tracking keys`() {
        val url = "https://example.test/search?q=hello%20world&lang=fr#top"
        assertEquals(url, PageActionPolicy.cleanCopyableLink(url))
    }

    @Test fun `nonweb or malformed links cannot reach Android clipboard`() {
        for (url in listOf("javascript:alert(1)", "file:///private", "about:blank",
                "https://user:password@example.test/", "https://example.test/\nInjection")) {
            assertNull("Unsafe clipboard URI: $url", PageActionPolicy.cleanCopyableLink(url))
        }
    }
}
