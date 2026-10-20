package com.ariy.retrolauncher.launch

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class CartridgeRomChecksumTest {
    private val rom = "123456789".toByteArray()
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
        }
    }.toByteArray()

    @Test fun knownCrcAndUppercaseEightDigitFormat() {
        assertEquals("CBF43926", CartridgeRomChecksum.read(ByteArrayInputStream(rom), false))
        assertNull(CartridgeRomChecksum.read(ByteArrayInputStream(byteArrayOf()), false))
    }

    @Test fun n64HeaderSignatureNormalizesByteOrderAndIgnoresRomBody() {
        val data = ByteArray(16384) { (it * 7).toByte() }
        byteArrayOf(0x80.toByte(), 0x37, 0x12, 0x40).copyInto(data)
        fun signature(bytes: ByteArray, zipped: Boolean = false) = CartridgeRomChecksum.read(
            ByteArrayInputStream(bytes), zipped, setOf("z64", "v64", "n64"), n64Header = true)
        val expected = java.util.zip.CRC32().apply { update(data, 0, 8192) }.value
        val value = "%08X".format(expected)
        assertEquals(value, signature(data))
        for (swap in listOf(1, 3)) {
            val swapped = ByteArray(data.size) { data[it xor swap] }
            assertEquals(value, signature(swapped))
        }
        assertEquals(value, signature(data.copyOf().apply { fill(0, 8192) }))
        assertEquals(value, signature(zip("game.z64" to data), true))
        assertNull(signature(zip("one.z64" to data, "two.v64" to data), true))
        assertNull(signature(data.copyOf(100)))
        assertNull(signature(ByteArray(8192)))
    }

    @Test fun zipHashesOnlyRomBytesNotArchiveOrReadme() {
        val bytes = zip("readme.txt" to "ignore".toByteArray(), "nested/game.GBA" to rom)
        assertEquals("CBF43926", CartridgeRomChecksum.read(ByteArrayInputStream(bytes), true))
    }

    @Test fun ambiguousOrRomlessArchivesDoNotGuess() {
        assertNull(CartridgeRomChecksum.read(ByteArrayInputStream(zip("one.gba" to rom, "two.gba" to rom)), true))
        assertNull(CartridgeRomChecksum.read(ByteArrayInputStream(zip("cover.png" to rom)), true))
    }

    @Test fun gbAndGbcZipMembersUseTheirOwnSystemExtensions() {
        for (extension in listOf("gb", "gbc")) {
            val bytes = zip("readme.txt" to rom, "nested/game.${extension.uppercase()}" to rom)
            assertEquals("CBF43926", CartridgeRomChecksum.read(ByteArrayInputStream(bytes), true, setOf(extension)))
            assertNull(CartridgeRomChecksum.read(ByteArrayInputStream(bytes), true, setOf("gba")))
        }
        val dual = zip("dual.gb" to rom)
        assertEquals("CBF43926", CartridgeRomChecksum.read(ByteArrayInputStream(dual), true, setOf("gb", "gbc")))
        val ambiguous = zip("one.gb" to rom, "two.gbc" to rom)
        assertNull(CartridgeRomChecksum.read(ByteArrayInputStream(ambiguous), true, setOf("gb", "gbc")))
    }
}