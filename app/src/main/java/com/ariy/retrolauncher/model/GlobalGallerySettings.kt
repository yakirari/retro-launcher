package com.ariy.retrolauncher.model

enum class GalleryTileSize(val label: String, val minimumVisibleItems: Int, val legacyCount: Int) {
    SMALL("Small", 3, 4),
    MEDIUM("Medium", 2, 2),
    LARGE("Large", 1, 1);

    companion object {
        fun fromLegacyCount(count: Int): GalleryTileSize = when (count) {
            1 -> LARGE
            4, 8 -> SMALL
            else -> MEDIUM
        }
    }
}

/** Global presentation only: platform filters never participate in resolving these settings. */
data class GlobalGallerySettings(
    val layoutMode: String = GRID,
    val gridTileCount: Int = DEFAULT_TILE_COUNT,
    val shelfTileCount: Int = DEFAULT_TILE_COUNT,
    val tileBackgroundMode: String = "transparent",
    val showGameCaptions: Boolean = true,
    val stackedTileCount: Int = 4
) {
    val tileCount: Int
        get() = when (layoutMode) { SHELF -> shelfTileCount; STACKED -> stackedTileCount; else -> gridTileCount }

    // Keep the existing per-layout preference keys compatible with installed versions.
    val tileSize: GalleryTileSize
        get() = GalleryTileSize.fromLegacyCount(tileCount)

    fun withTileSize(size: GalleryTileSize): GlobalGallerySettings = withTileCount(size.legacyCount)

    fun validated(): GlobalGallerySettings = copy(
        layoutMode = layoutMode.takeIf { it in setOf(GRID, SHELF, STACKED) } ?: GRID,
        gridTileCount = validTileCount(gridTileCount),
        shelfTileCount = validTileCount(shelfTileCount),
        stackedTileCount = validTileCount(stackedTileCount),
        tileBackgroundMode = tileBackgroundMode.takeIf { it in setOf("normal", "transparent", "glossy") }
            ?: "transparent"
    )

    fun selectPreset(mode: String): GlobalGallerySettings = copy(layoutMode = mode).validated()

    fun withTileCount(count: Int): GlobalGallerySettings {
        val settings = validated()
        return when (settings.layoutMode) {
            SHELF -> settings.copy(shelfTileCount = validTileCount(count))
            STACKED -> settings.copy(stackedTileCount = validTileCount(count))
            else -> settings.copy(gridTileCount = validTileCount(count))
        }
    }

    /** Complete snapshot, including the marker, written atomically by the preferences adapter. */
    fun toPreferenceValues(): Map<String, Any> = validated().let { settings ->
        mapOf(
            VERSION_KEY to VERSION,
            PRESET_KEY to settings.layoutMode,
            GRID_COUNT_KEY to settings.gridTileCount,
            SHELF_COUNT_KEY to settings.shelfTileCount,
            STACKED_COUNT_KEY to settings.stackedTileCount,
            BACKGROUND_KEY to settings.tileBackgroundMode,
            CAPTIONS_KEY to settings.showGameCaptions
        )
    }

    companion object {
        const val GRID = "gridstalgic"
        const val SHELF = "shelfstalgic"
        const val STACKED = "stacked"
        const val DEFAULT_TILE_COUNT = 2
        val ALLOWED_TILE_COUNTS = listOf(1, 2, 3, 4, 8)
        const val VERSION = 1
        const val VERSION_KEY = "global_gallery_settings_version"
        const val PRESET_KEY = "global_gallery_preset"
        const val GRID_COUNT_KEY = "global_gallery_grid_tile_count"
        const val SHELF_COUNT_KEY = "global_gallery_shelf_tile_count"
        const val STACKED_COUNT_KEY = "global_gallery_stacked_tile_count"
        const val BACKGROUND_KEY = "global_gallery_tile_background"
        const val CAPTIONS_KEY = "global_gallery_show_captions"

        private fun validTileCount(count: Int): Int = count.takeIf { it in ALLOWED_TILE_COUNTS } ?: DEFAULT_TILE_COUNT

        fun needsMigration(values: Map<String, *>): Boolean = values[VERSION_KEY] != VERSION

        /** First use intentionally resets legacy layouts/counts/backgrounds, not library or captions. */
        fun fromPreferenceValues(values: Map<String, *>): GlobalGallerySettings {
            if (needsMigration(values)) {
                return GlobalGallerySettings(showGameCaptions = values["show_game_captions"] as? Boolean ?: true)
            }
            return GlobalGallerySettings(
                layoutMode = values[PRESET_KEY] as? String ?: GRID,
                gridTileCount = values[GRID_COUNT_KEY] as? Int ?: DEFAULT_TILE_COUNT,
                shelfTileCount = values[SHELF_COUNT_KEY] as? Int ?: DEFAULT_TILE_COUNT,
                stackedTileCount = values[STACKED_COUNT_KEY] as? Int ?: 4,
                tileBackgroundMode = values[BACKGROUND_KEY] as? String ?: "transparent",
                showGameCaptions = values[CAPTIONS_KEY] as? Boolean ?: true
            ).validated()
        }
    }
}