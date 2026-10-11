package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.ui.libraryDisplayTitle
import org.junit.Assert.*
import org.junit.Test

class ScummVmGamesTest {
    @Test fun humanTitlesReplaceOnlyKnownKeys() {
        assertEquals("Day of the Tentacle", ScummVmGames.title("DOT"))
        assertEquals("Beneath a Steel Sky", ScummVmGames.title("BeneathS"))
        assertEquals("Monkey Island 2: LeChuck's Revenge", ScummVmGames.title("scumm:monkey2"))
        assertEquals("Sam & Max Hit the Road", entry("samnmax").libraryDisplayTitle())
        assertEquals("My favorite adventure", entry("My favorite adventure").libraryDisplayTitle())
        assertEquals("My favorite: monkey", entry("My favorite: monkey").libraryDisplayTitle())
        assertNull(ScummVmGames.title("monkeyupdate"))
    }
    @Test fun markerContentsBeatBogusExecutableMarkerNames() {
        assertEquals("emi", ScummVmGames.detect("EMI", listOf("monkeyupdate.svm" to "", "EMI.scummvm" to "EMI")))
        assertEquals("tentacle", ScummVmGames.detect("DOT", listOf("DOT.scummvm" to "tentacle", "dott.svm" to "", "tentacle.svm" to "")))
        assertEquals("monkey2", ScummVmGames.detect("folder", listOf("launcher.scummvm" to "scumm:monkey2")))
        assertNull(ScummVmGames.detect("unknown", listOf("monkey.svm" to "", "monkey2.svm" to "")))
    }
    @Test fun knownDataPairsWorkWithoutMarkersButIncompleteUnknownFoldersDoNot() {
        assertEquals("sky", ScummVmGames.detect("adventure", listOf("SKY.DNR" to null, "sky.dsk" to null)))
        assertEquals("monkey2", ScummVmGames.detect("adventure", listOf("MONKEY2.000" to null, "MONKEY2.001" to null)))
        assertNull(ScummVmGames.detect("unknown", listOf("DIG.LA0" to null)))
        assertNull(ScummVmGames.detect("monkey", emptyList()))
    }
    @Test fun registeredTargetsPreserveExactConfigSectionNotEngineId() {
        val games = ScummVmGames.parseConfig("""
            [scummvm]
            description=Global
            [monkey2-fr]
            gameid=monkey2
            engineid=scumm
            description=Monkey Island 2 (DOS/French)
            path=/saf/ROM/Monkey2
            [monkey-ega-demo]
            gameid=monkey
            description=The Secret of Monkey Island (EGA Demo/DOS/English)
            path=/demos/monkey
            [missing-data]
            description=Not a game
        """.trimIndent())
        assertEquals(listOf("monkey2-fr", "monkey-ega-demo"), games.map { it.target })
        assertEquals("monkey2", games.first().gameId)
        val extras = ScummVmGames.extrasForTarget("monkey2-fr")
        assertEquals("monkey2-fr", ScummVmGames.registeredTarget(extras))
        assertNull(ScummVmGames.registeredTarget("""{"scummvmGameId":"monkey2"}"""))
        assertFalse(ScummVmGames.validTarget("--path=/games monkey2"))
    }
    @Test fun editionSuffixesAndArticleOrderMatchOnlyTheSameGameCover() {
        val pairs = listOf(
            "monkey" to "Secret of Monkey Island, The",
            "monkey2" to "Monkey Island 2_ LeChuck's Revenge (DOS)",
            "emi" to "Escape From Monkey Island (Windows_English)",
            "grim" to "Grim Fandango (Windows_English)",
            "samnmax" to "Sam _ Max Hit the Road (CD_English)",
            "tlj" to "The Longest Journey (GOG.com_Windows_English)",
            "sky" to "Beneath a Steel Sky (v0.0368 CD_DOS)"
        )
        pairs.forEach { (id, title) -> assertEquals(id, LibretroArtworkCatalog.scummVmArtworkMatchKey(id), LibretroArtworkCatalog.scummVmArtworkMatchKey(title)) }
        assertNotEquals(LibretroArtworkCatalog.scummVmArtworkMatchKey("monkey"), LibretroArtworkCatalog.scummVmArtworkMatchKey("monkey2"))
    }
    @Test fun legacyRepairPreservesHistoryAndManualArtAndIsIdempotent() {
        val old = entry("DOT").copy(id = "test", imageUri = "file:///test-autofill.png", playCount = 4, lastPlayedAtEpochMillis = 99)
        val repaired = ScummVmGames.repairTitle(old)
        assertEquals("Day of the Tentacle", repaired.title)
        assertNull(repaired.imageUri)
        assertEquals(4L, repaired.playCount)
        assertEquals(99L, repaired.lastPlayedAtEpochMillis)
        assertEquals(repaired, ScummVmGames.repairTitle(repaired))
        assertEquals("file:///manual.png", ScummVmGames.repairTitle(old.copy(imageUri = "file:///manual.png")).imageUri)
    }
    @Test fun registeringRepairsExistingTileButNeverCollapsesDifferentRegisteredEditions() {
        val old = entry("monkey2").copy(id = "retained", intentDataUri = "content://games/Monkey2", playCount = 5)
        val registered = entry("Monkey Island 2: LeChuck's Revenge").copy(extrasJson = ScummVmGames.extrasForTarget("monkey2-fr"))
        val merged = requireNotNull(ScummVmGames.mergeRegistered(old, registered))
        assertEquals("retained", merged.id)
        assertEquals(5L, merged.playCount)
        assertEquals(old.intentDataUri, merged.intentDataUri)
        assertEquals("monkey2-fr", ScummVmGames.registeredTarget(merged.extrasJson))
        assertEquals(merged, ScummVmGames.mergeRegistered(merged, registered))
        assertNull(ScummVmGames.mergeRegistered(merged, registered.copy(extrasJson = ScummVmGames.extrasForTarget("monkey2-en"))))
    }

    private fun entry(title: String) = GameEntry(title = title, platform = "ScummVM", packageName = ScummVmGames.PACKAGE,
        sourceType = GameSourceType.CUSTOM_INTENT)
}