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
        assertTrue(WebspacePolicy.validName("Team " + String(Character.toChars(0x1F600))))
        assertFalse(WebspacePolicy.validName(""))
        assertFalse(WebspacePolicy.validName(" ".repeat(3)))
        assertFalse(WebspacePolicy.validName("A".repeat(31)))
    }

    @Test fun webspaceNamesRejectBidiAndLineSpoofingWithoutSplittingUnicode() {
        for (code in listOf(
            0x0000, 0x000A, 0x007F, 0x0085, 0x061C, 0x200E, 0x200F,
            0x2028, 0x2029, 0x202A, 0x202D, 0x202E, 0x2066, 0x2069
        )) {
            val name = "Team " + String(Character.toChars(code)) + "Private"
            assertFalse("Formatting/control U+${code.toString(16)}", WebspacePolicy.validName(name))
        }
        assertFalse(WebspacePolicy.validName("Team " + 0xD800.toChar()))
        assertFalse(WebspacePolicy.validName("Team " + 0xDC00.toChar()))
        assertFalse(WebspacePolicy.validName("Team " + 0xD800.toChar() + "X"))
        assertTrue(WebspacePolicy.validName("Team " + String(Character.toChars(0x1F512))))
    }
}
