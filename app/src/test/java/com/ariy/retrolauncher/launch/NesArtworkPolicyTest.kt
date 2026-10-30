package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class NesArtworkPolicyTest {
    @Test fun ownershipDoesNotDependOnGameIdentity() {
        val game = GameEntry(id = "anything", title = "An unknown future NES game", platform = "Nintendo Entertainment System",
            sourceType = GameSourceType.CUSTOM_INTENT, imageUri = "file:///data/anything-autofill.png#rev")
        assertTrue(NesArtworkPolicy.ownsAutomaticCover(game))
        assertTrue(NesArtworkPolicy.ownsAutomaticCover(game.copy(imageUri = "file:///data/anything-nes-portrait-v2-${"a".repeat(64)}.png")))
        assertFalse(NesArtworkPolicy.ownsAutomaticCover(game.copy(imageUri = "file:///data/anything-nes-bundled-v1-${"a".repeat(64)}.png")))
        assertFalse(NesArtworkPolicy.ownsAutomaticCover(game.copy(cartridgeLabelUri = "content://manual")))
        assertFalse(NesArtworkPolicy.ownsAutomaticCover(game.copy(imageUri = "file:///data/anything.png")))
        assertFalse(NesArtworkPolicy.ownsAutomaticCover(game.copy(platform = "Super Nintendo")))
        assertFalse(NesArtworkPolicy.ownsAutomaticCover(game.copy(imageUri = "file:///data/anything-nes-catalog-v1-token.png")))
    }
    @Test fun rejectsThumbnailsAndSevereResolutionDowngradesWithoutAssumingPortraitArt() {
        assertFalse(NesArtworkPolicy.acceptableDimensions(512, 360, 497, 680))
        assertTrue(NesArtworkPolicy.acceptableDimensions(512, 731, 256, 224))
        assertFalse(NesArtworkPolicy.acceptableDimensions(256, 177, 512, 512))
        assertFalse(NesArtworkPolicy.acceptableDimensions(256, 350, 1024, 1400))
    }
}