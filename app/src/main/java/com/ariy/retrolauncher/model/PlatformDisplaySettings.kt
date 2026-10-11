package com.ariy.retrolauncher.model

import org.json.JSONObject

data class PlatformDisplaySettings(
    val layoutMode: String = "grid",
    val gridColumns: Int = 4,
    val tileBackgroundMode: String = "normal",
    val showGameCaptions: Boolean = true
)

fun platformDisplayDefaults(platform: String?): PlatformDisplaySettings = when (GamePlatforms.normalizeOrNull(platform)) {
    in PcGameStores.platforms, "PC (Windows)" -> PlatformDisplaySettings("gallerystalgic", 3, "transparent")
    "MAME Arcade", "Final Burn Neo" -> PlatformDisplaySettings("gridstalgic", 2, "transparent")
    "Game Boy", "Game Boy Color", "Game Boy Advance", "Nintendo 64" -> PlatformDisplaySettings("gridstalgic", 3, "transparent")
    "Sony PlayStation", "Sony PlayStation 2", "Sony PlayStation Portable", "Nintendo Wii" -> PlatformDisplaySettings("gridstalgic", 3, "transparent")
    "Nintendo Switch" -> PlatformDisplaySettings("gallery", 3, "normal")
    else -> PlatformDisplaySettings()
}

private val layouts = setOf("grid", "gridstalgic", "gallery", "nostalgic", "shelfstalgic", "samestalgic", "gallerystalgic")
fun PlatformDisplaySettings.validated(defaults: PlatformDisplaySettings = PlatformDisplaySettings()) = copy(
    layoutMode = layoutMode.takeIf { it in layouts } ?: defaults.layoutMode,
    gridColumns = gridColumns.takeIf { it in setOf(1, 2, 3, 4, 8) } ?: defaults.gridColumns,
    tileBackgroundMode = tileBackgroundMode.takeIf { it in setOf("normal", "transparent", "glossy") } ?: defaults.tileBackgroundMode
)

fun parsePlatformDisplaySettings(raw: String?): Map<String, PlatformDisplaySettings> {
    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return emptyMap()
    return buildMap {
        json.keys().forEach { key ->
            val platform = GamePlatforms.normalizeOrNull(key) ?: return@forEach
            val row = json.optJSONObject(key) ?: return@forEach
            val defaults = platformDisplayDefaults(platform)
            put(platform, PlatformDisplaySettings(
                row.optString("layoutMode", defaults.layoutMode), row.optInt("gridColumns", defaults.gridColumns),
                row.optString("tileBackgroundMode", defaults.tileBackgroundMode), row.optBoolean("showGameCaptions", true)
            ).validated(defaults))
        }
    }
}

fun Map<String, PlatformDisplaySettings>.toDisplaySettingsJson(): String = JSONObject().apply {
    forEach { (platform, value) ->
        val setting = value.validated(platformDisplayDefaults(platform))
        put(platform, JSONObject().apply {
            put("layoutMode", setting.layoutMode); put("gridColumns", setting.gridColumns)
            put("tileBackgroundMode", setting.tileBackgroundMode); put("showGameCaptions", setting.showGameCaptions)
        })
    }
}.toString()