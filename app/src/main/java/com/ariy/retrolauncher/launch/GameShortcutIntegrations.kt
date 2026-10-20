package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.LibrarySetupPolicy
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.PcGameStores

/** Public Android shortcuts only. Package recognition is not a private-library API. */
internal object GameShortcutIntegrations {
    val ps3Packages = setOf("com.armsx3", "aenu.aps3e", "net.rpcs3", "net.rpcsx")
    val publishers = mapOf(GameNativeScan.PACKAGE to "GameNative") + PcGameStores.hubPackages
    const val PS3 = "Sony PlayStation 3"
    val choices = publishers + (PS3 to "PlayStation 3 shortcuts")
    const val SETUP = "1. Set Retro Launcher as the default Home app.\n2. Open the game in its hub / emulator and choose Add to home screen (if supported).\n3. Allow Android's shortcut request. Return here: shared shortcuts are added automatically, or use Import shared shortcuts.\nNo ROM folder or game ID is needed. A downloaded / registered game is not automatically a shared shortcut. Pins owned only by another Home app must be recreated; private libraries cannot be scanned."
    const val PS3_HELP = "Two ways to add PS3 games: scan ISO files in your PS3 ROM folder using ARMSX3, or import game shortcuts published by a PS3 emulator. Configure firmware and game access inside the emulator and test the game there first. ISO scanning does not import PKG / PUP / EBOOT files or private installed-game lists. Other PS3 apps have no verified direct-file preset here; use their game shortcut action if available."

    fun packages(choice: String): Set<String> = if (choice == PS3) ps3Packages else
        publishers.keys.filter { it == choice }.toSet()

    fun select(choice: String, shortcuts: List<InstalledShortcutOption>, games: List<GameEntry>, collection: String? = null): List<GameEntry> {
        val scoped = shortcuts.filter { it.packageName in packages(choice) }
        if (choice == GameNativeScan.PACKAGE) return GameNativeScan.select(scoped, games, collection)
        return LibrarySetupPolicy.missingHubShortcuts(scoped, games).map { option ->
            GameEntry(title = option.title, subtitle = option.sourceLabel,
                platform = PcGameStores.forShortcut(option) ?: GamePlatforms.inferFromPackageName(option.packageName),
                genre = collection?.takeIf(String::isNotBlank), sourceType = GameSourceType.SHORTCUT,
                packageName = option.packageName, shortcutId = option.shortcutId, userSerial = option.userSerial)
        }
    }
}