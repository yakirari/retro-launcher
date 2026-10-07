package com.ariy.retrolauncher

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.ariy.retrolauncher.model.GamePlatforms

fun resolvePreferredRomFolderUri(
    context: Context,
    authorizedRomTreeUri: String?,
    subfolderHint: String?,
    platform: String? = null,
    preferTreeUri: Boolean = false
): Uri? {
    val treeUri = authorizedRomTreeUri
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: return null
    val preferredFolder = resolvePreferredRomFolder(
        context = context,
        authorizedRomTreeUri = authorizedRomTreeUri,
        subfolderHint = subfolderHint,
        platform = platform
    ) ?: return null
    val documentId = runCatching { DocumentsContract.getDocumentId(preferredFolder.uri) }.getOrNull()
        ?: runCatching { DocumentsContract.getTreeDocumentId(preferredFolder.uri) }.getOrNull()
        ?: return preferredFolder.uri
    if (preferTreeUri) {
        val authority = preferredFolder.uri.authority ?: treeUri.authority
        if (!authority.isNullOrBlank()) {
            runCatching {
                DocumentsContract.buildTreeDocumentUri(authority, documentId)
            }.getOrNull()?.let { return it }
        }
    }
    return runCatching {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }.getOrDefault(preferredFolder.uri)
}

fun resolvePreferredRomFolder(
    context: Context,
    authorizedRomTreeUri: String?,
    subfolderHint: String?,
    platform: String? = null
): DocumentFile? {
    val treeUri = authorizedRomTreeUri
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: return null
    val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return null
    val hintCandidates = romFolderHintCandidates(subfolderHint, platform)
    if (hintCandidates.isEmpty()) return root

    val rootDirectories = childDirectories(root)
    Log.w(
        "RetroLauncher",
        "ROM resolver hint=$subfolderHint candidates=$hintCandidates rootName=${documentMatchNames(root)} directDirs=${rootDirectories.take(12).flatMap(::documentMatchNames).distinct()}"
    )
    val directMatches = matchingDirectories(rootDirectories, hintCandidates)
    if (directMatches.isNotEmpty()) {
        Log.w(
            "RetroLauncher",
            "ROM resolver directMatches=${directMatches.map(::documentMatchNames)} fileCounts=${directMatches.map(::directFileCount)} childDirs=${directMatches.flatMap(::childDirectories).take(12).flatMap(::documentMatchNames).distinct()}"
        )
    }
    val refinedDirectMatch = resolveNestedMatch(directMatches, hintCandidates)
    if (refinedDirectMatch != null) {
        Log.w(
            "RetroLauncher",
            "ROM resolver selected direct=${documentMatchNames(refinedDirectMatch)} files=${directFileCount(refinedDirectMatch)}"
        )
        return refinedDirectMatch
    }

    val containerDirectories = rootDirectories.filter(::isLikelyRomContainerDirectory)
    val nestedMatches = matchingDirectories(
        containerDirectories.flatMap(::childDirectories),
        hintCandidates
    )
    val refinedNestedMatch = resolveNestedMatch(nestedMatches, hintCandidates)
    if (refinedNestedMatch != null) {
        Log.w(
            "RetroLauncher",
            "ROM resolver selected nested=${documentMatchNames(refinedNestedMatch)} files=${directFileCount(refinedNestedMatch)}"
        )
        return refinedNestedMatch
    }

    val nonEmptyMatch = (directMatches + nestedMatches).firstOrNull(::documentDirectoryHasFiles)
    if (nonEmptyMatch != null) return nonEmptyMatch

    val matchingRoot = hintCandidates.firstOrNull { candidate -> romFolderNameMatches(root.name, candidate) }
    if (matchingRoot != null && documentDirectoryHasFiles(root)) {
        return root
    }

    return directMatches.firstOrNull()
        ?: nestedMatches.firstOrNull()
        ?: root.takeIf(::documentDirectoryHasFiles)
        ?: root
}

fun romFolderHintDisplayLabel(subfolderHint: String?): String? {
    val candidates = romFolderHintCandidates(subfolderHint)
    if (candidates.isEmpty()) return subfolderHint?.trim()?.takeIf { it.isNotBlank() }
    return candidates.joinToString(" or ") { it }
}

fun preferredRomFolderHintForPlatform(platform: String?): String? =
    when (GamePlatforms.normalizeOrNull(platform)) {
        "MAME Arcade" -> "mame"
        "Final Burn Neo" -> "fbneo"
        "DOS Games" -> "dos"
        "ScummVM" -> "scummvm"
        "Nintendo Entertainment System" -> "nes"
        "Sega Master System" -> "sms"
        "PC Engine / TurboGrafx-16" -> "pcengine"
        "Sega Genesis / Mega Drive" -> "md"
        "Game Boy" -> "gb"
        "Neo Geo" -> "neogeo"
        "Super Nintendo" -> "snes"
        "Sega CD" -> "segacd"
        "Sega 32X" -> "32x"
        "Sega Saturn" -> "saturn"
        "Sony PlayStation" -> "ps"
        "Nintendo 64" -> "n64"
        "Game Boy Color" -> "gbc"
        "Neo Geo Pocket" -> "ngp"
        "Sega Dreamcast" -> "dreamcast"
        "Neo Geo Pocket Color" -> "ngpc"
        "Sony PlayStation 2" -> "ps2"
        "Game Boy Advance" -> "gba"
        "Nintendo GameCube" -> "gamecube"
        "Nintendo DS" -> "nds"
        "Sony PlayStation Portable" -> "psp"
        "Sony PlayStation 3" -> "ps3"
        "Nintendo Wii" -> "wii"
        "Nintendo 3DS" -> "n3ds"
        "Sony PlayStation Vita" -> "vita"
        "Nintendo Switch" -> "switch"
        else -> null
    }

private fun romFolderHintCandidates(subfolderHint: String?, platform: String? = null): List<String> =
    buildList {
        addAll(romFolderHintCandidatesForRawHint(subfolderHint))
        addAll(romFolderHintCandidatesForRawHint(preferredRomFolderHintForPlatform(platform)))
    }.distinctBy { it.lowercase() }

private fun romFolderHintCandidatesForRawHint(subfolderHint: String?): List<String> {
    val normalized = normalizeRomFolderName(subfolderHint) ?: return emptyList()
    return when (normalized) {
        "mame", "arcade" -> listOf("mame", "arcade", "mamearcade")
        "fbneo", "finalburnneo", "finalburn" -> listOf("fbneo", "finalburnneo", "finalburn", "arcade")
        "dos", "dosgames" -> listOf("dos", "dosgames", "pcdos")
        "scummvm" -> listOf("scummvm")
        "nes" -> listOf("nes", "fc", "famicom", "nintendoentertainmentsystem")
        "sms", "mastersystem", "segamastersystem" -> listOf("sms", "mastersystem", "segamastersystem")
        "snes" -> listOf("snes", "supernintendo", "supernintendosystem", "supernintendoentertainmentsystem")
        "pcengine", "tg16", "pce" -> listOf("pcengine", "pce", "tg16", "turbografx16", "turbografx")
        "md", "genesis", "megadrive", "segagenesis" -> listOf("md", "genesis", "megadrive", "segagenesis")
        "neogeo" -> listOf("neogeo")
        "segacd", "megacd" -> listOf("segacd", "megacd", "sega cd", "mega cd")
        "32x", "sega32x" -> listOf("32x", "sega32x")
        "saturn", "segasaturn" -> listOf("saturn", "segasaturn")
        "psx", "ps1", "ps", "playstation" -> prioritizeRomFolderHint(
            primary = normalized,
            aliases = listOf("ps", "psx", "ps1", "playstation", "sonyplaystation")
        )
        "n64" -> listOf("n64", "nintendo64")
        "ngp", "neogeopocket" -> listOf("ngp", "neogeopocket")
        "dreamcast", "dc", "segadreamcast" -> listOf("dreamcast", "dc", "segadreamcast")
        "ngpc", "neogeopocketcolor" -> listOf("ngpc", "neogeopocketcolor")
        "ps2", "playstation2" -> listOf("ps2", "playstation2", "sonyplaystation2")
        "n3ds", "3ds" -> listOf("n3ds", "3ds", "nintendo3ds")
        "gamecube", "gc" -> listOf("gamecube", "gc", "nintendogamecube")
        "nds", "ds" -> listOf("nds", "ds", "nintendods")
        "gb" -> listOf("gb", "gameboy")
        "gbc" -> listOf("gbc", "gameboycolor")
        "gba" -> listOf("gba", "gameboyadvance")
        "psp" -> listOf("psp", "playstationportable", "sonyplaystationportable")
        "ps3", "playstation3" -> listOf("ps3", "playstation3", "sonyplaystation3")
        "wii" -> listOf("wii", "nintendowii")
        "switch" -> listOf("switch", "nintendoswitch")
        "vita", "psvita", "playstationvita" -> listOf("vita", "psvita", "playstationvita", "sonyplaystationvita")
        else -> listOf(subfolderHint?.trim()?.trim('/').orEmpty()).filter { it.isNotBlank() }
    }
}

private fun prioritizeRomFolderHint(primary: String, aliases: List<String>): List<String> =
    (listOf(primary) + aliases).distinctBy { it.lowercase() }

private fun childDirectories(directory: DocumentFile): List<DocumentFile> =
    runCatching { directory.listFiles().filter(DocumentFile::isDirectory) }.getOrDefault(emptyList())

private fun directFileCount(directory: DocumentFile): Int =
    runCatching { directory.listFiles().count(DocumentFile::isFile) }.getOrDefault(0)

private fun matchingDirectories(
    directories: List<DocumentFile>,
    hintCandidates: List<String>
): List<DocumentFile> =
    directories
        .mapNotNull { directory ->
            directoryMatchPriority(directory, hintCandidates)?.let { priority ->
                Triple(directory, priority.first, priority.second)
            }
        }
        .sortedWith(
            compareBy<Triple<DocumentFile, Int, Int>>(
                { it.second },
                { it.third },
                { normalizeRomFolderName(it.first.name).orEmpty() }
            )
        )
        .map { it.first }
        .distinctBy { it.uri.toString() }

private fun resolveNestedMatch(
    candidateDirectories: List<DocumentFile>,
    hintCandidates: List<String>
): DocumentFile? {
    val directNonEmpty = candidateDirectories.firstOrNull(::documentDirectoryHasFiles)
    if (directNonEmpty != null) return directNonEmpty

    val nestedMatches = matchingDirectories(candidateDirectories.flatMap(::childDirectories), hintCandidates)
    return nestedMatches.firstOrNull(::documentDirectoryHasFiles) ?: nestedMatches.firstOrNull()
}

private fun isLikelyRomContainerDirectory(directory: DocumentFile): Boolean {
    return documentMatchNames(directory).any { rawName ->
        val normalizedName = normalizeRomFolderName(rawName) ?: return@any false
        normalizedName == "rom" ||
            normalizedName == "roms" ||
            normalizedName == "games" ||
            normalizedName == "game" ||
            normalizedName == "emulation" ||
            normalizedName == "emulator" ||
            normalizedName == "emulators" ||
            normalizedName == "retroarch" ||
            normalizedName == "romcollection" ||
            normalizedName == "gamelibrary"
    }
}

private fun documentMatchNames(document: DocumentFile): List<String> =
    buildList {
        document.name?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        document.uri.lastPathSegment?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        runCatching { DocumentsContract.getDocumentId(document.uri) }
            .getOrNull()
            ?.substringAfterLast(':')
            ?.substringAfterLast('/')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(::add)
    }.distinct()

private fun romFolderNameMatches(rawName: String?, expectedName: String): Boolean {
    return romFolderNameMatchStrength(rawName, expectedName) != null
}

private fun romFolderNameMatchStrength(rawName: String?, expectedName: String): Int? {
    val normalizedName = normalizeRomFolderName(rawName) ?: return null
    val normalizedExpected = normalizeRomFolderName(expectedName) ?: return null
    val nameTokens = tokenizeRomFolderName(rawName)
    return when {
        normalizedName == normalizedExpected -> 0
        nameTokens.any { it == normalizedExpected } -> 1
        normalizedExpected.length >= 4 && normalizedName.startsWith(normalizedExpected) -> 2
        else -> null
    }
}

private fun directoryMatchPriority(
    directory: DocumentFile,
    hintCandidates: List<String>
): Pair<Int, Int>? {
    var bestCandidateIndex = Int.MAX_VALUE
    var bestMatchStrength = Int.MAX_VALUE
    documentMatchNames(directory).forEach { rawName ->
        hintCandidates.forEachIndexed { candidateIndex, candidate ->
            val strength = romFolderNameMatchStrength(rawName, candidate) ?: return@forEachIndexed
            if (
                candidateIndex < bestCandidateIndex ||
                    (candidateIndex == bestCandidateIndex && strength < bestMatchStrength)
            ) {
                bestCandidateIndex = candidateIndex
                bestMatchStrength = strength
            }
        }
    }
    return if (bestCandidateIndex == Int.MAX_VALUE) {
        null
    } else {
        bestCandidateIndex to bestMatchStrength
    }
}

private fun normalizeRomFolderName(rawName: String?): String? =
    rawName
        ?.trim()
        ?.trim('/')
        ?.lowercase()
        ?.replace(Regex("[^a-z0-9]+"), "")
        ?.takeIf { it.isNotBlank() }

private fun tokenizeRomFolderName(rawName: String?): List<String> =
    rawName
        ?.trim()
        ?.trim('/')
        ?.lowercase()
        ?.split(Regex("[^a-z0-9]+"))
        ?.mapNotNull { token -> token.takeIf(String::isNotBlank) }
        .orEmpty()

private fun documentDirectoryHasFiles(directory: DocumentFile): Boolean =
    runCatching {
        directory.listFiles().any { child ->
            child.isFile && !(child.name ?: "").contains("Zone.Identifier", ignoreCase = true)
        }
    }.getOrDefault(false)
