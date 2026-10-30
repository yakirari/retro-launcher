package com.ariy.retrolauncher.launch

import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.json.JSONObject

/** Full-file iNES SHA-1 identities, including the header: unknown/header-edited dumps fail closed. */
internal object NesLabelScans {
    const val MANIFEST = "cartridge-artwork/nes-scans.json"
    data class Scan(val url: String, val sha256: String, val sourceWidth: Int, val sourceHeight: Int,
        val left: Int, val top: Int, val width: Int, val height: Int)

    fun parse(text: String): Map<String, Scan> {
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1 && root.getString("platform") == "Nintendo Entertainment System")
        return buildMap {
            val entries = root.getJSONArray("entries")
            for (i in 0 until entries.length()) {
                val entry = entries.getJSONObject(i)
                if (!entry.optBoolean("reviewed")) continue
                val crop = entry.getJSONArray("crop")
                require(crop.length() == 4)
                val scan = Scan(entry.getString("url"), entry.getString("sha256"),
                    entry.getInt("sourceWidth"), entry.getInt("sourceHeight"),
                    crop.getInt(0), crop.getInt(1), crop.getInt(2), crop.getInt(3))
                val uri = java.net.URI(scan.url)
                require(uri.scheme == "https" && uri.host == "archive.org" && uri.userInfo == null &&
                    uri.path.startsWith("/download/") && uri.query == null && uri.fragment == null &&
                    !uri.path.contains(".."))
                require(scan.sha256.matches(Regex("[0-9a-f]{64}")))
                require(scan.sourceWidth in 64..5000 && scan.sourceHeight in 64..5000)
                require(scan.left >= 0 && scan.top >= 0 && scan.width in 200..5000 && scan.height in 200..5000 &&
                    scan.left.toLong() + scan.width <= scan.sourceWidth &&
                    scan.top.toLong() + scan.height <= scan.sourceHeight)
                require(scan.width.toFloat() / scan.height in 0.55f..0.75f)
                val identities = entry.getJSONArray("romSha1")
                require(identities.length() > 0)
                for (j in 0 until identities.length()) {
                    val identity = identities.getString(j)
                    require(identity.matches(Regex("[0-9a-f]{40}")) && !containsKey(identity))
                    put(identity, scan)
                }
            }
        }
    }

    /** Bounded raw/single-ROM ZIP reader; no title guessing, header rewriting or ROM upload. */
    fun identity(input: InputStream, zipped: Boolean): String? {
        var expanded = 0L
        fun hash(stream: InputStream, rom: Boolean): String? {
            val digest = MessageDigest.getInstance("SHA-1")
            val header = ByteArray(16)
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (total < 16) buffer.copyInto(header, total.toInt(), 0, minOf(count, 16 - total.toInt()))
                total += count
                expanded += count
                if (expanded > 16L * 1024 * 1024) return null
                if (rom) digest.update(buffer, 0, count)
            }
            if (!rom) return ""
            if (total <= 16 || header.take(4) != listOf(0x4e.toByte(), 0x45.toByte(), 0x53.toByte(), 0x1a.toByte())) return null
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        if (!zipped) return hash(input, true)
        val zip = ZipInputStream(input)
        var identity: String? = null
        var count = 0
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++count > 128) return null
            val rom = !entry.isDirectory && entry.name.endsWith(".nes", ignoreCase = true)
            if (rom && identity != null) return null
            val result = hash(zip, rom) ?: return null
            if (rom) identity = result
            zip.closeEntry()
        }
        return identity
    }
}