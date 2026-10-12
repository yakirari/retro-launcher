package com.ariy.retrolauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryPagingTest {
    @Test fun artworkPrefetchClampsOldFocusWhenSwitchingToShorterLists() {
        assertEquals(0..0, GalleryPaging.artworkPrefetchRange(1, 7, 4, 8))
        assertEquals(0..2, GalleryPaging.artworkPrefetchRange(3, 999, 4, 8))
        assertEquals(0..8, GalleryPaging.artworkPrefetchRange(20, -1, 4, 8))
        assertEquals(IntRange.EMPTY, GalleryPaging.artworkPrefetchRange(0, 999, 4, 8))
    }

    @Test fun artworkPrefetchRangesAlwaysFormValidSubListsIncludingOverflowInputs() {
        for (total in listOf(1, 2, 3, 48, 1000)) {
            for (focus in listOf(-10, 0, 7, 999, Int.MAX_VALUE)) {
                val range = GalleryPaging.artworkPrefetchRange(total, focus, 4, 8)
                val list = List(total) { it }
                assertEquals(range.toList(), list.subList(range.first, range.last + 1))
            }
        }
        assertEquals(0 until Int.MAX_VALUE,
            GalleryPaging.artworkPrefetchRange(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE))
    }
    @Test fun smallLibrariesStayCompleteAndLargeLibrariesStartWithOnePage() {
        assertEquals(0, GalleryPaging.initialCount(0))
        assertEquals(17, GalleryPaging.initialCount(17))
        assertEquals(48, GalleryPaging.initialCount(100_000))
    }
    @Test fun pagesExtendBeforeTheirBoundaryWithoutChangingExistingIndices() {
        assertEquals(48, GalleryPaging.nextCount(1000, 48, 35))
        assertEquals(96, GalleryPaging.nextCount(1000, 48, 36))
        assertEquals(100, GalleryPaging.nextCount(100, 96, 90))
        assertEquals(100, GalleryPaging.nextCount(100, 100, 99))
    }
    @Test fun focusJumpsLoadTheContainingPageAndNeverShrinkAnExistingPage() {
        assertEquals(528, GalleryPaging.countThrough(1000, 48, 500))
        assertEquals(528, GalleryPaging.countThrough(1000, 528, 2))
        assertEquals(1000, GalleryPaging.countThrough(1000, 48, 5000))
        assertEquals(Int.MAX_VALUE, GalleryPaging.countThrough(Int.MAX_VALUE, 48, Int.MAX_VALUE - 1))
    }
    @Test fun resizedLibrariesClampCountsSafely() {
        assertEquals(12, GalleryPaging.nextCount(12, 96, 10))
        assertEquals(0, GalleryPaging.nextCount(0, 96, 10))
    }
}