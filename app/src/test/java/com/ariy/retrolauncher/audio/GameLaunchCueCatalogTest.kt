package com.ariy.retrolauncher.audio

import com.ariy.retrolauncher.R
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.LaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameLaunchCueCatalogTest {
    @Test
    fun mapsSupportedPlatformsToTheirCues() {
        val cases = listOf(
            "Nintendo GameCube" to R.raw.cue_01_nintendo_gamecube_wii,
            "Nintendo Wii" to R.raw.cue_01_nintendo_gamecube_wii,
            "Nintendo 3DS" to R.raw.cue_02_nintendo_3ds,
            "Nintendo DS" to R.raw.cue_03_nintendo_ds,
            "NES" to R.raw.cue_04_nes,
            "Super Nintendo" to R.raw.cue_05_super_nintendo_snes,
            "Nintendo 64" to R.raw.cue_06_nintendo_64,
            "Game Boy Color" to R.raw.cue_07_gameboy_gbc,
            "Game Boy Advance" to R.raw.cue_08_gameboy_advance_gba,
            "Sony PlayStation" to R.raw.cue_09_playstation_1,
            "Sony PlayStation 2" to R.raw.cue_10_playstation_2,
            "Sony PlayStation Portable" to R.raw.cue_11_psp,
            "Sony PlayStation Vita" to R.raw.cue_12_ps_vita,
            "Sega Dreamcast" to R.raw.cue_13_dreamcast,
            "Sega Genesis / Mega Drive" to R.raw.cue_14_sega_genesis_megadrive,
            "PC Engine / TurboGrafx-16" to R.raw.cue_15_pc_engine_turbografx16,
            "Neo Geo" to R.raw.cue_16_neo_geo,
            "Neo Geo Pocket Color" to R.raw.cue_17_neo_geo_pocket_color,
            "MAME Arcade" to R.raw.cue_18_arcade_coin_insert,
            "Final Burn Neo" to R.raw.cue_18_arcade_coin_insert,
            "DOS Games" to R.raw.cue_19_dos_scummvm,
            "ScummVM" to R.raw.cue_19_dos_scummvm,
            "Steam" to R.raw.cue_20_pc_game_shortcut
        )

        cases.forEach { (platform, resourceId) ->
            assertEquals(platform, resourceId, GameLaunchCueCatalog.resourceFor(game(platform)))
        }
    }

    @Test
    fun infersCueFromLaunchTargetWhenPlatformIsMissing() {
        assertEquals(
            R.raw.cue_19_dos_scummvm,
            GameLaunchCueCatalog.resourceFor(
                game(null),
                LaunchTarget(sourceType = GameSourceType.CUSTOM_INTENT, packageName = "org.scummvm.scummvm")
            )
        )
        assertEquals(
            R.raw.cue_20_pc_game_shortcut,
            GameLaunchCueCatalog.resourceFor(
                game(null),
                LaunchTarget(sourceType = GameSourceType.SHORTCUT, packageName = "app.gamenative")
            )
        )
    }

    @Test
    fun leavesUnsupportedPlatformsWithoutACue() {
        assertNull(GameLaunchCueCatalog.resourceFor(game("Nintendo Switch")))
        assertNull(GameLaunchCueCatalog.resourceFor(game("Android")))
    }

    private fun game(platform: String?) = GameEntry(
        title = "Test game",
        platform = platform,
        sourceType = GameSourceType.CUSTOM_INTENT
    )
}
