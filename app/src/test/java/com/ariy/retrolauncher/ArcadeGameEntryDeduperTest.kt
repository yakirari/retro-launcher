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
        val entries = listOf(game("1", "R-Type", "rtypeu.zip"), game("2", "R-Type II", "rtype2.zip"), game("3", "R-Type", "rtypeu.zip").copy(platform = "Final Burn Neo"))
        assertEquals(entries, dedupeArcadeGameEntries(entries))
    }
    @Test fun copySuffixAndRawRomTitleDuplicatesCollapse() {
        assertEquals(1, dedupeArcadeGameEntries(listOf(game("1", "dkong3", "dkong3.zip"), game("2", "Donkey Kong 3", "dkong3 (1).zip"))).size)
    }
}