package com.ariy.retrolauncher.model

import android.net.Uri

object GamePlatforms {
    const val ANDROID = "Android"
    const val EMULATORS = "Emulators & Launchers"

    val values = listOf(
        "MAME Arcade",
        ANDROID,
        EMULATORS,
        "DOS Games",
        "Nintendo Entertainment System",
        "Sega Master System",
        "ScummVM",
        "PC Engine / TurboGrafx-16",
        "Sega Genesis / Mega Drive",
        "Final Burn Neo",
        "Game Boy",
        "Neo Geo",
        "Super Nintendo",
        "Sega CD",
        "Sega 32X",
        "Sega Saturn",
        "Sony PlayStation",
        "Nintendo 64",
        "Game Boy Color",
        "Neo Geo Pocket",
        "Sega Dreamcast",
        "Neo Geo Pocket Color",
        "Sony PlayStation 2",
        "Game Boy Advance",
        "Nintendo GameCube",
        "Nintendo DS",
        "Sony PlayStation Portable",
        "Nintendo Wii",
        "Nintendo 3DS",
        "Sony PlayStation Vita",
        "Nintendo Switch",
        "PC (Windows)",
        "Steam", "GOG", "Epic Games", "Amazon Games", "GameNative", "GameHub", "GameHub Lite"
    )

    private val aliasToPlatform = mapOf(
        "androidgames" to ANDROID,
        "emulators" to EMULATORS,
        "launchers" to EMULATORS,
        "nes" to "Nintendo Entertainment System",
        "snes" to "Super Nintendo",
        "n64" to "Nintendo 64",
        "z64" to "Nintendo 64",
        "v64" to "Nintendo 64",
        "gb" to "Game Boy",
        "gbc" to "Game Boy Color",
        "gba" to "Game Boy Advance",
        "nds" to "Nintendo DS",
        "3ds" to "Nintendo 3DS",
        "gc" to "Nintendo GameCube",
        "gamecube" to "Nintendo GameCube",
        "wii" to "Nintendo Wii",
        "switch" to "Nintendo Switch",
        "mastersystem" to "Sega Master System",
        "sms" to "Sega Master System",
        "md" to "Sega Genesis / Mega Drive",
        "mdemu" to "Sega Genesis / Mega Drive",
        "genesis" to "Sega Genesis / Mega Drive",
        "megadrive" to "Sega Genesis / Mega Drive",
        "segacd" to "Sega CD",
        "32x" to "Sega 32X",
        "saturn" to "Sega Saturn",
        "dreamcast" to "Sega Dreamcast",
        "dc" to "Sega Dreamcast",
        "psx" to "Sony PlayStation",
        "ps1" to "Sony PlayStation",
        "ps" to "Sony PlayStation",
        "playstation" to "Sony PlayStation",
        "playstation1" to "Sony PlayStation",
        "ps2" to "Sony PlayStation 2",
        "psp" to "Sony PlayStation Portable",
        "vita" to "Sony PlayStation Vita",
        "neogeo" to "Neo Geo",
        "ngp" to "Neo Geo Pocket",
        "ngpc" to "Neo Geo Pocket Color",
        "mame" to "MAME Arcade",
        "fbneo" to "Final Burn Neo",
        "scummvm" to "ScummVM",
        "dos" to "DOS Games",
        "pcengine" to "PC Engine / TurboGrafx-16",
        "tg16" to "PC Engine / TurboGrafx-16",
        "pc" to "PC (Windows)",
        "windows" to "PC (Windows)",
        "gamenative" to "GameNative",
        "steam" to "Steam", "gog" to "GOG", "epic" to "Epic Games",
        "gamehub" to "GameHub", "gamehublite" to "GameHub Lite"
    )

    private val packageToPlatform = mapOf(
        "com.github.stenzek.duckstation" to "Sony PlayStation",
        "xyz.aethersx2.android" to "Sony PlayStation 2",
        "org.ppsspp.ppsspp" to "Sony PlayStation Portable",
        "com.explusalpha.NesEmu" to "Nintendo Entertainment System",
        "com.explusalpha.MdEmu" to "Sega Genesis / Mega Drive",
        "org.mupen64plusae.v3.fzurita.pro" to "Nintendo 64",
        "com.dsemu.drastic" to "Nintendo DS",
        "io.recompiled.redream" to "Sega Dreamcast",
        "com.flycast.emulator" to "Sega Dreamcast",
        "com.PceEmu" to "PC Engine / TurboGrafx-16",
        "com.explusalpha.NeoEmu" to "Neo Geo",
        "com.explusalpha.Snes9xPlus" to "Super Nintendo",
        "com.explusalpha.GbaEmu" to "Game Boy Advance",
        "org.citra.emu" to "Nintendo 3DS",
        "org.azahar_emu.azahar" to "Nintendo 3DS",
        "org.dolphinemu.mmjr" to "Nintendo GameCube",
        "org.dolphinemu.dolphinemu" to "Nintendo GameCube",
        "org.vita3k.emulator" to "Sony PlayStation Vita",
        "dev.eden.eden_emulator" to "Nintendo Switch",
        "org.scummvm.scummvm" to "ScummVM",
        "org.devmiyax.yabasanshioro2.pro" to "Sega Saturn",
        "com.fishstix.dosbox" to "DOS Games",
        "app.gamenative" to "PC (Windows)"
    )

    private val platformToFilterLabel = mapOf(
        ANDROID to "Android Games",
        "DOS Games" to "ScummVM",
        "MAME Arcade" to "Arcade",
        "Final Burn Neo" to "Arcade",
        "PC Engine / TurboGrafx-16" to "Arcade",
        "Neo Geo" to "Arcade",
        "Neo Geo Pocket" to "Arcade",
        "Neo Geo Pocket Color" to "Arcade",
        "Sega CD" to "Sega",
        "Sega 32X" to "Sega"
    )

    private val hiddenFilterLabels = setOf("Sega")

    private val knownFilterLabels = (values + platformToFilterLabel.values)
        .distinctBy { it.lowercase() }

    fun normalizeOrNull(raw: String?): String? {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isBlank()) return null
        return values.firstOrNull { it.equals(normalized, ignoreCase = true) }
            ?: aliasToPlatform[normalizeAliasKey(normalized)]
    }

    fun inferFromUriString(rawUri: String?): String? {
        val uriString = rawUri?.takeIf { it.isNotBlank() } ?: return null
        val decoded = runCatching { Uri.decode(uriString) }.getOrDefault(uriString).lowercase()
        val tokens = Regex("[a-z0-9]+").findAll(decoded).map { it.value }.toList()
        return tokens.asReversed().firstNotNullOfOrNull { token -> aliasToPlatform[token] }
    }

    fun inferFromPackageName(rawPackageName: String?): String? {
        val packageName = rawPackageName?.trim().orEmpty()
        if (packageName.isBlank()) return null
        return packageToPlatform[packageName]
    }

    fun filterLabel(rawPlatform: String?): String? {
        val normalizedPlatform = normalizeOrNull(rawPlatform)
        if (normalizedPlatform != null) {
            return platformToFilterLabel[normalizedPlatform] ?: normalizedPlatform
        }
        val normalizedFilter = rawPlatform?.trim().orEmpty()
        if (normalizedFilter.isBlank()) return null
        return knownFilterLabels.firstOrNull { it.equals(normalizedFilter, ignoreCase = true) }
    }

    fun filterValues(): List<String> =
        values.mapNotNull(::filterLabel)
            .filter(::isVisibleFilterLabel)
            .distinctBy { it.lowercase() }

    fun matchesFilter(platform: String?, filter: String?): Boolean {
        val platformFilter = filterLabel(platform) ?: return false
        val requestedFilter = filterLabel(filter) ?: return false
        return platformFilter.equals(requestedFilter, ignoreCase = true)
    }

    fun isVisibleFilterLabel(filter: String?): Boolean {
        val normalizedFilter = filterLabel(filter) ?: return true
        return hiddenFilterLabels.none { hidden -> hidden.equals(normalizedFilter, ignoreCase = true) }
    }

    private fun normalizeAliasKey(raw: String): String =
        raw
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
}

object GameFilters {
    const val ALL = "All"

    fun normalizeFilter(raw: String?): String = when {
        raw.isNullOrBlank() -> ALL
        raw.equals(ALL, ignoreCase = true) -> ALL
        else -> raw.trim()
    }
}
