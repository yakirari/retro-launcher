package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GalleryTileSize

internal const val SHELF_ITEM_SPACING_DP = 16f

/** Selection never changes the layout footprint or artwork size. */
internal fun shelfSlotWidth(widthDp: Float, heightDp: Float, size: GalleryTileSize, density: Float = 1f): Float {
    // At either end, half the viewport is padding around the centered item.
    val slots = 2 * size.minimumVisibleItems - 1
    val spacing = kotlin.math.ceil(SHELF_ITEM_SPACING_DP * density) / density
    val fittingWidth = (widthDp - spacing * (slots - 1)) / slots
    val width = fittingWidth.coerceAtMost(shelfArtworkHeight(heightDp, size) * 1.6f + 16f)
        .coerceAtMost((widthDp - 24f).coerceAtLeast(1f)).coerceAtLeast(1f)
    return (kotlin.math.floor(width * density) / density).coerceAtLeast(1f / density)
}

internal fun shelfArtworkHeight(heightDp: Float, size: GalleryTileSize): Float {
    return (heightDp - 88f).coerceAtLeast(1f) / size.minimumVisibleItems
}

/** Fit visible objects inside equal slots, retaining proportions and a stable shelf baseline. */
internal fun shelfArtworkWidth(
    widthDp: Float, heightDp: Float, aspectRatio: Float, size: GalleryTileSize, density: Float = 1f
): Float {
    return (shelfArtworkHeight(heightDp, size) * aspectRatio.coerceAtLeast(0.1f))
        .coerceAtMost((shelfSlotWidth(widthDp, heightDp, size, density) - 16f).coerceAtLeast(1f))
        .coerceAtLeast(1f)
}
