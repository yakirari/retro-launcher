package com.ariy.retrolauncher

import com.ariy.retrolauncher.launch.ArcadeGameCatalog
import com.ariy.retrolauncher.launch.arcadeRomIdentity
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.toJsonString
import java.net.URI

/** Merge same-platform ROM identities or the exact same arcade file, never just shared cabinet art. */
internal fun dedupeArcadeGameEntries(entries: List<GameEntry>): List<GameEntry> {
    val result = mutableListOf<GameEntry>()
    val indexes = mutableMapOf<String, Int>()
    val fileIndexes = mutableMapOf<Pair<String, Long>, Int>()
    for (entry in entries) {
        val platform = entry.platform?.lowercase()
        // MAME and Final Burn can launch the same physical file. Keep distinct
        // platform ROM sets separate, but repair the old automatic double import.
        val files = if (platform in setOf("mame arcade", "final burn neo")) entry.allTargets().mapNotNull { target ->
            val uri = target.intentDataUri ?: return@mapNotNull null
            val path = runCatching { URI(uri).path }.getOrNull().orEmpty()
            if (path.substringAfterLast('.', "").lowercase() in setOf("zip", "7z", "chd"))
                uri to (target.userSerial ?: 0L) else null
        } else emptyList()
        val key = if (platform in setOf("mame arcade", "final burn neo")) {
            val romKeys = entry.allTargets().mapNotNull { target ->
                val filename = runCatching { URI(target.intentDataUri.orEmpty()).path?.substringAfterLast('/') }.getOrNull()
                filename?.takeIf { it.substringAfterLast('.', "").lowercase() in setOf("zip", "7z", "chd") }
                    ?.let(::arcadeRomIdentity)
            }.distinct()
            val identity = if (romKeys.isEmpty()) ArcadeGameCatalog.find(entry.title)?.rom else romKeys.singleOrNull()
            identity?.let { "$platform:$it" }
        } else null
        val index = files.firstNotNullOfOrNull(fileIndexes::get) ?: key?.let(indexes::get)
        if (index == null) {
            result += entry
            if (key != null) indexes[key] = result.lastIndex
            files.forEach { fileIndexes[it] = result.lastIndex }
        } else {
            val existing = result[index]
            val targets = (existing.allTargets() + entry.allTargets()).distinctBy { it.stableKey() }
            val primary = if (existing.needsLaunchSetup && !entry.needsLaunchSetup) entry else existing
            result[index] = primary.copy(
                id = existing.id,
                title = existing.title,
                platform = existing.platform,
                subtitle = existing.subtitle.ifBlank { entry.subtitle },
                genre = existing.genre ?: entry.genre,
                releaseYear = existing.releaseYear ?: entry.releaseYear,
                tagline = existing.tagline ?: entry.tagline,
                playCount = existing.playCount.coerceAtLeast(0).let { count ->
                    count + entry.playCount.coerceIn(0, Long.MAX_VALUE - count)
                },
                imageUri = existing.imageUri ?: entry.imageUri,
                lastPlayedAtEpochMillis = listOfNotNull(existing.lastPlayedAtEpochMillis, entry.lastPlayedAtEpochMillis).maxOrNull(),
                alternateTargetsJson = targets.filterNot { it.stableKey() == primary.primaryTarget().stableKey() }
                    .takeIf { it.isNotEmpty() }?.toJsonString(),
                preferredTargetKey = existing.preferredTargetKey ?: entry.preferredTargetKey,
                authorizedTreeUri = existing.authorizedTreeUri ?: entry.authorizedTreeUri
            )
            if (key != null) indexes[key] = index
            files.forEach { fileIndexes[it] = index }
        }
    }
    return result
}