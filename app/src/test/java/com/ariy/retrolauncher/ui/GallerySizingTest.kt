package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GalleryTileSize
import com.ariy.retrolauncher.model.GamePlatforms
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Test

class GallerySizingTest {
    @Test fun androidAndEmulatorAppTilesShareSquareSizing() {
        assertTrue(usesSquareAppIconTile(GamePlatforms.ANDROID))
        assertTrue(usesSquareAppIconTile(GamePlatforms.EMULATORS))
        assertTrue(usesSquareAppIconTile("emulators"))
        assertFalse(usesSquareAppIconTile("Nintendo Switch"))
    }

    @Test fun completeRowsFitWithSpacingAtPhysicalPixelDensities() {
        for ((width, height) in listOf(900f to 420f, 328f to 640f, 720f to 170f, 240f to 120f)) {
            for (size in GalleryTileSize.entries) {
                for (aspect in listOf(0.46f, 2f / 3f, 1f, 1.6f)) {
                    for (density in listOf(1f, 1.333333f, 1.5f, 2.75f)) {
                        for (spacing in listOf(4f, 10f)) {
                            val sizing = galleryGridSizing(width, height, size, aspect, spacing, density)
                            val heightPx = (sizing.itemHeightDp * density).roundToInt()
                            val spacingPx = (spacing * density).roundToInt()
                            val rows = size.minimumVisibleItems
                            assertTrue("$width x $height / $size / $aspect / $density",
                                heightPx * rows + spacingPx * (rows - 1) <= (height * density).roundToInt())
                            assertTrue(sizing.columns >= 1)
                            if (size == GalleryTileSize.LARGE) assertTrue(sizing.columns >= 2)
                            assertTrue(heightPx >= 1)
                            assertTrue(sizing.columns + spacingPx * (sizing.columns - 1) <=
                                (width * density).roundToInt())
                        }
                    }
                }
            }
        }
    }

    @Test fun largeFitsTwoColumnsAndACompleteRowEvenOnTallNarrowViewports() {
        for ((width, height) in listOf(328f to 640f, 120f to 360f, 240f to 1600f)) {
            for (aspect in listOf(0.46f, 2f / 3f, 1f, 1.6f)) {
                for (spacing in listOf(4f, 10f)) {
                    val sizing = galleryGridSizing(width, height, GalleryTileSize.LARGE, aspect, spacing)
                    assertTrue(sizing.columns >= 2)
                    assertTrue(sizing.itemHeightDp <= height)
                }
            }
        }
        for (size in listOf(GalleryTileSize.MEDIUM, GalleryTileSize.SMALL)) {
            assertEquals(1, galleryGridSizing(240f, 1600f, size).columns)
        }
        assertEquals(GalleryGridSizing(1, 1f), galleryGridSizing(0f, 0f, GalleryTileSize.LARGE))
    }

    @Test fun shorterViewportsAndTallerArtworkAutomaticallyAddColumns() {
        val size = GalleryTileSize.MEDIUM
        val tallViewport = galleryGridSizing(900f, 700f, size)
        val shortViewport = galleryGridSizing(900f, 200f, size)
        assertTrue(shortViewport.columns > tallViewport.columns)
        assertTrue(galleryGridSizing(900f, 200f, size, 0.46f).columns > shortViewport.columns)
    }

    @Test fun largeArcadeLayoutCanCapColumnsWithoutChangingDefaultSizing() {
        val automatic = galleryGridSizing(900f, 500f, GalleryTileSize.LARGE, 0.46f, 4f)
        val spacedOut = galleryGridSizing(900f, 500f, GalleryTileSize.LARGE, 0.46f, 4f, maximumColumns = 3)

        assertEquals(4, automatic.columns)
        assertEquals(3, spacedOut.columns)
        assertTrue(spacedOut.itemHeightDp >= automatic.itemHeightDp)
    }

    @Test fun stackedLargeThreeColumnPlatformSetIncludesRequestedSystems() {
        listOf(
            "Game Boy", "GameBoy", "Game Boy Color", "GBC", "DOS Games", "DOS", "ScummVM",
            "Nintendo Switch", "Switch",
            "Nintendo GameCube", "GameCube", "GC",
            "Sony PlayStation Portable", "PSP", "GG", "Game Gear", "Sega Game Gear",
            "MAME Arcade", "Final Burn Neo"
        ).forEach { platform ->
            assertTrue("$platform should use the three-column stacked-large layout",
                usesThreeColumnStackedLarge(platform))
        }

        listOf("Game Boy Advance", "Android").forEach { platform ->
            assertFalse("$platform should keep adaptive stacked-large sizing",
                usesThreeColumnStackedLarge(platform))
        }
    }

    @Test fun stackedLargeThreeColumnPlatformsReachThreeWhenAdaptiveSizingWouldUseTwo() {
        val automatic = galleryGridSizing(400f, 800f, GalleryTileSize.LARGE, 1f, 4f)
        val threeColumns = galleryGridSizing(
            400f, 800f, GalleryTileSize.LARGE, 1f, 4f,
            maximumColumns = 3, minimumColumns = 3
        )

        assertEquals(2, automatic.columns)
        assertEquals(3, threeColumns.columns)
    }

    @Test fun shortcutGalleriesCanUseFourSquareTilesPerRow() {
        val automatic = galleryGridSizing(400f, 800f, GalleryTileSize.LARGE, 1f, 4f)
        val shortcutTiles = galleryGridSizing(
            400f, 800f, GalleryTileSize.LARGE, 1f, 4f, minimumColumns = 4
        )

        assertEquals(2, automatic.columns)
        assertEquals(4, shortcutTiles.columns)
        assertTrue(shortcutTiles.itemHeightDp < automatic.itemHeightDp)
    }

    @Test fun sizeChoicesHaveOrderedHeightsAndHandleUnmeasuredViewport() {
        val large = galleryGridSizing(900f, 420f, GalleryTileSize.LARGE)
        val medium = galleryGridSizing(900f, 420f, GalleryTileSize.MEDIUM)
        val small = galleryGridSizing(900f, 420f, GalleryTileSize.SMALL)
        assertTrue(large.itemHeightDp > medium.itemHeightDp)
        assertTrue(medium.itemHeightDp > small.itemHeightDp)
        assertTrue(large.columns < medium.columns)
        assertTrue(medium.columns <= small.columns)
        assertEquals(GalleryGridSizing(1, 1f), galleryGridSizing(0f, 0f, GalleryTileSize.SMALL))
    }
}
