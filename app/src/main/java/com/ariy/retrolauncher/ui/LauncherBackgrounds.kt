package com.ariy.retrolauncher.ui

internal object LauncherBackgrounds {
    const val DEFAULT = "basement_7.png"
    private val names = mapOf(
        "basement_1.png" to "Neon Console Oasis",
        "basement_2.png" to "Mixtape Hideaway",
        "basement_3.png" to "Quarter Drop Arcade",
        "basement_4.png" to "Golden Era Game Room",
        "basement_5.png" to "Collector’s Vault",
        "basement_6.png" to "After Hours Workshop",
        "basement_7.png" to "Player One Lounge"
    )

    fun label(asset: String): String = names[asset.lowercase()]
        ?: asset.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')

    fun defaultFor(available: List<String>): String =
        available.firstOrNull { it.equals(DEFAULT, ignoreCase = true) } ?: "none"
}