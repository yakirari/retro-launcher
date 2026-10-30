package com.ariy.retrolauncher.model

import org.junit.Assert.*
import org.junit.Test

class CartridgeLabelSettingsTest {
    private fun game(platform: String) = GameEntry(title = "Test", platform = platform,
        sourceType = GameSourceType.CUSTOM_INTENT, imageUri = "content://covers/original",
        cartridgeColor = "grey")

    @Test fun nesStickerIsIndependentOfCoverAndShellColor() {
        val original = game("Nintendo Entertainment System")
        val custom = CartridgeLabelSettings.update(original, "content://labels/nes", "red")
        assertEquals("content://labels/nes", custom.cartridgeLabelUri)
        assertEquals(original.imageUri, custom.imageUri)
        assertEquals(original.cartridgeColor, custom.cartridgeColor)
        assertEquals(original, CartridgeLabelSettings.update(custom, null, null))
        assertFalse(HandheldCartridgeSettings.supportsPlatform(original.platform))
        assertTrue(CartridgeLabelSettings.supportsPlatform("nes"))
    }

    @Test fun handheldColorsStillNormalizeAndUnsupportedPlatformsStayUntouched() {
        assertEquals("red", CartridgeLabelSettings.update(game("Game Boy"), "label", " RED ").cartridgeColor)
        val unsupported = game("Sony PlayStation")
        assertSame(unsupported, CartridgeLabelSettings.update(unsupported, "label", "red"))
    }
}