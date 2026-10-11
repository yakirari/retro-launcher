package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class MainGalleryGamesTest {
    @Test fun mainGalleryHidesStandaloneAppsButPreservesGameShortcutsAndAndroidView() {
        val app = GameEntry(title = "GameHub", platform = "Android", sourceType = GameSourceType.ACTIVITY)
        val game = app.copy(title = "The Cave", platform = "GameHub Lite", sourceType = GameSourceType.SHORTCUT)
        val entries = listOf(app, game)
        assertEquals(listOf(game), mainGalleryGames(entries, GameFilters.ALL))
        assertEquals(entries, mainGalleryGames(entries, "Android"))
        assertEquals(entries, mainGalleryGames(entries, "Favorites"))
    }
}