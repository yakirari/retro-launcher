package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test

class NesBundledArtworkTest {
    private val cover = NesBundledArtwork.Cover("Future Game (USA)", "front/future.jpg", "a".repeat(64))
    @Test fun exactReleaseAndRevisionsShareFrontButOtherEditionsDoNot() {
        assertEquals(cover, NesBundledArtwork.find(listOf(cover), "Future Game (USA) (Rev 1).nes"))
        for (rom in listOf("Future Game (Japan).nes", "Future Game (USA) (Proto).nes",
            "Future Game Two Player (USA).nes", "Future Game.nes")) {
            assertNull(rom, NesBundledArtwork.find(listOf(cover), rom))
        }
    }
    @Test fun ambiguousSourcesFailClosed() {
        assertNull(NesBundledArtwork.find(listOf(cover, cover.copy(path = "front/alternate.jpg")), "Future Game (USA).nes"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsafeAssetPaths() {
        NesBundledArtwork.parse("""{"version":1,"covers":[{"romTitle":"Future Game (USA)","path":"../escape.jpg","sha256":"${"a".repeat(64)}"}]}""")
    }
}