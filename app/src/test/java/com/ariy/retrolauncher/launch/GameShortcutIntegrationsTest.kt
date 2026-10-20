package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.LibrarySetupPolicy
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.toJsonString
import org.junit.Assert.*
import org.junit.Test

class GameShortcutIntegrationsTest {
    private fun shortcut(pkg: String, id: String = "game", profile: Long = 0) = InstalledShortcutOption(
        title = "My game", subtitle = pkg, sourceLabel = pkg, packageName = pkg,
        shortcutId = id, userSerial = profile, shortLabel = "My game", longLabel = null)

    @Test fun allThreePcLaunchersAreDiscoverableWithoutInventedGameHubTemplates() {
        assertEquals(setOf("GameNative", "GameHub", "GameHub Lite"), GameShortcutIntegrations.publishers.values.toSet())
        assertEquals(setOf("gamehub.lite"), GameShortcutIntegrations.packages("gamehub.lite"))
        assertTrue(GameShortcutIntegrations.packages("unknown").isEmpty())
    }

    @Test fun ps3PublishedShortcutsAreAutomaticallyImportedFromKnownPackagesOnly() {
        val shortcuts = GameShortcutIntegrations.ps3Packages.map { shortcut(it) }
        assertEquals(shortcuts, LibrarySetupPolicy.missingHubShortcuts(shortcuts + shortcut("unknown.ps3"), emptyList()))
        val games = GameShortcutIntegrations.select(GameShortcutIntegrations.PS3, shortcuts, emptyList())
        assertEquals(4, games.size)
        assertTrue(games.all { it.platform == GameShortcutIntegrations.PS3 && it.sourceType == GameSourceType.SHORTCUT && it.intentDataUri == null && it.extrasJson == null })
    }

    @Test fun importsArePublisherScopedAndPreserveProfileAndOpaqueShortcutId() {
        val lite = shortcut("gamehub.lite", "opaque/id", 10)
        val entries = GameShortcutIntegrations.select("gamehub.lite", listOf(lite, shortcut("com.xiaoji.egggame")), emptyList())
        val game = entries.single()
        assertEquals("GameHub Lite", game.platform)
        assertEquals("opaque/id", game.shortcutId)
        assertEquals(10L, game.userSerial)
        assertTrue(GameShortcutIntegrations.select("gamehub.lite", listOf(lite), listOf(game)).isEmpty())
    }

    @Test fun ps3RescanDeduplicatesAlternateTargetsButKeepsOtherProfiles() {
        val primary = shortcut("com.armsx3")
        val alternate = shortcut("net.rpcsx")
        val entries = GameShortcutIntegrations.select(GameShortcutIntegrations.PS3, listOf(primary, alternate), emptyList())
        val existing: GameEntry = entries.first().copy(alternateTargetsJson = listOf(entries.last().primaryTarget()).toJsonString())
        val work = primary.copy(userSerial = 10)
        assertEquals(listOf(work), LibrarySetupPolicy.missingHubShortcuts(listOf(primary, alternate, work, work), listOf(existing)))
    }
}