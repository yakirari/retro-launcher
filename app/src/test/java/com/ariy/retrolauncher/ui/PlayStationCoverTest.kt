package com.ariy.retrolauncher.ui

import org.junit.Assert.*
import org.junit.Test

class PlayStationCoverTest {
    @Test fun removesPalBottomBannerAtItsExactBoundary() {
        val width = 240; val height = 200
        val pixels = IntArray(width * height) { i -> if (i / width >= 166) 0xff101010.toInt() else 0xffeeeeee.toInt() }
        for (y in 173..186) for (x in 45..190 step 7) pixels[y * width + x] = 0xffffffff.toInt()
        assertEquals(ArtworkBounds(0, 0, width, 166), playStationCoverBounds(pixels, width, height))
    }
    @Test fun cropDoesNotLeavePartOfTheLeftBannerBehind() {
        val width = 200; val height = 200
        val pixels = IntArray(width * height) { if (it % width < 32) 0xff101010.toInt() else 0xffbb7744.toInt() }
        for (y in 25..175 step 10) for (x in 8..24) pixels[y * width + x] = 0xffffffff.toInt()
        assertEquals(32, playStationCoverBounds(pixels, width, height).left)
    }
    @Test fun removesPrintedBlackSpineButKeepsArtwork() {
        val width = 200; val height = 200
        val pixels = IntArray(width*height) { i -> if (i%width < 28) 0xff101010.toInt() else 0xffb07040.toInt() }
        for (y in 30..150 step 12) for (x in 12..20) pixels[y*width+x] = 0xffffffff.toInt()
        val crop = playStationCoverBounds(pixels,width,height)
        assertTrue(crop.left in 24..28)
        assertEquals(width, crop.right)
        assertEquals(height, crop.bottom)
    }
    @Test fun unbrandedDarkArtAndFullBleedAreUnchanged() {
        for (color in listOf(0xff101010.toInt(), 0xffb07040.toInt())) {
            assertEquals(ArtworkBounds(0,0,200,200), playStationCoverBounds(IntArray(40000) { color },200,200))
        }
        val darkEdge = IntArray(40000) { if (it%200 < 28) 0xff101010.toInt() else 0xffb07040.toInt() }
        assertEquals(0, playStationCoverBounds(darkEdge,200,200).left)
    }
    @Test fun portraitAndTinyImagesAreNotCropped() {
        assertEquals(0, playStationCoverBounds(IntArray(200*300) { 0xff000000.toInt() },200,300).left)
        assertEquals(0, playStationCoverBounds(IntArray(32*32) { 0xff000000.toInt() },32,32).left)
    }
}