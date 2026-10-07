package com.ariy.retrolauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwitchTitleIdsTest {

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
}
