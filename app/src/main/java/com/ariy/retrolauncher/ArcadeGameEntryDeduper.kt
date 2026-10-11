package com.ariy.retrolauncher

import com.ariy.retrolauncher.launch.ArcadeGameCatalog
import com.ariy.retrolauncher.launch.arcadeRomIdentity
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.toJsonString
import java.net.URI

/** Merge known same-game tiles, not shared cabinets, sequel prefixes, or different platforms. */
internal fun dedupeArcadeGameEntries(entries: List<GameEntry>): List<GameEntry> {
    val result = mutableListOf<GameEntry>()
    val indexes = mutableMapOf<String, Int>()
    for (entry in entries) {
        val platform = entry.platform?.lowercase()
        val key = if (platform in setOf("mame arcade", "final burn neo")) {
            val romKeys = entry.allTargets().mapNotNull { target ->
                val filename = runCatching { URI(target.intentDataUri.orEmpty()).path?.substringAfterLast('/') }.getOrNull()
                filename?.takeIf { it.substringAfterLast('.', "").lowercase() in setOf("zip", "7z", "chd") }
                    ?.let(::arcadeRomIdentity)
            }.distinct()
            val identity = if (romKeys.isEmpty()) ArcadeGameCatalog.find(entry.title)?.rom else romKeys.singleOrNull()
            identity?.let { "$platform:$it" }
        } else null
        val index = key?.let(indexes::get)
        if (index == null) {
            result += entry
            if (key != null) indexes[key] = result.lastIndex
        } else {
            val existing = result[index]
            val targets = (existing.allTargets() + entry.allTargets()).distinctBy { it.stableKey() }
            result[index] = existing.copy(
                subtitle = existing.subtitle.ifBlank { entry.subtitle },
                genre = existing.genre ?: entry.genre,
                releaseYear = existing.releaseYear ?: entry.releaseYear,
                tagline = existing.tagline ?: entry.tagline,
                playCount = existing.playCount.coerceAtLeast(0).let { count ->
                    count + entry.playCount.coerceIn(0, Long.MAX_VALUE - count)
                },
                imageUri = existing.imageUri ?: entry.imageUri,
                lastPlayedAtEpochMillis = listOfNotNull(existing.lastPlayedAtEpochMillis, entry.lastPlayedAtEpochMillis).maxOrNull(),
                alternateTargetsJson = targets.filterNot { it.stableKey() == existing.primaryTarget().stableKey() }
                    .takeIf { it.isNotEmpty() }?.toJsonString(),
                preferredTargetKey = existing.preferredTargetKey ?: entry.preferredTargetKey,
                authorizedTreeUri = existing.authorizedTreeUri ?: entry.authorizedTreeUri
            )
        }
    }
    return result
}