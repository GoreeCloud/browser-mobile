package com.goreecloud.browsermobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.webkit.CookieManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.browsermobile.core.BrowsingLibrary
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs only against a disposable .qa application ID.
 *
 * Uses a synthetic local website host; tests cancel/dismiss behavior without
 * erasing any actual browsing data. Never run against the normal app package.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyConfirmationInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activity: Activity? = null
    private val syntheticHost = "confirmation-smoke.example.test"

    @Before
    fun launchIsolatedApp() {
        val context = instrumentation.targetContext
        require(context.packageName.endsWith(".qa")) {
            "Privacy instrumentation requires isolated .qa package, never a normal browser"
        }
        BrowsingLibrary(context).apply {
            updateHistoryEnabled(true)
            clear()
            record("https://$syntheticHost/")
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.goreecloud.com/")).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
        assertTrue("Floating More options button did not appear", awaitNode("More options") != null)
    }

    @After
    fun tearDownActivity() {
        activity?.let { running ->
            instrumentation.runOnMainSync { running.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun cancelHistoryClearPreservesSyntheticLocalHistory() {
        clickLabel("More options")
        clickLabel("Privacy & DNS")
        assertTrue("Privacy center did not open", awaitNode("Privacy center") != null)

        clickPrivacyAction("Clear recent websites and favicons")
        assertTrue("Destructive history action must show a confirmation",
            awaitNode("Clear recent websites?") != null)
        assertTrue("Confirmation must offer a cancel action", awaitNode("Cancel") != null)

        clickLabel("Cancel")
        assertTrue("Dialog should dismiss on Cancel", awaitAbsent("Clear recent websites?"))
        assertTrue("Canceled deletion must preserve the seeded local site",
            BrowsingLibrary(instrumentation.targetContext).sites.any {
                it.host == syntheticHost
            })
    }

    @Test
    fun cancelCookieClearClosesDialogWithoutProceeding() {
        val testUrl = "https://$syntheticHost/"
        val cookies = CookieManager.getInstance()
        instrumentation.runOnMainSync {
            cookies.setAcceptCookie(true)
            cookies.setCookie(testUrl, "qa_cancel=preserved; Path=/; Secure; SameSite=Lax")
            cookies.flush()
        }
        assertTrue("Synthetic QA cookie must exist before cancellation test",
            cookies.getCookie(testUrl)?.contains("qa_cancel=preserved") == true)
        clickLabel("More options")
        clickLabel("Privacy & DNS")
        assertTrue("Privacy center did not open", awaitNode("Privacy center") != null)

        clickPrivacyAction("Clear all cookies and web storage")
        assertTrue("Cookie deletion must require confirmation",
            awaitNode("Clear cookies and site data?") != null)
        assertTrue("User must be offered a non-destructive exit",
            awaitNode("Cancel") != null)
        clickLabel("Cancel")

        assertTrue("Cookie confirmation should dismiss without deletion",
            awaitAbsent("Clear cookies and site data?"))
        assertTrue("Browser data-clear action should remain available in the sheet",
            awaitNode("Clear all cookies and web storage") != null)
        assertTrue("Cancel must retain the original synthetic QA cookie",
            cookies.getCookie(testUrl)?.contains("qa_cancel=preserved") == true)
    }

    /** The Privacy sheet starts partially expanded and may need real swipe gestures. */
    private fun clickPrivacyAction(label: String) {
        repeat(12) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null && findNode(root, label) != null) {
                clickLabel(label)
                return
            }
            swipePrivacySheetUp()
        }
        throw AssertionError("Privacy control remained off-screen: $label")
    }

    /** WebView is covered by the modal sheet; this gesture scrolls QA sheet content. */
    private fun swipePrivacySheetUp() {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val firstY = metrics.heightPixels * 0.85f
        val lastY = metrics.heightPixels * 0.61f
        val downAt = SystemClock.uptimeMillis()
        for (step in 0..14) {
            val type = when (step) {
                0 -> MotionEvent.ACTION_DOWN
                14 -> MotionEvent.ACTION_UP
                else -> MotionEvent.ACTION_MOVE
            }
            val event = MotionEvent.obtain(
                downAt, SystemClock.uptimeMillis(), type,
                x, firstY + (lastY - firstY) * (step / 14f), 0
            )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                instrumentation.sendPointerSync(event)
            } finally {
                event.recycle()
            }
            Thread.sleep(22L)
        }
        instrumentation.waitForIdleSync()
    }

    private fun clickLabel(label: String) {
        var node = awaitNode(label)
            ?: throw AssertionError("Control not present: $label")
        while (!node.isClickable) {
            node = node.parent ?: throw AssertionError("Control has no clickable ancestor: $label")
        }
        assertTrue("Accessibility click failed: $label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun awaitNode(label: String, timeoutMs: Long = 6_000L): AccessibilityNodeInfo? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                findNode(root, label)?.let { return it }
            }
            Thread.sleep(100L)
        } while (SystemClock.uptimeMillis() < deadline)
        return null
    }

    private fun awaitAbsent(label: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 3_000L
        do {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null && findNode(root, label) == null) return true
            Thread.sleep(100L)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    private fun findNode(node: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        if (node.text?.toString() == label || node.contentDescription?.toString() == label) {
            return node
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNode(child, label)?.let { return it }
            }
        }
        return null
    }
}
