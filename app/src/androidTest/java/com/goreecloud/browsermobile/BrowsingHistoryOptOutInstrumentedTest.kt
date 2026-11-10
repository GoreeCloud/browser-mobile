package com.goreecloud.browsermobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.browsermobile.core.BrowsingLibrary
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic local history only. Never target the normal application profile.
 * This test intentionally does not launch WebView or contact an external site.
 */
@RunWith(AndroidJUnit4::class)
class BrowsingHistoryOptOutInstrumentedTest {
    @Test fun directOptOutDeletesHostIndexAndDoesNotResurrectOnRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        require(context.packageName.endsWith(".qa")) {
            "History privacy instrumentation requires an isolated .qa application ID"
        }
        val preferences = context.getSharedPreferences(
            "goreecloud_site_library", android.content.Context.MODE_PRIVATE
        )
        val fakeUrl = "https://synthetic-history.example.test/page?never-store=secret"
        val first = BrowsingLibrary(context)
        try {
            first.clear()
            first.updateHistoryEnabled(true)
            first.record(fakeUrl)
            assertEquals("synthetic-history.example.test", first.sites.single().host)
            assertTrue(preferences.contains("sites"))

            // Calling the preferences boundary directly must be as safe as
            // using the Privacy Center's confirmation handler.
            first.updateHistoryEnabled(false)
            assertFalse(first.historyEnabled)
            assertTrue(first.sites.isEmpty())
            assertFalse(preferences.getBoolean("history_enabled", true))
            assertFalse("Disabled history cannot retain a saved host index",
                preferences.contains("sites"))

            val reloaded = BrowsingLibrary(context)
            assertFalse(reloaded.historyEnabled)
            assertTrue(reloaded.sites.isEmpty())
            reloaded.record("https://still-disabled.example.test/")
            assertTrue(reloaded.sites.isEmpty())
            assertFalse(preferences.contains("sites"))

            // Opting back in does not resurrect an old pre-opt-out host.
            reloaded.updateHistoryEnabled(true)
            assertTrue(BrowsingLibrary(context).sites.isEmpty())
        } finally {
            first.clear()
            first.updateHistoryEnabled(false)
        }
    }
}
