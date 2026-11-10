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
    @Test fun legacyCustomLabelsCannotReintroduceSpoofedOrDuplicateWebspaces() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        require(context.packageName.endsWith(".qa")) {
            "Legacy Webspace fixture requires isolated .qa application"
        }
        val prefs = context.getSharedPreferences("goreecloud_webspaces", Context.MODE_PRIVATE)
        val originalCustom = prefs.getString("custom", null)
        val originalAssignments = prefs.getString("assignments", null)
        try {
            val legacy = JSONArray()
                .put(JSONObject().put("id", "custom-first").put("name", "QA Duplicate"))
                .put(JSONObject().put("id", "custom-second").put("name", "qa duplicate"))
                .put(JSONObject().put("id", "custom-bidi")
                    .put("name", "QA " + 0x202E.toChar() + "Spoof"))
                .put(JSONObject().put("id", "custom-valid")
                    .put("name", "QA " + String(Character.toChars(0x1F512))))
            assertTrue(prefs.edit().putString("custom", legacy.toString())
                .putString("assignments", "{}").commit())
            val manager = WebspacesManager(context)
            assertEquals(2, manager.custom.size)
            assertEquals("QA Duplicate", manager.custom[0].name)
            assertEquals("custom-first", manager.custom[0].id)
            assertEquals("custom-valid", manager.custom[1].id)
        } finally {
            val editor = prefs.edit()
            if (originalCustom == null) editor.remove("custom")
            else editor.putString("custom", originalCustom)
            if (originalAssignments == null) editor.remove("assignments")
            else editor.putString("assignments", originalAssignments)
            assertTrue(editor.commit())
        }
    }

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
