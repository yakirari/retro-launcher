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

    /** Named core presets are choices, never evidence that a private core is installed. */
    fun standaloneChoices(
        options: List<GameSystemEmulatorOption>, templates: List<com.ariy.retrolauncher.launch.CustomIntentTemplate>,
        platforms: Set<String>, installedPackages: Set<String>
    ): List<GameSystemEmulatorOption> = options.filter {
        it.platform in platforms &&
            templates[it.templateIndex].packageName in installedPackages
    }.distinctBy { Triple(it.platform, templates[it.templateIndex].packageName, it.retroArchSystemId) }
        .sortedWith(compareBy({ it.platform }, { templates[it.templateIndex].label }))

    suspend fun chooseStandalone(
        choices: List<GameSystemEmulatorOption>,
        templates: List<com.ariy.retrolauncher.launch.CustomIntentTemplate>, preferredPackage: String?,
        ask: suspend () -> GameSystemEmulatorOption?
    ): GameSystemEmulatorOption? = choices.firstOrNull {
        it.retroArchSystemId == null && templates[it.templateIndex].packageName == preferredPackage
    } ?: if (choices.isNotEmpty()) ask()?.takeIf { it in choices } else null

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

internal data class LibraryEmulatorChoice(val folder: String, val options: List<GameSystemEmulatorOption>)

internal enum class LibraryScanStage { CHECKING_FOLDER, FINDING_EMULATORS, SCANNING, CHOOSING_EMULATOR, ADDING }

internal data class LibraryScanProgress(
    val running: Boolean = false,
    val cancelled: Boolean = false,
    val stage: LibraryScanStage = LibraryScanStage.CHECKING_FOLDER,
    val platform: String = "",
    val currentGame: String = "",
    val emulatorChoice: LibraryEmulatorChoice? = null,
    val completedPlatforms: Int = 0,
    val totalPlatforms: Int = 0,
    val discovered: Int = 0,
    val imported: Int = 0,
    val skipped: Int = 0,
    val unconfigured: Int = 0,
    val errors: Int = 0,
    val deferredPlatforms: List<String> = emptyList(),
    val messages: List<String> = emptyList()
) {
    fun statusText(): String = when {
        !running && cancelled -> "Scan cancelled — games kept"
        !running && errors > 0 -> "Scan finished with errors"
        !running -> "Library ready"
        stage == LibraryScanStage.CHECKING_FOLDER -> "Checking ROM folder access…"
        stage == LibraryScanStage.FINDING_EMULATORS -> "Finding installed emulators…"
        stage == LibraryScanStage.CHOOSING_EMULATOR -> "Choose an emulator for $platform"
        stage == LibraryScanStage.ADDING -> "Adding $platform…"
        else -> "Scanning $platform…"
    }

    fun finished(cancelled: Boolean = false) = copy(running = false, cancelled = cancelled, emulatorChoice = null)
}