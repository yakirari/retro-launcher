package com.ariy.retrolauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SteamStoreArtworkCatalogTest {
    @Test fun preservesTitlePrefixesAndSubtitlesForDigitalGames() {
        assertEquals("Ori and the Blind Forest: Definitive Edition",
            SteamStoreArtworkCatalog.catalogTitle("Ori and the Blind Forest: Definitive Edition"))
        assertEquals("Tomb Raider", SteamStoreArtworkCatalog.catalogTitle("Tomb Raider.exe"))
    }
    @Test fun cleansAtoRegionSuffixWithoutStrippingAmongUs() {
        assertEquals("Ato", SteamStoreArtworkCatalog.catalogTitle("Ato US"))
        assertEquals("Among Us", SteamStoreArtworkCatalog.catalogTitle("Among Us"))
        assertEquals("198X", SteamStoreArtworkCatalog.catalogTitle("198X"))
    }

    @Test fun portraitsAreBoundToTheValidatedAppId() {
        assertEquals(listOf(
            "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1086010/library_600x900.jpg",
            "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1086010/portrait.png"
        ), SteamStoreArtworkCatalog.portraitImageUrls("1086010"))
        assertEquals(emptyList<String>(), SteamStoreArtworkCatalog.portraitImageUrls("../other"))
    }

    @Test
    fun selectsOnlyExactTitleNotSoundtrackOrSimilarName() {
        val json = """
            {"items":[
              {"name":"198X Soundtrack","id":1265840},
              {"name":"198X","id":1086010},
              {"name":"NightShift 198X","id":3933760}
            ]}
        """.trimIndent()

        assertEquals("1086010", SteamStoreArtworkCatalog.exactAppId(json, "198x"))
    }

    @Test
    fun rejectsNonHttpsOrFailedAppDetails() {
        assertNull(
            SteamStoreArtworkCatalog.appHeaderImage(
                """{"1086010":{"success":false,"data":{"header_image":"https://example.org/art.jpg"}}}""",
                "1086010"
            )
        )
        assertNull(
            SteamStoreArtworkCatalog.appHeaderImage(
                """{"1086010":{"success":true,"data":{"header_image":"http://example.org/art.jpg"}}}""",
                "1086010"
            )
        )
    }
}