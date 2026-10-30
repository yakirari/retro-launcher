package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

class ArcadeCabinetArtTest {
    @Test fun everyInventoryRomHasASuppliedFrontAndAliasesResolve() {
        val root = File("..").canonicalFile
        val assets = File(root, "app/src/main/assets")
        val mappings = parseArcadeCabinetArt(File(assets, "arcade-metadata/cabinet-art.json").readText())
        val sets = JSONObject(File(assets, "arcade-metadata/metadata.json").readText()).getJSONArray("sets")
        val inventory = (0 until sets.length()).map { sets.getJSONObject(it) }.filter { it.optBoolean("inventory") }
        assertEquals(383, inventory.size)
        inventory.forEach { set ->
            assertNotNull("Missing ${set.getString("rom")}", mappings[set.getString("rom")])
        }
        val kinds = inventory.groupingBy { mappings.getValue(it.getString("rom")).kind }.eachCount()
        assertEquals(131, kinds["period-style-conversion"])
        val presentations = JSONObject(File(assets, "arcade_presentations/index.json").readText())
            .getJSONArray("mappings")
            .let { rows -> (0 until rows.length()).associate { rows.getJSONObject(it).getString("asset") to rows.getJSONObject(it) } }
        mappings.values.distinctBy { it.asset }.forEach { art ->
            val packaged = File(assets, "arcade_presentations/front/${art.asset}")
            assertTrue("Missing packaged ${art.asset}", packaged.isFile)
            val bytes = packaged.readBytes()
            val expectedHash = presentations.getValue(art.asset).getString("frontSha256")
            val actualHash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { byte -> "%02x".format(byte) }
            assertEquals("Unexpected hash for ${art.asset}", expectedHash, actualHash)
            assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), bytes.copyOfRange(0, 8))
            assertEquals(460, ByteBuffer.wrap(bytes, 16, 4).int)
            assertEquals(1000, ByteBuffer.wrap(bytes, 20, 4).int)
        }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsafeAssetPaths() {
        parseArcadeCabinetArt("""{"mappings":[{"rom":"bad","asset":"../outside.png","kind":"missing"}]}""")
    }
}