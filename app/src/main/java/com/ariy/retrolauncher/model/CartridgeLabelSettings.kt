package com.ariy.retrolauncher.model

/** Custom sticker support is independent of decorative handheld shell controls. */
object CartridgeLabelSettings {
    fun supportsPlatform(platform: String?): Boolean =
        HandheldCartridgeSettings.supportsPlatform(platform) ||
            GamePlatforms.normalizeOrNull(platform) in setOf("Nintendo Entertainment System", "Super Nintendo")

    fun update(game: GameEntry, labelUri: String?, color: String?): GameEntry {
        if (!supportsPlatform(game.platform)) return game
        return game.copy(
            cartridgeLabelUri = labelUri?.takeIf { it.isNotBlank() },
            cartridgeColor = if (HandheldCartridgeSettings.supportsPlatform(game.platform)) {
                HandheldCartridgeSettings.normalizeColor(color)
            } else game.cartridgeColor
        )
    }
}