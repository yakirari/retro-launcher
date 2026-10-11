package com.ariy.retrolauncher

import com.ariy.retrolauncher.ui.curatedDisplayTitle

internal fun normalizeSwitchTitleId(rawValue: String?): String? {
    val candidate = rawValue
        ?.trim()
        ?.uppercase()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    if (candidate.length != 16 || !candidate.startsWith("0100")) return null
    if (!candidate.all { it in '0'..'9' || it in 'A'..'F' }) return null
    return candidate
}

internal fun normalizeSwitchBaseTitleId(rawValue: String?): String? =
    normalizeSwitchTitleId(rawValue)?.dropLast(3)?.plus("000")

internal fun preferSwitchBaseTitleId(candidates: Iterable<String?>): String? {
    val valid = candidates.mapNotNull(::normalizeSwitchTitleId).distinct()
    return valid.firstOrNull { it.endsWith("000") } ?: valid.firstOrNull()
}

/** Filenames often carry the title ID even when encrypted NCA headers cannot be inspected. */
internal fun switchTitleIdFromFilename(name: String?): String? {
    val candidates = Regex("""(?i)(?<![a-z0-9])0100[a-f0-9]{12}(?![a-z0-9])""")
        .findAll(name.orEmpty()).map { it.value }.toList()
    return preferSwitchBaseTitleId(candidates)
}

/** Rights ID is relative to the ticket body, AFTER the aligned signature block. */
internal fun switchTicketRightsIdOffset(signatureType: Int): Long? = when (signatureType) {
    0x010000, 0x010003 -> 0x240L + 0x160L
    0x010001, 0x010004 -> 0x140L + 0x160L
    0x010002, 0x010005 -> 0x80L + 0x160L
    0x010006 -> 0x40L + 0x160L
    else -> null
}

/** Only remove recognizable release metadata; editions, sequels and meaningful parentheses survive. */
internal fun switchFilenameDedupeKey(name: String?): String? {
    if (name.isNullOrBlank()) return null
    val cleaned = curatedDisplayTitle(name)
        .replace(Regex("""(?i)[\[({]\s*(?:base|update|upd|patch|dlc|addon|add-on|aoc|season\s*pass|modded)(?:\s+[\dv.]+)?\s*[\])}]"""), " ")
        .replace(Regex("""(?i)\s+(?:update|upd|patch)\s*\d*(?:\.\d+)*\s*$"""), " ")
        .replace(Regex("""(?i)\s+v\d+(?:\.\d+)*\s*$"""), " ")
    return cleaned.lowercase().replace("&", " and ")
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() && it !in setOf("the", "a", "an") }
        .joinToString("").takeIf { it.isNotBlank() }
}
