package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test

class NesPortraitArtworkCatalogTest {
    private fun page(images: String, platform: String = "Nintendo Entertainment System") =
        "<h1>Future Game: Two Players</h1><dl><dt>Platform</dt><dd>$platform</dd></dl>$images"
    private fun image(type: String, region: String, size: String = "512x731", host: String = "images.launchbox-app.com") =
        """<img src="https://$host/test.png" alt="Future Game: Two Players - $type ($region) - $size">"""

    @Test fun exactGameTypedPortraitCoverWinsOverGameplayAndWrongRegions() {
        val html = page(image("Screenshot - Gameplay", "World") + image("Box - Front", "Japan") +
            image("Box - Front - Reconstructed", "World") + image("Box - Front", "World", "512x350"))
        val result = NesPortraitArtworkCatalog.parseDetails(html, "Future Game - Two Player (World).nes")
        assertEquals(1, result.size)
        assertTrue(result.single().source.contains("Reconstructed"))
    }
    @Test fun rejectsPortsWrongGamesUnverifiedEditionsAndUnexpectedHosts() {
        val html = page(image("Box - Front", "World"))
        assertTrue(NesPortraitArtworkCatalog.parseDetails(html, "Future Game 2 (World).nes").isEmpty())
        assertTrue(NesPortraitArtworkCatalog.parseDetails(html, "Future Game Two Player (World) (Proto).nes").isEmpty())
        assertTrue(NesPortraitArtworkCatalog.parseDetails(page(image("Box - Front", "World"), "Nintendo Game Boy"), "Future Game Two Player (World).nes").isEmpty())
        assertTrue(NesPortraitArtworkCatalog.parseDetails(page(image("Box - Front", "World", host = "example.com")), "Future Game Two Player (World).nes").isEmpty())
    }
    @Test fun neutralCustomArtIsExplicitAndPrefersLargerSameRegionCover() {
        val result = NesPortraitArtworkCatalog.parseDetails(page(image("Fanart - Box - Front", "World") +
            image("Fanart - Box - Front", "Japan", "600x900") + image("Fanart - Box - Front", "Japan", "810x1080")),
            "Future Game Two Player (Japan) (En).nes")
        assertEquals(3, result.size)
        assertTrue(result.first().source.contains("Japan"))
    }
    @Test fun searchRequiresExactTitleAndNesPlatform() {
        val html = """<a href="/games/details/1-future"><h3>Future Game: Two Players</h3><p>Nintendo Entertainment System</p></a>
            <a href="/games/details/2-port"><h3>Future Game: Two Players</h3><p>Nintendo Game Boy</p></a>
            <a href="/games/details/3-sequel"><h3>Future Game 2</h3><p>Nintendo Entertainment System</p></a>"""
        assertEquals(listOf("https://gamesdb.launchbox-app.com/games/details/1-future"),
            NesPortraitArtworkCatalog.parseSearch(html, "Future Game Two Player (World).nes"))
    }
    @Test fun resolvesPreviewToOriginalRatherThanTrustingAdvertisedDimensions() {
        val html = page(image("Box - Front", "World")) +
            """<script id="__NUXT_DATA__" type="application/json">[{"imageFileName":1,"fullGameImageFileName":2},"test.png","original.png"]</script>"""
        assertEquals("https://images.launchbox-app.com/original.png",
            NesPortraitArtworkCatalog.parseDetails(html, "Future Game Two Player (World).nes").single().imageUrl)
    }
}