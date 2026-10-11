package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GamePlatforms
import kotlin.math.abs
import kotlin.math.ln

internal fun preferredArtworkAspectRatio(platform: String?): Float {
    val normalized = GamePlatforms.normalizeOrNull(platform) ?: platform?.trim().orEmpty()
    return when (normalized) {
        in com.ariy.retrolauncher.model.PcGameStores.platforms, "PC (Windows)" -> 2f / 3f
        "Nintendo Switch", "Nintendo 3DS", "Nintendo DS", "Sony PlayStation",
        "Sony PlayStation 2", "Sony PlayStation 3", "Sony PlayStation Portable",
        "Sony PlayStation Vita", "Nintendo GameCube", "Nintendo Wii" -> 0.70f
        "Game Boy", "Game Boy Color", "Game Boy Advance", "Nintendo Entertainment System",
        "Super Nintendo", "Nintendo 64", "Sega Master System", "Sega Genesis / Mega Drive",
        "Sega Saturn", "Sega Dreamcast", "Neo Geo", "Neo Geo Pocket", "Neo Geo Pocket Color" -> 0.72f
        "DOS Games" -> 1.50f
        "Android", "MAME Arcade", "Final Burn Neo" -> 1f
        else -> 0.70f
    }
}

internal fun compareArtworkAspectRatios(
    firstWidth: Int,
    firstHeight: Int,
    secondWidth: Int,
    secondHeight: Int,
    targetRatio: Float
): Int {
    val firstScore = artworkAspectRatioDistance(firstWidth, firstHeight, targetRatio)
    val secondScore = artworkAspectRatioDistance(secondWidth, secondHeight, targetRatio)
    return firstScore.compareTo(secondScore)
}

private fun artworkAspectRatioDistance(width: Int, height: Int, targetRatio: Float): Float {
    if (width <= 0 || height <= 0 || targetRatio <= 0f) return Float.POSITIVE_INFINITY
    return abs(ln((width.toFloat() / height) / targetRatio))
}