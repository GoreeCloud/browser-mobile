package com.goreecloud.browsermobile.core

/**
 * Conservative scroll-direction policy for native Android WebView browser chrome.
 *
 * Events are sourced from WebView's native scroll listener. Direction reversals
 * reset the accumulated distance, so small scroll jitter does not flicker the UI.
 * This class has no Android or Compose dependencies and makes no page network calls.
 */
class BrowserChromeScrollPolicy(private val thresholdPx: Int = 24) {
    init {
        require(thresholdPx > 0) { "Scroll threshold must be positive" }
    }

    private var distance = 0

    fun reset() {
        distance = 0
    }

    fun visibleAfterScroll(newY: Int, oldY: Int, currentlyVisible: Boolean): Boolean {
        if (newY <= 0) {
            reset()
            return true
        }
        val step = newY - oldY
        if (step == 0) return currentlyVisible

        if ((step > 0 && distance < 0) || (step < 0 && distance > 0)) {
            distance = 0
        }
        distance = (distance + step).coerceIn(-thresholdPx, thresholdPx)

        return when {
            distance >= thresholdPx -> false
            distance <= -thresholdPx -> true
            else -> currentlyVisible
        }
    }
}
