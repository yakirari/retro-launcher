package com.ariy.retrolauncher.ui

import org.junit.Assert.*
import org.junit.Test

class LauncherBackgroundsTest {
    @Test fun basementSevenIsDefaultOnlyWhenPackaged() {
        assertEquals("basement_7.png", LauncherBackgrounds.defaultFor((1..7).map { "basement_$it.png" }))
        assertEquals("none", LauncherBackgrounds.defaultFor(emptyList()))
    }

    @Test fun allBundledBackgroundsHaveUniqueDescriptiveNamesWithoutAssetChanges() {
        val labels = (1..7).map { LauncherBackgrounds.label("basement_$it.png") }
        assertEquals(7, labels.toSet().size)
        assertTrue(labels.all { !it.contains("basement", ignoreCase = true) })
        assertEquals("Player One Lounge", labels.last())
        assertEquals("custom room", LauncherBackgrounds.label("custom_room.jpg"))
    }
}