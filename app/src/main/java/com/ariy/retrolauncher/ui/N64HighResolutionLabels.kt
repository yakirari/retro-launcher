package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ariy.retrolauncher.launch.N64LabelScans
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException

/** Download one verified original scan and extract only its reviewed sticker rectangle. */
internal object N64HighResolutionLabels {
    fun load(root: File, signature: String, scans: Map<String, N64LabelScans.Scan>): Bitmap? {
        val scan = scans[signature] ?: return null
        // Changing crop/source metadata invalidates prepared labels without clearing other caches.
        val revision = MessageDigest.getInstance("SHA-256").digest(scan.toString().toByteArray())
            .take(8).joinToString("") { "%02x".format(it) }
        val cache = File(root, "$signature-$revision.png")
        if (cache.exists()) BitmapFactory.decodeFile(cache.path)?.let { return it }
        val failed = File(root, "$signature-$revision.retry")
        if (failed.exists() && System.currentTimeMillis() - failed.lastModified() < 24 * 60 * 60_000L) return null
        return try {
            val connection = URL(scan.url).openConnection() as HttpURLConnection
            val bytes = try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 12_000
                connection.setRequestProperty("User-Agent", "RetroLauncher-CartridgeLabels/1")
                check(connection.responseCode == 200)
                check(connection.url.protocol == "https" &&
                    (connection.url.host == "archive.org" || connection.url.host.endsWith(".archive.org")))
                connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        check(output.size() + count <= 1024 * 1024)
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            } finally { connection.disconnect() }
            check(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == scan.sha256)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            check(options.outWidth == scan.sourceWidth && options.outHeight == scan.sourceHeight)
            val original = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            val sticker = try { Bitmap.createBitmap(original, scan.left, scan.top, scan.width, scan.height) }
                finally { original.recycle() }
            val temp = File(root, "$signature-scan-v1.tmp")
            try {
                temp.outputStream().use { check(sticker.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                check(temp.renameTo(cache))
            } finally { temp.delete() }
            sticker
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed.writeText(""); null }
    }
}