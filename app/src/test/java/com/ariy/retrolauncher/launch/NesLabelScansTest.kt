package com.ariy.retrolauncher.launch

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class NesLabelScansTest {
    private fun rom() = ByteArray(32784).apply {
        this[0] = 0x4e; this[1] = 0x45; this[2] = 0x53; this[3] = 0x1a; this[4] = 2
    }
    private fun zip(vararg names: String): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip -> names.forEach {
            zip.putNextEntry(ZipEntry(it)); zip.write(rom()); zip.closeEntry()
        } }
    }.toByteArray()

    @Test fun rawAndSingleRomZipHaveTheSameExactIdentity() {
        val expected = MessageDigest.getInstance("SHA-1").digest(rom()).joinToString("") { "%02x".format(it) }
        assertEquals(expected, NesLabelScans.identity(ByteArrayInputStream(rom()), false))
        assertEquals(expected, NesLabelScans.identity(ByteArrayInputStream(zip("readme.txt", "game.NES")), true))
        val changed = rom().apply { this[7] = 8 }
        assertNotEquals(expected, NesLabelScans.identity(ByteArrayInputStream(changed), false))
    }

    @Test fun ambiguousInvalidAndOversizedInputsAreRejected() {
        assertNull(NesLabelScans.identity(ByteArrayInputStream(zip("a.nes", "b.nes")), true))
        assertNull(NesLabelScans.identity(ByteArrayInputStream(ByteArray(100)), false))
        assertNull(NesLabelScans.identity(ByteArrayInputStream(rom().copyOf(16 * 1024 * 1024 + 1)), false))
        assertNull(NesLabelScans.identity(ByteArrayInputStream(zip("readme.txt")), true))
    }

    private fun manifest() = File("src/main/assets/cartridge-artwork/nes-scans.json").readText()
    @Test fun reviewedManifestMatchesOnlyVerifiedUsaOriginal() {
        val scans = NesLabelScans.parse(manifest())
        assertEquals(setOf("799459548f9636dd263200da494f04058f5540e2"), scans.keys)
        assertEquals(1770, scans.values.single().width)
        assertTrue(NesLabelScans.parse(manifest().replace("\"reviewed\": true", "\"reviewed\": false")).isEmpty())
    }

    @Test fun unsafeSourceOrOutOfBoundsCropIsRejected() {
        for (text in listOf(manifest().replace("https://archive.org/download/", "https://example.com/download/"),
                manifest().replace("[1548, 0, 1770, 2934]", "[3000, 0, 1770, 2934]"))) {
            assertTrue(runCatching { NesLabelScans.parse(text) }.isFailure)
        }
    }
}