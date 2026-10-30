package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtworkMemoryTest {
    @Test fun manyShellColorsStayWithinByteBudgetWithoutRecyclingVisibleImages() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val cache = ArtworkMemoryCache<Bitmap>(image.allocationByteCount * 3) { it.allocationByteCount }
        for (color in 0 until 500) cache["shell-$color"] = image
        assertTrue(cache.sizeBytes() <= image.allocationByteCount * 3)
        assertNull(cache["shell-0"])
        assertSame(image, cache["shell-499"])
        cache.clear()
        assertEquals(0, cache.sizeBytes())
        assertFalse(image.isRecycled)
    }

    @Test fun cacheEvictsLeastRecentlyUsedAndAccountsForDifferentSizes() {
        val cache = ArtworkMemoryCache<ByteArray>(100) { it.size }
        cache["a"] = ByteArray(40)
        cache["b"] = ByteArray(40)
        assertNotNull(cache["a"])
        cache["c"] = ByteArray(60)
        assertNull(cache["b"])
        assertNotNull(cache["a"])
        assertEquals(100, cache.sizeBytes())
        cache["oversized"] = ByteArray(101)
        assertTrue(cache.sizeBytes() <= 100)
        assertNull(cache["oversized"])
    }

    @Test fun oversizedCabinetDecodeIsBoundedAndNormalFrontRemainsFullResolution() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val files = requireNotNull(context.assets.list("arcade_presentations/front"))
        var testedLarge = false
        var testedNormal = false
        for (file in files) {
            val path = "arcade_presentations/front/$file"
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            val large = maxOf(bounds.outWidth, bounds.outHeight) > 1024
            if ((large && testedLarge) || (!large && testedNormal)) continue
            val bitmap = requireNotNull(decodeSampledArtworkAsset(context, path))
            assertTrue(bitmap.width <= 1024 && bitmap.height <= 1024)
            if (!large) {
                assertEquals(bounds.outWidth, bitmap.width)
                assertEquals(bounds.outHeight, bitmap.height)
                testedNormal = true
            } else {
                assertTrue(bitmap.allocationByteCount < bounds.outWidth * bounds.outHeight * 4)
                testedLarge = true
            }
            bitmap.recycle()
            if (testedLarge && testedNormal) break
        }
        assertTrue("Pack must exercise both normal and oversized images", testedLarge && testedNormal)
    }
}