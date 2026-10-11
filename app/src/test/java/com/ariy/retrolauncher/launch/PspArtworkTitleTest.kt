package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class PspArtworkTitleTest {
    @Test fun numberedDumpNamesMatchCatalogNames() {
        assertEquals(pspArtworkMatchKey("MediEvil - Resurrection (Europe) (En,Fr) (v1.03).png"),
            pspArtworkMatchKey("0102 - MediEvil - Resurrection"))
        assertEquals("LocoRoco", pspArtworkTitle("0482 - LocoRoco (Asia) (En,Zh).iso"))
        assertEquals("LittleBigPlanet", pspArtworkTitle("2012 - LittleBigPlanet"))
    }

    @Test fun releaseLabelsDoNotBecomeSearchTerms() {
        assertEquals("Patapon 2", pspArtworkTitle("Patapon_2_USA_PSP-pSyPSP.iso"))
        assertEquals("Patapon", pspArtworkTitle("Patapon.EUR.PSP-NextLevel.iso"))
        assertEquals("Where is my Heart", pspArtworkTitle("Where_is_my_Heart_USA_PSN_PSP-PLAYASiA.iso"))
        assertEquals("BurnOut", pspArtworkTitle("BurnOut_USA_MULTi5_PROPER_READNFO_PSP-MUPSP.iso"))
    }

    @Test fun articleAndPunctuationDifferencesMatchButSequelsDoNot() {
        assertEquals(pspArtworkMatchKey("Castlevania Dracula X Chronicles INTERNAL READNFO PSP-pSyPSP"),
            pspArtworkMatchKey("Castlevania - The Dracula X Chronicles (USA).png"))
        assertEquals(pspArtworkMatchKey("Ratchet and Clank Size Matters PSP-pSyPSP"),
            pspArtworkMatchKey("Ratchet & Clank - Size Matters (Europe).png"))
        assertNotEquals(pspArtworkMatchKey("Patapon 2"), pspArtworkMatchKey("Patapon 3"))
        assertNotEquals(pspArtworkMatchKey("BurnOut"), pspArtworkMatchKey("Burnout Legends"))
    }

    @Test fun colonSubtitlesArePreserved() {
        assertEquals("God of War: Chains of Olympus", pspArtworkTitle("God of War: Chains of Olympus.iso"))
    }

    @Test fun pspSearchUsesCleanFullTitleAndDoesNotRequirePlatformWordsInHit() {
        val entry = GameEntry(title = "2026 - Assassin's Creed - Bloodlines", platform = "Sony PlayStation Portable",
            sourceType = GameSourceType.CUSTOM_INTENT)
        val query = entry.artworkSearchQueries(entry.title).first()
        assertTrue(query.contains("Assassin's Creed - Bloodlines"))
        assertFalse(query.contains("2026"))
        assertTrue(ArtworkSuggestionService.isRelevantArtworkTitle(query, "Assassin's Creed Bloodlines cover artwork"))
    }
}