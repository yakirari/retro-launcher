package com.ariy.retrolauncher.launch

private val discTrackNamePattern = Regex("""(?i)(?:^|[\s._()\[\]-])track[\s._-]*0*\d+(?:$|[\s._()\[\]-])""")
private val switchAddonNamePattern = Regex(
    """(?i)(?:^|[\s._-]|\[|\(|\{)(update|upd|patch|dlc|addon|add-on|aoc)(?:$|[\s._-]|\]|\)|\})"""
)

internal fun hasAllowedRomExtension(displayName: String, allowedExtensions: Set<String>): Boolean =
    displayName.substringAfterLast('.', "").lowercase() in allowedExtensions

/** Track BINs are data; a CUE/M3U/CHD is still a launchable descriptor even with 'track' in its name. */
internal fun isDiscTrackDataFile(displayName: String): Boolean =
    displayName.substringAfterLast('.', "").equals("bin", ignoreCase = true) &&
        discTrackNamePattern.containsMatchIn(displayName)

/** Validated package metadata takes precedence over ambiguous words in a game's filename. */
internal fun isSwitchBaseGame(titleId: String?, displayName: String): Boolean =
    if (titleId != null) titleId.endsWith("000") else !switchAddonNamePattern.containsMatchIn(displayName)