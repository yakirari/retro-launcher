package com.ariy.retrolauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.ariy.retrolauncher.launch.NesLabelScans
import com.ariy.retrolauncher.model.GameEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Reviewed exact-ROM labels only. Manual stickers and ordinary covers are never rewritten. */
internal object NesCartridgeLabelStore {
    private val lock = Mutex()
    private var scans: Map<String, NesLabelScans.Scan>? = null

    suspend fun load(context: Context, game: GameEntry): Bitmap? = lock.withLock {
        if (game.cartridgeLabelUri != null) return@withLock null
        val uri = game.intentDataUri?.let(Uri::parse) ?: return@withLock null
        if (uri.scheme !in setOf("content", "file")) return@withLock null
        val metadata = if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) (0 until cursor.columnCount).map { cursor.getString(it).orEmpty() } else emptyList()
            }.orEmpty()
        } else File(requireNotNull(uri.path)).let { listOf(it.name, "${it.length()}", "${it.lastModified()}") }
        val extension = metadata.firstOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
            .substringAfterLast('.').lowercase()
        if (extension !in setOf("nes", "zip")) return@withLock null
        val root = File(context.cacheDir, "nes-cartridge-labels-v1").apply { mkdirs() }
        fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val key = digest("$uri:$metadata".toByteArray())
        val signatureFile = File(root, "$key.sha1")
        val signature = if (signatureFile.exists()) signatureFile.readText().takeIf { it.matches(Regex("[0-9a-f]{40}")) }
            else context.contentResolver.openInputStream(uri)?.buffered()?.use {
                NesLabelScans.identity(it, extension == "zip")
            }?.also { signatureFile.writeText(it) }
        if (signature == null) return@withLock null
        val catalog = scans ?: context.assets.open(NesLabelScans.MANIFEST).bufferedReader().use {
            NesLabelScans.parse(it.readText())
        }.also { scans = it }
        val scan = catalog[signature] ?: return@withLock null
        loadScan(root, signature, scan)
    }

    /** Also used by device source-review tests without requiring any game ROM. */
    internal suspend fun loadScan(root: File, signature: String, scan: NesLabelScans.Scan): Bitmap? {
        fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val revision = digest(scan.toString().toByteArray()).take(16)
        val cache = File(root, "$signature-$revision.png")
        if (cache.exists()) BitmapFactory.decodeFile(cache.path)?.let { return it }
        val failed = File(root, "$signature-$revision.retry")
        if (failed.exists() && System.currentTimeMillis() - failed.lastModified() < 24 * 60 * 60_000L) return null
        return try {
            currentCoroutineContext().ensureActive()
            val connection = URL(scan.url).openConnection() as HttpURLConnection
            val bytes = try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 20_000
                check(connection.responseCode == 200)
                check(connection.url.protocol == "https" &&
                    (connection.url.host == "archive.org" || connection.url.host.endsWith(".archive.org")))
                connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        check(output.size() + count <= 8 * 1024 * 1024)
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            } finally { connection.disconnect() }
            check(digest(bytes) == scan.sha256)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            check(bounds.outWidth == scan.sourceWidth && bounds.outHeight == scan.sourceHeight)
            // Region decode avoids retaining a 16-megapixel full cartridge in the tile pipeline.
            @Suppress("DEPRECATION")
            val decoder = requireNotNull(android.graphics.BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false))
            val sticker = try {
                requireNotNull(decoder.decodeRegion(android.graphics.Rect(scan.left, scan.top,
                    scan.left + scan.width, scan.top + scan.height), BitmapFactory.Options().apply { inSampleSize = 2 }))
            } finally { decoder.recycle() }
            val temp = File(root, "$signature.tmp")
            try {
                temp.outputStream().use { check(sticker.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                check(temp.renameTo(cache))
            } finally { temp.delete() }
            sticker
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed.writeText(""); null }
    }
}