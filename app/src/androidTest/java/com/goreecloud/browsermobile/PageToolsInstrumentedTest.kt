package com.goreecloud.browsermobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import com.goreecloud.browsermobile.core.PageActionPolicy
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real WebView/Compose page-tool smoke checks, never normal browser data.
 * Uses a disposable .qa package and synthetic local HTML with no remote page
 * contents, searches, cookies or credentials.
 */
@RunWith(AndroidJUnit4::class)
class PageToolsInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activity: Activity? = null
    private var website: WebView? = null

    @Before
    fun launchWithSyntheticPage() {
        val target = instrumentation.targetContext
        require(target.packageName.endsWith(".qa")) {
            "Page-tools smoke must run on a disposable .qa app, not the normal browser"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.goreecloud.com/")).apply {
            setClass(target, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
        val view = awaitWebView()
            ?: throw AssertionError("WebView was not attached to the QA Activity")
        website = view
        instrumentation.runOnMainSync {
            view.loadDataWithBaseURL(
                "https://page-tools.example.test/",
                "<!doctype html><html><head><meta name='viewport' content='width=device-width'>" +
                    "<title>QA Page Tools</title></head><body><h1>orbitmarker</h1>" +
                    "<p>orbitmarker</p><p>orbitmarker</p></body></html>",
                "text/html", "UTF-8", null
            )
        }
        assertTrue("Synthetic page title was not loaded", awaitPageTitle(view, "QA Page Tools"))
        assertTrue("Floating menu must remain visible", awaitNode("More options") != null)
    }

    @After
    fun finishApp() {
        activity?.let { running ->
            instrumentation.runOnMainSync { running.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun textSizeIncreasesDecreasesAndResetsTheCurrentWebView() {
        val view = website ?: throw AssertionError("No QA WebView")
        assertEquals(100, readTextZoom(view))
        clickLabel("More options")
        clickLabel("Text size")
        assertTrue("Text-size sheet absent", awaitNode("Text size") != null)
        clickLabel("Larger")
        assertEquals("Larger should set WebView textZoom", 120, readTextZoom(view))
        clickLabel("Smaller")
        assertEquals("Smaller should restore default", 100, readTextZoom(view))
        clickLabel("Smaller")
        assertEquals("Text can be reduced to 80%", 80, readTextZoom(view))
        clickLabel("Reset text size to 100%")
        assertEquals("Reset must restore WebView textZoom", 100, readTextZoom(view))
    }

    @Test
    fun findInPageReportsMatchesNavigatesAndClosesWithoutSearchProvider() {
        clickLabel("More options")
        clickLabel("Find in page")
        assertTrue("Find controls must be shown", awaitNode("Close find in page") != null)

        val editable = awaitFocusedEditable()
            ?: throw AssertionError("Find field did not gain accessible input focus")
        val setText = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                "orbitmarker"
            )
        }
        assertTrue("Find input refused accessibility text entry",
            editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setText))
        assertTrue("Three synthetic matches not reported",
            awaitNode("1 of 3 matches", timeoutMs = 8_000L) != null)

        clickLabel("Next match")
        assertTrue("Next action did not advance the active WebView match",
            awaitNode("2 of 3 matches") != null)
        clickLabel("Previous match")
        assertTrue("Previous action did not return to the first match",
            awaitNode("1 of 3 matches") != null)

        clickLabel("Close find in page")
        assertTrue("Find interface remained after Close",
            awaitAbsent("Close find in page"))
        assertTrue("Address bar should remain available after closing search",
            awaitNode("Go to address") != null)
    }

    @Test
    fun desktopSiteUsesWebViewChromeVersionAndRestoresMobileDefaults() {
        val view = website ?: throw AssertionError("No QA WebView")
        var originalUserAgent = ""
        var originalWide = false
        var originalOverview = false
        instrumentation.runOnMainSync {
            originalUserAgent = view.settings.userAgentString
            originalWide = view.settings.useWideViewPort
            originalOverview = view.settings.loadWithOverviewMode
        }
        val expectedDesktop = PageActionPolicy.desktopUserAgent(originalUserAgent)
        assertNotNull("QA WebView should advertise a Chrome version", expectedDesktop)

        clickLabel("More options")
        clickLabel("Desktop site")
        assertTrue("Desktop request did not set WebView user agent",
            awaitUserAgent(view, expectedDesktop!!))
        instrumentation.runOnMainSync {
            assertTrue("Desktop request must enable wide viewport", view.settings.useWideViewPort)
            assertTrue("Desktop request must enable overview mode", view.settings.loadWithOverviewMode)
        }
        clickLabel("More options")
        assertTrue("Menu must expose the reversible mobile-site action",
            awaitNode("Mobile site") != null)
        clickLabel("Mobile site")
        assertTrue("Restoring mobile must restore the actual OEM user agent",
            awaitUserAgent(view, originalUserAgent))
        instrumentation.runOnMainSync {
            assertEquals("Must restore original OEM viewport", originalWide, view.settings.useWideViewPort)
            assertEquals("Must restore original overview mode", originalOverview, view.settings.loadWithOverviewMode)
        }
    }

    @Test
    fun cleanLinkActionIsAccessibleWithoutReadingOrOverwritingPersonalClipboard() {
        clickLabel("More options")
        assertTrue("Webpage menu must offer an explicit copy action",
            awaitNode("Copy clean link") != null)
        // No click: this smoke verifies the affordance, not the user's personal
        // OS clipboard. Deterministic clean-link behavior is JVM-tested.
    }

    private fun awaitUserAgent(view: WebView, expected: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 6_000L
        do {
            var value = ""
            instrumentation.runOnMainSync { value = view.settings.userAgentString }
            if (value == expected) return true
            Thread.sleep(100L)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    private fun readTextZoom(view: WebView): Int {
        var zoom = -1
        instrumentation.runOnMainSync { zoom = view.settings.textZoom }
        return zoom
    }

    private fun awaitPageTitle(view: WebView, expected: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 8_000L
        do {
            var title: String? = null
            instrumentation.runOnMainSync { title = view.title }
            if (title == expected) return true
            Thread.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    private fun awaitWebView(): WebView? {
        val deadline = SystemClock.uptimeMillis() + 8_000L
        do {
            var result: WebView? = null
            instrumentation.runOnMainSync {
                result = activity?.window?.decorView?.let(::findWebView)
            }
            if (result != null) return result
            Thread.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        return null
    }

    private fun findWebView(root: View): WebView? {
        if (root is WebView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                root.getChildAt(i)?.let { findWebView(it)?.let { v -> return v } }
            }
        }
        return null
    }

    private fun clickLabel(label: String) {
        var node = awaitNode(label)
            ?: throw AssertionError("Control not present: $label")
        while (!node.isClickable) {
            node = node.parent ?: throw AssertionError("No clickable ancestor for $label")
        }
        assertTrue("Accessibility click failed: $label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun awaitNode(label: String, timeoutMs: Long = 5_000L): AccessibilityNodeInfo? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            instrumentation.uiAutomation.rootInActiveWindow?.let {
                findNode(it, label)?.let { found -> return found }
            }
            Thread.sleep(120)
        } while (SystemClock.uptimeMillis() < deadline)
        return null
    }

    private fun awaitAbsent(label: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 4_000L
        do {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null && findNode(root, label) == null) return true
            Thread.sleep(120)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    private fun awaitFocusedEditable(): AccessibilityNodeInfo? {
        val deadline = SystemClock.uptimeMillis() + 4_000L
        do {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val focused = root?.let(::findFocusedEditable)
            if (focused != null) return focused
            Thread.sleep(120)
        } while (SystemClock.uptimeMillis() < deadline)
        return null
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findFocusedEditable(it)?.let { found -> return found } }
        }
        return null
    }

    private fun findNode(node: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        if (node.text?.toString() == label || node.contentDescription?.toString() == label) {
            return node
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNode(it, label)?.let { found -> return found } }
        }
        return null
    }
}
