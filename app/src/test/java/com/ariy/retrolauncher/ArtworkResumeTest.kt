package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkResumeTest {
    private fun game(id: String, image: String? = null) = GameEntry(
        id = id, title = id, imageUri = image, sourceType = GameSourceType.CUSTOM_INTENT
    )

    @Test fun reopeningReconstructsQueueFromOnlyMissingSavedCovers() {
        val games = listOf(game("missing"), game("blank", " "), game("covered", "file://cover"))
        assertEquals(listOf("missing", "blank"),
            missingArtworkToResume(games, emptySet(), emptyMap(), 10).map { it.id })
    }

    @Test fun activeSearchesAreNeverQueuedTwice() {
        assertEquals(emptyList<GameEntry>(),
            missingArtworkToResume(listOf(game("active")), setOf("active"), emptyMap(), 10))
    }

    @Test fun failedSearchWaitsForRetryDeadline() {
        val games = listOf(game("failed"))
        assertEquals(emptyList<GameEntry>(), missingArtworkToResume(games, emptySet(), mapOf("failed" to 100L), 99))
        assertEquals(games, missingArtworkToResume(games, emptySet(), mapOf("failed" to 100L), 100))
    }

    @Test fun manualCoverAddedDuringRetryAndRemovedGamesAreNotRetried() {
        assertEquals(emptyList<GameEntry>(), missingArtworkToResume(
            listOf(game("manual", "content://picked")), emptySet(), mapOf("manual" to 100L, "removed" to 100L), 200))
    }

    @Test fun blankTitlesAreNotSearched() {
        assertEquals(emptyList<GameEntry>(), missingArtworkToResume(
            listOf(game("blank").copy(title = " ")), emptySet(), emptyMap(), 10))
    }
}