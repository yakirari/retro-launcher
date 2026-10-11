package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.LaunchTarget
import com.ariy.retrolauncher.model.toJsonString
import com.ariy.retrolauncher.launch.isSwitchBaseGame
import java.net.URI

internal fun dedupeSwitchGameEntries(
    entries: List<GameEntry>,
    rawTitleIdsByGameId: Map<String, Map<String, String>>
): List<GameEntry> {
    val result = mutableListOf<GameEntry>()
    val indexByFamily = mutableMapOf<String, Int>()
    val indexesByFilename = mutableMapOf<String, MutableSet<Int>>()
    val familiesByIndex = mutableMapOf<Int, Set<String>>()
    val idsByIndex = mutableMapOf<Int, Map<String, String>>()

    entries.forEach { entry ->
        val platform = GamePlatforms.normalizeOrNull(entry.platform)
        val targetTitleIds = if (platform == "Nintendo Switch") {
            entry.allTargets().mapNotNull { target ->
                val id = normalizeSwitchTitleId(rawTitleIdsByGameId[entry.id]?.get(target.stableKey()))
                    ?: switchTitleIdFromFilename(switchTargetFilename(target))
                id?.let { target.stableKey() to it }
            }.toMap()
        } else {
            emptyMap()
        }
        val normalizedFamilies = targetTitleIds.values
            .mapNotNull(::normalizeSwitchBaseTitleId)
            .distinct()
        val familyKey = normalizedFamilies.singleOrNull()?.let { "$platform:$it" }
        val entryBaseTarget = if (platform == "Nintendo Switch") {
            preferredSwitchBaseTarget(entry, targetTitleIds)
        } else null
        val normalizedEntry = entryBaseTarget?.let(entry::promoteSwitchTarget) ?: entry
        val filenameKeys = if (platform == "Nintendo Switch" && normalizedFamilies.size <= 1) {
            entry.allTargets().mapNotNull { target ->
                switchTargetFilename(target)?.let(::switchFilenameDedupeKey)
            }.toSet()
        } else emptySet()
        val filenameMatches = filenameKeys.flatMap { indexesByFilename[it].orEmpty() }.distinct()
            .filter { index ->
                val existingFamilies = familiesByIndex[index].orEmpty()
                existingFamilies.size <= 1 &&
                    (existingFamilies.isEmpty() || normalizedFamilies.isEmpty() ||
                        existingFamilies == normalizedFamilies.toSet())
            }
        val existingIndex = familyKey?.let(indexByFamily::get)
            ?: filenameMatches.singleOrNull()

        if (existingIndex == null) {
            result += normalizedEntry
            if (familyKey != null) indexByFamily[familyKey] = result.lastIndex
            familiesByIndex[result.lastIndex] = normalizedFamilies.toSet()
            idsByIndex[result.lastIndex] = targetTitleIds
            filenameKeys.forEach { key -> indexesByFilename.getOrPut(key) { mutableSetOf() }.add(result.lastIndex) }
        } else {
            val existing = result[existingIndex]
            val existingTargetKeys = existing.allTargets().mapTo(mutableSetOf()) { it.stableKey() }
            val mergedTargets = (existing.alternateTargets() + normalizedEntry.allTargets())
                .distinctBy { it.stableKey() }
                .filterNot { it.stableKey() in existingTargetKeys }
            result[existingIndex] = existing.copy(
                subtitle = existing.subtitle.ifBlank { normalizedEntry.subtitle },
                lastPlayedAtEpochMillis = listOfNotNull(
                    existing.lastPlayedAtEpochMillis,
                    normalizedEntry.lastPlayedAtEpochMillis
                ).maxOrNull(),
                genre = existing.genre ?: normalizedEntry.genre,
                releaseYear = existing.releaseYear ?: normalizedEntry.releaseYear,
                tagline = existing.tagline ?: normalizedEntry.tagline,
                playCount = existing.playCount.coerceAtLeast(0).let { count ->
                    count + normalizedEntry.playCount.coerceIn(0, Long.MAX_VALUE - count)
                },
                imageUri = existing.imageUri ?: normalizedEntry.imageUri,
                alternateTargetsJson = (existing.alternateTargets() + mergedTargets)
                    .distinctBy { it.stableKey() }
                    .takeIf { it.isNotEmpty() }
                    ?.toJsonString()
            )
            val merged = result[existingIndex]
            val idsForGroup = idsByIndex[existingIndex].orEmpty() + targetTitleIds
            idsByIndex[existingIndex] = idsForGroup
            familiesByIndex[existingIndex] = familiesByIndex[existingIndex].orEmpty() + normalizedFamilies
            if (familyKey != null) indexByFamily[familyKey] = existingIndex
            filenameKeys.forEach { key -> indexesByFilename.getOrPut(key) { mutableSetOf() }.add(existingIndex) }
            val preferredBaseTarget = preferredSwitchBaseTarget(merged, idsForGroup)
            if (preferredBaseTarget != null) {
                result[existingIndex] = merged.promoteSwitchTarget(preferredBaseTarget)
            }
        }
    }

    return result
}

private fun switchTargetFilename(target: LaunchTarget): String? {
    val name = runCatching { URI(target.intentDataUri.orEmpty()).path?.substringAfterLast('/') }
        .getOrNull() ?: return null
    return name.takeIf { it.substringAfterLast('.', "").lowercase() in setOf("nsp", "nsz", "xci", "xcz") }
}

private fun preferredSwitchBaseTarget(entry: GameEntry, titleIds: Map<String, String>): LaunchTarget? {
    val targets = entry.allTargets().sortedByDescending { it.stableKey() == entry.preferredTargetKey }
    return targets.firstOrNull { titleIds[it.stableKey()]?.endsWith("000") == true }
        ?: targets.firstOrNull { target ->
            titleIds[target.stableKey()] == null &&
                switchTargetFilename(target)?.let { isSwitchBaseGame(null, it) } == true
        }
}

private fun GameEntry.promoteSwitchTarget(target: LaunchTarget): GameEntry {
    val ordered = listOf(target) + allTargets().filterNot { it.stableKey() == target.stableKey() }
    val primary = ordered.first()
    return copy(
        sourceType = primary.sourceType,
        subtitle = primary.label.ifBlank { subtitle },
        packageName = primary.packageName,
        activityName = primary.activityName,
        shortcutId = primary.shortcutId,
        userSerial = primary.userSerial,
        intentAction = primary.intentAction,
        intentDataUri = primary.intentDataUri,
        mimeType = primary.mimeType,
        categoriesCsv = primary.categoriesCsv,
        extrasJson = primary.extrasJson,
        alternateTargetsJson = ordered.drop(1).takeIf { it.isNotEmpty() }?.toJsonString(),
        preferredTargetKey = primary.stableKey()
    )
}