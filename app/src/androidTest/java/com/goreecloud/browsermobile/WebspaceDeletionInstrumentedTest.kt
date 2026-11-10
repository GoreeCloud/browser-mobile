package com.goreecloud.browsermobile

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.browsermobile.core.WebspacesManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic routing fixture, restricted to the isolated Browser .qa package. */
@RunWith(AndroidJUnit4::class)
class WebspaceDeletionInstrumentedTest {
    @Test fun deletingCustomWebspaceRemovesSavedRoutingInOnePreferenceTransaction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        require(context.packageName.endsWith(".qa")) {
            "Webspace deletion test requires isolated .qa application"
        }
        val name = "QA Atomic Removal"
        val host = "delete-fixture.example.test"
        val url = "https://$host/private"
        val manager = WebspacesManager(context)
        manager.custom.filter { it.name == name }.forEach { manager.remove(it.id) }
        val created = manager.create(name)
            ?: throw AssertionError("Could not create synthetic Webspace")
        try {
            manager.assign(url, created.id)
            assertEquals(created.id, manager.destination(url))
            val prefs = context.getSharedPreferences("goreecloud_webspaces", Context.MODE_PRIVATE)
            assertTrue(JSONObject(prefs.getString("assignments", "{}")).has(host))

            manager.remove(created.id)
            assertFalse(manager.isKnown(created.id))
            assertFalse(manager.assignments.containsKey(host))

            // SharedPreferences.apply() publishes both values atomically in
            // memory, including to a freshly instantiated manager.
            val restored = WebspacesManager(context)
            assertFalse(restored.isKnown(created.id))
            assertEquals("standard", restored.destination(url))
            assertFalse(JSONObject(prefs.getString("assignments", "{}")).has(host))
            val saved = JSONArray(prefs.getString("custom", "[]"))
            for (i in 0 until saved.length()) {
                assertNotEquals(created.id, saved.optJSONObject(i)?.optString("id"))
            }
        } finally {
            manager.remove(created.id)
            WebspacesManager(context).remove(created.id)
        }
    }
}
