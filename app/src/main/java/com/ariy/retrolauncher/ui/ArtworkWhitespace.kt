package com.ariy.retrolauncher.ui

internal data class ArtworkBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Only trim clearly uniform white/transparent outer margins, never colored image edges. */
internal fun artworkContentBounds(pixels: IntArray, width: Int, height: Int): ArtworkBounds {
    require(width > 0 && height > 0 && pixels.size.toLong() == width.toLong() * height)
    val full = ArtworkBounds(0, 0, width, height)
    val corners = intArrayOf(pixels[0], pixels[width - 1], pixels[(height - 1) * width], pixels.last())
    val transparent = corners.all { (it ushr 24) <= 8 }
    fun isWhite(color: Int) = (color ushr 24) >= 245 &&
        ((color shr 16) and 255) >= 245 && ((color shr 8) and 255) >= 245 && (color and 255) >= 245
    if (!transparent && !corners.all(::isWhite)) return full
    fun emptyPixel(color: Int) = if (transparent) (color ushr 24) <= 8 else isWhite(color)
    fun emptyRow(y: Int) = (0 until width).all { emptyPixel(pixels[y * width + it]) }
    fun emptyColumn(x: Int) = (0 until height).all { emptyPixel(pixels[it * width + x]) }
    fun margin(limit: Int, empty: (Int) -> Boolean): Int {
        var count = 0
        while (count <= limit && empty(count)) count++
        // Do not infer a crop for extremely wide padding or a completely blank image.
        return if (count > limit) 0 else count
    }
    val left = margin(width / 5) { emptyColumn(it) }
    val right = width - margin(width / 5) { emptyColumn(width - 1 - it) }
    val top = margin(height / 5) { emptyRow(it) }
    val bottom = height - margin(height / 5) { emptyRow(height - 1 - it) }
    return if (right > left && bottom > top) ArtworkBounds(left, top, right, bottom) else full
}