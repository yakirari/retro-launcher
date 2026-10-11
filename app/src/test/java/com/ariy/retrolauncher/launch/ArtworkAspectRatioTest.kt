package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkAspectRatioTest {
    @Test fun digitalLibrariesPreferFullPortraitCoversOverHeadersAndIcons() {
        for (platform in com.ariy.retrolauncher.model.PcGameStores.platforms + "PC (Windows)") {
            val ratio = preferredArtworkAspectRatio(platform)
            assertEquals(2f / 3f, ratio)
            assertTrue(compareArtworkAspectRatios(600, 900, 460, 215, ratio) < 0)
            assertTrue(compareArtworkAspectRatios(600, 900, 128, 128, ratio) < 0)
        }
    }
    @Test
    fun switchPrefersPortraitCoverArtwork() {
        val targetRatio = preferredArtworkAspectRatio("Nintendo Switch")

        assertTrue(targetRatio < 1f)
        assertTrue(compareArtworkAspectRatios(2, 3, 16, 9, targetRatio) < 0)
    }

    @Test
    fun artworkRatioTargetsReflectPlatformArtStyle() {
        assertTrue(preferredArtworkAspectRatio("Nintendo Switch") < 1f)
        assertTrue(preferredArtworkAspectRatio("DOS Games") > 1f)
        assertEquals(1f, preferredArtworkAspectRatio("Android"))
    }

    @Test
    fun invalidDimensionsDoNotBeatValidAspectMatches() {
        assertTrue(compareArtworkAspectRatios(0, 0, 2, 3, 0.7f) > 0)
    }
}