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
import com.goreecloud.browsermobile.core.WebspacesManager
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
    private val syntheticSpaceName = "QA Review Space"
    private var syntheticSpaceId: String? = null

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
        // Seed only inside the disposable QA package before the Activity loads its manager.
        WebspacesManager(context).apply {
            custom.filter { it.name == syntheticSpaceName }.map { it.id }.forEach(::remove)
            val seeded = create(syntheticSpaceName)
                ?: throw AssertionError("Failed to create the synthetic QA Webspace")
            syntheticSpaceId = seeded.id
            assign("https://$syntheticHost/", seeded.id)
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
        // This only removes data seeded in the disposable .qa application.
        syntheticSpaceId?.let { WebspacesManager(instrumentation.targetContext).remove(it) }
        syntheticSpaceId = null
    }

    @Test
    fun cancelHistoryClearPreservesSyntheticLocalHistory() {
        clickLabel("More options")
        clickLabel("Privacy & DNS")
        assertTrue("Privacy center did not open", awaitNode("Privacy center") != null)

        clickSheetAction("Clear recent websites and favicons")
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

        clickSheetAction("Clear all cookies and web storage")
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

    @Test
    fun cancelTurningOffHistoryKeepsOptInAndSyntheticHost() {
        clickLabel("More options")
        clickLabel("Privacy & DNS")
        assertTrue("Privacy center did not open", awaitNode("Privacy center") != null)

        clickHistorySwitch()
        assertTrue("Disabling history must require confirmation",
            awaitNode("Turn off visited websites?") != null)
        assertTrue("Confirmation must provide a non-destructive Cancel",
            awaitNode("Cancel") != null)
        clickLabel("Cancel")

        assertTrue("Disable-history dialog should close on Cancel",
            awaitAbsent("Turn off visited websites?"))
        val library = BrowsingLibrary(instrumentation.targetContext)
        assertTrue("Cancel must preserve the enabled history setting", library.historyEnabled)
        assertTrue("Cancel must retain the synthetic saved host",
            library.sites.any { it.host == syntheticHost })
    }

    @Test
    fun cancelCustomWebspaceRemovalPreservesSpaceAndRouting() {
        val spaceId = syntheticSpaceId
            ?: throw AssertionError("Synthetic QA Webspace was not seeded")
        clickLabel("More options")
        clickLabel("Webspaces")
        assertTrue("Webspaces panel did not open", awaitNode("Webspaces") != null)

        clickSheetAction("Remove $syntheticSpaceName Webspace")
        assertTrue("Webspace removal must require confirmation",
            awaitNode("Remove $syntheticSpaceName?") != null)
        assertTrue("Webspace removal needs a Cancel action", awaitNode("Cancel") != null)
        clickLabel("Cancel")

        assertTrue("Webspace confirmation should close on Cancel",
            awaitAbsent("Remove $syntheticSpaceName?"))
        val manager = WebspacesManager(instrumentation.targetContext)
        assertTrue("Cancel must preserve the synthetic Webspace",
            manager.custom.any { it.id == spaceId && it.name == syntheticSpaceName })
        assertTrue("Cancel must preserve its synthetic exact-host route",
            manager.assignments[syntheticHost] == spaceId)
    }

    private fun clickHistorySwitch() {
        repeat(12) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val label = root?.let { findNode(it, "Remember visited websites") }
            if (root != null && label != null) {
                val labelBounds = android.graphics.Rect()
                label.getBoundsInScreen(labelBounds)
                val switches = mutableListOf<AccessibilityNodeInfo>()
                collectCheckableControls(root, switches)
                // Compose's text and switch can be different semantics subtrees.
                // Match their screen row, never select the first switch in the sheet.
                val match = switches.mapNotNull { control ->
                    val bounds = android.graphics.Rect()
                    control.getBoundsInScreen(bounds)
                    val offset = kotlin.math.abs(bounds.centerY() - labelBounds.centerY())
                    if (bounds.left > labelBounds.right && offset <= 120) {
                        control to offset
                    } else null
                }.minByOrNull { it.second }?.first
                if (match != null) {
                    assertTrue("History switch must be enabled before cancellation", match.isChecked)
                    assertTrue("History switch did not accept accessibility click",
                        match.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                    instrumentation.waitForIdleSync()
                    return
                }
            }
            swipePrivacySheetUp()
        }
        throw AssertionError("Could not locate the switch beside the history setting")
    }

    private fun collectCheckableControls(
        node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isCheckable && node.isClickable) result.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectCheckableControls(it, result) }
        }
    }

    /** A bottom sheet starts partially expanded and may need real swipe gestures. */
    private fun clickSheetAction(label: String) {
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

    /** Drag the active modal sheet without touching the underlying WebView. */
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
