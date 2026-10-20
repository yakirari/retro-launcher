package com.ariy.retrolauncher.ui

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.orderedGamesForFilter
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class MainGalleryFilteringTest {
    @Test fun sharedGridAndGalleryPreparationHidesSavedEmulatorsWithoutRemovingGames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = JSONArray(context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE)
            .getString("games", "[]"))
        val saved = (0 until json.length()).map { GameEntry.fromJson(json.getJSONObject(it)) }
        val entries = saved + GameEntry(id = "filter-regression-emulator", title = "AetherSX2",
            packageName = "xyz.aethersx2.android", platform = GamePlatforms.EMULATORS, sourceType = GameSourceType.ACTIVITY)
        val mixed = orderedGamesForFilter(entries, GameFilters.ALL, GameSortOrder.ALPHABETICAL)
        assertFalse(mixed.any { it.sourceType == GameSourceType.ACTIVITY || it.platform == GamePlatforms.EMULATORS })
        assertEquals(entries.filterNot { it.sourceType == GameSourceType.ACTIVITY || it.platform == GamePlatforms.EMULATORS }
            .map { it.id }.toSet(), mixed.map { it.id }.toSet())
        val dedicated = orderedGamesForFilter(entries, GamePlatforms.EMULATORS, GameSortOrder.ALPHABETICAL)
        assertTrue(dedicated.any { it.id == "filter-regression-emulator" })
        assertTrue(dedicated.containsAll(saved.filter { it.platform == GamePlatforms.EMULATORS }))
    }
}