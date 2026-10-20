package com.ariy.retrolauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.ariy.retrolauncher.launch.CartridgeRomChecksum
import com.ariy.retrolauncher.launch.N64LabelDatabase
import com.ariy.retrolauncher.launch.N64LabelScans
import com.ariy.retrolauncher.model.GameEntry
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exact normalized ROM-header CRC matches. Never replaces ordinary box covers or uploads ROMs. */
internal object N64CartridgeLabelStore {
    private val lock = Mutex()
    private var reviewedScans: Map<String, N64LabelScans.Scan>? = null

    suspend fun load(context: Context, game: GameEntry): Bitmap? = lock.withLock {
        if (game.cartridgeLabelUri != null) return@withLock null
        val uri = game.intentDataUri?.let(Uri::parse) ?: return@withLock null
        if (uri.scheme !in setOf("content", "file")) return@withLock null
        val extension = uri.lastPathSegment.orEmpty().substringAfterLast('.').lowercase()
        if (extension !in setOf("zip", "z64", "n64", "v64")) return@withLock null
        val root = File(context.cacheDir, "n64-cartridge-labels-v1").apply { mkdirs() }
        val metadata = if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) (0 until cursor.columnCount).joinToString(":") { cursor.getString(it).orEmpty() } else ""
            }.orEmpty()
        } else File(requireNotNull(uri.path)).let { "${it.name}:${it.length()}:${it.lastModified()}" }
        // Avoid reopening/decompressing a large ROM while scrolling; metadata changes invalidate it.
        val identity = MessageDigest.getInstance("SHA-256").digest("$uri:$metadata".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val signatureFile = File(root, "$identity.crc")
        val signature = if (signatureFile.exists()) signatureFile.readText() else
            context.contentResolver.openInputStream(uri)?.buffered()?.use {
                CartridgeRomChecksum.read(it, extension == "zip", setOf("z64", "n64", "v64"), n64Header = true)
            }?.also { signatureFile.writeText(it) } ?: return@withLock null
        // Prefer real scan pixels over the tiny database thumbnail, including on existing installs.
        val scans = reviewedScans ?: context.assets.open(N64LabelScans.MANIFEST).bufferedReader().use {
            N64LabelScans.parse(it.readText())
        }.also { reviewedScans = it }
        N64HighResolutionLabels.load(root, signature, scans)?.let { return@withLock it }
        val cached = File(root, "$signature.png")
        if (cached.exists()) BitmapFactory.decodeFile(cached.path)?.let { return@withLock it }
        val database = File(root, "labels.db")
        if (!database.exists()) {
            val temp = File(root, "labels.download")
            val connection = URL(N64LabelDatabase.URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("User-Agent", "RetroLauncher-CartridgeLabels/1")
                check(connection.responseCode == 200)
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                connection.inputStream.use { input -> temp.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= N64LabelDatabase.SIZE)
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                } }
                check(total == N64LabelDatabase.SIZE && digest.digest().joinToString("") { "%02x".format(it) } == N64LabelDatabase.SHA256)
                check(temp.renameTo(database))
            } finally { connection.disconnect(); temp.delete() }
        }
        val pixels = N64LabelDatabase.read(database, signature) ?: return@withLock null
        Bitmap.createBitmap(pixels, N64LabelDatabase.WIDTH, N64LabelDatabase.HEIGHT, Bitmap.Config.ARGB_8888)
            .also { bitmap -> cached.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
}