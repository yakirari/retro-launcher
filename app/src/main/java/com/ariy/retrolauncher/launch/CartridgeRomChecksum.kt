package com.ariy.retrolauncher.launch

import java.io.InputStream
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipInputStream

/** Hash the ROM bytes, never the ZIP container. Ambiguous multi-ROM archives are not guessed. */
internal object CartridgeRomChecksum {
    private const val MAX_BYTES = 64L * 1024 * 1024

    fun read(input: InputStream, zipped: Boolean, romExtensions: Set<String> = setOf("gba"),
        n64Header: Boolean = false): String? {
        fun hash(stream: InputStream): String? {
            val crc = CRC32()
            val buffer = ByteArray(32 * 1024)
            val header = if (n64Header) ByteArray(8192) else null
            var total = 0L
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (header != null && total < header.size) {
                    buffer.copyInto(header, total.toInt(), 0, minOf(count, header.size - total.toInt()))
                }
                total += count
                if (total > MAX_BYTES) return null
                crc.update(buffer, 0, count)
            }
            if (header != null) {
                if (total < header.size) return null
                // Normalize .z64 (big endian), .v64 (byte-swapped) and .n64 (word-swapped).
                val magic = header.take(4).map { it.toInt() and 255 }
                val swap = when (magic) {
                    listOf(0x80, 0x37, 0x12, 0x40) -> 0
                    listOf(0x37, 0x80, 0x40, 0x12) -> 1
                    listOf(0x40, 0x12, 0x37, 0x80) -> 3
                    else -> return null
                }
                crc.reset()
                for (i in header.indices) crc.update(header[i xor swap].toInt() and 255)
            }
            return if (total == 0L) null else String.format(Locale.ROOT, "%08X", crc.value)
        }
        if (!zipped) return hash(input)
        val zip = ZipInputStream(input)
        var checksum: String? = null
        var entries = 0
        var expanded = 0L
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > 128) return null
            if (!entry.isDirectory && entry.name.substringAfterLast('.').lowercase(Locale.ROOT) in romExtensions) {
                if (checksum != null) return null
                checksum = hash(zip) ?: return null
            } else {
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    expanded += count
                    if (expanded > MAX_BYTES) return null
                }
            }
            zip.closeEntry()
        }
        return checksum
    }
}