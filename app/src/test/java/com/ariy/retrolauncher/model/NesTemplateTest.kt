package com.ariy.retrolauncher.model

import org.junit.Assert.*
import org.junit.Test

class NesTemplateTest {
    @Test fun labelRetainsCompleteArtworkOriginalColorsAndShellMask() {
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Nintendo Entertainment System"))
        assertTrue(spec.fitCompleteArtwork)
        assertTrue(spec.alignCompleteArtworkToTop)
        assertTrue(spec.clipArtworkToTransparencyMask)
        assertTrue(spec.detectArtworkPlacementFromTransparency)
        assertFalse(spec.applyVintageArtwork)
        assertEquals(0xFF737373.toInt(), spec.artworkBackgroundColor)
        assertEquals(0f, spec.preTrimTopFraction, 0f)
        assertEquals(1f, spec.preTrimHeightFraction, 0f)
        assertEquals(307f / 493f, spec.artworkWidthFraction * 960f /
            (spec.artworkHeightFraction * 768f), 0.0001f)
    }
}