package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.ui.libraryDisplayTitle
import org.junit.Assert.*
import org.junit.Test

class ArcadeGameCatalogTest {
    @Test fun expandsKnownRomNamesAndExplicitClones() {
        assertEquals("BurgerTime", ArcadeGameCatalog.find("btime.zip")?.title)
        assertEquals("Street Fighter II: The World Warrior", ArcadeGameCatalog.find("sf2j")?.title)
        assertEquals("Defender", ArcadeGameCatalog.find("defendg")?.title)
    }
    @Test fun doesNotGuessUnlistedSequels() {
        assertEquals("Dig Dug II", ArcadeGameCatalog.find("digdug2")?.title)
        assertNotEquals(ArcadeGameCatalog.find("digdug")?.rom, ArcadeGameCatalog.find("digdug2")?.rom)
        assertNotEquals(ArcadeGameCatalog.find("sf2")?.rom, ArcadeGameCatalog.find("sf2ce")?.rom)
        assertNull(ArcadeGameCatalog.find("digdug999"))
        assertNull(ArcadeGameCatalog.find("unknown"))
    }
    @Test fun ownedSequelsAndVariantNamesAreNotConfusedWithPredecessors() {
        assertEquals("Donkey Kong 3", ArcadeGameCatalog.find("dkong3.zip")?.title)
        assertEquals("R-Type II", ArcadeGameCatalog.find("rtype2.zip")?.title)
        assertEquals("Teenage Mutant Ninja Turtles: Turtles in Time", ArcadeGameCatalog.find("tmnt22p.zip")?.title)
        assertEquals("Double Dragon", ArcadeGameCatalog.find("ddragonu.zip")?.title)
        assertEquals("Out Run", ArcadeGameCatalog.find("outrunb.zip")?.title)
        assertEquals("X-Men", ArcadeGameCatalog.find("xmen2p.zip")?.title)
    }
    @Test fun savedCustomTitleAndOtherPlatformsArePreserved() {
        val game = GameEntry(title = "btime", platform = "MAME Arcade", sourceType = GameSourceType.CUSTOM_INTENT)
        assertEquals("BurgerTime", game.libraryDisplayTitle())
        assertEquals("My BurgerTime", game.copy(title = "My BurgerTime").libraryDisplayTitle())
        assertEquals("btime", game.copy(platform = "Nintendo DS").libraryDisplayTitle())
    }
}