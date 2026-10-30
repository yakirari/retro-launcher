package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibretroArtworkCatalogTest {
    @Test fun nesSelectionIsGenericAndRejectsWrongRegionsPublishersAndUnknownVariants() {
        val entries = LibretroArtworkCatalog.parseDirectoryIndex("""
            <a href="Future%20Game%20(Japan).png">jp</a>
            <a href="Future%20Game%20(USA).png">us</a>
            <a href="Future%20Game%20(USA)%20(Tengen)%20(Unl).png">publisher</a>
            <a href="Future%20Game%20(USA)%20%5Bh%5D.png">hack</a>
        """)
        fun select(rom: String) = LibretroArtworkCatalog.selectNesEntries(entries, "Wrong shortened title", rom).map { it.displayTitle }
        assertEquals(listOf("Future Game (Japan)"), select("Future Game (Japan) (En).nes"))
        assertEquals(listOf("Future Game (USA) (Tengen) (Unl)"), select("Future Game (USA) (Tengen) (Unl).nes"))
        assertTrue(select("Future Game (Europe).nes").isEmpty())
        assertTrue(select("Future Game Two Player (USA).nes").isEmpty())
        assertEquals(listOf("Future Game (USA)"), select("Future Game (USA) (Rev 1).nes"))
    }
    @Test fun nesUsesFullRomTitleAndExactEditionBeforeLegacyHackAliases() {
        val entries = LibretroArtworkCatalog.parseDirectoryIndex("""
            <a href="Double%20Dragon%20II%20-%20The%20Revenge%20(1989)(Acclaim)(US)[h].png">old</a>
            <a href="Double%20Dragon%20II%20-%20The%20Revenge%20(USA).png">base</a>
            <a href="Double%20Dragon%20II%20-%20The%20Revenge%20(USA)%20(Rev%201).png">rev</a>
        """)
        val selected = LibretroArtworkCatalog.selectNesEntries(entries, "Double Dragon II - The",
            "Double Dragon II - The Revenge (USA) (Rev 1).nes")
        assertEquals(listOf("Double Dragon II - The Revenge (USA) (Rev 1)"), selected.map { it.displayTitle })
        assertEquals(2, LibretroArtworkCatalog.selectNesEntries(entries, "Unused",
            "Double Dragon II - The Revenge (USA) (En).nes").size)
        assertEquals(0, LibretroArtworkCatalog.selectNesEntries(entries, "Double Dragon II - The Revenge",
            "Double Dragon III (USA).nes").size)
    }
    @Test fun apostrophesInsideDoubleQuotedCatalogLinksDoNotDropGames() {
        val entries = LibretroArtworkCatalog.parseDirectoryIndex(
            """<a href="Eternal%20Darkness%20-%20Sanity's%20Requiem%20(USA).png">Cover</a>
                <a href='Luigi%27s%20Mansion%20(USA).png'>Cover</a>""")
        assertEquals(2, entries.size)
        assertEquals(LibretroArtworkCatalog.artworkMatchKey("Eternal Darkness - Sanity's Requiem"), entries[0].matchKey)
        assertEquals(LibretroArtworkCatalog.artworkMatchKey("Luigi's Mansion"), entries[1].matchKey)
    }
    @Test fun exactRomRegionRanksBeforeOtherRegionsWithoutGuessingUnknownRegion() {
        assertEquals(0, LibretroArtworkCatalog.regionRank("Game (Europe) (En,Fr)", "Game (Europe).iso"))
        assertEquals(2, LibretroArtworkCatalog.regionRank("Game (Japan)", "Game (Europe).iso"))
        assertEquals(1, LibretroArtworkCatalog.regionRank("Game (World)", "Game (USA).z64"))
        assertEquals(0, LibretroArtworkCatalog.regionRank("Game (Japan)", "Game.iso"))
    }
    @Test
    fun directoryIndexMatchesExactTitleAndRegionVariants() {
        val entries = LibretroArtworkCatalog.parseDirectoryIndex(
            """
                <a href="198X%20(USA).png">198X (USA).png</a>
                <a href="198X%20(Europe).png">198X (Europe).png</a>
                <a href="Another%20Game.png">Another Game.png</a>
            """.trimIndent()
        )

        assertEquals(listOf("198x", "198x"), entries.take(2)
            .filter { it.matchKey == LibretroArtworkCatalog.artworkMatchKey("198x") }
            .map { it.matchKey })
    }

    @Test
    fun catalogMatchKeyIgnoresCommonRegionTags() {
        assertEquals(
            LibretroArtworkCatalog.artworkMatchKey("Super Mario World"),
            LibretroArtworkCatalog.artworkMatchKey("Super Mario World (USA)")
        )
    }
}