package com.ariy.retrolauncher.launch

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class N64LabelDatabaseTest {
    @Test fun readsExactSignatureAndBgraPixelsWithoutGuessing() {
        val file = File.createTempFile("n64-label-test", ".db")
        try {
            val bytes = ByteArray(0x4100 + 25_600)
            bytes[0] = 7
            "Analogue-Co".toByteArray().copyInto(bytes, 1)
            bytes.fill(0xff.toByte(), 0x100, 0x4100)
            byteArrayOf(0x78, 0x56, 0x34, 0x12).copyInto(bytes, 0x100)
            byteArrayOf(0x33, 0x22, 0x11, 0xff.toByte()).copyInto(bytes, 0x4100)
            file.writeBytes(bytes)
            val pixels = requireNotNull(N64LabelDatabase.read(file, "12345678"))
            assertEquals(74 * 86, pixels.size)
            assertEquals(0xff112233.toInt(), pixels[0])
            assertNull(N64LabelDatabase.read(file, "87654321"))
            file.writeBytes(bytes.copyOf(0x4104))
            assertNull(N64LabelDatabase.read(file, "12345678"))
        } finally { file.delete() }
    }
}