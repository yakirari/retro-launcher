package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkStateTest {
    private val original = GameEntry(
        id = "game", title = "Game", platform = "Super Nintendo",
        sourceType = GameSourceType.CUSTOM_INTENT
    )

    @Test fun missingArtworkIsFilledWithoutChangingOtherMetadata() {
        val played = original.copy(lastPlayedAtEpochMillis = 123L)
        assertEquals(played.copy(imageUri = "new"), played.withAutofilledArtwork(original, "new"))
    }

    @Test fun concurrentManualArtworkSelectionIsPreserved() {
        val edited = original.copy(imageUri = "manual")
        assertEquals(edited, edited.withAutofilledArtwork(original, "downloaded"))
    }

    @Test fun renamedGameRejectsStaleArtwork() {
        val renamed = original.copy(title = "Another Game")
        assertEquals(renamed, renamed.withAutofilledArtwork(original, "downloaded"))
    }

    @Test fun refreshCanReplaceExistingArtworkWithoutClearingItFirst() {
        val existing = original.copy(imageUri = "old")
        assertEquals(existing.copy(imageUri = "new"), existing.withAutofilledArtwork(existing, "new"))
    }
}