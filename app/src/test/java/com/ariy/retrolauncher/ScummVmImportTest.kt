package com.ariy.retrolauncher

import com.ariy.retrolauncher.launch.ScummVmGames
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.LaunchTarget
import org.junit.Assert.*
import org.junit.Test

class ScummVmImportTest {
    @Test fun batchAndSingleImportsRepairLegacyPrimaryTargetAndAreIdempotent() {
        val old = GameEntry(id = "keep", title = "monkey2", platform = "ScummVM", sourceType = GameSourceType.CUSTOM_INTENT,
            packageName = ScummVmGames.PACKAGE, intentDataUri = "content://games/Monkey2", playCount = 8)
        val incoming = old.copy(id = "new", title = "Monkey Island 2: LeChuck's Revenge", intentDataUri = "scummvm:monkey2-fr",
            extrasJson = ScummVmGames.extrasForTarget("monkey2-fr"), playCount = 0)
        val target = LaunchTarget(sourceType = GameSourceType.CUSTOM_INTENT, packageName = incoming.packageName,
            intentDataUri = incoming.intentDataUri, extrasJson = incoming.extrasJson)
        val library = mutableListOf(old)
        assertEquals(UpsertOutcome.MERGED, upsertOrMergeInPlace(library, incoming, target).outcome)
        assertEquals(1, library.size)
        assertEquals("keep", library.single().id)
        assertEquals(8L, library.single().playCount)
        assertEquals("monkey2-fr", ScummVmGames.registeredTarget(library.single().extrasJson))
        assertEquals(UpsertOutcome.DUPLICATE, upsertOrMergeInPlace(library, incoming, target).outcome)
    }
}