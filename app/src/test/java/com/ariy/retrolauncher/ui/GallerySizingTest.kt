package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GalleryTileSize
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Test

class GallerySizingTest {
    @Test fun completeRowsFitWithSpacingAtPhysicalPixelDensities() {
        for ((width, height) in listOf(900f to 420f, 328f to 640f, 720f to 170f, 240f to 120f)) {
            for (size in GalleryTileSize.entries) {
                for (aspect in listOf(0.46f, 2f / 3f, 1f, 1.6f)) {
                    for (density in listOf(1f, 1.333333f, 1.5f, 2.75f)) {
                        for (spacing in listOf(4f, 10f)) {
                            val sizing = galleryGridSizing(width, height, size, aspect, spacing, density)
                            val heightPx = (sizing.itemHeightDp * density).roundToInt()
                            val spacingPx = (spacing * density).roundToInt()
                            val rows = size.minimumVisibleItems
                            assertTrue("$width x $height / $size / $aspect / $density",
                                heightPx * rows + spacingPx * (rows - 1) <= (height * density).roundToInt())
                            assertTrue(sizing.columns >= 1)
                            if (size == GalleryTileSize.LARGE) assertTrue(sizing.columns >= 2)
                            assertTrue(heightPx >= 1)
                            assertTrue(sizing.columns + spacingPx * (sizing.columns - 1) <=
                                (width * density).roundToInt())
                        }
                    }
                }
            }
        }
    }

    @Test fun largeFitsTwoColumnsAndACompleteRowEvenOnTallNarrowViewports() {
        for ((width, height) in listOf(328f to 640f, 120f to 360f, 240f to 1600f)) {
            for (aspect in listOf(0.46f, 2f / 3f, 1f, 1.6f)) {
                for (spacing in listOf(4f, 10f)) {
                    val sizing = galleryGridSizing(width, height, GalleryTileSize.LARGE, aspect, spacing)
                    assertTrue(sizing.columns >= 2)
                    assertTrue(sizing.itemHeightDp <= height)
                }
            }
        }
        for (size in listOf(GalleryTileSize.MEDIUM, GalleryTileSize.SMALL)) {
            assertEquals(1, galleryGridSizing(240f, 1600f, size).columns)
        }
        assertEquals(GalleryGridSizing(1, 1f), galleryGridSizing(0f, 0f, GalleryTileSize.LARGE))
    }

    @Test fun shorterViewportsAndTallerArtworkAutomaticallyAddColumns() {
        val size = GalleryTileSize.MEDIUM
        val tallViewport = galleryGridSizing(900f, 700f, size)
        val shortViewport = galleryGridSizing(900f, 200f, size)
        assertTrue(shortViewport.columns > tallViewport.columns)
        assertTrue(galleryGridSizing(900f, 200f, size, 0.46f).columns > shortViewport.columns)
    }

    @Test fun sizeChoicesHaveOrderedHeightsAndHandleUnmeasuredViewport() {
        val large = galleryGridSizing(900f, 420f, GalleryTileSize.LARGE)
        val medium = galleryGridSizing(900f, 420f, GalleryTileSize.MEDIUM)
        val small = galleryGridSizing(900f, 420f, GalleryTileSize.SMALL)
        assertTrue(large.itemHeightDp > medium.itemHeightDp)
        assertTrue(medium.itemHeightDp > small.itemHeightDp)
        assertTrue(large.columns < medium.columns)
        assertTrue(medium.columns <= small.columns)
        assertEquals(GalleryGridSizing(1, 1f), galleryGridSizing(0f, 0f, GalleryTileSize.SMALL))
    }
}
