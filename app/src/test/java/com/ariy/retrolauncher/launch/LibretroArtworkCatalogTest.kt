package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class LibretroArtworkCatalogTest {
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