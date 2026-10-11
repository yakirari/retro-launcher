package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.PcGameStores

private val knownLauncherPackages by lazy { CustomIntentTemplates.all.map { it.packageName }.toSet() + PcGameStores.hubPackages.keys }

internal fun isEmulatorLauncherPackage(packageName: String?): Boolean =
    packageName in knownLauncherPackages || GamePlatforms.inferFromPackageName(packageName) != null

internal fun standaloneActivityPlatform(packageName: String?): String =
    if (isEmulatorLauncherPackage(packageName)) GamePlatforms.EMULATORS else GamePlatforms.ANDROID

internal fun classifyStandaloneActivity(entry: GameEntry): GameEntry =
    if (entry.sourceType == GameSourceType.ACTIVITY) entry.copy(platform = standaloneActivityPlatform(entry.packageName)) else entry

internal fun installedEmulatorLaunchers(activities: List<InstalledActivityOption>, games: List<GameEntry>): List<GameEntry> {
    val existing = games.flatMap { it.allTargets() }
        .filter { it.sourceType == GameSourceType.ACTIVITY && it.intentDataUri.isNullOrBlank() && it.extrasJson.isNullOrBlank() }
        .map { it.packageName to (it.userSerial ?: 0L) }.toSet()
    return activities.filter { isEmulatorLauncherPackage(it.packageName) }
        .distinctBy { it.packageName to it.userSerial }
        .filterNot { (it.packageName to it.userSerial) in existing }
        .map { option ->
            val hub = PcGameStores.hubPackages[option.packageName]
            GameEntry(title = hub ?: option.sourceLabel.ifBlank { option.title }, subtitle = if (hub != null) "Game launcher" else "Emulator", platform = GamePlatforms.EMULATORS,
                sourceType = GameSourceType.ACTIVITY, packageName = option.packageName,
                activityName = option.activityName, userSerial = option.userSerial)
        }
}