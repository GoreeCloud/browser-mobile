package com.goreecloud.browsermobile.core

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class FilterListsTest {
    @Test fun defaultSubscriptionsAreExactlyRequestedPair() {
        assertEquals(
            setOf(FilterSource.OISD_SMALL, FilterSource.STEVENBLACK),
            FilterSource.entries.filter { it.enabledByDefault }.toSet()
        )
        assertTrue(FilterSource.entries.all { it.url.startsWith("https://") })
    }

    @Test fun parsesOisdDomainWildcardsButNotAbpRules() {
        val list = DomainFilterParser.parse(StringReader(
            "# oisd small\nexample.com\nsub.example.net\n||malicious.test^\n*.untrusted.net\n"
        ), FilterFormat.DOMAINS)
        assertEquals(setOf("example.com", "sub.example.net"), list)
    }

    @Test fun parsesStevenBlackAndIgnoresNonSinkMappings() {
        val list = DomainFilterParser.parse(StringReader(
            "# StevenBlack\n0.0.0.0 analytics.example.com ads.example.net # explanation\n" +
                "127.0.0.1 tracking.example.org\n192.168.0.20 useful.example.org\n" +
                "0.0.0.0 localhost\n"
        ), FilterFormat.HOSTS)
        assertEquals(
            setOf("analytics.example.com", "ads.example.net", "tracking.example.org"), list
        )
    }

    @Test fun matcherHonorsDomainBoundaries() {
        val matcher = DomainRuleMatcher(setOf("ads.example.com"))
        assertTrue(matcher.matchesUrl("https://ads.example.com/a"))
        assertTrue(matcher.matchesUrl("https://deeper.ads.example.com/a"))
        assertFalse(matcher.matchesUrl("https://ads.example.com.evil.net/"))
        assertFalse(matcher.matchesUrl("https://notads.example.com/"))
        assertFalse(matcher.matchesUrl("javascript:alert(1)"))
    }

    @Test fun oversizedRuleSetIsRejected() {
        val reader = StringReader("domain.test\n".repeat(DomainFilterParser.MAX_LINES + 1))
        assertThrows(IllegalArgumentException::class.java) {
            DomainFilterParser.parse(reader, FilterFormat.DOMAINS)
        }
    }

    @Test fun domainValidationRejectsLocalhostAndAddresses() {
        assertNull(DomainFilterParser.normalizedDomain("localhost"))
        assertNull(DomainFilterParser.normalizedDomain("127.0.0.1"))
        assertNull(DomainFilterParser.normalizedDomain("example..com"))
        assertNull(DomainFilterParser.normalizedDomain("not a domain.com"))
        assertEquals("good.example.com", DomainFilterParser.normalizedDomain("GOOD.Example.com."))
    }
}
