package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms

/** Platform-wide policy, independent of game titles, ROM hashes and device library contents. */
internal object NesArtworkPolicy {
    fun ownsAutomaticCover(game: GameEntry): Boolean {
        if (GamePlatforms.normalizeOrNull(game.platform) != "Nintendo Entertainment System" || game.cartridgeLabelUri != null) return false
        val source = game.imageUri?.substringBefore('#')?.substringBefore('?') ?: return false
        val name = source.substringAfterLast('/')
        return source.startsWith("file:") && (name in setOf(
            "${game.id}-autofill.png", "${game.id}-nes-reviewed-v1.png") ||
            name.matches(Regex("${Regex.escape(game.id)}-nes-(?:catalog-v1|portrait-v2)-[a-f0-9]{64}\\.png")))
    }

    // Dimensions guard against severe downgrades; they do not prove sharpness or authenticity.
    fun acceptableDimensions(width: Int, height: Int, oldWidth: Int, oldHeight: Int): Boolean =
        minOf(width, height) >= 240 && width.toFloat() / height in 0.5f..0.85f &&
            width.toLong() * height >= oldWidth.toLong() * oldHeight / 2
}