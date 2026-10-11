package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkQueriesTest {
    @Test fun subtitlesAndDottedTitlesAreNotMisreadAsUriSchemesOrExtensions() {
        assertEquals("Zelda: Tears of the Kingdom", normalizedArtworkTitle("Zelda: Tears of the Kingdom.nsp", true))
        assertEquals("S T A L K E R", normalizedArtworkTitle("S.T.A.L.K.E.R.", true))
        assertEquals("Dr Mario", normalizedArtworkTitle("Dr. Mario", true))
        assertEquals("Zelda: Tears of the Kingdom", normalizedArtworkTitle("content://games/switch/Zelda: Tears of the Kingdom.nsp", true))
    }

    @Test fun editedSearchTitleTakesPriorityOverTheOldRomFilename() {
        val entry = GameEntry(title = "Old title", platform = "Nintendo Switch",
            sourceType = GameSourceType.CUSTOM_INTENT, intentDataUri = "content://games/Old%20title.nsp")
        val queries = entry.artworkSearchQueries("Animal Well")
        assertTrue(queries.first().startsWith("Animal Well "))
        assertTrue(queries.none { it.contains("Old title") })
    }
    @Test
    fun switchArtworkQueriesPreferGameArtworkOverPackaging() {
        val entry = GameEntry(
            title = "198x",
            platform = "Nintendo Switch",
            sourceType = GameSourceType.CUSTOM_INTENT,
            intentDataUri = "content://example/198x.nsp"
        )

        val queries = entry.artworkSearchQueries("198x")
        assertTrue(queries.first().contains("198x", ignoreCase = true))
        assertTrue(queries.any { it.contains("key art", ignoreCase = true) })
        assertTrue(queries.any { it.contains("game artwork", ignoreCase = true) })
        assertTrue(queries.none { it.contains("nsp", ignoreCase = true) })
        assertTrue(queries.none { it.contains("box art", ignoreCase = true) })
        assertTrue(queries.none { it.contains("front cover scan", ignoreCase = true) })
    }

    @Test
    fun atoSwitchArtworkQueriesTargetGameArtRatherThanPackaging() {
        val entry = GameEntry(
            title = "Ato",
            platform = "Nintendo Switch",
            sourceType = GameSourceType.CUSTOM_INTENT
        )

        val queries = entry.artworkSearchQueries("Ato")
        assertTrue(queries.first().contains("Ato", ignoreCase = true))
        assertTrue(queries.first().contains("game artwork", ignoreCase = true))
        assertTrue(queries.none { it.contains("box art", ignoreCase = true) })
    }

    @Test
    fun switchArtworkQueriesUseBaseNameForModdedFiles() {
        val entry = GameEntry(
            title = "Hollow Knight",
            platform = "Nintendo Switch",
            sourceType = GameSourceType.CUSTOM_INTENT,
            intentDataUri = "content://example/Hollow%20Knight%20[Modded].nsp"
        )

        val queries = entry.artworkSearchQueries("Hollow Knight")
        assertTrue(queries.first().contains("Hollow Knight", ignoreCase = true))
        assertTrue(queries.none { it.contains("modded", ignoreCase = true) })
        assertTrue(queries.none { it.contains("box art", ignoreCase = true) })
        assertTrue(queries.none { it.contains("nsp", ignoreCase = true) })
        assertTrue(queries.any { it.contains("game artwork", ignoreCase = true) })
    }

    @Test
    fun uriLikeFallbackTitlesAreReducedToLeafNames() {
        assertEquals("198x", normalizedArtworkTitle("6ebd-3766:ROM/switch/198x.nsp", stripFileExtension = true))
        assertEquals("198x", normalizedArtworkTitle("/storage/emulated/0/ROM/switch/198x.nsp", stripFileExtension = true))
    }

    @Test
    fun removesSwitchTitleIdsButPreservesMeaningfulTitleNumbers() {
        assertEquals("Blasphemous 2", normalizedArtworkTitle("Blasphemous 2 [0100ABCDEF123456].nsp", stripFileExtension = true))
    }
}
