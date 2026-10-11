package com.ariy.retrolauncher.ui

import org.junit.Assert.*
import org.junit.Test

class FilterNavigationTest {
    private val filters = listOf("All", "Arcade", "Nintendo Switch", "ScummVM")

    @Test fun platformsWrapInBothDirectionsIncludingScummVm() {
        assertEquals("All", FilterNavigation.adjacent("ScummVM", filters, 1))
        assertEquals("ScummVM", FilterNavigation.adjacent("All", filters, -1))
        assertEquals("Nintendo Switch", FilterNavigation.adjacent("ScummVM", filters, -1))
        filters.forEach { filter ->
            val next = requireNotNull(FilterNavigation.adjacent(filter, filters, 1))
            assertEquals(filter, FilterNavigation.adjacent(next, filters, -1))
        }
    }

    @Test fun temporaryWorkerSnapshotNeverSkipsBackwardSelectedPlatform() {
        assertFalse(FilterNavigation.shouldSkipEmpty(false, 0, 1000, false))
        assertTrue(FilterNavigation.shouldSkipEmpty(true, 0, 1000, false))
        assertFalse(FilterNavigation.shouldSkipEmpty(true, 10, 1000, false))
        assertFalse(FilterNavigation.shouldSkipEmpty(true, 0, 0, false))
        assertFalse(FilterNavigation.shouldSkipEmpty(true, 0, 1000, true))
    }

    @Test fun missingCaseInsensitiveAndSingleFiltersHaveStableNavigation() {
        assertEquals("ScummVM", FilterNavigation.adjacent("missing", filters, -1))
        assertEquals("All", FilterNavigation.adjacent("missing", filters, 1))
        assertEquals("All", FilterNavigation.adjacent("scummvm", filters, 1))
        assertEquals("ScummVM", FilterNavigation.adjacent("ScummVM", listOf("ScummVM"), -1))
        assertNull(FilterNavigation.adjacent("ScummVM", emptyList(), 1))
    }
}