package com.ariy.retrolauncher.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale

internal object ScummVmBundledArtwork {
    private const val DIRECTORY = "artwork/scummvm"
    private const val BLACK_EDGE_THRESHOLD = 2
    @Volatile private var cachedIds: Set<String>? = null

    internal fun makeEdgeBlackTransparent(pixels: IntArray, width: Int, height: Int): Int {
        require(width > 0 && height > 0 && pixels.size == width * height)
        val queue = IntArray(pixels.size)
        var tail = 0

        fun enqueue(index: Int) {
            val pixel = pixels[index]
            if (pixel ushr 24 == 0 ||
                (pixel ushr 16 and 0xff) > BLACK_EDGE_THRESHOLD ||
                (pixel ushr 8 and 0xff) > BLACK_EDGE_THRESHOLD ||
                (pixel and 0xff) > BLACK_EDGE_THRESHOLD) return
            pixels[index] = pixel and 0x00ffffff
            queue[tail++] = index
        }

        val lastRow = (height - 1) * width
        for (x in 0 until width) {
            enqueue(x)
            enqueue(lastRow + x)
        }
        for (y in 1 until height - 1) {
            enqueue(y * width)
            enqueue(y * width + width - 1)
        }

        var head = 0
        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            if (x > 0) enqueue(index - 1)
            if (x + 1 < width) enqueue(index + 1)
            if (index >= width) enqueue(index - width)
            if (index + width < pixels.size) enqueue(index + width)
        }
        return tail
    }

    internal fun resolveAssetId(availableIds: Set<String>, candidates: List<String?>): String? {
        val ids = availableIds.sortedByDescending(String::length)
        return candidates.asSequence().filterNotNull().map { candidate ->
            candidate.trim().lowercase(Locale.ROOT)
                .substringAfterLast('/')
                .substringAfterLast(':')
                .removeSuffix(".scummvm")
                .removeSuffix(".svm")
        }.firstNotNullOfOrNull { value ->
            ids.firstOrNull { id ->
                value == id || value.startsWith("$id-") || value.startsWith("${id}_")
            }
        }
    }

    fun assetId(context: Context, game: GameEntry): String? {
        if (GamePlatforms.normalizeOrNull(game.platform) != "ScummVM") return null
        val ids = cachedIds ?: synchronized(this) {
            cachedIds ?: context.assets.list(DIRECTORY).orEmpty()
                .filter { it.matches(Regex("[a-z0-9_-]+\\.png")) }
                .map { it.removeSuffix(".png") }
                .toSet()
                .also { cachedIds = it }
        }
        return resolveAssetId(ids, listOf(
            ScummVmGames.registeredTarget(game.extrasJson),
            game.shortcutId,
            game.intentDataUri,
            ScummVmGames.artworkGameId(game.title)
        ))
    }

    fun save(context: Context, game: GameEntry, assetId: String, store: ImportedIconStore): String {
        val assetPath = "$DIRECTORY/$assetId.png"
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val name = "${game.id}-scummvm-bundled-alpha-v2-$assetId-$hash"
        store.existingBitmapUri(name)?.let { return it }
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Unable to decode bundled ScummVM artwork: $assetPath")
        val pixels = IntArray(source.width * source.height)
        val bitmap = try {
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
            makeEdgeBlackTransparent(pixels, source.width, source.height)
            Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
        } finally {
            source.recycle()
        }
        return try {
            store.saveBitmap(name, bitmap)
                ?: throw IOException("Unable to save bundled ScummVM artwork: $assetPath")
        } finally {
            bitmap.recycle()
        }
    }
}
