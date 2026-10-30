package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GamePlatforms
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/** Exact-title lookup in Libretro's public, community-maintained box-art directories. */
internal object LibretroArtworkCatalog {
    private const val ROOT = "https://thumbnails.libretro.com/"
    private const val MAX_INDEX_BYTES = 6 * 1024 * 1024
    private val cachedEntries = ConcurrentHashMap<String, List<CatalogEntry>>()
    private val failedIndexUntil = ConcurrentHashMap<String, Long>()

    private val systemDirectories = mapOf(
        "MAME Arcade" to "MAME",
        "Final Burn Neo" to "FBNeo - Arcade Games",
        "DOS Games" to "DOS",
        "ScummVM" to "ScummVM",
        "Nintendo Entertainment System" to "Nintendo - Nintendo Entertainment System",
        "Super Nintendo" to "Nintendo - Super Nintendo Entertainment System",
        "Nintendo 64" to "Nintendo - Nintendo 64",
        "Game Boy" to "Nintendo - Game Boy",
        "Game Boy Color" to "Nintendo - Game Boy Color",
        "Game Boy Advance" to "Nintendo - Game Boy Advance",
        "Nintendo GameCube" to "Nintendo - GameCube",
        "Nintendo DS" to "Nintendo - Nintendo DS",
        "Nintendo 3DS" to "Nintendo - Nintendo 3DS",
        "Nintendo Wii" to "Nintendo - Wii",
        "Sega Master System" to "Sega - Master System - Mark III",
        "Sega Genesis / Mega Drive" to "Sega - Mega Drive - Genesis",
        "Sega CD" to "Sega - Mega-CD - Sega CD",
        "Sega 32X" to "Sega - 32X",
        "Sega Saturn" to "Sega - Saturn",
        "Sega Dreamcast" to "Sega - Dreamcast",
        "Neo Geo" to "SNK - Neo Geo",
        "Neo Geo Pocket" to "SNK - Neo Geo Pocket",
        "Neo Geo Pocket Color" to "SNK - Neo Geo Pocket Color",
        "PC Engine / TurboGrafx-16" to "NEC - PC Engine - TurboGrafx 16",
        "Sony PlayStation" to "Sony - PlayStation",
        "Sony PlayStation 2" to "Sony - PlayStation 2",
        "Sony PlayStation Portable" to "Sony - PlayStation Portable",
        "Sony PlayStation Vita" to "Sony - PlayStation Vita"
    )

    fun fetchCandidates(platform: String?, rawTitle: String, romName: String? = null): List<ArtworkCandidate> {
        val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return emptyList()
        val directoryName = systemDirectories[normalizedPlatform] ?: return emptyList()
        val isPsp = normalizedPlatform == "Sony PlayStation Portable"
        val isScummVm = normalizedPlatform == "ScummVM"
        val isNes = normalizedPlatform == "Nintendo Entertainment System"
        val titleKey = when {
            isNes -> artworkMatchKey(romName?.takeIf { it.isNotBlank() } ?: rawTitle)
            isPsp -> pspArtworkMatchKey(rawTitle)
            isScummVm -> scummVmArtworkMatchKey(rawTitle)
            else -> artworkMatchKey(rawTitle)
        }
        if (titleKey.isBlank()) return emptyList()

        val directoryUrl = "$ROOT${encodePath(directoryName)}/Named_Boxarts/"
        if (System.currentTimeMillis() < (failedIndexUntil[directoryUrl] ?: 0L)) return emptyList()
        val entries = cachedEntries[directoryUrl] ?: loadDirectory(directoryUrl)?.also {
            cachedEntries[directoryUrl] = it
        } ?: run {
            failedIndexUntil[directoryUrl] = System.currentTimeMillis() + 5 * 60_000L
            return emptyList()
        }

        val matchingEntries = if (isNes) {
            selectNesEntries(entries, rawTitle, romName)
        } else entries
        val matches = matchingEntries.asSequence()
            .filter { (when {
                isNes -> it.matchKey
                isPsp -> pspArtworkMatchKey(it.displayTitle)
                isScummVm -> scummVmArtworkMatchKey(it.displayTitle)
                else -> it.matchKey
            }) == titleKey }
            .toList()
        val bestRegion = matches.minOfOrNull { regionRank(it.displayTitle, romName ?: rawTitle) }
        return matches.asSequence()
            .filter { !isNes || regionRank(it.displayTitle, romName ?: rawTitle) == 0 }
            .filter { regionRank(it.displayTitle, romName ?: rawTitle) == bestRegion }
            .take(6)
            .map { entry ->
                ArtworkCandidate(
                    title = entry.displayTitle,
                    source = "Libretro box-art catalog",
                    imageUrl = URL(URL(directoryUrl), entry.href).toString()
                )
            }
            .toList()
    }

    internal fun regionRank(candidate: String, romName: String): Int {
        fun regions(value: String): Set<String> = Regex("\\(([^)]*)\\)").findAll(value)
            .flatMap { it.groupValues[1].lowercase().split(Regex("[, /]+")).asSequence() }
            .map { when (it) { "u", "us" -> "usa"; "e", "eu" -> "europe"; "j", "jp" -> "japan"; else -> it } }
            .filter { it in setOf("usa", "europe", "japan", "world", "australia") }.toSet()
        val wanted = regions(romName)
        if (wanted.isEmpty()) return 0 // Do not invent a region when the ROM gives no evidence.
        val offered = regions(candidate)
        return when {
            offered.any { it in wanted } -> 0
            "world" in offered -> 1
            else -> 2
        }
    }

    /** Prefer the actual NES filename/edition, not a shortened tile title or an old hack alias. */
    internal fun selectNesEntries(entries: List<CatalogEntry>, title: String, romName: String?): List<CatalogEntry> {
        val rom = romName?.let { normalizedArtworkTitle(it, stripFileExtension = true) }.orEmpty()
        val key = artworkMatchKey(rom.ifBlank { title })
        val matches = entries.filter { it.matchKey == key && regionRank(it.displayTitle, romName ?: title) == 0 &&
            nesEditionKey(it.displayTitle) == nesEditionKey(romName ?: title) }
        val filename = romName?.substringBeforeLast('.', romName).orEmpty()
        matches.filter { it.displayTitle.equals(filename, ignoreCase = true) }.takeIf { it.isNotEmpty() }?.let { return it }
        // A legacy GoodNES dump/hack name must not win merely by appearing first in the index.
        val modern = matches.filter { !it.displayTitle.contains('[') &&
            !Regex("\\((?:19|20)[0-9x]{2}(?:-[0-9x]{2}){0,2}\\)", RegexOption.IGNORE_CASE).containsMatchIn(it.displayTitle) }
        return modern.ifEmpty { matches }
    }

    /** Retain publisher, prototype and other edition qualifiers; ignore only neutral metadata. */
    private fun nesEditionKey(name: String): Set<String> = Regex("[\\(\\[]([^\\)\\]]*)[\\)\\]]").findAll(name)
        .map { it.groupValues[1].lowercase().trim() }
        .filterNot { group ->
            group.matches(Regex("rev(?:ision)?\\s*[a-z0-9]+")) ||
                group.split(Regex("[, /]+" )).all { it in setOf("usa", "us", "u", "europe", "eu", "e", "japan", "jp", "j", "world", "australia",
                    "en", "ja", "fr", "de", "es", "it", "pt", "nl", "sv", "no", "da", "fi", "zh", "ko") }
        }.toSet()

    internal fun artworkMatchKey(rawTitle: String): String {
        val title = normalizedArtworkTitle(rawTitle, stripFileExtension = true).orEmpty()
            .replace(Regex("(?i)\\s*\\((?:usa|us|europe|eu|japan|jp|world|en|fr|de|es|it|rev(?:ision)?\\s*[a-z0-9]*)\\)"), " ")
        return title.lowercase().replace(Regex("[^a-z0-9]+"), "")
    }

    internal fun scummVmArtworkMatchKey(rawTitle: String): String =
        ScummVmGames.artworkTitle(rawTitle).lowercase(java.util.Locale.ROOT)
            .replace(Regex("^the\\s+"), "")
            // Generic URI/title cleanup treats subtitle colons as URI schemes; retain the whole game name.
            .replace(Regex("[^a-z0-9]+"), "")

    private fun loadDirectory(directoryUrl: String): List<CatalogEntry>? = runCatching {
        val connection = (URL(directoryUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RetroLauncher/1.0 (artwork catalog lookup)")
            setRequestProperty("Accept", "text/html")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val html = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_INDEX_BYTES) { "Artwork index is too large" }
                    output.write(buffer, 0, count)
                }
                output.toString(StandardCharsets.UTF_8.name())
            }
            parseDirectoryIndex(html)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    internal fun parseDirectoryIndex(html: String): List<CatalogEntry> {
        val anchorPattern = Regex("""(?is)<a\s+href=(["'])(.*?)\1[^>]*>([^<]+)</a>""")
        return anchorPattern.findAll(html).mapNotNull { match ->
            val href = match.groupValues[2]
            if (href.startsWith("?") || href.startsWith("/") || href.endsWith("/")) return@mapNotNull null
            val decodedName = URLDecoder.decode(href.replace("+", "%2B"), StandardCharsets.UTF_8.name())
            val extension = decodedName.substringAfterLast('.', "").lowercase()
            if (extension !in setOf("png", "jpg", "jpeg", "webp")) return@mapNotNull null
            val displayTitle = decodedName.substringBeforeLast('.', decodedName)
            val matchKey = artworkMatchKey(displayTitle)
            if (matchKey.isBlank()) null else CatalogEntry(href, displayTitle, matchKey)
        }.toList()
    }

    private fun encodePath(path: String): String = path.split('/').joinToString("/") { segment ->
        java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }

    internal data class CatalogEntry(
        val href: String,
        val displayTitle: String,
        val matchKey: String
    )
}