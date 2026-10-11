package com.ariy.retrolauncher.ui

/** Detect printed black PS1 packaging bands on US left-spine and PAL bottom-band covers.
 * Only used when the PS1 template supplies its own branding. Originals remain untouched.
 */
internal fun playStationCoverBounds(pixels: IntArray, width: Int, height: Int): ArtworkBounds {
    require(width > 0 && height > 0 && pixels.size.toLong() == width.toLong() * height)
    val full = ArtworkBounds(0, 0, width, height)
    if (width < 64 || height < 64 || width.toFloat()/height !in 0.85f..1.3f) return full
    fun dark(pixel: Int) = (pixel ushr 24) > 240 &&
        ((pixel ushr 16) and 255) < 48 && ((pixel ushr 8) and 255) < 48 && (pixel and 255) < 48
    fun light(pixel: Int) = (pixel ushr 24) > 240 &&
        ((pixel ushr 16) and 255) > 150 && ((pixel ushr 8) and 255) > 150 && (pixel and 255) > 150

    fun printedBand(depth: Int, span: Int, pixelAt: (Int, Int) -> Int): Int {
        val spanStart = span / 12
        val spanEnd = span - spanStart
        val darkFractions = FloatArray(depth) { d ->
            (spanStart until spanEnd).count { dark(pixelAt(d, it)) }.toFloat() / (spanEnd - spanStart)
        }
        val minimum = (depth * 0.07f).toInt()
        val maximum = (depth * 0.23f).toInt()
        val start = (depth * 0.015f).toInt()
        val probe = (depth * 0.015f).toInt().coerceAtLeast(2)
        var bestBoundary = 0
        var bestContrast = 0.35
        for (boundary in minimum..maximum) {
            val bandDark = (start until boundary).map { darkFractions[it] }.average()
            val inside = (boundary - probe until boundary).map { darkFractions[it] }.average()
            val outside = (boundary until boundary + probe).map { darkFractions[it] }.average()
            val contrast = inside - outside
            if (bandDark < 0.65 || outside > 0.50 || contrast <= bestContrast) continue
            val lightCount = (start until boundary).sumOf { d ->
                (spanStart until spanEnd).count { light(pixelAt(d, it)) }
            }
            val lightFraction = lightCount.toFloat() / ((boundary - start) * (spanEnd - spanStart))
            // Blank black margins and unprinted scene edges are not packaging.
            if (lightFraction !in 0.015f..0.32f) continue
            bestBoundary = boundary
            bestContrast = contrast
        }
        return bestBoundary
    }
    val left = printedBand(width, height) { d, y -> pixels[y * width + d] }
    if (left > 0) return ArtworkBounds(left, 0, width, height)
    val bottomBand = printedBand(height, width) { d, x -> pixels[(height - 1 - d) * width + x] }
    return if (bottomBand > 0) ArtworkBounds(0, 0, width, height - bottomBand) else full
}