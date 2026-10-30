package com.ariy.retrolauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/** Byte-bounded, thread-safe storage. Eviction never recycles images still used by Compose. */
internal class ArtworkMemoryCache<T : Any>(maxBytes: Int, private val bytes: (T) -> Int) {
    private val cache = object : LruCache<String, T>(maxBytes) {
        override fun sizeOf(key: String, value: T): Int = bytes(value).coerceAtLeast(1)
    }

    operator fun get(key: String): T? = cache.get(key)
    operator fun set(key: String, value: T) { cache.put(key, value) }
    fun clear() = cache.evictAll()
    fun sizeBytes(): Int = cache.size()
}

/** Reopen assets for the second pass; an AssetInputStream need not support reset(). */
internal fun decodeSampledArtworkAsset(context: Context, path: String, maxDimension: Int = 1024): Bitmap? {
    require(maxDimension > 0)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = 1
        while (bounds.outWidth / inSampleSize > maxDimension || bounds.outHeight / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
    }
    val decoded = context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    return if (path.startsWith("arcade_presentations/front/")) transparentArcadeFront(decoded) else decoded
}

/** Alpha-only treatment for the verified flat surround; source assets and interior RGB remain intact. */
internal fun transparentArcadeFront(source: Bitmap): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    val background = arcadeFrontBackgroundMask(pixels, source.width, source.height)
    if (background.none { it }) return source
    for (i in pixels.indices) if (background[i]) pixels[i] = pixels[i] and 0x00ffffff
    return Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
}