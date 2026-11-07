package com.goreecloud.browsermobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabActionPolicyTest {
    @Test fun `eligible HTTPS pages may be duplicated until tab limit`() {
        assertTrue(TabActionPolicy.canDuplicate("https://example.test/p?token=a#part", 1))
        assertTrue(TabActionPolicy.canDuplicate("https://example.test/", 11))
        assertFalse(TabActionPolicy.canDuplicate("https://example.test/", 12))
        assertFalse(TabActionPolicy.canDuplicate("https://example.test/", 0))
    }

    @Test fun `cannot duplicate unsafe or nonweb content`() {
        for (url in listOf("", "about:blank", "file:///a", "javascript:alert(1)",
                "http://example.test/", "https://user:pass@example.test/",
                "https://example.test/\nInjected", "https://example.test/" + "x".repeat(3000))) {
            assertFalse("Unsafe page duplicated: $url", TabActionPolicy.canDuplicate(url, 2))
        }
    }

    @Test fun `snapshot captures URL and bounded labels without page or WebView data`() {
        val snapshot = TabActionPolicy.snapshot(
            "https://example.test/article?p=1#top",
            "A".repeat(200),
            "custom-space",
            true,
            closedAtElapsedMs = 1000L
        )
        assertNotNull(snapshot)
        assertEquals("https://example.test/article?p=1#top", snapshot!!.url)
        assertEquals(100, snapshot.title.length)
        assertEquals("custom-space", snapshot.webspaceId)
        assertTrue(snapshot.pinnedWebspace)
    }

    @Test fun `snapshot refuses nonweb pages and invalid elapsed clock`() {
        assertNull(TabActionPolicy.snapshot("about:blank", "New Tab", "standard", false, 10L))
        assertNull(TabActionPolicy.snapshot("http://example.test", "Site", "standard", false, 10L))
        assertNull(TabActionPolicy.snapshot("https://example.test", "Site", "standard", false, -1L))
    }

    @Test fun `one page can be reopened for at most thirty seconds`() {
        val s = TabActionPolicy.snapshot("https://example.test", "Site", "standard", false, 1000L)
        assertTrue(TabActionPolicy.canReopen(s, 1000L, 1))
        assertTrue(TabActionPolicy.canReopen(s, 31_000L, 1))
        assertFalse(TabActionPolicy.canReopen(s, 31_001L, 1))
        assertFalse(TabActionPolicy.canReopen(s, 999L, 1))
    }

    @Test fun `recovery fails closed when tab limit reached or no snapshot`() {
        val s = TabActionPolicy.snapshot("https://example.test", "Site", "standard", false, 1000L)
        assertFalse(TabActionPolicy.canReopen(s, 1010L, 12))
        assertFalse(TabActionPolicy.canReopen(s, 1010L, 0))
        assertFalse(TabActionPolicy.canReopen(null, 1010L, 1))
    }

    @Test fun `blank and oversized titles or webspace labels are bounded`() {
        val s = TabActionPolicy.snapshot(
            "https://example.test/", "", "W".repeat(200), false, 5L
        )
        assertEquals("Website", s!!.title)
        assertEquals(64, s.webspaceId.length)
    }
}
