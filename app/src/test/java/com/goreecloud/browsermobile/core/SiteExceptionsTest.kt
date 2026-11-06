package com.goreecloud.browsermobile.core

import org.junit.Assert.*
import org.junit.Test

class SiteExceptionsTest {
    @Test fun onlyHttpPagesCanBeExempted() {
        assertNull(SiteExceptions.hostForPage("file:///tmp/ads.html"))
        assertNull(SiteExceptions.hostForPage("javascript:alert(1)"))
        assertNull(SiteExceptions.hostForPage("about:blank"))
    }

    @Test fun siteIdentityIgnoresPortPathAndScheme() {
        assertEquals("news.example.com", SiteExceptions.hostForPage("HTTPS://NEWS.EXAMPLE.COM:8443/a?q=x"))
        assertEquals("news.example.com", SiteExceptions.hostForPage("http://news.example.com/"))
    }

    @Test fun unrelatedAndLookalikeSitesAreNotExempted() {
        val allowed = setOf("example.com")
        assertTrue(SiteExceptions.isAllowed(allowed, "https://example.com/article"))
        assertFalse(SiteExceptions.isAllowed(allowed, "https://ads.example.com/"))
        assertFalse(SiteExceptions.isAllowed(allowed, "https://example.com.evil.test/"))
        assertFalse(SiteExceptions.isAllowed(allowed, "https://another.example.com/"))
    }

    @Test fun refusesAmbiguousCredentialsAndLocalhost() {
        assertNull(SiteExceptions.hostForPage("https://user@example.com/"))
        assertNull(SiteExceptions.hostForPage("https://localhost:8080"))
        assertNull(SiteExceptions.hostForPage("https://example"))
    }
}
