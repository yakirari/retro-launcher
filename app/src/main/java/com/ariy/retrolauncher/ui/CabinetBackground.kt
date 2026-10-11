package com.ariy.retrolauncher.ui

/** Only edge-connected near-white/transparent pixels are background; white logos survive. */
internal fun cabinetBackgroundMask(pixels: IntArray, width: Int, height: Int): BooleanArray {
    require(width > 0 && height > 0 && width.toLong() * height == pixels.size.toLong())
    val mask = BooleanArray(pixels.size)
    val queue = IntArray(pixels.size)
    var head = 0
    var tail = 0
    fun add(index: Int) {
        if (mask[index]) return
        val pixel = pixels[index]
        if ((pixel ushr 24) > 8 && !((pixel ushr 16 and 255) >= 245 &&
                (pixel ushr 8 and 255) >= 245 && (pixel and 255) >= 245)) return
        mask[index] = true
        queue[tail++] = index
    }
    for (x in 0 until width) { add(x); add((height - 1) * width + x) }
    for (y in 0 until height) { add(y * width); add(y * width + width - 1) }
    while (head < tail) {
        val index = queue[head++]
        val x = index % width
        if (x > 0) add(index - 1)
        if (x + 1 < width) add(index + 1)
        if (index >= width) add(index - width)
        if (index + width < pixels.size) add(index + width)
    }
    return mask
}