package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class EmulatorLaunchersTest {
    @Test fun ordinaryAndroidGamesAreNotEmulatorsJustBecauseTheirLaunchPayloadIsEmpty() {
        assertEquals(GamePlatforms.ANDROID, standaloneActivityPlatform("com.chucklefish.stardewvalley"))
        assertEquals(GamePlatforms.EMULATORS, standaloneActivityPlatform("org.ppsspp.ppsspp"))
        assertEquals(GamePlatforms.EMULATORS, standaloneActivityPlatform("gamehub.lite"))
        val old = com.ariy.retrolauncher.model.GameEntry(title = "Custom title", platform = GamePlatforms.ANDROID,
            sourceType = GameSourceType.ACTIVITY, packageName = "org.scummvm.scummvm", imageUri = "custom-art")
        assertEquals(old.copy(platform = GamePlatforms.EMULATORS), classifyStandaloneActivity(old))
        assertEquals(old.copy(sourceType = GameSourceType.SHORTCUT), classifyStandaloneActivity(old.copy(sourceType = GameSourceType.SHORTCUT)))
        assertEquals("Android Games", GamePlatforms.filterLabel("Android"))
        assertTrue(GamePlatforms.matchesFilter(GamePlatforms.ANDROID, "Android Games"))
        assertFalse(GamePlatforms.matchesFilter(GamePlatforms.EMULATORS, "Android Games"))
    }
    private fun activity(pkg: String, name: String = "Main") = InstalledActivityOption(name, pkg, name, pkg, "$pkg.$name", 0)
    @Test fun gameHubsBecomeStandaloneLaunchersWithStableNamesAndNoGamePayload() {
        val apps = listOf(activity("com.xiaoji.egggame"), activity("gamehub.lite"))
        val entries = installedEmulatorLaunchers(apps, emptyList())
        assertEquals(listOf("GameHub", "GameHub Lite"), entries.map { it.title })
        assertTrue(entries.all { it.platform == GamePlatforms.EMULATORS && it.sourceType == GameSourceType.ACTIVITY })
        assertTrue(entries.all { it.subtitle == "Game launcher" && it.extrasJson == null && it.shortcutId == null && it.intentDataUri == null })
        assertTrue(installedEmulatorLaunchers(apps, entries).isEmpty())
        assertTrue(installedEmulatorLaunchers(apps, entries.map { it.copy(title = "My hub") }).isEmpty())
        val shortcuts = entries.map { it.copy(sourceType = GameSourceType.SHORTCUT, shortcutId = "game", platform = "Steam") }
        assertEquals(2, installedEmulatorLaunchers(apps, shortcuts).size)
    }
    @Test fun installedEmulatorsBecomeAndroidLaunchersWithoutGamePayloads() {
        val entries = installedEmulatorLaunchers(listOf(activity("org.scummvm.scummvm"), activity("org.ppsspp.ppsspp"), activity("other.app")), emptyList())
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.platform == GamePlatforms.EMULATORS && it.sourceType == GameSourceType.ACTIVITY })
        assertTrue(entries.all { it.intentDataUri == null && it.extrasJson == null })
        assertTrue(installedEmulatorLaunchers(listOf(activity("org.scummvm.scummvm"), activity("org.ppsspp.ppsspp")), entries).isEmpty())
    }
    @Test fun gameEntriesDoNotReplaceStandaloneLaunchersAndAliasesAreDeduped() {
        val activity = activity("org.scummvm.scummvm")
        val standalone = installedEmulatorLaunchers(listOf(activity), emptyList()).single()
        val game = standalone.copy(platform = "ScummVM", intentDataUri = "scummvm:monkey")
        assertEquals(1, installedEmulatorLaunchers(listOf(activity, activity.copy(activityName = "OtherAlias")), listOf(game)).size)
        assertTrue(installedEmulatorLaunchers(listOf(activity), listOf(standalone.copy(title = "Custom name"))).isEmpty())
    }
}