package com.goreecloud.browsermobile.core

import org.junit.Assert.*
import org.junit.Test

class WebspacePolicyTest {
    @Test fun builtinRoutingHonorsHostBoundaries() {
        assertEquals("google", WebspacePolicy.route("https://mail.google.com/mail"))
        assertEquals("proton", WebspacePolicy.route("https://drive.proton.me"))
        assertEquals("standard", WebspacePolicy.route("https://notgoogle.com"))
        assertEquals("standard", WebspacePolicy.route("https://example.com"))
    }

    @Test fun explicitRoutingWinsWithoutSubdomainWildcard() {
        val assigned = mapOf("mail.google.com" to "custom-work")
        assertEquals("custom-work", WebspacePolicy.route("https://mail.google.com/", assigned))
        assertEquals("google", WebspacePolicy.route("https://google.com", assigned))
        assertEquals("google", WebspacePolicy.route("https://calendar.google.com", assigned))
    }

    @Test fun customNamesMustBeReadableAndBounded() {
        assertTrue(WebspacePolicy.validName("Personal"))
        assertFalse(WebspacePolicy.validName(""))
        assertFalse(WebspacePolicy.validName(" ".repeat(3)))
        assertFalse(WebspacePolicy.validName("A".repeat(31)))
    }
}
