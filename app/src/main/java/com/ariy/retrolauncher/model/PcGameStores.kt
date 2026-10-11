package com.ariy.retrolauncher.model

import org.json.JSONObject

object PcGameStores {
    val platforms = setOf("Steam", "GOG", "Epic Games", "Amazon Games", "GameNative", "GameHub", "GameHub Lite")
    fun usesShortcutAdding(platform: String?): Boolean = GamePlatforms.normalizeOrNull(platform) in platforms
    val hubPackages = mapOf("com.xiaoji.egggame" to "GameHub", "gamehub.lite" to "GameHub Lite")
    fun fromAppLabel(label: String): String? = when (label.lowercase().replace(" ", "")) {
        "gamenative" -> "GameNative"
        "gamehub" -> "GameHub"
        "gamehublite" -> "GameHub Lite"
        else -> null
    }
    fun forShortcut(option: com.ariy.retrolauncher.launch.InstalledShortcutOption): String? =
        if (option.packageName == "app.gamenative") fromExtras(option.extrasJson) ?: "GameNative"
        else hubPackages[option.packageName] ?: fromAppLabel(option.sourceLabel)
    fun fromExtras(raw: String?): String? = runCatching {
        when (JSONObject(raw.orEmpty()).optString("game_source")) {
            "STEAM" -> "Steam"
            "GOG" -> "GOG"
            "EPIC" -> "Epic Games"
            "AMAZON" -> "Amazon Games"
            else -> null
        }
    }.getOrNull()
    fun classify(game: GameEntry): GameEntry {
        if (game.sourceType == GameSourceType.SHORTCUT && game.packageName in hubPackages &&
            (game.platform == null || game.platform == "PC (Windows)" || game.platform == "Android")) {
            return game.copy(platform = hubPackages[game.packageName])
        }
        if (game.sourceType == GameSourceType.ACTIVITY || game.packageName != "app.gamenative") return game
        if (game.platform !in platforms && game.platform != "PC (Windows)") return game
        return game.copy(platform = fromExtras(game.extrasJson)
            ?: game.platform?.takeIf { it in platforms }
            ?: "GameNative")
    }
}