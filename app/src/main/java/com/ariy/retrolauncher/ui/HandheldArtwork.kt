package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.ariy.retrolauncher.resolveArtworkSourceUri
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.HandheldCartridgeSettings
import com.ariy.retrolauncher.model.NostalgicOverlaySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

private val labelSourceCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}
private val composedLabelCache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}
private val artworkColorCache = LruCache<String, Int>(256)
private val settledLabelCache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}
private val blankLabelCache = LruCache<Int, Bitmap>(3)

/** A small, saturation-weighted histogram ignores white packaging and dark outlines. */
internal fun handheldArtworkColor(source: Bitmap): Int? {
    val weights = FloatArray(24)
    val reds = FloatArray(24)
    val greens = FloatArray(24)
    val blues = FloatArray(24)
    val hsv = FloatArray(3)
    for (y in 0 until 32) for (x in 0 until 32) {
        val pixel = source.getPixel(
            ((x + 0.5f) * source.width / 32).toInt().coerceAtMost(source.width - 1),
            ((y + 0.5f) * source.height / 32).toInt().coerceAtMost(source.height - 1)
        )
        Color.colorToHSV(pixel, hsv)
        if (Color.alpha(pixel) < 128 || hsv[1] < 0.18f || hsv[2] < 0.18f) continue
        val bucket = (hsv[0] / 15).toInt().coerceIn(0, 23)
        val weight = hsv[1] * hsv[1] * hsv[2]
        weights[bucket] += weight
        reds[bucket] += Color.red(pixel) * weight
        greens[bucket] += Color.green(pixel) * weight
        blues[bucket] += Color.blue(pixel) * weight
    }
    val bucket = weights.indices.maxByOrNull { weights[it] } ?: return null
    val weight = weights[bucket]
    if (weight == 0f) return null
    Color.RGBToHSV((reds[bucket] / weight).toInt(), (greens[bucket] / weight).toInt(),
        (blues[bucket] / weight).toInt(), hsv)
    // Muted plastic rather than a luminous solid fill; the mould supplies highlights/shadows.
    hsv[1] = hsv[1].coerceIn(0.28f, 0.65f)
    hsv[2] = hsv[2].coerceIn(0.42f, 0.72f)
    return Color.HSVToColor(hsv)
}

@Composable
internal fun rememberHandheldShellColor(game: GameEntry, artwork: ImageBitmap?): Int {
    val fallback = HandheldCartridgeSettings.shellColor(game.platform, game.cartridgeColor)
    if (HandheldCartridgeSettings.normalizeColor(game.cartridgeColor) != null) return fallback
    val source = artwork?.asAndroidBitmap() ?: return fallback
    val key = "${game.imageUri}:${game.cartridgeLabelUri}:${source.generationId}"
    return produceState(artworkColorCache.get(key) ?: fallback, key, fallback) {
        value = withContext(Dispatchers.Default) {
            artworkColorCache.get(key) ?: (handheldArtworkColor(source) ?: fallback)
                .also { artworkColorCache.put(key, it) }
        }
    }.value
}

/** Full label documents keep their white margins and printed borders; no cover whitespace trim. */
@Composable
internal fun rememberHandheldLabelSource(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(uri?.let(labelSourceCache::get)?.asImageBitmap(), uri) {
        value = if (uri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            labelSourceCache.get(uri)?.asImageBitmap() ?: runCatching {
                val source = resolveArtworkSourceUri(uri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1024 || bounds.outHeight / inSampleSize > 1024) inSampleSize *= 2
                }
                context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?.also { labelSourceCache.put(uri, it) }?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

/** Neutral mould tinting never touches the separately rendered game label. */
internal fun tintHandheldShell(source: Bitmap, color: Int): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        if ((pixel ushr 24) == 0) continue
        val shade = (pixel ushr 16) and 255
        fun channel(shift: Int): Int {
            val base = (color ushr shift) and 255
            return if (shade <= 128) base * shade / 128
            else base + (255 - base) * (shade - 128) / 127
        }
        pixels[i] = (pixel and -0x1000000) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    return Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
}

/** Derived label only: source documents and ordinary cover views are never rewritten. */
@Composable
internal fun rememberHandheldLabel(
    game: GameEntry,
    spec: NostalgicOverlaySpec,
    artwork: ImageBitmap?,
    isCustomLabel: Boolean,
    loadSticker: suspend (Context, GameEntry) -> Bitmap? = { context, entry ->
        if (entry.platform == "Nintendo 64") N64CartridgeLabelStore.load(context, entry)
        else CartridgeLabelStore.load(context, entry)
    }
): ImageBitmap? {
    if (!HandheldCartridgeSettings.supportsPlatform(game.platform) && game.platform != "Nintendo 64") return artwork
    val context = LocalContext.current
    val identity = "label-v6:${game.platform}:${game.intentDataUri}:${game.imageUri}:" +
        "${game.cartridgeLabelUri}:${spec.drawableResourceId}"
    val blank = blankLabelCache.get(spec.drawableResourceId) ?: buildHandheldLabel(spec, null, true)
        .also { blankLabelCache.put(spec.drawableResourceId, it) }
    // Reset state when a recycled slot changes games. Never show another game's art or a temporary
    // miniature box cover before its sticker arrives. Settled labels survive fast scrolling.
    return key(identity) {
        produceState(settledLabelCache.get(identity)?.asImageBitmap() ?: blank.asImageBitmap(),
            artwork, isCustomLabel) {
            val sourced = withContext(Dispatchers.IO) {
                try {
                    if (isCustomLabel) null else loadSticker(context.applicationContext, game)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null // Offline/unmatched: use cover only AFTER sticker lookup finishes.
                }
            }
            val source = if (isCustomLabel) artwork?.asAndroidBitmap() else sourced ?: artwork?.asAndroidBitmap()
            if (source != null) {
                value = withContext(Dispatchers.Default) {
                    val fullLabel = isCustomLabel || sourced != null
                    val cacheKey = "$identity:${source.generationId}:$fullLabel"
                    (composedLabelCache.get(cacheKey) ?: buildHandheldLabel(spec, source, fullLabel)
                        .also { composedLabelCache.put(cacheKey, it) })
                        .also { settledLabelCache.put(identity, it) }.asImageBitmap()
                }
            }
        }.value
    }
}

internal fun buildHandheldLabel(
    spec: NostalgicOverlaySpec,
    artwork: Bitmap?,
    isCustomLabel: Boolean
): Bitmap {
    val width = 640
    val height = (width * spec.artworkHeightFraction / spec.artworkWidthFraction / spec.aspectRatio).toInt()
        .coerceIn(100, 900)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    // The entire well is the sticker: no duplicate title/platform bands stealing print area.
    // Match unused space to the artwork instead of framing a tiny cover in dark/cream bars.
    canvas.drawColor(if (isCustomLabel) 0xFFF0EDE4.toInt()
        else artwork?.let(::handheldArtworkColor) ?: 0xFFF0EDE4.toInt())
    if (artwork != null) {
        val scale = min(width.toFloat() / artwork.width, height.toFloat() / artwork.height)
        val w = artwork.width * scale; val h = artwork.height * scale
        canvas.drawBitmap(artwork, null, RectF((width-w)/2, (height-h)/2, (width+w)/2, (height+h)/2), paint)
    }
    return bitmap
}