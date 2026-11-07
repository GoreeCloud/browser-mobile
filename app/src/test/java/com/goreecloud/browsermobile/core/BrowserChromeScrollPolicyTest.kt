package com.goreecloud.browsermobile.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserChromeScrollPolicyTest {
    @Test fun `downward scrolling hides after threshold`() {
        val policy = BrowserChromeScrollPolicy(20)
        assertTrue(policy.visibleAfterScroll(13, 5, true))
        assertTrue(policy.visibleAfterScroll(20, 13, true))
        assertFalse(policy.visibleAfterScroll(26, 20, true))
    }

    @Test fun `upward scrolling reveals after threshold`() {
        val policy = BrowserChromeScrollPolicy(18)
        assertFalse(policy.visibleAfterScroll(100, 92, false))
        assertFalse(policy.visibleAfterScroll(94, 100, false))
        assertTrue(policy.visibleAfterScroll(81, 94, false))
    }

    @Test fun `small jitter and direction change do not flicker`() {
        val policy = BrowserChromeScrollPolicy(20)
        assertTrue(policy.visibleAfterScroll(30, 20, true))
        assertTrue(policy.visibleAfterScroll(27, 30, true))
        assertTrue(policy.visibleAfterScroll(36, 27, true))
        assertFalse(policy.visibleAfterScroll(48, 36, true))
    }

    @Test fun `scrolling to page top restores chrome immediately`() {
        val policy = BrowserChromeScrollPolicy()
        assertFalse(policy.visibleAfterScroll(100, 50, true))
        assertTrue(policy.visibleAfterScroll(0, 100, false))
    }

    @Test fun `reset clears previous accumulated distance`() {
        val policy = BrowserChromeScrollPolicy(20)
        assertTrue(policy.visibleAfterScroll(10, 2, true))
        policy.reset()
        assertTrue(policy.visibleAfterScroll(24, 16, true))
        assertFalse(policy.visibleAfterScroll(38, 24, true))
    }

    @Test fun `stationary events preserve current visibility`() {
        val policy = BrowserChromeScrollPolicy()
        assertFalse(policy.visibleAfterScroll(500, 500, false))
        assertTrue(policy.visibleAfterScroll(500, 500, true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `threshold must be positive`() {
        BrowserChromeScrollPolicy(0)
    }
}
