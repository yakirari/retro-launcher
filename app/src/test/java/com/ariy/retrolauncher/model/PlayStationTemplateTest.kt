package com.ariy.retrolauncher.model

import org.junit.Assert.*
import org.junit.Test

class PlayStationTemplateTest {
    @Test fun jewelCaseShowsTheFullCoverInsteadOfSupplyingDuplicateBranding() {
        val spec = NostalgicOverlays.specForPlatform("Sony PlayStation")!!
        assertFalse(spec.trimPlayStationSpine)
        assertTrue(spec.fitArtworkToWindow)
        assertFalse(spec.detectArtworkPlacementFromTransparency)
        assertEquals(215f / 960f, spec.artworkLeftFraction, 0.0001f)
        assertEquals(607f / 960f, spec.artworkWidthFraction, 0.0001f)
        assertEquals(0f, spec.preTrimLeftFraction, 0.0001f)
        assertEquals(1f, spec.preTrimWidthFraction, 0.0001f)
    }
}