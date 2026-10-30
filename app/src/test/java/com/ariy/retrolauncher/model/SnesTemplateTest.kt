package com.ariy.retrolauncher.model

import com.ariy.retrolauncher.R
import org.junit.Assert.*
import org.junit.Test

class SnesTemplateTest {
    @Test fun labelArtworkFillsBothWindowsAndClipsToRoundedCorners() {
        val usa = requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo", 2.2f))
        val japan = requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo", 2.85f))

        assertEquals(R.drawable.nostalgic_overlay_snes, usa.drawableResourceId)
        assertEquals(R.drawable.nostalgic_overlay_snes_japan, japan.drawableResourceId)
        for (spec in listOf(usa, japan)) {
            assertFalse(spec.fitCompleteArtwork)
            assertFalse(spec.fitArtworkToWindow)
            assertTrue(spec.artworkCornerRadiusFraction > 0f)
        }
    }
}
