package com.ariy.retrolauncher

import com.ariy.retrolauncher.launch.InstalledShortcutOption
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.PcGameStores

/** Android-free decisions, shared by setup, refresh and tests. */
internal object LibrarySetupPolicy {
    fun migrateCompleted(stored: Boolean?, hasExistingPreferences: Boolean): Boolean =
        stored ?: hasExistingPreferences

    fun isSharedHubPackage(packageName: String): Boolean =
        packageName == "app.gamenative" || packageName in PcGameStores.hubPackages

    fun missingHubShortcuts(options: List<InstalledShortcutOption>, games: List<GameEntry>): List<InstalledShortcutOption> {
        val existing = games.flatMap { it.allTargets() }
            .filter { it.sourceType == GameSourceType.SHORTCUT }
            .map { Triple(it.packageName, it.shortcutId, it.userSerial) }.toSet()
        return options.filter { isSharedHubPackage(it.packageName) &&
            (it.packageName != "app.gamenative" || com.ariy.retrolauncher.launch.GameNativeScan.isPublishedGameShortcut(it)) }
            .distinctBy { Triple(it.packageName, it.shortcutId, it.userSerial) }
            .filterNot { Triple(it.packageName, it.shortcutId, it.userSerial) in existing }
    }

    /** Existing configured package first, otherwise stable package/key order. No core guesses. */
    fun selectStandalone(
        options: List<GameSystemEmulatorOption>, templates: List<com.ariy.retrolauncher.launch.CustomIntentTemplate>,
        platform: String, installedPackages: Set<String>, preferredPackage: String?
    ): GameSystemEmulatorOption? = options.filter {
        it.platform == platform && it.retroArchSystemId == null &&
            templates[it.templateIndex].packageName in installedPackages
    }.sortedWith(compareBy<GameSystemEmulatorOption> {
        if (templates[it.templateIndex].packageName == preferredPackage) 0 else 1
    }.thenBy { templates[it.templateIndex].packageName }.thenBy { it.key }).firstOrNull()
}

internal data class LibraryScanProgress(
    val running: Boolean = false,
    val cancelled: Boolean = false,
    val platform: String = "",
    val completedPlatforms: Int = 0,
    val totalPlatforms: Int = 0,
    val discovered: Int = 0,
    val imported: Int = 0,
    val skipped: Int = 0,
    val unconfigured: Int = 0,
    val errors: Int = 0,
    val messages: List<String> = emptyList()
) {
    fun finished(cancelled: Boolean = false) = copy(running = false, cancelled = cancelled)
}