package com.ariy.retrolauncher.launch

/** Power-of-two sampling bounds decoded bitmap memory before allocation. */
internal fun artworkSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    require(width > 0 && height > 0 && maxDimension > 0)
    var sample = 1
    while ((width.toLong() + sample - 1) / sample > maxDimension ||
        (height.toLong() + sample - 1) / sample > maxDimension
    ) {
        require(sample <= Int.MAX_VALUE / 2) { "Image dimensions are too large" }
        sample *= 2
    }
    return sample
}