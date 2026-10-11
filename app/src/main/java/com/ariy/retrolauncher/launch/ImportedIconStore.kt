package com.ariy.retrolauncher.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AtomicFile
import com.ariy.retrolauncher.stampArtworkUri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException

class ImportedIconStore(private val context: Context) {
    fun saveShortcutIcon(fileName: String, drawable: Drawable): String? {
        val bitmap = drawable.toBitmap() ?: return null
        return saveBitmap(fileName, bitmap)
    }

    fun existingBitmapUri(fileName: String): String? {
        val file = imageFile(fileName)
        if (!file.isFile || file.length() == 0L) return null
        return stampArtworkUri(Uri.fromFile(file).toString(), file.lastModified())
    }

    fun matchesShortcutIcon(fileName: String, imageUri: String?, drawable: Drawable): Boolean {
        // A manual replacement saved under the same ID is not an imported icon unless pixels match.
        val file = imageFile(fileName)
        if (imageUri == null || com.ariy.retrolauncher.resolveArtworkSourceUri(imageUri) != Uri.fromFile(file)) return false
        val saved = BitmapFactory.decodeFile(file.path) ?: return false
        val icon = drawable.toBitmap()
        return try { icon != null && saved.sameAs(icon) } finally { saved.recycle() }
    }

    fun downloadImage(fileName: String, imageUrl: String): String? {
        val bitmap = downloadBitmap(imageUrl) ?: return null
        return try { saveBitmap(fileName, bitmap) } finally { bitmap.recycle() }
    }

    fun downloadFirstAvailableImage(
        fileName: String,
        imageUrls: Iterable<String>,
        maxAttempts: Int = 12,
        cancellationCheck: () -> Unit = {}
    ): String? {
        if (maxAttempts <= 0) return null
        return imageUrls
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(maxAttempts)
            .firstNotNullOfOrNull { imageUrl ->
                cancellationCheck()
                val bitmap = downloadBitmap(imageUrl, cancellationCheck) ?: return@firstNotNullOfOrNull null
                try {
                    cancellationCheck()
                    saveBitmap(fileName, bitmap)
                } finally { bitmap.recycle() }
            }
    }

    fun downloadBestAspectImage(
        fileName: String,
        imageUrls: Iterable<String>,
        targetAspectRatio: Float,
        maxAttempts: Int = 6,
        cancellationCheck: () -> Unit = {}
    ): String? {
        if (maxAttempts <= 0 || targetAspectRatio <= 0f) return null
        var bestBitmap: Bitmap? = null
        var bestDistance = Float.POSITIVE_INFINITY

        val candidates = imageUrls.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(maxAttempts)
        try {
            for (imageUrl in candidates) {
                cancellationCheck()
                val bitmap = downloadBitmap(imageUrl, cancellationCheck) ?: continue
                val comparison = compareArtworkAspectRatios(
                    firstWidth = bitmap.width,
                    firstHeight = bitmap.height,
                    secondWidth = bestBitmap?.width ?: 0,
                    secondHeight = bestBitmap?.height ?: 0,
                    targetRatio = targetAspectRatio
                )
                if (bestBitmap == null || comparison < 0) {
                    bestBitmap?.recycle()
                    bestBitmap = bitmap
                    bestDistance = kotlin.math.abs(kotlin.math.ln((bitmap.width.toFloat() / bitmap.height) / targetAspectRatio))
                } else {
                    bitmap.recycle()
                }
                if (bestDistance == 0f) break
            }

            cancellationCheck()
            val chosenBitmap = bestBitmap ?: return null
            return saveBitmap(fileName, chosenBitmap)
        } finally {
            bestBitmap?.recycle()
        }
    }

    private fun downloadBitmap(imageUrl: String, cancellationCheck: () -> Unit = {}): Bitmap? = runCatching {
        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        connection.doInput = true
        val temporaryFile = File.createTempFile("artwork-", ".download", context.cacheDir)
        try {
            connection.connect()
            require(connection.responseCode in 200..299) { "Artwork download failed" }
            require(connection.contentLengthLong <= MAX_DOWNLOAD_BYTES) { "Artwork download is too large" }
            connection.inputStream.use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        cancellationCheck()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_DOWNLOAD_BYTES) { "Artwork download is too large" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporaryFile.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid artwork image" }
            val options = BitmapFactory.Options().apply {
                inSampleSize = artworkSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_DIMENSION)
            }
            BitmapFactory.decodeFile(temporaryFile.path, options)
        } finally {
            connection.disconnect()
            temporaryFile.delete()
        }
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        null
    }

    fun saveBitmap(fileName: String, bitmap: Bitmap): String? {
        val file = imageFile(fileName)
        return synchronized(writeLocks[(file.absolutePath.hashCode() and Int.MAX_VALUE) % writeLocks.size]) {
            val atomicFile = AtomicFile(file)
            runCatching {
                val output = atomicFile.startWrite()
                try {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode artwork" }
                    atomicFile.finishWrite(output)
                } catch (error: Throwable) {
                    atomicFile.failWrite(output)
                    throw error
                }
                stampArtworkUri(Uri.fromFile(file).toString())
            }.getOrNull()
        }
    }

    private fun imageFile(fileName: String): File {
        val directory = File(context.filesDir, "shortcut-icons").apply { mkdirs() }
        return File(directory, "$fileName.png")
    }

    private companion object {
        const val MAX_DOWNLOAD_BYTES = 16L * 1024 * 1024
        const val MAX_IMAGE_DIMENSION = 1024
        val writeLocks = Array(32) { Any() }
    }
}

private fun Drawable.toBitmap(): Bitmap? = when (this) {
    is BitmapDrawable -> bitmap
    else -> {
        val width = intrinsicWidth.takeIf { it > 0 } ?: 144
        val height = intrinsicHeight.takeIf { it > 0 } ?: 144
        runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                setBounds(0, 0, canvas.width, canvas.height)
                draw(canvas)
            }
        }.getOrNull()
    }
}
