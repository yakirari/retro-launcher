package com.ariy.retrolauncher.model

import org.junit.Assert.*
import org.junit.Test

class GlobalGallerySettingsTest {
    @Test fun namedSizesMapExistingPreferencesWithoutResettingOtherSettings() {
        val sizes = mapOf(1 to GalleryTileSize.LARGE, 2 to GalleryTileSize.MEDIUM,
            3 to GalleryTileSize.MEDIUM, 4 to GalleryTileSize.SMALL, 8 to GalleryTileSize.SMALL)
        for (preset in listOf(GlobalGallerySettings.GRID, GlobalGallerySettings.STACKED, GlobalGallerySettings.SHELF)) {
            for ((count, size) in sizes) {
                val old = GlobalGallerySettings(tileBackgroundMode = "glossy", showGameCaptions = false)
                    .selectPreset(preset).withTileCount(count)
                val loaded = GlobalGallerySettings.fromPreferenceValues(old.toPreferenceValues())
                assertEquals(size, loaded.tileSize)
                assertEquals(old, loaded)
            }
        }
        assertEquals(GalleryTileSize.MEDIUM, GlobalGallerySettings().tileSize)
        assertEquals(GalleryTileSize.SMALL, GlobalGallerySettings().selectPreset(GlobalGallerySettings.STACKED).tileSize)
    }

    @Test fun eachLayoutRemembersNamedSizesAcrossReloads() {
        val settings = GlobalGallerySettings().withTileSize(GalleryTileSize.LARGE)
            .selectPreset(GlobalGallerySettings.STACKED).withTileSize(GalleryTileSize.MEDIUM)
            .selectPreset(GlobalGallerySettings.SHELF).withTileSize(GalleryTileSize.SMALL)
        val loaded = GlobalGallerySettings.fromPreferenceValues(settings.toPreferenceValues())
        assertEquals(GalleryTileSize.LARGE, loaded.selectPreset(GlobalGallerySettings.GRID).tileSize)
        assertEquals(GalleryTileSize.MEDIUM, loaded.selectPreset(GlobalGallerySettings.STACKED).tileSize)
        assertEquals(GalleryTileSize.SMALL, loaded.selectPreset(GlobalGallerySettings.SHELF).tileSize)
        assertEquals(listOf("Small", "Medium", "Large"), GalleryTileSize.entries.map { it.label })
        assertEquals(listOf(3, 2, 1), GalleryTileSize.entries.map { it.minimumVisibleItems })
    }

    @Test fun freshDefaultsAreGridTwoTilesAndTransparent() {
        val settings = GlobalGallerySettings.fromPreferenceValues(emptyMap<String, Any>())
        assertEquals(GlobalGallerySettings.GRID, settings.layoutMode)
        assertEquals(2, settings.tileCount)
        assertEquals(2, settings.selectPreset(GlobalGallerySettings.SHELF).tileCount)
        assertEquals("transparent", settings.tileBackgroundMode)
        assertTrue(settings.showGameCaptions)
    }

    @Test fun firstUseResetsLegacyPresentationButPreservesCaptionsAndLibrary() {
        val legacy = mapOf(
            "launcher_layout_mode" to "gallerystalgic",
            "launcher_grid_columns" to 8,
            "launcher_tile_background" to "glossy",
            "platform_display_settings_v1" to """{"Nintendo Switch":{"layoutMode":"gallery","gridColumns":3}}""",
            "show_game_captions" to false,
            "games" to "existing library",
            "launcher_background" to "custom",
            // Partially written preset values without a marker must not bypass migration.
            GlobalGallerySettings.PRESET_KEY to GlobalGallerySettings.SHELF,
            GlobalGallerySettings.GRID_COUNT_KEY to 4
        )
        assertTrue(GlobalGallerySettings.needsMigration(legacy))
        val settings = GlobalGallerySettings.fromPreferenceValues(legacy)
        assertEquals(GlobalGallerySettings(showGameCaptions = false), settings)
        val migrated = legacy + settings.toPreferenceValues()
        assertFalse(GlobalGallerySettings.needsMigration(migrated))
        assertEquals(settings, GlobalGallerySettings.fromPreferenceValues(migrated))
        legacy.forEach { (key, value) ->
            if (key !in settings.toPreferenceValues()) assertEquals(value, migrated[key])
        }
    }

    @Test fun eachPresetRemembersIndependentCountsAcrossSwitchesAndReloads() {
        val grid = GlobalGallerySettings().withTileCount(8)
        val shelf = grid.selectPreset(GlobalGallerySettings.SHELF).withTileCount(3)
        assertEquals(3, shelf.tileCount)
        val reloaded = GlobalGallerySettings.fromPreferenceValues(shelf.toPreferenceValues())
        assertEquals(shelf, reloaded)
        assertEquals(8, reloaded.selectPreset(GlobalGallerySettings.GRID).tileCount)
        val changedGrid = reloaded.selectPreset(GlobalGallerySettings.GRID).withTileCount(1)
        assertEquals(1, changedGrid.tileCount)
        assertEquals(3, changedGrid.selectPreset(GlobalGallerySettings.SHELF).tileCount)
    }

    @Test fun onlyRequestedCountsAndSupportedPresetsAreAccepted() {
        assertEquals(listOf(1, 2, 3, 4, 8), GlobalGallerySettings.ALLOWED_TILE_COUNTS)
        for (preset in listOf(GlobalGallerySettings.GRID, GlobalGallerySettings.SHELF, GlobalGallerySettings.STACKED)) {
            for (count in GlobalGallerySettings.ALLOWED_TILE_COUNTS) {
                val settings = GlobalGallerySettings().selectPreset(preset).withTileCount(count)
                assertEquals(count, settings.tileCount)
                assertEquals(settings, GlobalGallerySettings.fromPreferenceValues(settings.toPreferenceValues()))
            }
            for (count in listOf(-1, 0, 5, 6, 7, 9, Int.MAX_VALUE)) {
                assertEquals(2, GlobalGallerySettings().selectPreset(preset).withTileCount(count).tileCount)
            }
        }
        for (mode in listOf("grid", "gallery", "nostalgic", "samestalgic", "gallerystalgic", "unknown")) {
            assertEquals(GlobalGallerySettings.GRID, GlobalGallerySettings().selectPreset(mode).layoutMode)
        }
    }

    @Test fun stackedRemembersItsOwnColumnsWithoutMigratingExistingSettings() {
        val old = GlobalGallerySettings().withTileCount(3).selectPreset(GlobalGallerySettings.SHELF).withTileCount(8)
        val oldValues = old.toPreferenceValues() - GlobalGallerySettings.STACKED_COUNT_KEY
        assertFalse(GlobalGallerySettings.needsMigration(oldValues))
        val stacked = GlobalGallerySettings.fromPreferenceValues(oldValues).selectPreset(GlobalGallerySettings.STACKED)
        assertEquals(4, stacked.tileCount)
        val saved = stacked.withTileCount(2)
        val loaded = GlobalGallerySettings.fromPreferenceValues(saved.toPreferenceValues())
        assertEquals(saved, loaded)
        assertEquals(2, loaded.tileCount)
        assertEquals(3, loaded.selectPreset(GlobalGallerySettings.GRID).tileCount)
        assertEquals(8, loaded.selectPreset(GlobalGallerySettings.SHELF).tileCount)
    }

    @Test fun malformedOrMissingSavedValuesFallBackIndependently() {
        val invalid = mapOf(
            GlobalGallerySettings.VERSION_KEY to GlobalGallerySettings.VERSION,
            GlobalGallerySettings.PRESET_KEY to "gallery",
            GlobalGallerySettings.GRID_COUNT_KEY to 99,
            GlobalGallerySettings.SHELF_COUNT_KEY to "4",
            GlobalGallerySettings.BACKGROUND_KEY to false,
            GlobalGallerySettings.CAPTIONS_KEY to "false"
        )
        assertEquals(GlobalGallerySettings(), GlobalGallerySettings.fromPreferenceValues(invalid))
        assertEquals(GlobalGallerySettings(), GlobalGallerySettings.fromPreferenceValues(mapOf(
            GlobalGallerySettings.VERSION_KEY to GlobalGallerySettings.VERSION
        )))
        val validShelf = invalid + mapOf(
            GlobalGallerySettings.PRESET_KEY to GlobalGallerySettings.SHELF,
            GlobalGallerySettings.SHELF_COUNT_KEY to 4,
            GlobalGallerySettings.BACKGROUND_KEY to "invalid"
        )
        assertEquals(GlobalGallerySettings(GlobalGallerySettings.SHELF, 2, 4),
            GlobalGallerySettings.fromPreferenceValues(validShelf))
        assertTrue(GlobalGallerySettings.needsMigration(mapOf(GlobalGallerySettings.VERSION_KEY to "1")))
    }

    @Test fun globalBackgroundAndCaptionsSurvivePresetSwitchesAndIgnorePlatformProfiles() {
        for (background in listOf("normal", "transparent", "glossy")) {
            val settings = GlobalGallerySettings(tileBackgroundMode = background, showGameCaptions = false)
                .withTileCount(4).selectPreset(GlobalGallerySettings.SHELF).withTileCount(1)
            for (platform in listOf("All", "Nintendo Switch", "MAME Arcade", "PC (Windows)", "Favorites")) {
                val values = settings.toPreferenceValues() + mapOf(
                    "genre_filter" to platform,
                    "platform_display_settings_v1" to """{"$platform":{"layoutMode":"gallery","gridColumns":8,"tileBackgroundMode":"normal","showGameCaptions":true}}""",
                    "launcher_grid_columns" to 8
                )
                val reloaded = GlobalGallerySettings.fromPreferenceValues(values)
                assertEquals(platform, settings, reloaded)
                val grid = reloaded.selectPreset(GlobalGallerySettings.GRID)
                assertEquals(4, grid.tileCount)
                assertEquals(background, grid.tileBackgroundMode)
                assertFalse(grid.showGameCaptions)
            }
        }
    }
}