package com.ariy.retrolauncher.launch

import java.io.File
import java.io.RandomAccessFile

/** Analogue 3D labels: 4096 little-endian header CRCs followed by padded BGRA images. */
internal object N64LabelDatabase {
    const val WIDTH = 74
    const val HEIGHT = 86
    const val SIZE = 23_082_240L
    const val SHA256 = "0378f644c232d417840094571457cc8b31ea9720c1f003dcd8c9af269145fd18"
    const val URL = "https://archive.org/download/Analogue3Dcartdatabase/labels.db"

    fun read(file: File, signature: String): IntArray? = RandomAccessFile(file, "r").use { input ->
        if (input.length() < 0x4100 || input.readUnsignedByte() != 7) return null
        val brand = ByteArray(11).also(input::readFully).toString(Charsets.US_ASCII)
        if (brand != "Analogue-Co") return null
        val wanted = signature.toLongOrNull(16) ?: return null
        input.seek(0x100)
        var position = -1
        for (i in 0 until 4096) {
            val crc = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            if (crc == 0xffffffffL) break
            if (crc == wanted) { position = i; break }
        }
        if (position < 0) return null
        val offset = 0x4100L + position * 25_600L
        if (offset + WIDTH * HEIGHT * 4 > input.length()) return null
        input.seek(offset)
        IntArray(WIDTH * HEIGHT) {
            val blue = input.readUnsignedByte()
            val green = input.readUnsignedByte()
            val red = input.readUnsignedByte()
            val alpha = input.readUnsignedByte()
            (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
}