package com.ariy.retrolauncher.launch

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class HandheldCartridgeArtworkTest {
    private val systems = mapOf("GB" to "gb", "GBC" to "gbc", "GBA" to "gba")
    private fun catalog(system: String) = HandheldCartridgeArtwork.parse(
        File("src/main/assets/artwork/${systems.getValue(system)}/index.json").readText(), system
    )
    private fun allLabels() = systems.keys.flatMap { catalog(it).entries }.associate { it.toPair() }

    @Test fun bundledCatalogContainsExactPlatformCrcMappings() {
        val labels = allLabels()
        assertEquals(347, labels.size)
        assertEquals(342, labels.values.count { it.title != null })
        assertEquals(5, labels.values.count { it.title == null })
        assertEquals("Gargoyle's Quest (USA, Europe)",
            HandheldCartridgeArtwork.find(labels, "GB", "0030e61f")?.title)
        assertEquals("Pocket Bomberman (Europe) (SGB Enhanced)",
            HandheldCartridgeArtwork.find(labels, "GB", "0FD1AE54")?.title)
        assertNotNull(HandheldCartridgeArtwork.find(labels, "GBA", "5B925FC9"))
        assertNull(HandheldCartridgeArtwork.find(labels, "GBC", "5B925FC9"))
    }

    @Test fun bundledImagesMatchTheirManifestHashesAndDimensions() {
        val labels = allLabels().values
        for (label in labels) {
            val image = File("src/main/assets/artwork/${systems.getValue(label.system)}/${label.path}")
            assertTrue("Missing ${label.path}", image.isFile)
            val bytes = image.readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            assertEquals(label.path, label.sha256, digest)
            assertTrue(label.path, bytes.size <= 4 * 1024 * 1024)
            assertArrayEquals(label.path, byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
                bytes.copyOfRange(0, 8))
            val width = ((bytes[16].toInt() and 255) shl 24) or ((bytes[17].toInt() and 255) shl 16) or
                ((bytes[18].toInt() and 255) shl 8) or (bytes[19].toInt() and 255)
            val height = ((bytes[20].toInt() and 255) shl 24) or ((bytes[21].toInt() and 255) shl 16) or
                ((bytes[22].toInt() and 255) shl 8) or (bytes[23].toInt() and 255)
            assertEquals(label.path, label.width, width)
            assertEquals(label.path, label.height, height)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAssetPathsOutsideTheSystemCrcNamespace() {
        HandheldCartridgeArtwork.parse(
            """{"version":1,"platform":"GB","labels":[{"system":"GB","crc32":"0030E61F","title":"Game","matchStatus":"exact","path":"../escape.png","sha256":"${"a".repeat(64)}","width":256,"height":256}]}""",
            "GB"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAStatusThatClaimsAnUnverifiedTitle() {
        val row = JSONObject().apply {
            put("system", "GB")
            put("crc32", "DA0224E3")
            put("title", JSONObject.NULL)
            put("matchStatus", "exact")
            put("path", "GB/GB-DA0224E3.png")
            put("sha256", "a".repeat(64))
            put("width", 256)
            put("height", 256)
        }
        HandheldCartridgeArtwork.parse(
            JSONObject().put("version", 1).put("platform", "GB")
                .put("labels", org.json.JSONArray().put(row)).toString(),
            "GB"
        )
    }
}
