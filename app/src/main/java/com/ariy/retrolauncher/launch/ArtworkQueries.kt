package com.ariy.retrolauncher.launch

import android.net.Uri
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import java.io.File

fun GameEntry.artworkSearchQuery(
    fallbackTitle: String,
    includeCoverDescriptor: Boolean = true
): String = artworkSearchQueries(
    fallbackTitle = fallbackTitle,
    includeCoverDescriptor = includeCoverDescriptor
).firstOrNull().orEmpty()

fun GameEntry.artworkSearchQueries(
    fallbackTitle: String,
    includeCoverDescriptor: Boolean = true
): List<String> {
    val fileName = romFileNameForArtworkSearch()
    val baseTitle = normalizedArtworkTitle(fallbackTitle)
        ?: normalizedArtworkTitle(fileName)
        ?: fallbackTitle.trim().takeIf { it.isNotBlank() }
        ?: fileName?.trim()?.takeIf { it.isNotBlank() }
        ?: title.trim().takeIf { it.isNotBlank() }
        ?: platform?.trim()?.takeIf { it.isNotBlank() }
        ?: ""
    if (supportsArcadeGameplayArtworkQuery()) {
        return listOf(
            listOfNotNull(baseTitle.takeIf { it.isNotBlank() }, "mame").joinToString(" ")
        )
    }
    if (supportsAndroidAppIconArtworkQuery()) {
        return buildList {
            add(listOf(baseTitle, GamePlatforms.ANDROID, "app icon").joinToString(" "))
            add(listOf(baseTitle, "app icon").joinToString(" "))
            add(listOf(baseTitle, "icon").joinToString(" "))
            add(baseTitle)
        }.map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    }
    val platformTerms = artworkPlatformSearchTerms(platform)
        .filterNot { term -> baseTitle.contains(term, ignoreCase = true) }
    val primaryPlatformTerm = platformTerms.firstOrNull()
    return buildList {
        add(
            listOfNotNull(
                baseTitle.takeIf { it.isNotBlank() },
                *platformTerms.toTypedArray(),
                "cover art".takeIf { includeCoverDescriptor }
            ).joinToString(" ")
        )
        primaryPlatformTerm?.let { term ->
            add(listOf(baseTitle, term, "box art").joinToString(" "))
            add(listOf(baseTitle, term, "cover art").joinToString(" "))
        }
        add(listOf(baseTitle, "cover art").joinToString(" "))
        add(baseTitle)
    }.map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
}

fun GameEntry.arcadeRomShortNameForArtworkSearch(): String? =
    romFileNameForArtworkSearch()
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }

fun GameEntry.arcadeOverlayAssetBaseNames(): List<String> {
    val baseNames = linkedSetOf<String>()
    title.toArcadeOverlayAssetSlug()?.let(baseNames::add)
    arcadeRomShortNameForArtworkSearch()?.let(baseNames::add)
    return baseNames.toList()
}

fun findMatchingArcadeOverlayAssetName(
    availableAssets: Map<String, String>,
    assetBaseNames: Collection<String>
): String? {
    if (availableAssets.isEmpty() || assetBaseNames.isEmpty()) return null

    val supportedAssets = availableAssets.values
        .filter { assetName ->
            when (assetName.substringAfterLast('.', "").lowercase()) {
                "png", "jpg", "jpeg" -> true
                else -> false
            }
        }

    val normalizedAssetsByStem = supportedAssets
        .mapNotNull { assetName ->
            normalizeArcadeOverlayAssetKey(assetName.substringBeforeLast('.', assetName))
                ?.let { assetStem -> assetStem to assetName }
        }
        .groupBy(
            keySelector = { (assetStem, _) -> assetStem },
            valueTransform = { (_, assetName) -> assetName }
        )

    val normalizedCandidates = assetBaseNames
        .mapNotNull(::normalizeArcadeOverlayAssetKey)
        .distinct()

    normalizedCandidates.forEach { candidate ->
        normalizedAssetsByStem[candidate]
            ?.let(::preferredArcadeOverlayAssetName)
            ?.let { return it }
    }

    normalizedCandidates.forEach { candidate ->
        normalizedAssetsByStem.entries
            .filter { (assetStem, _) ->
                assetStem.length >= 4 && candidate.length > assetStem.length && candidate.startsWith(assetStem)
            }
            .maxByOrNull { (assetStem, _) -> assetStem.length }
            ?.value
            ?.let(::preferredArcadeOverlayAssetName)
            ?.let { return it }
    }

    return null
}

fun isGenericArcadeFallbackAssetName(assetName: String): Boolean {
    val stem = assetName.substringBeforeLast('.', assetName).lowercase()
    return stem == "mame"
}

private fun GameEntry.supportsArcadeGameplayArtworkQuery(): Boolean =
    platform.equals("MAME Arcade", ignoreCase = true) ||
        platform.equals("Final Burn Neo", ignoreCase = true)

private fun GameEntry.supportsAndroidAppIconArtworkQuery(): Boolean =
    platform.equals(GamePlatforms.ANDROID, ignoreCase = true)

fun normalizedArtworkTitle(raw: String?, stripFileExtension: Boolean = false): String? {
    val trimmed = raw?.trim().orEmpty()
    val baseName = if (stripFileExtension && trimmed.contains('.')) {
        trimmed.substringBeforeLast('.', trimmed).trim()
    } else {
        trimmed
    }
    if (baseName.isBlank()) return null
    val cleaned = baseName
        .replace('_', ' ')
        .replace('.', ' ')
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("\\[[^]]*]"), " ")
        .replace(Regex("""\{[^}]*\}"""), " ")
        .replace(Regex("\\b(?:usa|europe|japan|world|rev\\s*[a-z0-9]+|v\\d+(?:\\.\\d+)*)\\b", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return cleaned.ifBlank { baseName.takeIf { it.isNotBlank() } }
}

private fun artworkPlatformSearchTerms(rawPlatform: String?): List<String> {
    val platform = GamePlatforms.normalizeOrNull(rawPlatform) ?: rawPlatform?.trim().orEmpty()
    if (platform.isBlank()) return emptyList()
    return when (platform) {
        "Nintendo Entertainment System" -> listOf("Nintendo Entertainment System", "NES")
        "Super Nintendo" -> listOf("Super Nintendo", "SNES")
        "Nintendo 64" -> listOf("Nintendo 64", "N64")
        "Nintendo GameCube" -> listOf("Nintendo GameCube", "GameCube")
        "Nintendo DS" -> listOf("Nintendo DS", "NDS")
        "Nintendo 3DS" -> listOf("Nintendo 3DS", "3DS")
        "Nintendo Wii" -> listOf("Nintendo Wii", "Wii")
        "Nintendo Switch" -> listOf("Nintendo Switch")
        "Game Boy" -> listOf("Game Boy")
        "Game Boy Color" -> listOf("Game Boy Color")
        "Game Boy Advance" -> listOf("Game Boy Advance")
        "Sega Master System" -> listOf("Sega Master System", "Master System")
        "Sega Genesis / Mega Drive" -> listOf("Sega Genesis", "Mega Drive")
        "Sega CD" -> listOf("Sega CD", "Mega CD")
        "Sega 32X" -> listOf("Sega 32X", "32X")
        "Sega Saturn" -> listOf("Sega Saturn")
        "Sega Dreamcast" -> listOf("Sega Dreamcast", "Dreamcast")
        "Sony PlayStation" -> listOf("PlayStation", "PS1", "PSX")
        "Sony PlayStation 2" -> listOf("PlayStation 2", "PS2")
        "Sony PlayStation Portable" -> listOf("PlayStation Portable", "PSP")
        "Sony PlayStation 3" -> listOf("PlayStation 3", "PS3")
        "Sony PlayStation Vita" -> listOf("PlayStation Vita", "PS Vita")
        "PC Engine / TurboGrafx-16" -> listOf("PC Engine", "TurboGrafx-16")
        "Neo Geo Pocket" -> listOf("Neo Geo Pocket", "NGP")
        "Neo Geo Pocket Color" -> listOf("Neo Geo Pocket Color", "NGPC")
        "Final Burn Neo" -> listOf("Final Burn Neo", "arcade")
        "DOS Games" -> listOf("DOS")
        else -> listOf(platform)
    }
}

private fun GameEntry.romFileNameForArtworkSearch(): String? {
    val candidates = sequenceOf(
        preferredTarget()?.intentDataUri,
        intentDataUri
    ) + allTargets().asSequence().map { it.intentDataUri }
    return candidates
        .mapNotNull { it?.takeIf(String::isNotBlank) }
        .mapNotNull { uriString ->
            runCatching {
                val parsed = Uri.parse(uriString)
                val segment = parsed.lastPathSegment.orEmpty().ifBlank {
                    File(uriString).name
                }
                val decoded = Uri.decode(segment).ifBlank { segment }
                val withoutExt = decoded.substringBeforeLast('.', decoded).trim()
                withoutExt.ifBlank { null }
            }.getOrNull()
        }
        .firstOrNull()
}

private fun String?.toArcadeOverlayAssetSlug(): String? =
    this
        ?.trim()
        ?.lowercase()
        ?.replace("&", "and")
        ?.replace(Regex("[^a-z0-9]+"), "")
        ?.takeIf { it.isNotBlank() }

private fun normalizeArcadeOverlayAssetKey(raw: String?): String? =
    raw
        ?.trim()
        ?.lowercase()
        ?.replace(Regex("[^a-z0-9]+"), "")
        ?.takeIf { it.isNotBlank() }

private fun preferredArcadeOverlayAssetName(assetNames: List<String>): String? =
    assetNames.minByOrNull { assetName ->
        when (assetName.substringAfterLast('.', "").lowercase()) {
            "png" -> 0
            "jpg" -> 1
            "jpeg" -> 2
            else -> 3
        }
    }
