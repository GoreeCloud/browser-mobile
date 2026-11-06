package com.goreecloud.browsermobile.core

import org.junit.Assert.*
import org.junit.Test

class VisitPolicyTest {
    @Test fun onlyWebsiteHostsAreStored() {
        assertEquals("example.com", VisitPolicy.host("https://example.com/private?q=secret"))
        assertNull(VisitPolicy.host("file:///secrets.txt"))
        assertNull(VisitPolicy.host("javascript:alert(1)"))
    }

    @Test fun countsVisitsWithoutSavingPathsOrSearchQueries() {
        val one = VisitPolicy.record(emptyList(), "https://example.com/account?token=hidden", 10)
        val two = VisitPolicy.record(one, "https://example.com/search?q=secret", 20)
        assertEquals(1, two.size)
        assertEquals(2, two.first().visits)
        assertEquals("https://example.com/", two.first().url)
        assertFalse(two.toString().contains("secret"))
        assertFalse(two.toString().contains("hidden"))
    }

    @Test fun sortRecentAndMostVisited() {
        var sites = VisitPolicy.record(emptyList(), "https://a.example", 1)
        sites = VisitPolicy.record(sites, "https://b.example", 2)
        sites = VisitPolicy.record(sites, "https://a.example", 3)
        assertEquals("a.example", VisitPolicy.mostVisited(sites).first().host)
        assertEquals("a.example", VisitPolicy.recent(sites).first().host)
        assertEquals(2, sites.size)
    }
}
