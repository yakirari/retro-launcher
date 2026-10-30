package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GalleryTileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfPresentationTest {
    @Test fun portraitAndCartridgeUseTheSameHeightWhenWidthAllows() {
        val portrait = shelfArtworkWidth(1000f, 400f, 0.67f, GalleryTileSize.LARGE)
        val cartridge = shelfArtworkWidth(1000f, 400f, 1.5f, GalleryTileSize.LARGE)
        assertEquals(portrait / 0.67f, cartridge / 1.5f, 0.01f)
        assertEquals(312f, portrait / 0.67f, 0.01f)
    }

    @Test fun wideObjectsStayInsideViewportAndDenseModesReduceSize() {
        assertEquals(shelfSlotWidth(400f, 400f, GalleryTileSize.LARGE) - 16f,
            shelfArtworkWidth(400f, 400f, 3f, GalleryTileSize.LARGE), 0.01f)
        assertTrue(shelfArtworkWidth(1000f, 400f, 1f, GalleryTileSize.SMALL) <
            shelfArtworkWidth(1000f, 400f, 1f, GalleryTileSize.LARGE))
        assertTrue(shelfArtworkWidth(0f, 0f, 0f, GalleryTileSize.LARGE) >= 1f)
    }

    @Test fun allShapesFitWithinTheSameStableSlotWithAGutter() {
        for (size in GalleryTileSize.entries) {
            val slot = shelfSlotWidth(720f, 420f, size)
            for (aspect in listOf(0.5f, 0.67f, 1f, 1.5f, 3f)) {
                val width = shelfArtworkWidth(720f, 420f, aspect, size)
                assertTrue(width <= slot - 16f)
                assertTrue(width / aspect <= shelfArtworkHeight(420f, size) + 0.01f)
            }
        }
    }

    @Test fun centeredShelfFitsRequestedCompleteItemsEvenAtEitherEnd() {
        for ((width, height) in listOf(900f to 420f, 328f to 640f, 720f to 170f, 80f to 120f)) {
            for (density in listOf(1f, 1.333333f, 1.5f, 2.75f)) {
                for (size in GalleryTileSize.entries) {
                    val slot = shelfSlotWidth(width, height, size, density)
                    val sidePadding = (width - slot) / 2f
                    val rows = size.minimumVisibleItems
                    val rightEdge = sidePadding + rows * slot + (rows - 1) * SHELF_ITEM_SPACING_DP
                    assertTrue("$width / $density / $size: $rightEdge", rightEdge <= width + 0.001f)
                    assertTrue(shelfArtworkHeight(height, size) + 20f <= height)
                }
            }
        }
    }
}