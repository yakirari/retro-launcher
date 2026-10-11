package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class SwitchScanDedupeTest {
    @Test fun releaseVersionsRegionsAndExtensionsShareOneScanKey() {
        assertEquals(switchDisplayDedupeKey("Castlevania Dominus Collection.nsp"),
            switchDisplayDedupeKey("Castlevania Dominus Collection Upd 3 [v196608] (USA).xci"))
        assertEquals(switchDisplayDedupeKey("Game [0100ABCD12345000][v0].nsp"),
            switchDisplayDedupeKey("Game [0100ABCD12345800][v65536].nsp"))
    }

    @Test
    fun stripSwitchAddonSuffixesBeforeDedupe() {
        assertEquals(
            "legendofzeldabreathofwild",
            switchDisplayDedupeKey("The Legend of Zelda - Breath of the Wild [Update].nsp")
        )
        assertEquals(
            "legendofzeldabreathofwild",
            switchDisplayDedupeKey("The Legend of Zelda - Breath of the Wild (DLC).nsp")
        )
        assertEquals(
            "supermariobroswonder",
            switchDisplayDedupeKey("Super Mario Bros Wonder [Season Pass].nsp")
        )
        assertEquals(
            "hollowknight",
            switchDisplayDedupeKey("Hollow Knight [Modded].nsp")
        )
    }
}
