package com.goreecloud.browsermobile.core

/**
 * Browser chrome visibility driven by the user's actual vertical finger motion.
 *
 * Using WebView's reported scroll position to hide its own toolbar was unstable:
 * the toolbar changes WebView height, which can produce synthetic scroll-position
 * changes in the opposite direction. Finger coordinates remain independent of
 * viewport resizing and also support websites with nested scrolling containers.
 *
 * Pointer events are observed (never consumed) by WebView; no page scripts,
 * tracking, permissions, network requests, or persisted user data are involved.
 */
class BrowserChromeScrollPolicy(
    private val thresholdPx: Int = 96,
    private val minTransitionMs: Long = 260L
) {
    init {
        require(thresholdPx > 0) { "Gesture threshold must be positive" }
        require(minTransitionMs >= 0) { "Transition interval cannot be negative" }
    }

    private var lastY: Float? = null
    private var accumulated: Float = 0f
    private var lastTransitionAtMs: Long? = null

    /** Reset when changing tabs, navigating or explicitly revealing controls. */
    fun reset() {
        endGesture()
        lastTransitionAtMs = null
    }

    fun beginGesture(y: Float) {
        lastY = y
        accumulated = 0f
        // Cooldown applies only within a continuous touch gesture. Carrying it
        // across separate swipes can ignore a quick, intentional reverse swipe.
        lastTransitionAtMs = null
    }

    /** Never hide or reveal unless a single-finger gesture is in progress. */
    fun visibleAfterMove(y: Float, eventTimeMs: Long, visible: Boolean): Boolean {
        val previous = lastY ?: return visible
        lastY = y
        val delta = y - previous
        if (delta == 0f) return visible

        // Restart the travel count on direction reversals to reject jitter.
        if ((delta > 0f && accumulated < 0f) || (delta < 0f && accumulated > 0f)) {
            accumulated = 0f
        }
        accumulated = (accumulated + delta).coerceIn(-thresholdPx.toFloat(), thresholdPx.toFloat())

        val targetVisible = when {
            accumulated <= -thresholdPx -> false // finger moves up: page scrolls down
            accumulated >= thresholdPx -> true   // finger moves down: page scrolls up
            else -> visible
        }
        if (targetVisible == visible) return visible

        val last = lastTransitionAtMs
        if (last != null && eventTimeMs >= last && eventTimeMs - last < minTransitionMs) {
            return visible
        }
        accumulated = 0f
        lastTransitionAtMs = eventTimeMs
        return targetVisible
    }

    fun endGesture() {
        lastY = null
        accumulated = 0f
    }
}
