package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.File

class ArcadeImportPolicyTest {
    private fun rom(name: String) = ArcadeRomForImport(name, "content://rom/$name")

    @Test fun importsKnownAndUnknownGamesWithoutRequiringArtwork() {
        val result = selectArcadeRomsForImport(listOf(rom("digdug2.zip"), rom("newgame.zip")), emptySet(), emptySet())
        assertEquals(2, result.size)
        assertTrue(result.any { it.title == "Dig Dug II" })
        assertTrue(result.any { it.title == "newgame" })
    }
    @Test fun prefersOriginalOverCopiesRegardlessOfInputOrder() {
        val result = selectArcadeRomsForImport(listOf(rom("1942 (1).zip"), rom("1942.zip"), rom("1942.7z")), emptySet(), emptySet())
        assertEquals("content://rom/1942.zip", result.single().romUri)
    }
    @Test fun doesNotCollapseSequelsOrDistinctEditionsBySharedArtwork() {
        val result = selectArcadeRomsForImport(listOf(rom("dkongo.zip"), rom("dkongjr.zip"), rom("dkong3.zip"), rom("mk3.zip"), rom("umk3.zip")), emptySet(), emptySet())
        assertEquals(5, result.size)
    }
    @Test fun existingRomKeysAndUrisPreventReimportWhileOtherPlatformsStaySeparate() {
        val result = selectArcadeRomsForImport(listOf(rom("ffightu.zip"), rom("sf2.zip"), rom("dkong3.zip")), setOf("ffight"), setOf("content://rom/sf2.zip"))
        assertEquals("Donkey Kong 3", result.single().title)
    }
    @Test fun unrelatedFilesAreNotRoms() {
        assertTrue(selectArcadeRomsForImport(listOf(rom("notes.txt"), rom("art.png")), emptySet(), emptySet()).isEmpty())
    }
    @Test fun fullDeviceInventoryIsImportableWithoutAnyCabinetAssets() {
        val metadata = JSONObject(File("src/main/assets/arcade-metadata/metadata.json").readText()).getJSONArray("sets")
        val inventory = (0 until metadata.length()).map { metadata.getJSONObject(it) }
            .filter { it.optBoolean("inventory") }.map { rom(it.getString("rom") + ".zip") }
        assertEquals(383, inventory.size)
        val result = selectArcadeRomsForImport(inventory, emptySet(), emptySet())
        val expectedKeys = inventory.map { arcadeRomIdentity(it.fileName) }.toSet()
        assertEquals(expectedKeys.size, result.size)
        assertTrue(result.size > 370)
        val repeat = selectArcadeRomsForImport(inventory, expectedKeys.filterNotNull().toSet(), emptySet())
        assertTrue(repeat.isEmpty())
        println("Device inventory: ${inventory.size} ROM sets -> ${result.size} distinct games, all importable without artwork")
    }
}