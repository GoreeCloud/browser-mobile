package com.goreecloud.browsermobile.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PageToolsPolicyTest {
    @Test fun `text zoom starts at 100 and advances in 20-percent steps`() {
        assertEquals(120, PageToolsPolicy.stepTextZoom(100, 1))
        assertEquals(80, PageToolsPolicy.stepTextZoom(100, -1))
        assertEquals(100, PageToolsPolicy.stepTextZoom(120, -1))
    }

    @Test fun `text zoom stops at boundaries and rejects extreme values`() {
        assertEquals(80, PageToolsPolicy.stepTextZoom(80, -1))
        assertEquals(200, PageToolsPolicy.stepTextZoom(200, 1))
        assertEquals(80, PageToolsPolicy.stepTextZoom(Int.MIN_VALUE, -1))
        assertEquals(200, PageToolsPolicy.stepTextZoom(Int.MAX_VALUE, 1))
        assertEquals(120, PageToolsPolicy.stepTextZoom(100, Int.MAX_VALUE))
    }

    @Test fun `normal find text is not normalized or sent to a search provider`() {
        val query = "MøbiLe 日本語 123"
        assertEquals(query, PageToolsPolicy.limitFindQuery(query))
        assertEquals("", PageToolsPolicy.limitFindQuery(""))
    }

    @Test fun `find text is bounded without cutting a unicode surrogate pair`() {
        assertEquals(160, PageToolsPolicy.limitFindQuery("x".repeat(800)).length)
        val boundary = "x".repeat(159) + "🔎" + "y".repeat(40)
        assertEquals("x".repeat(159), PageToolsPolicy.limitFindQuery(boundary))
        assertEquals("🔎", PageToolsPolicy.limitFindQuery("🔎"))
    }
}
