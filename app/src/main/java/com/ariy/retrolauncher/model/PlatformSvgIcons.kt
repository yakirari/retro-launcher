package com.ariy.retrolauncher.model

import android.content.res.Resources
import androidx.annotation.RawRes
import com.ariy.retrolauncher.R

object PlatformSvgIcons {
    // Add new badges by dropping the SVG into app/src/main/res/raw as-is,
    // then linking the platform name to the generated R.raw resource below.
    private val resourceByPlatform = mapOf(
        GamePlatforms.ANDROID to R.raw.platform_android,
        "Sega" to R.raw.platform_sega,
        "Nintendo Entertainment System" to R.raw.platform_nintendo_entertainment_system,
        "Super Nintendo" to R.raw.platform_super_nintendo,
        "Game Boy Advance" to R.raw.platform_game_boy_advance,
        "Game Boy" to R.raw.platform_game_boy,
        "Game Boy Color" to R.raw.platform_game_boy_color,
        "Nintendo 64" to R.raw.platform_nintendo_64,
        "Nintendo 3DS" to R.raw.platform_nintendo_3ds,
        "Nintendo DS" to R.raw.platform_nintendo_ds,
        "Nintendo GameCube" to R.raw.platform_nintendo_gamecube,
        "Nintendo Wii" to R.raw.platform_nintendo_wii,
        "Nintendo Switch" to R.raw.platform_nintendo_switch,
        "Sega Master System" to R.raw.platform_sega,
        "Sega Genesis / Mega Drive" to R.raw.platform_sega_genesis_mega_drive,
        "Sega CD" to R.raw.platform_sega,
        "Sega 32X" to R.raw.platform_sega,
        "Sega Saturn" to R.raw.platform_sega_saturn,
        "Sega Dreamcast" to R.raw.platform_sega_dreamcast,
        "Sony PlayStation" to R.raw.platform_sony_playstation,
        "Sony PlayStation 2" to R.raw.platform_sony_playstation_2,
        "Sony PlayStation 3" to R.raw.platform_sony_playstation_3,
        "Sony PlayStation Portable" to R.raw.platform_sony_playstation_portable,
        "Sony PlayStation Vita" to R.raw.platform_sony_playstation_vita,
        "Neo Geo" to R.raw.platform_neogeo,
        "Neo Geo Pocket" to R.raw.platform_neogeo,
        "Neo Geo Pocket Color" to R.raw.platform_neogeo,
        "Arcade" to R.raw.platform_mame_arcade,
        "MAME Arcade" to R.raw.platform_mame_arcade,
        "Final Burn Neo" to R.raw.platform_final_burn_neo,
        "ScummVM" to R.raw.platform_scummvm,
        "DOS Games" to R.raw.platform_dos_games,
        "PC Engine / TurboGrafx-16" to R.raw.platform_pc_engine_turbografx_16
    )

    @RawRes
    fun rawResourceForPlatform(platform: String?): Int? {
        val key = GamePlatforms.normalizeOrNull(platform)
            ?: GamePlatforms.inferFromUriString(platform)
            ?: platform?.trim()
        return key?.let {
            resourceByPlatform.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
        }
    }

    fun svgForPlatform(resources: Resources, platform: String?): String? {
        val resourceId = rawResourceForPlatform(platform) ?: return null
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }
}
