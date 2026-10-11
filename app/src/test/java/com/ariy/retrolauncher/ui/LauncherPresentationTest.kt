package com.ariy.retrolauncher.ui

import org.junit.Assert.*
import org.junit.Test

class LauncherPresentationTest {
    @Test fun tileCaptionsUseGameContinuationRatherThanEmulatorLabels() {
        assertEquals(TileTitle("Castlevania", "Dominus Collection Upd 3"),
            tileTitle("Castlevania Dominus Collection Upd 3"))
        assertEquals(TileTitle("Castelvania", "dominus collection upd 3"),
            tileTitle("Castelvania dominus collection upd 3"))
        assertEquals(TileTitle("Final Fantasy VII", "Rebirth"), tileTitle("Final Fantasy VII: Rebirth"))
        assertEquals(TileTitle("Metroid", "Dread"), tileTitle("Metroid - Dread (USA).nsp"))
    }

    @Test fun compactCaptionsRetainTheWholeTitleAndShortNamesHaveNoSubtitle() {
        assertEquals(TileTitle("Castlevania Dominus Collection Upd 3"),
            tileTitle("Castlevania Dominus Collection Upd 3", split = false))
        assertEquals(TileTitle("DOOM (1993)"), tileTitle("DOOM (1993)"))
        assertEquals(TileTitle("Dr. Mario"), tileTitle("Dr. Mario"))
    }

    @Test fun longUnstructuredCaptionsSplitWithoutLosingWords() {
        val original = "A Very Long Game Name With An Interesting Subtitle"
        val caption = tileTitle(original)
        assertEquals(original, "${caption.primary} ${caption.secondary}")
    }

    @Test fun removesRomMetadataButPreservesGameName() {
        assertEquals("Super Mario World", curatedDisplayTitle("Super_Mario_World (USA) [!].sfc"))
        assertEquals("Game", curatedDisplayTitle("Game (Europe) (En,Fr,De) (Rev 1).iso"))
        assertEquals("Game", curatedDisplayTitle("Game [0100123456780000] (v1.2).nsp"))
    }

    @Test fun preservesMeaningfulParenthesesAndPunctuation() {
        assertEquals("DOOM (1993)", curatedDisplayTitle("DOOM (1993)"))
        assertEquals("Dr. Mario", curatedDisplayTitle("Dr. Mario (USA).nes"))
        assertEquals("Pokemon (Let's Go!)", curatedDisplayTitle("Pokemon (Let's Go!).xci"))
        assertEquals("Final Fantasy VII: Rebirth", curatedDisplayTitle("Final Fantasy VII: Rebirth"))
    }

    @Test fun doesNotRemoveArbitraryExtensionsOrRenameApps() {
        assertEquals("App.Name", curatedDisplayTitle("App.Name"))
        assertEquals("My_App", curatedDisplayTitle("My_App"))
        assertEquals("Game (Special Edition)", curatedDisplayTitle("Game (Special Edition).iso"))
    }

    @Test fun blankOrMetadataOnlyNamesNeverBecomeEmpty() {
        assertEquals("Untitled game", curatedDisplayTitle("   "))
        assertEquals("(USA).nes", curatedDisplayTitle("(USA).nes"))
    }

    @Test fun phoneGridClampsUnusableDensities() {
        assertEquals(2, responsiveGridColumns(328f, 4))
        assertEquals(3, responsiveGridColumns(328f, 8))
        assertEquals(1, responsiveGridColumns(260f, 4))
    }

    @Test fun tabletGridRetainsRequestedDensity() {
        assertEquals(4, responsiveGridColumns(900f, 4))
        assertEquals(8, responsiveGridColumns(900f, 8))
        assertEquals(1, responsiveGridColumns(900f, 1))
    }

    @Test fun gridColumnsAreAlwaysPositiveEvenForTinyViewports() {
        assertEquals(1, responsiveGridColumns(0f, 8))
        assertEquals(1, responsiveGridColumns(328f, 0))
    }

    @Test fun galleryFitsLandscapeHeightAndPhoneWidth() {
        val landscapeWidth = responsiveGalleryWidth(800f, 260f, 0.9f, 0.714f)
        assertTrue(landscapeWidth / 0.714f <= 172.01f)
        val portraitWidth = responsiveGalleryWidth(320f, 640f, 0.98f, 1.25f)
        assertTrue(portraitWidth <= 304f)
    }

    @Test fun tinyGalleryViewportNeverProducesNegativeSizes() {
        assertTrue(responsiveGalleryWidth(10f, 20f, 0.9f, 0.7f) > 0)
    }
}