package com.ariy.retrolauncher.model

import org.junit.Assert.*
import org.junit.Test

class PcGameStoresTest {
    @Test fun shortcutLibrariesDoNotOfferManualRomAdding() {
        for (platform in PcGameStores.platforms) assertTrue(PcGameStores.usesShortcutAdding(platform))
        assertFalse(PcGameStores.usesShortcutAdding("PC (Windows)"))
        assertFalse(PcGameStores.usesShortcutAdding("Android"))
        assertFalse(PcGameStores.usesShortcutAdding("Nintendo Switch"))
        assertFalse(PcGameStores.usesShortcutAdding(null))
    }
    @Test fun explicitStoresClassifyButOpaqueShortcutsNeverGuessSteam() {
        val game = GameEntry(title = "Game", sourceType = GameSourceType.SHORTCUT, packageName = "app.gamenative", platform = "PC (Windows)")
        assertEquals("GameNative", PcGameStores.classify(game).platform)
        assertEquals("Steam", PcGameStores.classify(game.copy(platform = "Steam")).platform)
        assertEquals("GOG", PcGameStores.classify(game.copy(extrasJson = """{"game_source":"GOG"}""")).platform)
        assertEquals("Android", PcGameStores.classify(game.copy(sourceType = GameSourceType.ACTIVITY, platform = "Android")).platform)
    }
    @Test fun hubShortcutsUseActualAppLabelNotGuessedPackageOrStore() {
        fun shortcut(label: String) = com.ariy.retrolauncher.launch.InstalledShortcutOption(
            title = "Game", subtitle = "unknown.publisher", sourceLabel = label,
            packageName = "unknown.publisher", shortcutId = "opaque", userSerial = 0,
            shortLabel = "Game", longLabel = null, extrasJson = """{"game_source":"STEAM"}""")
        assertEquals("GameHub", PcGameStores.forShortcut(shortcut("GameHub")))
        assertEquals("GameHub Lite", PcGameStores.forShortcut(shortcut("GameHub Lite")))
        assertEquals("GameHub Lite", PcGameStores.forShortcut(shortcut("Localized app").copy(packageName = "gamehub.lite")))
        assertEquals("GameHub", PcGameStores.forShortcut(shortcut("Localized app").copy(packageName = "com.xiaoji.egggame")))
        assertNull(PcGameStores.forShortcut(shortcut("Unrelated")))
        assertEquals("GameNative", PcGameStores.forShortcut(shortcut("GameNative").copy(packageName = "app.gamenative", extrasJson = null)))
    }
    @Test fun digitalLibrariesHaveIndependentGalleryAndTemplateDefaults() {
        for (platform in PcGameStores.platforms + "PC (Windows)") {
            assertEquals(PlatformDisplaySettings("gallerystalgic", 3, "transparent"), platformDisplayDefaults(platform))
            assertNotNull(PlatformSvgIcons.rawResourceForPlatform(platform))
            val frame = NostalgicOverlays.specForPlatform(platform)!!
            assertEquals(2f / 3f, frame.aspectRatio, 0.0001f)
            assertEquals(2f / 3f, frame.artworkWidthFraction * 600f / (frame.artworkHeightFraction * 900f), 0.0001f)
            assertNotEquals(NostalgicOverlays.specForPlatform("Nintendo Switch")?.drawableResourceId,
                NostalgicOverlays.specForPlatform(platform)?.drawableResourceId)
        }
    }
}