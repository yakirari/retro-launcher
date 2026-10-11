package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkImageSizingTest {
    @Test fun smallImagesKeepTheirResolution() {
        assertEquals(1, artworkSampleSize(600, 900, 1024))
    }

    @Test fun largePortraitsAreSampledBeforeDecoding() {
        assertEquals(8, artworkSampleSize(4000, 6000, 1024))
    }

    @Test fun panoramasAreBoundedByTheirLongestDimension() {
        assertEquals(16, artworkSampleSize(12000, 600, 1024))
    }

    @Test fun handlesOddDimensionsWithoutExceedingLimit() {
        assertEquals(2, artworkSampleSize(1025, 700, 1024))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        artworkSampleSize(0, 900, 1024)
    }
}