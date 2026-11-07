package com.goreecloud.browsermobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Android/WebView/Compose smoke checks. Run against a separate QA package
 * on an authorized test device: never execute on a user's data-bearing install.
 *
 * These checks cover accessibility-visible chrome transitions, not screenshot
 * pixel parity, animation frame timing, TalkBack or every website's scroll model.
 */
@RunWith(AndroidJUnit4::class)
class ChromeGestureInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activity: Activity? = null

    @Before
    fun launchBrowser() {
        val target = instrumentation.targetContext
        require(target.packageName.endsWith(".qa")) {
            "Device smoke tests require an isolated .qa application ID, not the normal browser"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.goreecloud.com/")).apply {
            setClass(target, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
        assertTrue("Browser chrome and WebView must load", awaitChrome(visible = true, timeoutMs = 12_000))
    }

    @After
    fun finishBrowser() {
        val running = activity
        if (running != null) {
            instrumentation.runOnMainSync { running.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun repeatedDownwardAndUpwardPageSwipesKeepBothBarsInSync() {
        repeat(3) { cycle ->
            swipe(upward = true)
            assertTrue("Cycle ${cycle + 1}: downward page scroll should hide both bars",
                awaitChrome(visible = false))
            swipe(upward = false)
            assertTrue("Cycle ${cycle + 1}: upward page scroll should reveal both bars",
                awaitChrome(visible = true))
        }
    }

    @Test
    fun rapidSeparateOppositeSwipesHideThenRevealBothBars() {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val high = metrics.heightPixels * 0.32f
        val low = metrics.heightPixels * 0.74f

        // Two short-duration but deliberate separate finger gestures.
        sendSwipe(x, low, high, steps = 6, pauseMs = 6L)
        assertTrue("Fast upward swipe must hide both bars",
            awaitChrome(visible = false, timeoutMs = 2_000))
        sendSwipe(x, high, low, steps = 6, pauseMs = 6L)
        assertTrue("Next deliberate reverse gesture must reveal both bars",
            awaitChrome(visible = true, timeoutMs = 2_000))
    }

    @Test
    fun shortVerticalMovementDoesNotHideChrome() {
        val display = instrumentation.targetContext.resources.displayMetrics
        val x = display.widthPixels * 0.5f
        val startY = display.heightPixels * 0.56f
        sendSwipe(x, startY, startY - 32f)
        assertTrue("Small accidental movement must leave chrome visible",
            awaitChrome(visible = true, timeoutMs = 2_000))
    }

    private fun swipe(upward: Boolean) {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val high = metrics.heightPixels * 0.32f
        val low = metrics.heightPixels * 0.74f
        sendSwipe(x, if (upward) low else high, if (upward) high else low)
    }

    private fun sendSwipe(
        x: Float, startY: Float, endY: Float,
        steps: Int = 12, pauseMs: Long = 24L
    ) {
        require(steps >= 2 && pauseMs >= 0L)
        val downTime = SystemClock.uptimeMillis()
        for (step in 0..steps) {
            val fraction = step / steps.toFloat()
            val action = when (step) {
                0 -> MotionEvent.ACTION_DOWN
                steps -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val event = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), action,
                x, startY + (endY - startY) * fraction, 0
            )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
            Thread.sleep(pauseMs)
        }
        instrumentation.waitForIdleSync()
    }

    private fun awaitChrome(visible: Boolean, timeoutMs: Long = 4_000L): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            Thread.sleep(120L)
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null) {
                val webView = hasClass(root, "android.webkit.WebView")
                val go = hasDescription(root, "Go to address")
                val home = hasDescription(root, "Home")
                if (webView && go == visible && home == visible) return true
            }
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    private fun hasDescription(node: AccessibilityNodeInfo, label: String): Boolean {
        if (node.contentDescription?.toString() == label) return true
        for (i in 0 until node.childCount) {
            if (node.getChild(i)?.let { hasDescription(it, label) } == true) return true
        }
        return false
    }

    private fun hasClass(node: AccessibilityNodeInfo, className: String): Boolean {
        if (node.className?.toString() == className) return true
        for (i in 0 until node.childCount) {
            if (node.getChild(i)?.let { hasClass(it, className) } == true) return true
        }
        return false
    }
}
