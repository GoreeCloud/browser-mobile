package com.goreecloud.browsermobile.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserChromeScrollPolicyTest {
    @Test fun `upward finger travel hides after threshold`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 40)
        policy.beginGesture(200f)
        assertTrue(policy.visibleAfterMove(180f, 100L, true))
        assertTrue(policy.visibleAfterMove(161f, 110L, true))
        assertFalse(policy.visibleAfterMove(159f, 120L, true))
    }

    @Test fun `downward finger travel restores after threshold`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 30)
        policy.beginGesture(100f)
        assertFalse(policy.visibleAfterMove(120f, 1000L, false))
        assertTrue(policy.visibleAfterMove(132f, 1010L, false))
    }

    @Test fun `layout-originated changes cannot trigger visibility`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 24)
        // No gesture has begun. WebView may relayout, but no pointer is moving.
        assertTrue(policy.visibleAfterMove(-500f, 0L, true))
        assertFalse(policy.visibleAfterMove(500f, 100L, false))
    }

    @Test fun `tiny jitter does not hide controls`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 25)
        policy.beginGesture(100f)
        assertTrue(policy.visibleAfterMove(92f, 10L, true))
        assertTrue(policy.visibleAfterMove(98f, 20L, true))
        assertTrue(policy.visibleAfterMove(91f, 30L, true))
        assertTrue(policy.visibleAfterMove(93f, 40L, true))
    }

    @Test fun `direction reversal resets accumulated distance`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 30)
        policy.beginGesture(100f)
        assertTrue(policy.visibleAfterMove(79f, 10L, true))
        assertTrue(policy.visibleAfterMove(84f, 20L, true))
        assertTrue(policy.visibleAfterMove(65f, 30L, true))
        assertFalse(policy.visibleAfterMove(53f, 40L, true))
    }

    @Test fun `transition cooldown blocks immediate reverse flicker`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 20, minTransitionMs = 250L)
        policy.beginGesture(100f)
        assertFalse(policy.visibleAfterMove(70f, 1000L, true))
        assertFalse(policy.visibleAfterMove(95f, 1050L, false))
        assertFalse(policy.visibleAfterMove(105f, 1130L, false))
        assertTrue(policy.visibleAfterMove(120f, 1251L, false))
    }

    @Test fun `continued same-direction swipe does not oscillate`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 22)
        policy.beginGesture(300f)
        assertFalse(policy.visibleAfterMove(270f, 100L, true))
        assertFalse(policy.visibleAfterMove(245f, 120L, false))
        assertFalse(policy.visibleAfterMove(220f, 140L, false))
    }

    @Test fun `endGesture stops responding to old touch events`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 20)
        policy.beginGesture(200f)
        policy.endGesture()
        assertTrue(policy.visibleAfterMove(120f, 100L, true))
        policy.beginGesture(160f)
        assertTrue(policy.visibleAfterMove(150f, 110L, true))
        assertFalse(policy.visibleAfterMove(130f, 120L, true))
    }

    @Test fun `new gesture does not inherit old partial distance`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 22)
        policy.beginGesture(200f)
        assertTrue(policy.visibleAfterMove(185f, 100L, true))
        policy.endGesture()
        policy.beginGesture(100f)
        assertTrue(policy.visibleAfterMove(90f, 200L, true))
        assertFalse(policy.visibleAfterMove(77f, 300L, true))
    }

    @Test fun `reset clears gesture and cooldown on explicit navigation`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 15, minTransitionMs = 300L)
        policy.beginGesture(100f)
        assertFalse(policy.visibleAfterMove(80f, 100L, true))
        policy.reset()
        assertFalse(policy.visibleAfterMove(120f, 110L, false))
        policy.beginGesture(120f)
        assertTrue(policy.visibleAfterMove(140f, 115L, false))
    }

    @Test fun `stationary pointer preserves visibility`() {
        val policy = BrowserChromeScrollPolicy(thresholdPx = 15)
        policy.beginGesture(100f)
        assertTrue(policy.visibleAfterMove(100f, 100L, true))
        assertFalse(policy.visibleAfterMove(100f, 200L, false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `threshold must be positive`() {
        BrowserChromeScrollPolicy(thresholdPx = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `transition cooldown must be nonnegative`() {
        BrowserChromeScrollPolicy(minTransitionMs = -1L)
    }
}
