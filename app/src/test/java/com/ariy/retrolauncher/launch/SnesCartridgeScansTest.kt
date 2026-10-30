package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test

class SnesCartridgeScansTest {
    private val scan = SnesCartridgeScans.Label("Future Game (Europe)", "labels/future-europe.jpg", 570, 200)
    @Test fun bundledScansSupportRegionlessCompactFilenames() {
        val labels = SnesCartridgeScans.parse(java.io.File("src/main/assets/snes_labels/index.json").readText())
        assertEquals(109, labels.size)
        assertTrue(labels.all { it.path.startsWith("labels/") && it.path.endsWith(".jpg") })
        assertEquals(48, labels.count { it.sourcePack == "supplied-jpeg-pack" })
        assertEquals(61, labels.count { it.sourcePack == "game-labels-batch2-final" })
        for (label in labels.filter { it.sourcePack == "supplied-jpeg-pack" }) {
            val rom = label.title.substringBefore(" (").lowercase().replace(Regex("[^a-z0-9]"), "") + ".zip"
            assertNotNull("${label.title}: $rom",
                SnesCartridgeScans.find(labels.filter { it.sourcePack == "supplied-jpeg-pack" }, rom))
        }
        assertEquals("labels/doom-europe.jpg", SnesCartridgeScans.findWithFallback(labels, "doom.zip")?.path)
        assertEquals("labels/doom-usa.jpg",
            SnesCartridgeScans.findWithFallback(labels, "Doom (USA).zip")?.path)
        for (title in listOf("megaman10", "megaman12", "megaman13")) {
            assertEquals("labels/$title.jpg", SnesCartridgeScans.findWithFallback(labels, "$title.sfc")?.path)
        }
    }
    @Test fun suppliedLabelCatalogMatchesKnownAndMissingRegions() {
        val labels = SnesCartridgeScans.parse(java.io.File("src/main/assets/snes_labels/index.json").readText())
            .filter { it.sourcePack == "game-labels-batch2-final" }
        assertEquals(61, labels.size)
        for (label in labels) {
            assertEquals(label, SnesCartridgeScans.find(labels, "${label.title}.zip"))
            assertNotNull(label.title, SnesCartridgeScans.find(labels, "${label.title.substringBefore(" (")}.zip"))
        }
        assertEquals("labels/aladdin-usa.jpg", SnesCartridgeScans.find(labels, "aladdin (USA).zip")?.path)
        assertEquals("labels/aladdin-japan.jpg", SnesCartridgeScans.find(labels, "aladdin (Japan).zip")?.path)
        assertEquals("labels/starfox-japan.jpg", SnesCartridgeScans.find(labels, "starfox (Japan).zip")?.path)
        assertEquals("labels/chronotrigger-usa.jpg", SnesCartridgeScans.find(labels, "chronotrigger.zip")?.path)
        assertNull(SnesCartridgeScans.find(labels, "Chrono Trigger (Japan).zip"))
        assertEquals("labels/fzero-japan.jpg", SnesCartridgeScans.find(labels, "F-Zero (Japan).sfc")?.path)
        assertEquals("labels/doom-usa.jpg", SnesCartridgeScans.find(labels, "doom.zip")?.path)
        assertEquals("labels/superbomberman3-japan.jpg",
            SnesCartridgeScans.find(labels, "superbomberman3.zip")?.path)
        assertEquals("labels/supermariorpglegendofthesevenstars-usa.jpg",
            SnesCartridgeScans.find(labels, "Super Mario RPG - Legend of the Seven Stars (USA).zip")?.path)
        val regionlessMarioRpg = SnesCartridgeScans.find(labels, "supermariorpglegendofthesevenstars.zip")
        assertEquals("labels/supermariorpglegendofthesevenstars-japan.jpg", regionlessMarioRpg?.path)
        assertTrue(regionlessMarioRpg!!.width > regionlessMarioRpg.height)
        assertEquals("labels/supermariorpglegendofthesevenstars-japan.jpg",
            SnesCartridgeScans.findForRom(labels, "supermariorpglegendofthesevenstars (2).sfc")?.path)
        assertEquals("labels/supermariorpglegendofthesevenstars-usa.jpg",
            SnesCartridgeScans.findForRom(labels, "supermariorpglegendofthesevenstars (USA) (2).sfc")?.path)
        assertNull(SnesCartridgeScans.findForRom(labels, "supermariorpglegendofthesevenstars (Proto) (2).sfc"))
    }
    @Test fun updatedCatalogTakesPriorityAndFinalBatchLabelsRemainFallbacks() {
        val updated = SnesCartridgeScans.Label("Doom (Europe)", "labels/doom-europe.jpg", 1800, 621)
        val restored = SnesCartridgeScans.Label("Doom (USA)", "labels/doom-usa.jpg", 2044, 1030, "game-labels-batch2-final")
        assertEquals(updated, SnesCartridgeScans.findWithFallback(listOf(updated), listOf(restored), "doom.zip"))
        assertEquals(restored, SnesCartridgeScans.findWithFallback(listOf(updated), listOf(restored), "Doom (USA).zip"))
    }
    @Test fun regionAndEditionMustMatchPhysicalScan() {
        assertEquals(scan, SnesCartridgeScans.find(listOf(scan), "Future Game (Europe) (Rev 1).sfc"))
        for (rom in listOf("Future Game (USA).sfc", "Future Game (Japan).sfc",
            "Future Game (Europe) (Proto).sfc", "Future Game Two Player (Europe).sfc")) {
            assertNull(rom, SnesCartridgeScans.find(listOf(scan), rom))
        }
    }
    @Test fun specialShellColorsApplyOnlyToTheExactSnesGames() {
        fun game(title: String, platform: String = "Super Nintendo") = com.ariy.retrolauncher.model.GameEntry(
            id = "shell-color-test", title = title, platform = platform,
            sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT)
        assertEquals(0xFFB52027.toInt(), SnesCartridgeScans.shellColor(game("Doom")))
        assertEquals(0xFF202124.toInt(), SnesCartridgeScans.shellColor(game("Killer Instinct")))
        for (title in listOf("Doom II", "Ultimate Doom", "Killer Instinct 2", "Chrono Trigger")) {
            assertNull(title, SnesCartridgeScans.shellColor(game(title)))
        }
        assertNull(SnesCartridgeScans.shellColor(game("Doom", "PC (Windows)")))
    }
    @Test fun conflictingScansDoNotSelectArbitrarily() {
        assertNull(SnesCartridgeScans.find(listOf(scan, scan.copy(path = "labels/alternate.jpg")), "Future Game (Europe).sfc"))
        assertNull(SnesCartridgeScans.find(listOf(scan, scan.copy(path = "labels/alternate.jpg")), "Future Game.sfc"))
    }
    @Test fun missingRegionUsesAvailableStickerWithTheBestFixedShellFit() {
        assertEquals(scan, SnesCartridgeScans.find(listOf(scan), "futuregame.zip"))
        val usa = SnesCartridgeScans.Label("Future Game (USA)", "labels/future-usa.jpg", 469, 223)
        val japan = SnesCartridgeScans.Label("Future Game (Japan)", "labels/future-japan.jpg", 648, 201)
        for (candidates in listOf(listOf(scan, usa), listOf(usa, scan))) {
            assertEquals(usa, SnesCartridgeScans.find(candidates, "futuregame.zip"))
        }
        assertEquals(japan, SnesCartridgeScans.find(listOf(scan, japan), "futuregame.zip"))
        assertNull(SnesCartridgeScans.find(listOf(scan), "futuregame (Proto).zip"))
        assertNull(SnesCartridgeScans.find(listOf(scan), "othergame.zip"))
    }
    @Test fun equalFitsHaveDeterministicSelectionIndependentOfCatalogOrder() {
        val usa = scan.copy(title = "Future Game (USA)", path = "labels/a-usa.jpg")
        for (candidates in listOf(listOf(scan, usa), listOf(usa, scan))) {
            assertEquals(usa, SnesCartridgeScans.find(candidates, "Future Game.zip"))
        }
    }
    @Test(expected = IllegalArgumentException::class) fun pathsMustRemainInsideBundledLabels() {
        SnesCartridgeScans.parse("""{"version":1,"labels":[{"romTitle":"Future Game (Europe)","path":"../box.jpg"}]}""")
    }
}