package com.ariy.retrolauncher.ui

/** Selection never changes the layout footprint or artwork size. */
internal fun shelfSlotWidth(widthDp: Float, heightDp: Float, count: Int): Float {
    val fraction = when (count) { 1 -> 0.48f; 2 -> 0.40f; 3 -> 0.32f; 4 -> 0.27f; 8 -> 0.18f; else -> 0.48f }
    return (widthDp * fraction).coerceAtMost(shelfArtworkHeight(heightDp, count) * 1.6f + 16f)
        .coerceAtMost((widthDp - 24f).coerceAtLeast(1f)).coerceAtLeast(1f)
}

internal fun shelfArtworkHeight(heightDp: Float, count: Int): Float {
    val fraction = when (count) { 1 -> 0.70f; 2 -> 0.66f; 3 -> 0.62f; 4 -> 0.58f; 8 -> 0.50f; else -> 0.70f }
    return (heightDp - 88f).coerceAtLeast(1f) * fraction
}

/** Fit visible objects inside equal slots, retaining proportions and a stable shelf baseline. */
internal fun shelfArtworkWidth(widthDp: Float, heightDp: Float, aspectRatio: Float, count: Int): Float {
    return (shelfArtworkHeight(heightDp, count) * aspectRatio.coerceAtLeast(0.1f))
        .coerceAtMost((shelfSlotWidth(widthDp, heightDp, count) - 16f).coerceAtLeast(1f))
        .coerceAtLeast(1f)
}
