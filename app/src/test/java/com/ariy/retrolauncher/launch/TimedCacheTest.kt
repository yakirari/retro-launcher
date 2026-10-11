package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test

class TimedCacheTest {
    @Test fun entriesExpireEvenWhenAccessedRepeatedly() {
        var now = 0L
        val cache = TimedCache<String, String>(2, 100) { now }
        cache.put("roms", "old inventory")
        now = 99
        assertEquals("old inventory", cache.get("roms"))
        now = 100
        assertNull(cache.get("roms"))
    }

    @Test fun refreshReplacesInventoryAndResetsExpiry() {
        var now = 0L
        val cache = TimedCache<String, List<String>>(2, 100) { now }
        cache.put("roms", listOf("A"))
        now = 90
        cache.put("roms", listOf("A", "B"))
        now = 100
        assertEquals(listOf("A", "B"), cache.get("roms"))
    }

    @Test fun evictsLeastRecentlyUsedEntries() {
        val cache = TimedCache<String, String>(2, 100)
        cache.put("a", "A")
        cache.put("b", "B")
        assertEquals("A", cache.get("a"))
        cache.put("c", "C")
        assertNull(cache.get("b"))
        assertEquals("A", cache.get("a"))
        assertEquals("C", cache.get("c"))
    }

    @Test fun permissionChangeCanInvalidateAllInventories() {
        val cache = TimedCache<String, String>(2, 100)
        cache.put("tree", "inventory")
        cache.clear()
        assertNull(cache.get("tree"))
    }
}