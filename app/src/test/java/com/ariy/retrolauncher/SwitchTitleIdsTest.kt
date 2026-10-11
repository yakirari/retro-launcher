package com.ariy.retrolauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwitchTitleIdsTest {

    @Test fun extractsFilenameIdsAndPrefersBundledBase() {
        assertEquals("0100ABCD12345800", switchTitleIdFromFilename("Game [0100abcd12345800][v3].nsp"))
        assertEquals("0100ABCD12345000", switchTitleIdFromFilename("Game [0100ABCD12345800] [0100ABCD12345000].xci"))
        assertNull(switchTitleIdFromFilename("Game [0200ABCD12345000].nsp"))
        assertNull(switchTitleIdFromFilename("x0100ABCD12345000x.nsp"))
    }

    @Test fun ticketOffsetsIncludeSignatureBlock() {
        assertEquals(0x2A0L, switchTicketRightsIdOffset(0x010004))
        assertEquals(0x3A0L, switchTicketRightsIdOffset(0x010003))
        assertEquals(0x1E0L, switchTicketRightsIdOffset(0x010005))
        assertNull(switchTicketRightsIdOffset(0))
    }

    @Test fun filenameKeysIgnoreReleaseTagsButNotDistinctEditions() {
        assertEquals(switchFilenameDedupeKey("Game (USA) [v0].nsp"),
            switchFilenameDedupeKey("Game [Update 3] [v65536].nsp"))
        assertEquals(switchFilenameDedupeKey("Game.nsp"), switchFilenameDedupeKey("Game Upd 3.nsp"))
        org.junit.Assert.assertNotEquals(switchFilenameDedupeKey("Game.nsp"),
            switchFilenameDedupeKey("Game (Special Edition).nsp"))
    }

    @Test
    fun normalizeSwitchTitleIdUppercasesValidValues() {
        assertEquals(
            "0100ABCD12345000",
            normalizeSwitchTitleId(" 0100abcd12345000 ")
        )
    }

    @Test
    fun normalizeSwitchTitleIdRejectsInvalidValues() {
        assertNull(normalizeSwitchTitleId("0100ABCD12348"))
        assertNull(normalizeSwitchTitleId("0200ABCD12345000"))
        assertNull(normalizeSwitchTitleId("0100ABCD12345ZZZ"))
    }

    @Test
    fun normalizeSwitchBaseTitleIdKeepsBaseIdsStable() {
        assertEquals(
            "0100ABCD12345000",
            normalizeSwitchBaseTitleId("0100ABCD12345000")
        )
    }

    @Test
    fun normalizeSwitchBaseTitleIdCollapsesVariantsToBaseFamily() {
        assertEquals(
            "0100ABCD12345000",
            normalizeSwitchBaseTitleId("0100ABCD12345800")
        )
        assertEquals(
            "0100ABCD12345000",
            normalizeSwitchBaseTitleId("0100abcd12345fff")
        )
    }

    @Test
    fun prefersBaseApplicationIdAmongPackageTitleIds() {
        assertEquals(
            "0100ABCD12345000",
            preferSwitchBaseTitleId(listOf("0100ABCD12345800", "0100ABCD12345000"))
        )
    }
}
