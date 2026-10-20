package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.LaunchTarget
import org.junit.Assert.*
import org.junit.Test

class GameEntryMergeTest {
    private fun game(id: String, title: String, rom: String) = GameEntry(
        id = id, title = title, platform = "Super Nintendo",
        sourceType = GameSourceType.CUSTOM_INTENT, packageName = "com.retroarch",
        intentDataUri = rom
    )

    private fun target(game: GameEntry) = LaunchTarget(
        sourceType = game.sourceType, packageName = game.packageName,
        intentDataUri = game.intentDataUri
    )

    @Test fun cartridgeSettingsKeepExistingOverridesAndFillMissingValues() {
        val original = game("a", "Game", "content://rom/a").copy(
            imageUri = "content://art/cover", cartridgeLabelUri = "content://art/manual-label", cartridgeColor = "gold"
        )
        val incoming = game("b", "Game", "content://rom/b").copy(
            cartridgeLabelUri = "content://art/incoming-label", cartridgeColor = "red"
        )
        val library = mutableListOf(original)
        upsertOrMergeInPlace(library, incoming, target(incoming))
        assertEquals(original.cartridgeLabelUri, library.single().cartridgeLabelUri)
        assertEquals(original.cartridgeColor, library.single().cartridgeColor)
        assertEquals(original.imageUri, library.single().imageUri)

        val empty = mutableListOf(original.copy(cartridgeLabelUri = null, cartridgeColor = null))
        upsertOrMergeInPlace(empty, incoming, target(incoming))
        assertEquals(incoming.cartridgeLabelUri, empty.single().cartridgeLabelUri)
        assertEquals(incoming.cartridgeColor, empty.single().cartridgeColor)
    }

    @Test fun launchSetupRepairAndRepeatedImportPreserveCartridgeOverrides() {
        val pending = game("a", "Game", "content://rom/a").copy(
            needsLaunchSetup = true, packageName = null,
            cartridgeLabelUri = "content://art/label", cartridgeColor = "indigo"
        )
        val ready = pending.copy(id = "b", needsLaunchSetup = false, packageName = "emulator",
            cartridgeLabelUri = null, cartridgeColor = null)
        val library = mutableListOf(pending)
        assertEquals(UpsertOutcome.MERGED, upsertOrMergeInPlace(library, ready, target(ready)).outcome)
        assertEquals(pending.cartridgeLabelUri, library.single().cartridgeLabelUri)
        assertEquals(pending.cartridgeColor, library.single().cartridgeColor)
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, ready, target(ready)).outcome)
        assertEquals(pending.cartridgeLabelUri, library.single().cartridgeLabelUri)
    }

    @Test fun newScanResultPreservesExistingTilesAndMetadata() {
        val original = game("a", "Game A", "content://rom/a").copy(
            imageUri = "file:///cover.png", genre = "Favorites", lastPlayedAtEpochMillis = 123L
        )
        val unrelated = original.copy(id = "other", platform = "Game Boy")
        val library = mutableListOf(original, unrelated)
        val added = game("b", "Game B", "content://rom/b")
        val result = upsertOrMergeInPlace(library, added, target(added))
        assertEquals(UpsertOutcome.ADDED, result.outcome)
        assertEquals(listOf(original, unrelated, added), library)
    }

    @Test fun repeatedImportIsIdempotent() {
        val original = game("a", "Game A", "content://rom/a")
        val library = mutableListOf(original)
        val duplicate = original.copy(id = "new-id")
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, duplicate, target(duplicate)).outcome)
        assertEquals(listOf(original), library)
    }

    @Test fun alternateRomPreservesIdentityArtworkAndPlayHistory() {
        val original = game("a", "Game A", "content://rom/a").copy(
            imageUri = "file:///cover.png", lastPlayedAtEpochMillis = 123L
        )
        val alternate = game("b", "game a", "content://rom/alternate")
        val library = mutableListOf(original)
        assertEquals(UpsertOutcome.MERGED, upsertOrMergeInPlace(library, alternate, target(alternate)).outcome)
        val merged = library.single()
        assertEquals(original.id, merged.id)
        assertEquals(original.imageUri, merged.imageUri)
        assertEquals(original.lastPlayedAtEpochMillis, merged.lastPlayedAtEpochMillis)
        assertEquals(2, merged.allTargets().size)
    }
}