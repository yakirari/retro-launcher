package com.ariy.retrolauncher

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
