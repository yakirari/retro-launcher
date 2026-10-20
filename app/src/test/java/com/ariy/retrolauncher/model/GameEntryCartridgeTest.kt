package com.ariy.retrolauncher.model

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GameEntryCartridgeTest {
    private fun game() = GameEntry(
        id = "handheld", title = "Game", platform = "Game Boy",
        sourceType = GameSourceType.CUSTOM_INTENT, imageUri = "content://art/cover"
    )

    @Test fun independentCartridgeSettingsRoundTrip() {
        HandheldCartridgeSettings.colorChoices.forEach { (key, _) ->
            val original = game().copy(cartridgeLabelUri = "content://art/label", cartridgeColor = key)
            val restored = GameEntry.fromJson(JSONObject(original.toJson().toString()))
            assertEquals(original, restored)
            assertEquals("content://art/cover", restored.imageUri)
        }
    }

    @Test fun legacyMissingFieldsDefaultToNull() {
        val legacy = JSONObject("""{"id":"old","title":"Legacy","sourceType":"CUSTOM_INTENT"}""")
        val restored = GameEntry.fromJson(legacy)
        assertNull(restored.cartridgeLabelUri)
        assertNull(restored.cartridgeColor)
        assertEquals("Legacy", restored.title)
    }

    @Test fun explicitJsonNullIsNotTheLiteralStringNull() {
        val json = game().toJson().put("cartridgeLabelUri", JSONObject.NULL).put("cartridgeColor", JSONObject.NULL)
        val restored = GameEntry.fromJson(JSONObject(json.toString()))
        assertNull(restored.cartridgeLabelUri)
        assertNull(restored.cartridgeColor)
        assertEquals(game(), restored)
    }

    @Test fun blankAndUnsetSettingsRoundTripAsNull() {
        assertEquals(game(), GameEntry.fromJson(JSONObject(game().toJson().toString())))
        val blank = game().toJson().put("cartridgeLabelUri", " ").put("cartridgeColor", "")
        assertNull(GameEntry.fromJson(blank).cartridgeLabelUri)
        assertNull(GameEntry.fromJson(blank).cartridgeColor)
    }

    @Test fun paletteUsesOnlySupportedOpaqueKeysAndAutoIsNull() {
        assertEquals(listOf(null, "grey", "graphite", "indigo", "red", "blue", "green", "gold"),
            HandheldCartridgeSettings.colorChoices.map { it.first })
        assertEquals("grey", HandheldCartridgeSettings.normalizeColor(" GREY "))
        assertNull(HandheldCartridgeSettings.normalizeColor("auto"))
        assertNull(HandheldCartridgeSettings.normalizeColor("clear purple"))
    }

    @Test fun controlsAreRestrictedToGbGbcAndGbaIncludingAliases() {
        listOf("GB", "GBC", "GBA", "Game Boy", "Game Boy Color", "Game Boy Advance").forEach {
            assertTrue(it, HandheldCartridgeSettings.supportsPlatform(it))
        }
        listOf(null, "Nintendo DS", "Nintendo Switch", "Sony PlayStation", "MAME Arcade").forEach {
            assertFalse(HandheldCartridgeSettings.supportsPlatform(it))
        }
    }
}