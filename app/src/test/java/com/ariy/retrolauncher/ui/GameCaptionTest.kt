package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class GameCaptionTest {
    private fun game() = GameEntry(title = "My Pac-Man", subtitle = "RetroArch", platform = "MAME Arcade", sourceType = GameSourceType.CUSTOM_INTENT,
        intentDataUri = "content://com.android.externalstorage.documents/document/SD%3AROM%2Fpacman.zip")
    @Test fun arcadeShowsYearAndGameplayWithoutChangingLaunchSubtitle() {
        val entry = game()
        assertTrue(entry.libraryGameCaption()!!.startsWith("1980 · "))
        assertTrue(entry.libraryGameCaption()!!.contains("maze pellets"))
        assertEquals("RetroArch", entry.subtitle)
        assertEquals("My Pac-Man", entry.libraryDisplayTitle())
    }
    @Test fun manualCaptionsOverrideAutomaticAndPersistAcrossJson() {
        val entry = game().copy(releaseYear = "1981", tagline = "One more credit!")
        assertEquals("1981 · One more credit!", GameEntry.fromJson(entry.toJson()).libraryGameCaption())
    }
    @Test fun missingNonArcadeFactsAreNotInvented() {
        assertNull(game().copy(platform = "Nintendo Switch").libraryGameCaption())
        assertEquals("A portable adventure", game().copy(platform = "Nintendo Switch", tagline = "A portable adventure").libraryGameCaption())
    }
}