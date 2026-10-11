package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test

class PlatformScanRulesTest {
    @Test fun onlySupportedExtensionsAreCandidates() {
        val extensions = setOf("sfc", "smc")
        assertTrue(hasAllowedRomExtension("Game.SFC", extensions))
        assertFalse(hasAllowedRomExtension("Game.sfc.bak", extensions))
        assertFalse(hasAllowedRomExtension("Game.webp", extensions))
        assertFalse(hasAllowedRomExtension("README", extensions))
    }

    @Test fun trackDataIsSkippedButDescriptorsRemainLaunchable() {
        assertTrue(isDiscTrackDataFile("Game (Track 01).BIN"))
        assertFalse(isDiscTrackDataFile("Game Track 01.cue"))
        assertFalse(isDiscTrackDataFile("Game Track 01.chd"))
        assertFalse(isDiscTrackDataFile("Game.bin"))
    }

    @Test fun packageTitleIdWinsOverAddonWordsInBaseGameTitle() {
        assertTrue(isSwitchBaseGame("0100123456780000", "Extra Game.nsp"))
        assertFalse(isSwitchBaseGame("0100123456780800", "Game.nsp"))
    }

    @Test fun addonFilenameFilteringIsUsedWhenMetadataIsUnavailable() {
        assertTrue(isSwitchBaseGame(null, "Game.nsp"))
        assertFalse(isSwitchBaseGame(null, "Game [Update].nsp"))
        assertFalse(isSwitchBaseGame(null, "Game (DLC).nsp"))
        assertFalse(isSwitchBaseGame(null, "Castlevania Dominus Collection Upd 3.nsp"))
        assertTrue(isSwitchBaseGame(null, "Extra Game.nsp"))
        assertTrue(isSwitchBaseGame("0100123456780000", "Game Upd 3 bundled.xci"))
    }
}