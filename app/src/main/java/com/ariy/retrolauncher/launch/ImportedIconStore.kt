package com.ariy.retrolauncher.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.ariy.retrolauncher.stampArtworkUri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ImportedIconStore(private val context: Context) {
    fun saveShortcutIcon(fileName: String, drawable: Drawable): String? {
        val bitmap = drawable.toBitmap() ?: return null
        return saveBitmap(fileName, bitmap)
    }

    fun existingBitmapUri(fileName: String): String? {
        val file = imageFile(fileName)
        if (!file.exists()) return null
        return stampArtworkUri(Uri.fromFile(file).toString(), file.lastModified())
    }

    fun downloadImage(fileName: String, imageUrl: String): String? {
        val bitmap = runCatching {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.doInput = true
            connection.connect()
            connection.inputStream.use(BitmapFactory::decodeStream)
        }.getOrNull() ?: return null
        return saveBitmap(fileName, bitmap)
    }

    fun downloadFirstAvailableImage(
        fileName: String,
        imageUrls: Iterable<String>,
        maxAttempts: Int = 6
    ): String? {
        if (maxAttempts <= 0) return null
        return imageUrls
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(maxAttempts)
            .firstNotNullOfOrNull { imageUrl -> downloadImage(fileName, imageUrl) }
    }

    fun saveBitmap(fileName: String, bitmap: Bitmap): String? {
        val file = imageFile(fileName)
        return runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            stampArtworkUri(Uri.fromFile(file).toString())
        }.getOrNull()
    }

    private fun imageFile(fileName: String): File {
        val directory = File(context.filesDir, "shortcut-icons").apply { mkdirs() }
        return File(directory, "$fileName.png")
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
