package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class LibretroArtworkCatalogTest {
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