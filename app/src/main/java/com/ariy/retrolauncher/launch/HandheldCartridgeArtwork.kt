package com.ariy.retrolauncher.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import org.json.JSONObject

internal object HandheldCartridgeArtwork {
    private const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    private val systems = setOf("GB", "GBC", "GBA")

    data class Label(
        val system: String,
        val crc32: String,
        val title: String?,
        val path: String,
        val sha256: String,
        val width: Int,
        val height: Int
    )

    @Volatile private var cached: Map<String, Map<String, Label>>? = null

    internal fun parse(text: String, expectedSystem: String): Map<String, Label> {
        val json = JSONObject(text)
        require(expectedSystem in systems)
        require(json.getInt("version") == 1 && json.getString("platform") == expectedSystem)
        val rows = json.getJSONArray("labels")
        val labels = linkedMapOf<String, Label>()
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val system = row.getString("system")
            val crc32 = row.getString("crc32")
            val path = row.getString("path")
            val sha256 = row.getString("sha256")
            val width = row.getInt("width")
            val height = row.getInt("height")
            val title = if (row.isNull("title")) null else row.getString("title").takeIf { it.isNotBlank() }

            require(system == expectedSystem)
            require(crc32.matches(Regex("[0-9A-F]{8}")))
            require(path == "labels/$system-$crc32.png")
            require(sha256.matches(Regex("[a-f0-9]{64}")))
            require(width in 64..4096 && height in 32..2048)
            require(row.getString("matchStatus") == if (title == null) "unmatched" else "exact")
            require(labels.put("$system:$crc32", Label(system, crc32, title, path, sha256, width, height)) == null)
        }
        return labels
    }

    internal fun find(labels: Map<String, Label>, system: String, checksum: String): Label? =
        labels["$system:${checksum.uppercase(Locale.ROOT)}"]

    fun find(context: Context, system: String, checksum: String): Label? =
        find(catalog(context, system), system, checksum)

    internal fun catalog(context: Context, system: String): Map<String, Label> {
        require(system in systems)
        return cached?.get(system) ?: synchronized(this) {
            val catalogs = cached.orEmpty()
            catalogs[system] ?: context.assets.open("artwork/${platformFolder(system)}/index.json")
                .bufferedReader().use { parse(it.readText(), system) }
                .also { cached = catalogs + (system to it) }
        }
    }

    fun load(context: Context, label: Label): Bitmap {
        val bytes = context.assets.open("artwork/${platformFolder(label.system)}/${label.path}").use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size() + count <= MAX_IMAGE_BYTES) { "Bundled cartridge artwork too large: ${label.path}" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
        check(digest == label.sha256) { "Bundled cartridge artwork hash mismatch: ${label.path}" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        check(bounds.outWidth == label.width && bounds.outHeight == label.height) {
            "Bundled cartridge artwork dimensions changed: ${label.path}"
        }
        val aspectRange = if (label.system == "GBA") 1.6f..2.3f else 0.85f..1.4f
        check(bounds.outWidth.toFloat() / bounds.outHeight in aspectRange) {
            "Bundled cartridge artwork has an unexpected aspect ratio: ${label.path}"
        }
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Could not decode bundled cartridge artwork: ${label.path}"
        }
    }

    private fun platformFolder(system: String): String = when (system) {
        "GB" -> "gb"
        "GBC" -> "gbc"
        "GBA" -> "gba"
        else -> error("Unsupported handheld artwork system: $system")
    }
}
