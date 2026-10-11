package com.ariy.retrolauncher.model

import org.junit.Assert.*
import org.junit.Test

class PlatformDisplaySettingsTest {
    @Test fun arcadeDefaultsAreStaticTwoColumnTransparent() {
        assertEquals(PlatformDisplaySettings("gridstalgic", 2, "transparent"), platformDisplayDefaults("MAME Arcade"))
        assertNotEquals(platformDisplayDefaults("MAME Arcade"), platformDisplayDefaults("Nintendo Switch"))
        assertEquals(3, platformDisplayDefaults("gb").gridColumns)
    }
    @Test fun independentProfilesSurviveJsonRoundTrip() {
        val profiles = mapOf("MAME Arcade" to PlatformDisplaySettings("gallery", 1, "glossy", false), "Nintendo Switch" to PlatformDisplaySettings("grid", 4, "normal"))
        assertEquals(profiles, parsePlatformDisplaySettings(profiles.toDisplaySettingsJson()))
        val reset = profiles - "MAME Arcade"
        assertEquals(profiles["Nintendo Switch"], parsePlatformDisplaySettings(reset.toDisplaySettingsJson())["Nintendo Switch"])
        assertEquals(2, platformDisplayDefaults("MAME Arcade").gridColumns)
    }
    @Test fun requestedConsoleDefaultsUseThreeTransparentNostalgicColumns() {
        for (platform in listOf("ps1", "ps2", "psp", "wii", "n64")) {
            assertEquals(platform, PlatformDisplaySettings("gridstalgic",3,"transparent"), platformDisplayDefaults(platform))
        }
    }
    @Test fun n64CartridgeLeavesSpaceWithoutShrinkingOtherPlatforms() {
        val n64 = requireNotNull(NostalgicOverlays.specForPlatform("Nintendo 64"))
        assertEquals(0.88f, n64.gridArtworkScale, 0.001f)
        for (platform in listOf("ps1", "ps2", "psp", "wii", "gb", "snes")) {
            assertEquals(platform, 1f, requireNotNull(NostalgicOverlays.specForPlatform(platform)).gridArtworkScale, 0.001f)
        }
    }
    @Test fun malformedAndUnsupportedSettingsFallBackSafely() {
        assertTrue(parsePlatformDisplaySettings("bad json").isEmpty())
        val profile = parsePlatformDisplaySettings("""{"MAME Arcade":{"layoutMode":"bad","gridColumns":99,"tileBackgroundMode":"bad"},"unknown":{"gridColumns":1}}""")
        assertEquals(platformDisplayDefaults("MAME Arcade"), profile["MAME Arcade"])
        assertEquals(1, profile.size)
    }
}