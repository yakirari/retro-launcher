package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class GameNativeScanTest {
    private fun shortcut(id: Int = 203160, source: String = "STEAM", title: String = "Tomb Raider") = InstalledShortcutOption(
        title, "app.gamenative", "GameNative", "app.gamenative", "game_$id", 0, title, title,
        intentAction = GameNativeScan.ACTION, extrasJson = """{"app_id":$id,"game_source":"$source"}""")

    @Test fun scansRealShortcutContractIntoAndroidShortcutWithoutRomFolder() {
        val game = GameNativeScan.select(listOf(shortcut()), emptyList()).single()
        assertEquals("Tomb Raider", game.title)
        assertEquals("Steam", game.platform)
        assertEquals(GameNativeScan.ACTION, game.intentAction)
        assertEquals(GameNativeScan.Key(203160, "STEAM", 0), GameNativeScan.key(game.extrasJson))
        assertNull(game.intentDataUri)
        assertEquals(GameSourceType.SHORTCUT, game.sourceType)
        assertEquals("game_203160", game.shortcutId)
    }
    @Test fun rescanSkipsExistingManualEntriesAndDuplicatePublishedShortcuts() {
        val entries = GameNativeScan.select(listOf(shortcut(), shortcut().copy(shortcutId = "another")), emptyList())
        assertEquals(1, entries.size)
        assertTrue(GameNativeScan.select(listOf(shortcut()), entries).isEmpty())
    }
    @Test fun storeIdentityIsPreservedAndInvalidOrOtherProfileEntriesAreIgnored() {
        val entries = GameNativeScan.select(listOf(shortcut(), shortcut(id = 500, source = "GOG"), shortcut(source = "BOGUS"),
            shortcut(id = 0), shortcut().copy(packageName = "other"), shortcut().copy(intentAction = "other"),
            shortcut().copy(userSerial = 10)), emptyList(), "Favorites")
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.genre == "Favorites" })
        assertNull(GameNativeScan.key("""{"app_id":1.5,"game_source":"STEAM"}"""))
        assertNull(GameNativeScan.key("bad"))
    }
    @Test fun existingShortcutTargetsAreAlsoRecognizedByLaunchIdentity() {
        val existing = GameEntry(title = "Renamed", sourceType = GameSourceType.SHORTCUT, packageName = "app.gamenative",
            shortcutId = "game_203160", userSerial = 0, extrasJson = shortcut().extrasJson)
        assertTrue(GameNativeScan.select(listOf(shortcut()), listOf(existing)).isEmpty())
    }
    @Test fun androidRedactedShortcutsNeedNoGuessedIdOrStoreAndRescanIsIdempotent() {
        val opaque = shortcut().copy(intentAction = null, extrasJson = null)
        val entries = GameNativeScan.select(listOf(opaque), emptyList())
        assertEquals(1, entries.size)
        assertEquals(GameSourceType.SHORTCUT, entries.single().sourceType)
        assertEquals("GameNative", entries.single().platform)
        assertNull(entries.single().extrasJson)
        assertTrue(GameNativeScan.select(listOf(opaque), entries).isEmpty())
        assertTrue(GameNativeScan.select(listOf(opaque.copy(shortcutId = "settings")), emptyList()).isEmpty())
    }
}