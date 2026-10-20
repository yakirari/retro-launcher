package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class MainGalleryGamesTest {
    @Test fun sharedPreparedListExcludesRealEmulatorTilesForBothGridAndGallery() {
        val emulators = listOf("AetherSX2" to "xyz.aethersx2.android", "Dolphin Emulator" to "org.dolphinemu.dolphinemu",
            "DuckStation" to "com.github.stenzek.duckstation", "PPSSPP" to "org.ppsspp.ppsspp").map { (title, pkg) ->
            GameEntry(title = title, packageName = pkg, platform = GamePlatforms.EMULATORS, sourceType = GameSourceType.ACTIVITY)
        }
        val ps2Game = GameEntry(title = "Bully", platform = "Sony PlayStation 2",
            sourceType = GameSourceType.CUSTOM_INTENT, packageName = "xyz.aethersx2.android")
        val entries = emulators + ps2Game
        for (filter in listOf(GameFilters.ALL, GameFilters.ALL.lowercase())) {
            val prepared = com.ariy.retrolauncher.orderedGamesForFilter(entries, filter, GameSortOrder.ALPHABETICAL)
            assertEquals(listOf(ps2Game), prepared)
            assertEquals(prepared, mainGalleryGames(prepared, filter))
        }
        assertEquals(emulators.map { it.title }.sorted(), com.ariy.retrolauncher.orderedGamesForFilter(
            entries, GamePlatforms.EMULATORS, GameSortOrder.ALPHABETICAL).map { it.title })
        assertEquals(listOf(ps2Game), com.ariy.retrolauncher.orderedGamesForFilter(
            entries, "Sony PlayStation 2", GameSortOrder.ALPHABETICAL))
        assertEquals(emptyList<GameEntry>(), com.ariy.retrolauncher.orderedGamesForFilter(
            emulators, GameFilters.ALL, GameSortOrder.ALPHABETICAL))
    }

    @Test fun mixedViewHidesEmulatorsRegardlessOfLaunchSourceButKeepsTheirGallery() {
        val emulator = GameEntry(title = "DuckStation", platform = GamePlatforms.EMULATORS,
            sourceType = GameSourceType.SHORTCUT)
        val game = GameEntry(title = "Ridge Racer", platform = "Sony PlayStation",
            sourceType = GameSourceType.SHORTCUT)
        val entries = listOf(emulator, game)
        assertEquals(listOf(game), mainGalleryGames(entries, GameFilters.ALL))
        assertEquals(listOf(game), mainGalleryGames(entries, GameFilters.ALL.lowercase()))
        assertEquals(entries, mainGalleryGames(entries, GamePlatforms.EMULATORS))
        assertEquals(entries, mainGalleryGames(entries, "Favorites"))
    }

    @Test fun mainGalleryHidesStandaloneAppsButPreservesGameShortcutsAndAndroidView() {
        val app = GameEntry(title = "GameHub", platform = "Android", sourceType = GameSourceType.ACTIVITY)
        val game = app.copy(title = "The Cave", platform = "GameHub Lite", sourceType = GameSourceType.SHORTCUT)
        val entries = listOf(app, game)
        assertEquals(listOf(game), mainGalleryGames(entries, GameFilters.ALL))
        assertEquals(entries, mainGalleryGames(entries, "Android"))
        assertEquals(entries, mainGalleryGames(entries, "Favorites"))
    }
}