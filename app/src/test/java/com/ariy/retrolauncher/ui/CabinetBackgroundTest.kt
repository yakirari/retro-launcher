package com.ariy.retrolauncher.ui

import org.junit.Assert.*
import org.junit.Test

class CabinetBackgroundTest {
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