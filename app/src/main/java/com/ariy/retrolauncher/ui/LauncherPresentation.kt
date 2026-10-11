package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.launch.ArcadeGameCatalog
import com.ariy.retrolauncher.launch.ScummVmGames
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import java.net.URI

private val romExtensions = setOf(
    "zip", "7z", "nes", "fds", "sfc", "smc", "gb", "gbc", "gba", "nds", "3ds", "cci",
    "cxi", "xci", "xcz", "nsp", "nsz", "iso", "bin", "cue", "chd", "gdi", "cdi", "m3u",
    "pbp", "cso", "rvz", "gcz", "gcm", "wia", "wbfs", "sms", "md", "gen", "smd", "z64",
    "n64", "v64", "pce", "sgx", "32x", "ngp", "ngc", "ngpc"
)
private val metadataGroup = Regex("""\s*[\[(]([^\])]+)[\])]""")
private val metadata = Regex(
    """(?i)^(?:usa|us|europe|eu|japan|jp|world|en|fr|de|es|it|pal|ntsc|rev(?:ision)?\s*[\w.]+|v\d[\w.]*|disc\s*\d+|disk\s*\d+|\d{16}|[a-f0-9]{16}|[!bh]|t[-+].*|(?:en|fr|de|es|it|ja)(?:[, +](?:en|fr|de|es|it|ja))+)$"""
)

/** Conservative, display-only cleanup: preserve subtitles and meaningful parenthesized names. */
internal fun curatedDisplayTitle(raw: String): String {
    val trimmed = raw.trim()
    val hasRomExtension = trimmed.substringAfterLast('.', "").lowercase() in romExtensions
    var title = if (hasRomExtension) trimmed.substringBeforeLast('.') else trimmed
    title = metadataGroup.replace(title) { match ->
        if (metadata.matches(match.groupValues[1].trim())) " " else match.value
    }
    if (hasRomExtension) title = title.replace('_', ' ')
    title = title.replace(Regex("""\s+"""), " ").trim()
    return title.ifBlank { trimmed.ifBlank { "Untitled game" } }
}

internal data class TileTitle(val primary: String, val secondary: String? = null)

internal fun GameEntry.libraryDisplayTitle(): String {
    if (GamePlatforms.normalizeOrNull(platform) == "ScummVM") {
        return ScummVmGames.title(title) ?: title
    }
    if (platform == "MAME Arcade" || platform == "Final Burn Neo") {
        // Only expand known short names; keep custom/user-authored titles intact.
        ArcadeGameCatalog.find(title)?.let { return it.title }
    }
    return title
}

/** Game metadata only: do not surface emulator labels as game descriptions. */
private fun GameEntry.arcadeMetadata(): ArcadeGameCatalog.Game? =
    if (GamePlatforms.normalizeOrNull(platform) in setOf("MAME Arcade", "Final Burn Neo")) {
        val filename = runCatching { URI(intentDataUri.orEmpty()).path?.substringAfterLast('/') }.getOrNull()
        ArcadeGameCatalog.find(filename) ?: ArcadeGameCatalog.find(title)
    } else null

internal fun GameEntry.libraryReleaseYear(): Int? =
    (releaseYear?.trim()?.takeIf { it.matches(Regex("[0-9]{4}")) } ?: arcadeMetadata()?.year)?.toIntOrNull()

internal fun GameEntry.libraryGameCaption(): String? {
    val arcade = arcadeMetadata()
    val year = libraryReleaseYear()?.toString()
    val phrase = tagline?.trim()?.takeIf(String::isNotBlank) ?: arcade?.catchphrase
    return listOfNotNull(year, phrase).joinToString(" · ").takeIf(String::isNotBlank)
}

/** Tile captions describe the game, not the configured launch target. Stored names are untouched. */
internal fun tileTitle(raw: String, split: Boolean = true): TileTitle {
    val title = curatedDisplayTitle(raw)
    if (!split) return TileTitle(title)
    val separator = Regex("""\s*:\s+|\s+[–—-]\s+""").find(title)
    if (separator != null && separator.range.first > 0) {
        return TileTitle(title.substring(0, separator.range.first).trim(),
            title.substring(separator.range.last + 1).trim().takeIf { it.isNotEmpty() })
    }
    val franchise = listOf("Castlevania", "Castelvania", "The Legend of Zelda", "Final Fantasy",
        "Super Mario", "Sonic the Hedgehog", "Dragon Quest", "Mega Man", "Pokemon", "Pokémon")
        .firstOrNull { title.startsWith("$it ", ignoreCase = true) }
    if (franchise != null) {
        return TileTitle(title.take(franchise.length), title.drop(franchise.length).trim())
    }
    if (title.length <= 30) return TileTitle(title)
    // For otherwise unstructured long names, use a word boundary instead of truncating information.
    val boundary = title.lastIndexOf(' ', 24).takeIf { it >= 10 } ?: return TileTitle(title)
    return TileTitle(title.take(boundary), title.drop(boundary + 1))
}

/** Respect the chosen density, but never create unreadably narrow cards. */
internal fun responsiveGridColumns(widthDp: Float, requestedColumns: Int): Int {
    val width = widthDp.coerceAtLeast(1f)
    val minimumWidth = if (requestedColumns >= 8) 88f else 136f
    val fittingColumns = ((width + 10f) / (minimumWidth + 10f)).toInt().coerceAtLeast(1)
    return requestedColumns.coerceAtLeast(1).coerceAtMost(fittingColumns)
}

internal fun responsiveGalleryWidth(widthDp: Float, heightDp: Float, fraction: Float, aspectRatio: Float): Float {
    val availableWidth = (widthDp - 16f).coerceAtLeast(1f)
    val availableHeight = (heightDp - 88f).coerceAtLeast(1f)
    return (widthDp * fraction).coerceAtMost(availableWidth)
        .coerceAtMost(availableHeight * aspectRatio.coerceAtLeast(0.1f)).coerceAtLeast(1f)
}