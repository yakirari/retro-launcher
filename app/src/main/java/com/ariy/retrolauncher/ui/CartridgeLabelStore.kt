package com.ariy.retrolauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.ariy.retrolauncher.launch.CartridgeRomChecksum
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject

/** Exact CRC matches only. No title substitution, ROM upload, credentials, or library mutation. */
internal object CartridgeLabelStore {
    private const val BASE = "https://art.slot-cfw.fyi/"
    private const val DAY = 24 * 60 * 60 * 1000L
    private val workers = Semaphore(2)
    private val indexLock = Mutex()
    private var index: JSONObject? = null

    suspend fun load(context: Context, game: GameEntry): Bitmap? = workers.withPermit {
        val system = when (GamePlatforms.normalizeOrNull(game.platform)) {
            "Game Boy" -> "GB"
            "Game Boy Color" -> "GBC"
            "Game Boy Advance" -> "GBA"
            else -> return@withPermit null
        }
        if (game.cartridgeLabelUri != null) return@withPermit null
        val romExtensions = when (system) {
            "GB" -> setOf("gb")
            "GBC" -> setOf("gbc", "gb") // Dual-compatible Color releases can retain .gb.
            else -> setOf("gba")
        }
        val uri = game.intentDataUri?.let(Uri::parse) ?: return@withPermit null
        if (uri.scheme !in listOf("content", "file")) return@withPermit null
        val metadata = if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) (0 until cursor.columnCount).joinToString(":") { cursor.getString(it).orEmpty() }
                else ""
            }.orEmpty()
        } else File(requireNotNull(uri.path)).let { "${it.name}:${it.length()}:${it.lastModified()}" }
        val extension = uri.lastPathSegment.orEmpty().substringAfterLast('.').lowercase()
        if (extension !in romExtensions && extension != "zip") return@withPermit null
        val root = File(context.cacheDir, "cartridge-labels-v1").apply { mkdirs() }
        val identity = MessageDigest.getInstance("SHA-256").digest("$uri:$metadata".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val checksumFile = File(root, "$system-$identity.crc")
        val checksum = if (checksumFile.exists()) checksumFile.readText() else {
            context.contentResolver.openInputStream(uri)?.buffered()?.use {
                CartridgeRomChecksum.read(it, extension == "zip", romExtensions)
            }?.also { checksumFile.writeText(it) }
        } ?: return@withPermit null
        val imageFile = File(root, "$system-$checksum.png")
        if (imageFile.exists()) BitmapFactory.decodeFile(imageFile.path)?.let { return@withPermit it }
        val miss = File(root, "$system-$checksum.missing")
        if (miss.exists() && System.currentTimeMillis() - miss.lastModified() < DAY) return@withPermit null
        val catalog = indexLock.withLock {
            index ?: run {
                val file = File(root, "index.json")
                val text = if (file.exists() && System.currentTimeMillis() - file.lastModified() < DAY) file.readText()
                    else download("index.json", 8 * 1024 * 1024).toString(Charsets.UTF_8).also { file.writeText(it) }
                JSONObject(text).also { index = it }
            }
        }
        val path = catalog.optJSONObject(checksum)?.optString("support-texture").orEmpty()
        // Never follow arbitrary paths/hosts supplied by a remote catalog.
        if (!path.matches(Regex("$system/support-texture/[0-9A-Fa-f]{8}\\.png"))) {
            miss.writeText("")
            return@withPermit null
        }
        val bytes = download(path, 4 * 1024 * 1024)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        // GBA labels are wide; GB/GBC labels are near-square. Never reuse another system's shape.
        val aspectRange = if (system == "GBA") 1.6f..2.3f else 0.85f..1.4f
        if (bounds.outWidth !in 64..4096 || bounds.outHeight !in 32..2048 ||
            bounds.outWidth.toFloat() / bounds.outHeight !in aspectRange) return@withPermit null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withPermit null
        imageFile.writeBytes(bytes)
        bitmap
    }

    private fun download(path: String, limit: Int): ByteArray {
        val connection = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "RetroLauncher-CartridgeLabels/1")
            check(connection.responseCode == 200) { "Cartridge artwork unavailable" }
            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= limit) { "Cartridge artwork too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }
}