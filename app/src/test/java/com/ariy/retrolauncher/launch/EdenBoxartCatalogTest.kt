package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class EdenBoxartCatalogTest {
    @Test
    fun createsEdenFilenameFromGameTitle() {
        assertEquals(listOf("1-2-switch", "12switch"), EdenBoxartCatalog.filenameSlugs("1-2-Switch"))
        assertEquals(listOf("20xx"), EdenBoxartCatalog.filenameSlugs("20XX"))
    }

    @Test
    fun emptyTitlesDoNotCreateLookupCandidates() {
        assertEquals(emptyList<String>(), EdenBoxartCatalog.filenameSlugs("/ROM/switch/.nsp"))
    }
}