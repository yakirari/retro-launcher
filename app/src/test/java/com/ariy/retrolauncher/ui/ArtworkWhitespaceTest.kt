package com.ariy.retrolauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkWhitespaceTest {
    private fun borderedImage(border: Int, content: Int): IntArray = IntArray(100) { index ->
        val x = index % 10
        val y = index / 10
        if (x == 0 || x == 9 || y == 0 || y == 9) border else content
    }

    @Test fun trimsUniformWhitePadding() {
        assertEquals(ArtworkBounds(1, 1, 9, 9), artworkContentBounds(borderedImage(-1, 0xFF223344.toInt()), 10, 10))
    }

    @Test fun trimsTransparentPadding() {
        assertEquals(ArtworkBounds(1, 1, 9, 9), artworkContentBounds(borderedImage(0, 0xFF223344.toInt()), 10, 10))
    }

    @Test fun doesNotCropColoredOrBlackArtworkEdges() {
        assertEquals(ArtworkBounds(0, 0, 10, 10), artworkContentBounds(borderedImage(0xFF000000.toInt(), -1), 10, 10))
    }

    @Test fun preservesBlankImagesAndTinyImages() {
        assertEquals(ArtworkBounds(0, 0, 10, 10), artworkContentBounds(IntArray(100) { -1 }, 10, 10))
        assertEquals(ArtworkBounds(0, 0, 1, 1), artworkContentBounds(intArrayOf(0), 1, 1))
    }

    @Test fun preservesWhiteMarginsContainingVisibleDetails() {
        val pixels = borderedImage(-1, 0xFF223344.toInt())
        pixels[5] = 0xFF000000.toInt()
        assertEquals(ArtworkBounds(1, 0, 9, 9), artworkContentBounds(pixels, 10, 10))
    }
}