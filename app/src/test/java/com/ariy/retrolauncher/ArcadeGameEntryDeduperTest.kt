package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class ArcadeGameEntryDeduperTest {
    private fun game(id: String, title: String, rom: String) = GameEntry(
        id = id, title = title, platform = "MAME Arcade", sourceType = GameSourceType.CUSTOM_INTENT,
        intentDataUri = "content://rom/$rom", packageName = "org.libretro.retroarch"
    )
    @Test fun sameRomUnderDifferentTitlesPreservesMetadataHistoryAndTargets() {
        val original = game("old", "My favorite", "ddragonu.zip").copy(imageUri = "file://manual", genre = "Favorites", lastPlayedAtEpochMillis = 10)
        val duplicate = game("new", "Double Dragon", "ddragon.zip").copy(lastPlayedAtEpochMillis = 20)
        val merged = dedupeArcadeGameEntries(listOf(original, duplicate)).single()
        assertEquals("old", merged.id)
        assertEquals("My favorite", merged.title)
        assertEquals("file://manual", merged.imageUri)
        assertEquals("Favorites", merged.genre)
        assertEquals(20L, merged.lastPlayedAtEpochMillis)
        assertEquals(2, merged.allTargets().size)
        assertEquals(listOf(merged), dedupeArcadeGameEntries(listOf(merged)))
    }
    @Test fun sequelsAndPlatformsStaySeparateAndOrderIsStable() {
        val entries = listOf(game("1", "R-Type", "mame/rtypeu.zip"), game("2", "R-Type II", "mame/rtype2.zip"), game("3", "R-Type", "fbneo/rtypeu.zip").copy(platform = "Final Burn Neo"))
        assertEquals(entries, dedupeArcadeGameEntries(entries))
    }
    @Test fun samePhysicalArcadeFileAcrossEmulatorsRepairsDoubleImportAndPreservesUserData() {
        val original = game("old", "My 1941", "arcade/1941.zip").copy(imageUri = "file://manual", playCount = 3)
        val duplicate = game("new", "1941", "arcade/1941.zip").copy(platform = "Final Burn Neo",
            packageName = "com.example.fbneo", playCount = 2, lastPlayedAtEpochMillis = 42)
        val merged = dedupeArcadeGameEntries(listOf(original, duplicate)).single()
        assertEquals(original.id, merged.id)
        assertEquals(original.title, merged.title)
        assertEquals(original.imageUri, merged.imageUri)
        assertEquals(original.platform, merged.platform)
        assertEquals(5L, merged.playCount)
        assertEquals(42L, merged.lastPlayedAtEpochMillis)
        assertEquals(2, merged.allTargets().size)
        assertEquals(listOf(merged), dedupeArcadeGameEntries(listOf(merged)))
    }
    @Test fun exactFileMergeRepairsPlaceholderButDoesNotMergeDifferentProfiles() {
        val pending = game("pending", "My 1941", "arcade/1941.zip").copy(packageName = null, needsLaunchSetup = true)
        val ready = game("ready", "1941", "arcade/1941.zip").copy(platform = "Final Burn Neo", packageName = "fbneo")
        val merged = dedupeArcadeGameEntries(listOf(pending, ready)).single()
        assertFalse(merged.needsLaunchSetup)
        assertEquals("fbneo", merged.packageName)
        assertEquals(pending.id, merged.id)
        assertEquals(2, dedupeArcadeGameEntries(listOf(pending, ready.copy(userSerial = 10))).size)
    }
    @Test fun copySuffixAndRawRomTitleDuplicatesCollapse() {
        assertEquals(1, dedupeArcadeGameEntries(listOf(game("1", "dkong3", "dkong3.zip"), game("2", "Donkey Kong 3", "dkong3 (1).zip"))).size)
    }
}