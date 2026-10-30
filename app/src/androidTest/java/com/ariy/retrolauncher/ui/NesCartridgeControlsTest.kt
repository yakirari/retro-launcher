package com.ariy.retrolauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ariy.retrolauncher.model.CartridgeLabelSettings
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NesCartridgeControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nesCanResetCustomStickerWithoutChangingCoverOrExposingShellColors() {
        val game = mutableStateOf(GameEntry(title = "NES game", platform = "Nintendo Entertainment System",
            sourceType = GameSourceType.CUSTOM_INTENT, imageUri = "content://test/cover",
            cartridgeLabelUri = "content://test/label"))
        compose.setContent {
            MaterialTheme {
                HandheldCartridgeControls(game.value, { true }) { label, color ->
                    game.value = CartridgeLabelSettings.update(game.value, label, color)
                }
            }
        }
        compose.onNodeWithText("Choose cartridge label").assertIsEnabled()
        compose.onNodeWithText("Shell color").assertDoesNotExist()
        compose.onNodeWithText("Use automatic label").performClick()
        compose.runOnIdle {
            assertNull(game.value.cartridgeLabelUri)
            assertEquals("content://test/cover", game.value.imageUri)
            assertNull(game.value.cartridgeColor)
            val restored = GameEntry.fromJson(CartridgeLabelSettings.update(game.value,
                "content://test/label", null).toJson())
            assertEquals("content://test/label", restored.cartridgeLabelUri)
            assertEquals(game.value.imageUri, restored.imageUri)
        }
    }
}