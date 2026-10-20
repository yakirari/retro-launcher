package com.ariy.retrolauncher.model

/** Opaque decorative shell colors; null lets the renderer match artwork, with a platform fallback. */
object HandheldCartridgeSettings {
    val colorChoices: List<Pair<String?, String>> = listOf(
        null to "Auto", "grey" to "Gray", "graphite" to "Graphite",
        "indigo" to "Indigo", "red" to "Red", "blue" to "Blue",
        "green" to "Green", "gold" to "Gold"
    )

    fun supportsPlatform(platform: String?): Boolean = GamePlatforms.normalizeOrNull(platform) in
        setOf("Game Boy", "Game Boy Color", "Game Boy Advance")

    fun normalizeColor(color: String?): String? = color?.trim()?.lowercase()
        ?.takeIf { key -> colorChoices.any { it.first == key } }

    fun shellColor(platform: String?, color: String?): Int = when (normalizeColor(color)
        ?: when (GamePlatforms.normalizeOrNull(platform)) {
            "Game Boy Color" -> "indigo"
            "Game Boy Advance" -> "graphite"
            else -> "grey"
        }) {
        "graphite" -> 0xFF454650.toInt()
        "indigo" -> 0xFF7975AE.toInt()
        "red" -> 0xFFC74742.toInt()
        "blue" -> 0xFF437CB9.toInt()
        "green" -> 0xFF57946C.toInt()
        "gold" -> 0xFFB9A05A.toInt()
        else -> 0xFFBDBDB5.toInt()
    }
}