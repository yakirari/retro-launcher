package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import org.json.JSONObject

class N64LabelScansTest {
    private val manifest = File("src/main/assets/${N64LabelScans.MANIFEST}").readText()
    private val scans = N64LabelScans.parse(manifest)
    @Test fun reviewedStickerCropsAreLargerThanThumbnailsAndInsideVerifiedSources() {
        for (signature in listOf("89239B0E", "AC631DA0", "8E94F2DD", "7021064B", "BE5799F3", "E5CDED18", "B393776D")) {
            val scan = requireNotNull(scans[signature])
            assertTrue(scan.width >= 300 && scan.height >= 350)
            assertTrue(scan.left >= 0 && scan.top >= 0)
            assertTrue(scan.left + scan.width <= scan.sourceWidth)
            assertTrue(scan.top + scan.height <= scan.sourceHeight)
            assertTrue(scan.width.toFloat() / scan.height in 0.8f..0.95f)
            assertEquals(64, scan.sha256.length)
            assertTrue(scan.url.startsWith("https://archive.org/download/"))
            assertTrue(scan.url.contains("%20"))
            assertFalse(scan.url.contains(" "))
        }
    }

    @Test fun unknownAndUnreviewedRegionalVariantsNeverBorrowAnotherLabel() {
        assertNull(scans["7F1DF0FA"]) // Japanese Shindou is not the USA sticker.
        assertNull(scans["9BFFFB3F"]) // Reject the badly noisy Harvest Moon scan.
        assertNull(scans["00000000"])
        assertEquals("Super Mario 64 (U) Cart.jpg", scans["B393776D"]?.filename)
    }

    @Test fun unreviewedEntriesNeverBecomeRuntimeSources() {
        val json = JSONObject(manifest)
        val entries = json.getJSONArray("entries")
        for (i in 0 until entries.length()) entries.getJSONObject(i).put("reviewed", false)
        assertTrue(N64LabelScans.parse(json.toString()).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class) fun duplicateSignaturesAreRejected() {
        val json = JSONObject(manifest)
        val entries = json.getJSONArray("entries")
        entries.put(entries.getJSONObject(0))
        N64LabelScans.parse(json.toString())
    }

    @Test(expected = IllegalArgumentException::class) fun outOfBoundsCropsAreRejected() {
        val json = JSONObject(manifest)
        json.getJSONArray("entries").getJSONObject(0).getJSONArray("crop").put(0, 10000)
        N64LabelScans.parse(json.toString())
    }
}