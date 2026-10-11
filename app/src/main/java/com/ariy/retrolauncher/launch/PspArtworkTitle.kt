package com.ariy.retrolauncher.launch

/** Search-only cleanup of PSP dump labels. Never guess missing subtitles or rewrite saved names. */
internal fun pspArtworkTitle(raw: String): String {
    var title = raw.trim().replace(Regex("""(?i)\.(?:iso|cso|chd|pbp|png|jpg|jpeg)$"""), "")
        .replace('_', ' ')
        .replace(Regex("""^\d{4}\s*-\s*"""), "")
        .replace(Regex("""(?i)(?:[ .-]+)PSP[- ][a-z0-9]+$"""), "")
        .replace('.', ' ')
    title = title.replace(Regex("""(?i)\b(?:MULTI\d+|PROPER|READNFO|INTERNAL|REPACK|PSN|USA|EUR|EUROPE|JAPAN)\b"""), " ")
        .replace(Regex("""\([^)]*\)|\[[^]]*]"""), " ")
        .replace(Regex("""\s+"""), " ").trim()
    return title.ifBlank { raw.trim() }
}

internal fun pspArtworkMatchKey(raw: String): String = pspArtworkTitle(raw).lowercase()
    .replace("&", " and ")
    .split(Regex("[^a-z0-9]+"))
    .filter { it.isNotBlank() && it !in setOf("the", "a", "an") }
    .joinToString("")