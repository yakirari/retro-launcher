package com.ariy.retrolauncher.ui

import org.junit.Assert.*
import org.junit.Test

class CabinetBackgroundTest {
    @Test fun arcadeSurroundIsRemovedWithoutKeyingBlackOrEnclosedDetails() {
        val pixels = IntArray(49) { 0xff0c0c0c.toInt() }
        for (y in 1..5) for (x in 1..5) pixels[y * 7 + x] = 0xff000000.toInt()
        pixels[24] = 0xff0c0c0c.toInt()
        pixels[25] = 0xff0b0b0b.toInt()
        val mask = arcadeFrontBackgroundMask(pixels, 7, 7)
        assertEquals(24, mask.count { it })
        assertTrue(mask[0])
        assertFalse(mask[8])
        assertFalse(mask[24])
        assertFalse(mask[25])
    }

    @Test fun arcadeBackgroundFloodDoesNotWrapAcrossRows() {
        val pixels = IntArray(25) { 0xff202020.toInt() }
        pixels[9] = 0xff0c0c0c.toInt()
        pixels[10] = 0xff202020.toInt()
        pixels[11] = 0xff0c0c0c.toInt()
        pixels[8] = 0xff0c0c0c.toInt()
        val mask = arcadeFrontBackgroundMask(pixels, 5, 5)
        assertTrue(mask[8])
        assertFalse(mask[11])
        assertFalse(mask[12])
    }

    @Test fun removesOutsideWhiteButPreservesEnclosedWhiteMarquee() {
        val pixels = IntArray(25) { -1 }
        for (y in 1..3) for (x in 1..3) pixels[y*5+x] = 0xff202020.toInt()
        pixels[12] = -1
        val mask = cabinetBackgroundMask(pixels, 5, 5)
        assertTrue(mask[0])
        assertFalse(mask[12])
        assertFalse(mask[6])
    }
    @Test fun removesNearWhiteAndTransparencyButNotBlackCabinet() {
        val mask = cabinetBackgroundMask(intArrayOf(0xfffafafa.toInt(), 0, 0xff000000.toInt(), 0xffeeeeee.toInt()), 2, 2)
        assertTrue(mask[0]); assertTrue(mask[1]); assertFalse(mask[2]); assertFalse(mask[3])
    }
}