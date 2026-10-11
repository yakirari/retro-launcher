package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer

class ArcadeCabinetArtTest {
    @Test fun everyInventoryRomHasAFullTransparentCabinetAndAliasesResolve() {
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
        mappings.values.distinctBy { it.asset }.forEach { art ->
            val source = File(root, "artwork/full-cabinets/${art.asset}")
            val packaged = File(assets, "full_cabinets/${art.asset}")
            assertTrue("Missing packaged ${art.asset}", packaged.isFile)
            assertArrayEquals(source.readBytes(), packaged.readBytes())
            val bytes = packaged.readBytes()
            assertArrayEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), bytes.copyOfRange(0, 8))
            assertEquals(960, ByteBuffer.wrap(bytes, 16, 4).int)
            assertEquals(768, ByteBuffer.wrap(bytes, 20, 4).int)
            assertEquals(6, bytes[25].toInt()) // RGBA; decoded alpha/shape audited by standalone Java verifier.
        }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsafeAssetPaths() {
        parseArcadeCabinetArt("""{"mappings":[{"rom":"bad","asset":"../outside.png","kind":"missing"}]}""")
    }
}