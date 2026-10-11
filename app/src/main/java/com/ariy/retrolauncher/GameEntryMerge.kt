package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.LaunchTarget
import com.ariy.retrolauncher.model.toJsonString
import com.ariy.retrolauncher.launch.ScummVmGames

internal data class UpsertResult(val outcome: UpsertOutcome, val gameId: String, val title: String)

internal enum class UpsertOutcome { ADDED, MERGED, DUPLICATE }

/** Additive imports preserve existing tiles, metadata, ordering, and launch targets. */
internal fun upsertOrMergeInPlace(
    current: MutableList<GameEntry>,
    entry: GameEntry,
    target: LaunchTarget,
    resolveEntryPlatform: (GameEntry) -> String? = { GamePlatforms.normalizeOrNull(it.platform) }
): UpsertResult {
    if (GamePlatforms.normalizeOrNull(entry.platform) == "ScummVM" && ScummVmGames.registeredTarget(entry.extrasJson) != null) {
        val matches = current.mapIndexedNotNull { index, existing ->
            ScummVmGames.mergeRegistered(existing, entry)?.let { index to it }
        }
        if (matches.size == 1) {
            val (index, repaired) = matches.single()
            val unchanged = current[index] == repaired
            current[index] = repaired
            return UpsertResult(if (unchanged) UpsertOutcome.DUPLICATE else UpsertOutcome.MERGED, repaired.id, repaired.title)
        }
    }
    val normalizedTitle = entry.title.trim().lowercase()
    val normalizedPlatform = resolveEntryPlatform(entry)
    val existingIndex = current.indexOfFirst { existing ->
        val hasIdentity = !target.shortcutId.isNullOrBlank() || !target.intentDataUri.isNullOrBlank() ||
            (!target.packageName.isNullOrBlank() && !target.activityName.isNullOrBlank())
        val sameTarget = hasIdentity && (target.sourceType != com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT ||
            resolveEntryPlatform(existing) == normalizedPlatform) && existing.allTargets().any {
            it.sourceType == target.sourceType && it.packageName == target.packageName &&
                it.shortcutId == target.shortcutId && it.userSerial == target.userSerial &&
                (target.sourceType == com.ariy.retrolauncher.model.GameSourceType.SHORTCUT ||
                    (it.activityName == target.activityName && it.intentDataUri == target.intentDataUri))
        }
        val sameRom = !target.intentDataUri.isNullOrBlank() &&
            resolveEntryPlatform(existing) == normalizedPlatform &&
            existing.allTargets().any { it.intentDataUri == target.intentDataUri }
        // Only public payload keys establish equivalence with a manually configured
        // GameNative target. Never reconstruct a redacted payload from shortcut IDs.
        val incomingGameNativeKey = if (target.packageName == com.ariy.retrolauncher.launch.GameNativeScan.PACKAGE)
            com.ariy.retrolauncher.launch.GameNativeScan.key(target.extrasJson, target.userSerial ?: 0L) else null
        val samePublishedGame = incomingGameNativeKey != null && existing.allTargets().any {
            it.packageName == com.ariy.retrolauncher.launch.GameNativeScan.PACKAGE &&
                (it.sourceType == com.ariy.retrolauncher.model.GameSourceType.SHORTCUT ||
                    it.intentAction == com.ariy.retrolauncher.launch.GameNativeScan.ACTION) &&
                com.ariy.retrolauncher.launch.GameNativeScan.key(it.extrasJson, it.userSerial ?: 0L) == incomingGameNativeKey
        }
        sameTarget || sameRom || samePublishedGame || (target.sourceType != com.ariy.retrolauncher.model.GameSourceType.SHORTCUT &&
            existing.title.trim().lowercase() == normalizedTitle && resolveEntryPlatform(existing) == normalizedPlatform)
    }
    if (existingIndex == -1) {
        current += entry
        return UpsertResult(UpsertOutcome.ADDED, entry.id, entry.title)
    }
    val existing = current[existingIndex]
    // A scan can repair an unconfigured tile, but cannot replace a user's working target
    // with a placeholder. Identity, custom title/artwork and play history stay intact.
    if (entry.needsLaunchSetup) return UpsertResult(UpsertOutcome.DUPLICATE, existing.id, existing.title)
    if (existing.needsLaunchSetup && !entry.needsLaunchSetup) {
        val repaired = existing.copy(
            subtitle = entry.subtitle, needsLaunchSetup = false,
            sourceType = entry.sourceType, packageName = entry.packageName,
            activityName = entry.activityName, shortcutId = entry.shortcutId, userSerial = entry.userSerial,
            intentAction = entry.intentAction, intentDataUri = entry.intentDataUri,
            mimeType = entry.mimeType, categoriesCsv = entry.categoriesCsv, extrasJson = entry.extrasJson
        )
        current[existingIndex] = repaired
        return UpsertResult(UpsertOutcome.MERGED, repaired.id, repaired.title)
    }
    val duplicate = existing.allTargets().any {
        it.sourceType == target.sourceType && it.packageName == target.packageName &&
            it.shortcutId == target.shortcutId && it.userSerial == target.userSerial &&
            (target.sourceType == com.ariy.retrolauncher.model.GameSourceType.SHORTCUT ||
                (it.activityName == target.activityName && it.intentDataUri == target.intentDataUri))
    }
    if (duplicate) return UpsertResult(UpsertOutcome.DUPLICATE, existing.id, existing.title)
    val merged = existing.copy(
        subtitle = existing.subtitle.ifBlank { target.label },
        genre = existing.genre ?: entry.genre,
        releaseYear = existing.releaseYear ?: entry.releaseYear,
        tagline = existing.tagline ?: entry.tagline,
        platform = existing.platform ?: entry.platform,
        imageUri = existing.imageUri ?: entry.imageUri,
        alternateTargetsJson = (existing.alternateTargets() + target).toJsonString()
    )
    current[existingIndex] = merged
    return UpsertResult(UpsertOutcome.MERGED, merged.id, merged.title)
}