package com.ariy.retrolauncher.model

/** Global presentation only: platform filters never participate in resolving these settings. */
data class GlobalGallerySettings(
    val layoutMode: String = GRID,
    val gridTileCount: Int = DEFAULT_TILE_COUNT,
    val shelfTileCount: Int = DEFAULT_TILE_COUNT,
    val tileBackgroundMode: String = "transparent",
    val showGameCaptions: Boolean = true
) {
    val tileCount: Int
        get() = if (layoutMode == SHELF) shelfTileCount else gridTileCount

    fun validated(): GlobalGallerySettings = copy(
        layoutMode = layoutMode.takeIf { it == GRID || it == SHELF } ?: GRID,
        gridTileCount = validTileCount(gridTileCount),
        shelfTileCount = validTileCount(shelfTileCount),
        tileBackgroundMode = tileBackgroundMode.takeIf { it in setOf("normal", "transparent", "glossy") }
            ?: "transparent"
    )

    fun selectPreset(mode: String): GlobalGallerySettings = copy(layoutMode = mode).validated()

    fun withTileCount(count: Int): GlobalGallerySettings {
        val settings = validated()
        return if (settings.layoutMode == SHELF) settings.copy(shelfTileCount = validTileCount(count))
        else settings.copy(gridTileCount = validTileCount(count))
    }

    /** Complete snapshot, including the marker, written atomically by the preferences adapter. */
    fun toPreferenceValues(): Map<String, Any> = validated().let { settings ->
        mapOf(
            VERSION_KEY to VERSION,
            PRESET_KEY to settings.layoutMode,
            GRID_COUNT_KEY to settings.gridTileCount,
            SHELF_COUNT_KEY to settings.shelfTileCount,
            BACKGROUND_KEY to settings.tileBackgroundMode,
            CAPTIONS_KEY to settings.showGameCaptions
        )
    }

    companion object {
        const val GRID = "gridstalgic"
        const val SHELF = "shelfstalgic"
        const val DEFAULT_TILE_COUNT = 2
        val ALLOWED_TILE_COUNTS = listOf(1, 2, 3, 4, 8)
        const val VERSION = 1
        const val VERSION_KEY = "global_gallery_settings_version"
        const val PRESET_KEY = "global_gallery_preset"
        const val GRID_COUNT_KEY = "global_gallery_grid_tile_count"
        const val SHELF_COUNT_KEY = "global_gallery_shelf_tile_count"
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
                tileBackgroundMode = values[BACKGROUND_KEY] as? String ?: "transparent",
                showGameCaptions = values[CAPTIONS_KEY] as? Boolean ?: true
            ).validated()
        }
    }
}