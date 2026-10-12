package com.ariy.retrolauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfPresentationTest {
    @Test fun portraitAndCartridgeUseTheSameHeightWhenWidthAllows() {
        val portrait = shelfArtworkWidth(1000f, 400f, 0.67f, 1)
        val cartridge = shelfArtworkWidth(1000f, 400f, 1.5f, 1)
        assertEquals(portrait / 0.67f, cartridge / 1.5f, 0.01f)
        assertEquals(218.4f, portrait / 0.67f, 0.01f)
    }

    @Test fun wideObjectsStayInsideViewportAndDenseModesReduceSize() {
        assertEquals(shelfSlotWidth(400f, 400f, 1) - 16f,
            shelfArtworkWidth(400f, 400f, 3f, 1), 0.01f)
        assertTrue(shelfArtworkWidth(1000f, 400f, 1f, 8) < shelfArtworkWidth(1000f, 400f, 1f, 1))
        assertTrue(shelfArtworkWidth(0f, 0f, 0f, 1) >= 1f)
    }

    @Test fun allShapesFitWithinTheSameStableSlotWithAGutter() {
        for (count in listOf(1, 2, 3, 4, 8)) {
            val slot = shelfSlotWidth(720f, 420f, count)
            for (aspect in listOf(0.5f, 0.67f, 1f, 1.5f, 3f)) {
                val width = shelfArtworkWidth(720f, 420f, aspect, count)
                assertTrue(width <= slot - 16f)
                assertTrue(width / aspect <= shelfArtworkHeight(420f, count) + 0.01f)
            }
        }
    }
}