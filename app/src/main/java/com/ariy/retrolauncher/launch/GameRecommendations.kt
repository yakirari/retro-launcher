package com.ariy.retrolauncher.launch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RawRes
import com.ariy.retrolauncher.R
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.LaunchTarget
import com.ariy.retrolauncher.model.PlatformSvgIcons
import com.ariy.retrolauncher.inferSwitchTitleIdFromPackageUri
import com.ariy.retrolauncher.normalizeSwitchBaseTitleId
import com.ariy.retrolauncher.normalizeSwitchTitleId
import com.ariy.retrolauncher.resolvePreferredRomFolder
import com.ariy.retrolauncher.listRomDocuments
import com.caverock.androidsvg.SVG
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

data class GameRecommendation(
    val displayEntry: GameEntry,
    val launchEntry: GameEntry,
    val reason: String,
    val previewAssetPath: String? = null,
    val isCurated: Boolean = true
)

data class ArcadeScanCandidate(
    val title: String,
    val romUri: String
)

data class ScummVmScanCandidate(
    val title: String,
    val folderUri: String,
    val gameId: String? = null,
    val registeredTarget: String? = null
)

data class PlatformRomScanCandidate(
    val title: String,
    val romUri: String
)

private data class ResolvedPlatformRomScanCandidate(
    val title: String,
    val romUri: String,
    val dedupeKey: String,
    val priority: Int
)

private data class PlatformScanSpec(
    val allowedExtensions: Set<String>,
    val extensionPriority: List<String>,
    val accepts: (Context, AvailableRom) -> Boolean = { _, _ -> true },
    val candidateDedupeKey: (Context, AvailableRom, String) -> String? =
        { _, rom, title -> defaultPlatformScanDedupeKey(title, rom) },
    val existingGameDedupeKeys: (Context, GameEntry) -> Set<String> =
        { _, game -> defaultPlatformScanDedupeKeys(game) }
)

private data class RecommendationCsvColumns(
    val titleIndex: Int,
    val romIndex: Int,
    val reasonIndex: Int? = null
)

private data class RecommendationRule(
    val id: String? = null,
    val title: String,
    val romFileName: String,
    val reason: String,
    val titleAliases: List<String> = emptyList(),
    val acceptedRomSets: List<String> = emptyList(),
    val acceptedFileNames: List<String> = emptyList(),
    val preferredRomSet: String? = null,
    val overlayAssetName: String? = null,
    val priority: Int? = null
)

private data class AvailableRom(
    val displayName: String,
    val uriString: String,
    val normalizedStems: List<String>
)

private data class RecommendationCursorState(
    val afterRecommendationId: String?,
    var hasReachedStart: Boolean = afterRecommendationId == null,
    var cursorFound: Boolean = afterRecommendationId == null
)

private data class RecommendationSource(
    val filterLabel: String,
    val platform: String,
    val romFolderHint: String,
    @RawRes val priorityListResId: Int? = null,
    val includeDirectories: Boolean = false,
    val buildLaunchEntry: (Context, String, RecommendationRule, AvailableRom, GameEntry?) -> GameEntry?,
    val buildPreviewAssetPath: (Context, RecommendationRule) -> String? = { _, _ -> null },
    val key: String = recommendationSourceKey(filterLabel, platform, romFolderHint)
)

private val cachedAvailableRoms = TimedCache<String, List<AvailableRom>>(32, 30_000)
private const val WILDCARD_RECOMMENDATION_REASON = "Wild card"
private val ignoredRecommendationExtensions = setOf("txt", "png", "jpg", "jpeg", "pdf", "doc", "docx", "csv")
private val archiveExtensions = setOf("zip", "7z")
private val explicitPlatformScanPlatforms = setOf(
    "Final Burn Neo",
    "Nintendo Entertainment System",
    "Sega Master System",
    "PC Engine / TurboGrafx-16",
    "Sega Genesis / Mega Drive",
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
    "Nintendo Switch"
)

private fun defaultPlatformScanDedupeKey(
    title: String,
    rom: AvailableRom
): String? =
    normalizeLooseKey(title)
        ?: normalizeLooseStem(rom.displayName)

private fun defaultPlatformScanDedupeKeys(game: GameEntry): Set<String> =
    buildSet {
        normalizeLooseKey(game.title)?.let(::add)
        scanCandidateDisplayNameFromUriString(game.intentDataUri)
            ?.let(::normalizeLooseStem)
            ?.let(::add)
    }

private fun scanCandidateDisplayNameFromUriString(uriString: String?): String? {
    val raw = uriString?.trim().orEmpty().ifBlank { return null }
    val parsed = runCatching { Uri.parse(raw) }.getOrNull()
    return parsed?.lastPathSegment
        ?.substringAfterLast('/')
        ?.let(Uri::decode)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun romFileExtension(displayName: String?): String? =
    displayName
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }

private fun hasAllowedPlatformScanExtension(
    rom: AvailableRom,
    allowedExtensions: Set<String>
): Boolean = hasAllowedRomExtension(rom.displayName, allowedExtensions)

private fun acceptsPlatformRom(context: Context, rom: AvailableRom, spec: PlatformScanSpec): Boolean =
    hasAllowedPlatformScanExtension(rom, spec.allowedExtensions) &&
        spec.accepts(context, rom)

private fun platformScanPriority(
    rom: AvailableRom,
    extensionPriority: List<String>
): Int {
    val extension = romFileExtension(rom.displayName)
    val preferredIndex = extensionPriority.indexOf(extension)
    return if (preferredIndex >= 0) preferredIndex else extensionPriority.size + 10
}

private fun discImageScanSpec(
    allowedExtensions: Set<String>,
    extensionPriority: List<String>
): PlatformScanSpec = PlatformScanSpec(
    allowedExtensions = allowedExtensions,
    extensionPriority = extensionPriority,
    accepts = { _, rom -> !isDiscTrackDataFile(rom.displayName) }
)

private fun switchTitleIdFromRom(
    context: Context,
    rom: AvailableRom
): String? {
    val uri = runCatching { Uri.parse(rom.uriString) }.getOrNull() ?: return null
    return normalizeSwitchTitleId(inferSwitchTitleIdFromPackageUri(context, uri))
}

internal fun switchDisplayDedupeKey(displayName: String?): String? {
    return com.ariy.retrolauncher.switchFilenameDedupeKey(displayName)
}

private fun switchBaseTitleIdFromRom(
    context: Context,
    rom: AvailableRom
): String? = normalizeSwitchBaseTitleId(switchTitleIdFromRom(context, rom))

private fun switchTitleIdFromGameEntry(
    context: Context,
    game: GameEntry
): String? {
    val uri = game.intentDataUri?.takeIf { it.isNotBlank() }?.let {
        runCatching { Uri.parse(it) }.getOrNull()
    } ?: return null
    return normalizeSwitchTitleId(inferSwitchTitleIdFromPackageUri(context, uri))
}

private fun switchBaseTitleIdFromGameEntry(
    context: Context,
    game: GameEntry
): String? = normalizeSwitchBaseTitleId(switchTitleIdFromGameEntry(context, game))

private fun switchScanSpec(): PlatformScanSpec = PlatformScanSpec(
    allowedExtensions = setOf("xci", "xcz", "nsp", "nsz"),
    extensionPriority = listOf("xci", "nsp", "xcz", "nsz"),
    accepts = { context, rom ->
        val extensionAllowed = hasAllowedPlatformScanExtension(rom, setOf("xci", "xcz", "nsp", "nsz"))
        if (!extensionAllowed) {
            false
        } else {
            isSwitchBaseGame(switchTitleIdFromRom(context, rom), rom.displayName)
        }
    },
    candidateDedupeKey = { context, rom, title ->
        switchBaseTitleIdFromRom(context, rom)
            ?: switchDisplayDedupeKey(rom.displayName)
            ?: defaultPlatformScanDedupeKey(title, rom)
    },
    existingGameDedupeKeys = { context, game ->
        buildSet {
            switchBaseTitleIdFromGameEntry(context, game)?.let(::add)
            game.allTargets().forEach { target ->
                scanCandidateDisplayNameFromUriString(target.intentDataUri)
                    ?.let(::switchDisplayDedupeKey)?.let(::add)
            }
            addAll(defaultPlatformScanDedupeKeys(game))
        }
    }
)

private fun explicitPlatformScanSpec(
    platform: String,
    packageName: String? = null,
    retroArchSystemId: String? = null
): PlatformScanSpec? {
    val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return null
    val normalizedPackageName = packageName?.trim()?.takeIf { it.isNotBlank() }
    val usesRetroArch = normalizedPackageName == "com.retroarch" ||
        normalizedPackageName == "com.retroarch.aarch64"
    // A null package is local discovery, not a launch contract. Retain supported
    // cartridge archives even when the emulator will be configured after import.
    val discoveryOnly = normalizedPackageName == null
    return when (normalizedPlatform) {
        "Final Burn Neo" -> PlatformScanSpec(
            allowedExtensions = setOf("zip", "7z", "chd"),
            extensionPriority = listOf("zip", "7z", "chd")
        )
        "Nintendo Entertainment System" -> PlatformScanSpec(
            allowedExtensions = setOf("nes", "fds", "unf", "unif") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.NesEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("nes", "fds", "unf", "unif", "zip", "7z")
        )
        "Sega Master System" -> PlatformScanSpec(
            allowedExtensions = setOf("sms", "bin") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.MdEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("sms", "bin", "zip", "7z")
        )
        "PC Engine / TurboGrafx-16" -> PlatformScanSpec(
            allowedExtensions = setOf("pce", "sgx", "cue", "chd", "iso", "bin", "m3u") +
                if (discoveryOnly || normalizedPackageName == "com.PceEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("m3u", "cue", "chd", "iso", "pce", "sgx", "bin", "zip", "7z"),
            accepts = { _, rom -> !isDiscTrackDataFile(rom.displayName) }
        )
        "Sega Genesis / Mega Drive" -> PlatformScanSpec(
            allowedExtensions = setOf("md", "bin", "gen", "smd") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.MdEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("md", "bin", "gen", "smd", "zip", "7z")
        )
        "Game Boy" -> PlatformScanSpec(
            allowedExtensions = setOf("gb", "sgb") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.GbcEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("gb", "sgb", "zip", "7z")
        )
        "Neo Geo" -> PlatformScanSpec(
            allowedExtensions = setOf("zip", "7z"),
            extensionPriority = listOf("zip", "7z")
        )
        "Super Nintendo" -> PlatformScanSpec(
            allowedExtensions = setOf("sfc", "smc", "fig", "swc") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.Snes9xPlus") archiveExtensions else emptySet(),
            extensionPriority = listOf("sfc", "smc", "fig", "swc", "zip", "7z")
        )
        "Sega CD" -> discImageScanSpec(
            allowedExtensions = setOf("cue", "chd", "iso", "bin", "m3u"),
            extensionPriority = listOf("m3u", "cue", "chd", "iso", "bin")
        )
        "Sega 32X" -> PlatformScanSpec(
            allowedExtensions = setOf("32x", "bin", "md", "gen") +
                if (discoveryOnly || normalizedPackageName == "com.explusalpha.MdEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("32x", "bin", "md", "gen", "zip", "7z")
        )
        "Sega Saturn" -> discImageScanSpec(
            allowedExtensions = setOf("cue", "chd", "iso", "bin", "m3u"),
            extensionPriority = listOf("m3u", "cue", "chd", "iso", "bin")
        )
        "Sony PlayStation" -> discImageScanSpec(
            allowedExtensions = setOf("pbp", "chd", "cue", "bin", "iso", "m3u"),
            extensionPriority = listOf("m3u", "pbp", "chd", "cue", "iso", "bin")
        )
        "Nintendo 64" -> PlatformScanSpec(
            allowedExtensions = setOf("z64", "n64", "v64") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "org.mupen64plusae.v3.fzurita.pro") archiveExtensions else emptySet(),
            extensionPriority = listOf("z64", "n64", "v64", "zip", "7z")
        )
        "Game Boy Color" -> PlatformScanSpec(
            allowedExtensions = setOf("gbc") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.GbcEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("gbc", "zip", "7z")
        )
        "Neo Geo Pocket" -> PlatformScanSpec(
            allowedExtensions = setOf("ngp") +
                if (discoveryOnly || normalizedPackageName == "com.explusalpha.NgpEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("ngp", "zip", "7z")
        )
        "Sega Dreamcast" -> discImageScanSpec(
            allowedExtensions = setOf("gdi", "cue", "chd", "cdi", "bin", "m3u"),
            extensionPriority = listOf("m3u", "gdi", "cue", "chd", "cdi", "bin")
        )
        "Neo Geo Pocket Color" -> PlatformScanSpec(
            allowedExtensions = setOf("ngc", "ngpc", "npc") +
                if (discoveryOnly || normalizedPackageName == "com.explusalpha.NgpEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("ngpc", "ngc", "npc", "zip", "7z")
        )
        "Sony PlayStation 2" -> discImageScanSpec(
            allowedExtensions = setOf("iso", "chd", "cso", "bin"),
            extensionPriority = listOf("chd", "iso", "cso", "bin")
        )
        "Game Boy Advance" -> PlatformScanSpec(
            allowedExtensions = setOf("gba", "agb") +
                if (discoveryOnly || usesRetroArch || normalizedPackageName == "com.explusalpha.GbaEmu") archiveExtensions else emptySet(),
            extensionPriority = listOf("gba", "agb", "zip", "7z")
        )
        "Nintendo GameCube" -> PlatformScanSpec(
            allowedExtensions = setOf("rvz", "gcz", "iso", "gcm", "wia", "ciso"),
            extensionPriority = listOf("rvz", "gcz", "iso", "gcm", "wia", "ciso")
        )
        "Nintendo DS" -> PlatformScanSpec(
            allowedExtensions = setOf("nds") + if (discoveryOnly || usesRetroArch) archiveExtensions else emptySet(),
            extensionPriority = listOf("nds", "zip", "7z")
        )
        "Sony PlayStation Portable" -> PlatformScanSpec(
            allowedExtensions = setOf("iso", "cso", "pbp"),
            extensionPriority = listOf("iso", "cso", "pbp")
        )
        "Nintendo Wii" -> PlatformScanSpec(
            allowedExtensions = setOf("rvz", "wbfs", "iso", "wia", "ciso"),
            extensionPriority = listOf("rvz", "wbfs", "iso", "wia", "ciso")
        )
        "Nintendo 3DS" -> PlatformScanSpec(
            allowedExtensions = setOf("3ds", "cci", "cxi", "app") +
                if (usesRetroArch && retroArchSystemId == "3ds") archiveExtensions else emptySet(),
            extensionPriority = listOf("3ds", "cci", "cxi", "app", "zip", "7z")
        )
        "Nintendo Switch" -> switchScanSpec()
        else -> null
    }
}

private fun recommendationSourceKey(
    filterLabel: String,
    platform: String,
    romFolderHint: String
): String = listOf(filterLabel, platform, romFolderHint).joinToString(":").systemId()

private fun platformRecommendationSource(
    platform: String,
    romFolderHint: String,
    @RawRes priorityListResId: Int? = null,
    filterLabel: String = GamePlatforms.filterLabel(platform) ?: platform,
    preferredTemplatePackages: List<String> = emptyList(),
    retroArchCoreSoFile: String? = null,
    includeDirectories: Boolean = false,
    buildPreviewAssetPath: (Context, RecommendationRule) -> String? = { _, _ -> null }
): RecommendationSource = RecommendationSource(
    filterLabel = filterLabel,
    platform = platform,
    romFolderHint = romFolderHint,
    priorityListResId = priorityListResId,
    includeDirectories = includeDirectories,
    buildLaunchEntry = { context, id, rule, rom, templateGame ->
        buildRecommendationLaunchEntry(
            context = context,
            id = id,
            title = rule.title,
            platform = platform,
            rom = rom,
            templateGame = templateGame,
            preferredTemplatePackages = preferredTemplatePackages,
            retroArchCoreSoFile = retroArchCoreSoFile
        )
    },
    buildPreviewAssetPath = buildPreviewAssetPath
)

object GameRecommendations {
    private val cachedRules = mutableMapOf<Int, List<RecommendationRule>>()

    fun invalidateRomInventory() = cachedAvailableRoms.clear()

    private val sources = listOf(
        platformRecommendationSource(
            platform = "MAME Arcade",
            romFolderHint = "mame",
            retroArchCoreSoFile = "mame2003_plus_libretro_android.so",
            buildPreviewAssetPath = previewAsset@{ context, rule ->
                val availableAssets = runCatching {
                    context.assets.list("overlay_mame")
                        ?.filterNot { it.contains("Zone.Identifier", ignoreCase = true) }
                        ?.associateBy { it.lowercase() }
                        .orEmpty()
                }.getOrDefault(emptyMap())
                val assetName = findMatchingArcadeOverlayAssetName(
                    availableAssets = availableAssets,
                    assetBaseNames = rule.previewAssetBaseNames()
                )?.takeUnless(::isGenericArcadeFallbackAssetName) ?: return@previewAsset null
                "overlay_mame/$assetName"
            }
        ),
        platformRecommendationSource(
            platform = "Final Burn Neo",
            romFolderHint = "fbneo",
            retroArchCoreSoFile = "fbneo_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "ScummVM",
            romFolderHint = "scummvm",
            preferredTemplatePackages = listOf("org.scummvm.scummvm"),
            includeDirectories = true
        ),
        platformRecommendationSource(
            platform = "DOS Games",
            romFolderHint = "dos",
            retroArchCoreSoFile = "dosbox_pure_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Nintendo Entertainment System",
            romFolderHint = "nes",
            preferredTemplatePackages = listOf("com.explusalpha.NesEmu"),
            retroArchCoreSoFile = "mesen_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Sega Master System",
            romFolderHint = "sms",
            preferredTemplatePackages = listOf("com.explusalpha.MdEmu"),
            retroArchCoreSoFile = "genesis_plus_gx_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "PC Engine / TurboGrafx-16",
            romFolderHint = "pcengine",
            preferredTemplatePackages = listOf("com.PceEmu")
        ),
        platformRecommendationSource(
            platform = "Sega Genesis / Mega Drive",
            romFolderHint = "md",
            preferredTemplatePackages = listOf("com.explusalpha.MdEmu"),
            retroArchCoreSoFile = "genesis_plus_gx_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Game Boy",
            romFolderHint = "gb",
            preferredTemplatePackages = listOf("com.explusalpha.GbcEmu"),
            retroArchCoreSoFile = "gambatte_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Neo Geo",
            romFolderHint = "neogeo",
            preferredTemplatePackages = listOf("com.explusalpha.NeoEmu")
        ),
        platformRecommendationSource(
            platform = "Super Nintendo",
            romFolderHint = "snes",
            preferredTemplatePackages = listOf("com.explusalpha.Snes9xPlus"),
            retroArchCoreSoFile = "snes9x_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Sega CD",
            romFolderHint = "segacd",
            preferredTemplatePackages = listOf("com.explusalpha.MdEmu"),
            retroArchCoreSoFile = "genesis_plus_gx_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Sega 32X",
            romFolderHint = "32x",
            preferredTemplatePackages = listOf("com.explusalpha.MdEmu")
        ),
        platformRecommendationSource(
            platform = "Sega Saturn",
            romFolderHint = "saturn",
            retroArchCoreSoFile = "yabasanshiro_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Sony PlayStation",
            romFolderHint = "ps",
            preferredTemplatePackages = listOf("com.github.stenzek.duckstation"),
            retroArchCoreSoFile = "beetle_psx_hw_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Nintendo 64",
            romFolderHint = "n64",
            preferredTemplatePackages = listOf("org.mupen64plusae.v3.fzurita.pro"),
            retroArchCoreSoFile = "mupen64plus_next_gles3_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Game Boy Color",
            romFolderHint = "gbc",
            preferredTemplatePackages = listOf("com.explusalpha.GbcEmu"),
            retroArchCoreSoFile = "gambatte_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Neo Geo Pocket",
            romFolderHint = "ngp",
            preferredTemplatePackages = listOf("com.explusalpha.NgpEmu")
        ),
        platformRecommendationSource(
            platform = "Sega Dreamcast",
            romFolderHint = "dreamcast",
            preferredTemplatePackages = listOf("io.recompiled.redream", "com.flycast.emulator"),
            retroArchCoreSoFile = "flycast_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Neo Geo Pocket Color",
            romFolderHint = "ngpc",
            preferredTemplatePackages = listOf("com.explusalpha.NgpEmu")
        ),
        platformRecommendationSource(
            platform = "Sony PlayStation 2",
            romFolderHint = "ps2",
            preferredTemplatePackages = listOf("xyz.aethersx2.android")
        ),
        platformRecommendationSource(
            platform = "Game Boy Advance",
            romFolderHint = "gba",
            preferredTemplatePackages = listOf("com.explusalpha.GbaEmu"),
            retroArchCoreSoFile = "mgba_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Nintendo GameCube",
            romFolderHint = "gamecube",
            preferredTemplatePackages = listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")
        ),
        platformRecommendationSource(
            platform = "Nintendo DS",
            romFolderHint = "nds",
            preferredTemplatePackages = listOf("com.dsemu.drastic"),
            retroArchCoreSoFile = "melonds_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Sony PlayStation Portable",
            romFolderHint = "psp",
            preferredTemplatePackages = listOf("org.ppsspp.ppsspp"),
            retroArchCoreSoFile = "ppsspp_libretro_android.so"
        ),
        platformRecommendationSource(
            platform = "Nintendo Wii",
            romFolderHint = "wii",
            preferredTemplatePackages = listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")
        ),
        platformRecommendationSource(
            platform = "Nintendo 3DS",
            romFolderHint = "n3ds",
            preferredTemplatePackages = listOf("org.azahar_emu.azahar", "org.citra.emu")
        ),
        platformRecommendationSource(
            platform = "Nintendo Switch",
            romFolderHint = "switch",
            preferredTemplatePackages = listOf("dev.eden.eden_emulator")
        )
    )

    fun supportsFilter(filter: String): Boolean {
        val normalizedFilter = GamePlatforms.filterLabel(filter) ?: GameFilters.normalizeFilter(filter)
        return sources.any { it.filterLabel.equals(normalizedFilter, ignoreCase = true) }
    }

    fun supportedFilters(): List<String> =
        sources.map { it.filterLabel }.distinctBy { it.lowercase() }

    fun supportsPlatformScan(platform: String?): Boolean {
        val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return false
        return normalizedPlatform.equals("MAME Arcade", ignoreCase = true) ||
            normalizedPlatform.equals("ScummVM", ignoreCase = true) ||
            normalizedPlatform in explicitPlatformScanPlatforms
    }

    fun filterHasAvailableRoms(
        context: Context,
        authorizedRomTreeUri: String?,
        filter: String
    ): Boolean {
        if (authorizedRomTreeUri.isNullOrBlank()) return false
        val normalizedFilter = GamePlatforms.filterLabel(filter) ?: GameFilters.normalizeFilter(filter)
        return sources.any { source ->
            source.filterLabel.equals(normalizedFilter, ignoreCase = true) &&
                sourceHasAvailableRoms(
                    context = context,
                    authorizedRomTreeUri = authorizedRomTreeUri,
                    source = source
                )
        }
    }

    fun filtersWithAvailableRoms(
        context: Context,
        authorizedRomTreeUri: String?
    ): List<String> {
        if (authorizedRomTreeUri.isNullOrBlank()) return emptyList()
        return supportedFilters().filter { filter ->
            filterHasAvailableRoms(
                context = context,
                authorizedRomTreeUri = authorizedRomTreeUri,
                filter = filter
            )
        }
    }

    fun scanImportableMameRoms(
        context: Context,
        authorizedRomTreeUri: String?,
        games: List<GameEntry>,
        cancellationCheck: () -> Unit = {}
    ): List<ArcadeScanCandidate> {
        if (authorizedRomTreeUri.isNullOrBlank()) return emptyList()
        val source = sources.firstOrNull { it.platform.equals("MAME Arcade", ignoreCase = true) }
            ?: return emptyList()
        val visibleRomUris = games.flatMapTo(mutableSetOf(), ::gameRomUriCandidates)
        val existingKeys = games
            .asSequence()
            .filter { game -> game.platform.equals("MAME Arcade", ignoreCase = true) }
            .flatMap { game ->
                val filenames = gameRomUriCandidates(game).mapNotNull { uri ->
                    runCatching { java.net.URI(uri).path?.substringAfterLast('/') }.getOrNull()
                }
                (filenames.mapNotNull(::arcadeRomIdentity) + listOfNotNull(ArcadeGameCatalog.find(game.title)?.rom)).asSequence()
            }
            .toSet()
        val roms = availableRomsForSource(context, authorizedRomTreeUri, source, forceRefresh = true, cancellationCheck = cancellationCheck)
            .map { rom ->
                cancellationCheck()
                ArcadeRomForImport(rom.displayName, rom.uriString)
            }
        cancellationCheck()
        return selectArcadeRomsForImport(roms, existingKeys, visibleRomUris)
    }

    fun scanImportableScummVmFolders(
        context: Context,
        authorizedRomTreeUri: String?,
        games: List<GameEntry>,
        cancellationCheck: () -> Unit = {}
    ): List<ScummVmScanCandidate> {
        if (authorizedRomTreeUri.isNullOrBlank()) return emptyList()
        val source = sources.firstOrNull { it.platform.equals("ScummVM", ignoreCase = true) }
            ?: return emptyList()
        val directory = resolvePreferredRomFolder(
            context = context,
            authorizedRomTreeUri = authorizedRomTreeUri,
            subfolderHint = source.romFolderHint,
            platform = source.platform,
            cancellationCheck = cancellationCheck
        ) ?: return emptyList()
        val documents = listRomDocuments(context, directory, cancellationCheck)
        val configGames = documents.filter { !it.isDirectory && it.name.equals("scummvm.ini", true) }.flatMap { config ->
            cancellationCheck()
            val text = context.contentResolver.openInputStream(config.uri)?.use { input ->
                val bytes = input.readNBytes(2 * 1024 * 1024 + 1)
                require(bytes.size <= 2 * 1024 * 1024) { "ScummVM config is too large" }
                String(bytes, Charsets.UTF_8)
            }.orEmpty()
            ScummVmGames.parseConfig(text)
        }
        val published = publishedScummVmTargets(context)
        return documents
            .asSequence()
            .filter { it.isDirectory }
            .mapNotNull { folder ->
                cancellationCheck()
                val detected = detectScummVmFolder(context, folder.uri, cancellationCheck)
                val registered = configGames.filter { game ->
                    Uri.decode(game.path).trimEnd('/').substringAfterLast('/').equals(folder.name, true)
                }.singleOrNull() ?: published.firstOrNull { it.target == detected }
                ScummVmScanCandidate(
                    title = registered?.title ?: ScummVmGames.title(detected) ?: wildcardRecommendationTitleFor(folder.name),
                    folderUri = folder.uri.toString(),
                    gameId = detected,
                    registeredTarget = registered?.target
                )
            }
            // Existing folder entries may have broken legacy targets. Let shared upsert repair them.
            .distinctBy { candidate -> candidate.folderUri }
            .sortedBy { candidate -> candidate.title.lowercase() }
            .toList()
    }

    fun scanImportablePlatformRoms(
        context: Context,
        authorizedRomTreeUri: String?,
        games: List<GameEntry>,
        platform: String,
        packageName: String? = null,
        retroArchSystemId: String? = null,
        cancellationCheck: () -> Unit = {}
    ): List<PlatformRomScanCandidate> {
        if (authorizedRomTreeUri.isNullOrBlank()) return emptyList()
        val scanSpec = explicitPlatformScanSpec(
            platform = platform,
            packageName = packageName,
            retroArchSystemId = retroArchSystemId
        ) ?: return emptyList()
        val source = sources.firstOrNull {
            it.platform.equals(platform, ignoreCase = true) && !it.includeDirectories
        } ?: return emptyList()
        val visibleRomUris = games.flatMapTo(mutableSetOf(), ::gameRomUriCandidates)
        val existingScanKeys = games
            .asSequence()
            .filter { game -> game.platform.equals(platform, ignoreCase = true) }
            .flatMap { game -> scanSpec.existingGameDedupeKeys(context, game).asSequence() }
            .toSet()
        val rules = loadSourceRules(context, source)
        return availableRomsForSource(context, authorizedRomTreeUri, source, forceRefresh = true, cancellationCheck = cancellationCheck)
            .asSequence()
            .filterNot { rom -> rom.uriString in visibleRomUris }
            .filter { rom ->
                cancellationCheck()
                acceptsPlatformRom(context, rom, scanSpec)
            }
            .mapNotNull { rom ->
                val matchedRule = bestMatchingRuleForRom(rom, rules) ?: wildcardRecommendationRuleFor(rom)
                val title = matchedRule.title.trim().ifBlank {
                    wildcardRecommendationTitleFor(rom.displayName)
                }
                val dedupeKey = scanSpec.candidateDedupeKey(context, rom, title)
                    ?: return@mapNotNull null
                ResolvedPlatformRomScanCandidate(
                    title = title,
                    romUri = rom.uriString,
                    dedupeKey = dedupeKey,
                    priority = platformScanPriority(rom, scanSpec.extensionPriority)
                )
            }
            .filterNot { candidate -> candidate.dedupeKey in existingScanKeys }
            .groupBy { candidate -> candidate.dedupeKey }
            .values
            .mapNotNull { grouped ->
                grouped
                    .sortedWith(
                        compareBy<ResolvedPlatformRomScanCandidate> { it.priority }
                            .thenBy { normalizeLooseKey(it.title)?.length ?: it.title.length }
                            .thenBy { it.title.lowercase() }
                            .thenBy { it.romUri.lowercase() }
                    )
                    .firstOrNull()
            }
            .map { candidate ->
                PlatformRomScanCandidate(
                    title = candidate.title,
                    romUri = candidate.romUri
                )
            }
            .sortedBy { candidate -> candidate.title.lowercase() }
            .toList()
    }

    fun suggestionsForFilter(
        context: Context,
        filter: String,
        authorizedRomTreeUri: String?,
        games: List<GameEntry>,
        excludedRecommendationIds: Set<String> = emptySet(),
        afterRecommendationId: String? = null,
        limit: Int = 1
    ): List<GameRecommendation> {
        if (limit <= 0) return emptyList()
        val normalizedFilter = GamePlatforms.filterLabel(filter) ?: GameFilters.normalizeFilter(filter)
        if (normalizedFilter.equals(GameFilters.ALL, ignoreCase = true)) return emptyList()
        val visibleGames = gamesMatchingFilter(games, normalizedFilter)
        val matchingSources = sources.filter { it.filterLabel.equals(normalizedFilter, ignoreCase = true) }
        if (matchingSources.isEmpty()) return emptyList()

        val selectedRecommendations = mutableListOf<GameRecommendation>()
        val excludedIds = excludedRecommendationIds.toMutableSet()
        val cursorState = RecommendationCursorState(afterRecommendationId)
        val cursorSourceKey = matchingSources.firstOrNull { source ->
            afterRecommendationId?.startsWith("recommendation:${source.key}:") == true
        }?.key
        for (source in matchingSources) {
            if (selectedRecommendations.size >= limit) break
            if (afterRecommendationId != null && !cursorState.cursorFound && cursorSourceKey != null && source.key != cursorSourceKey) {
                continue
            }
            selectedRecommendations += suggestionsForSource(
                context = context,
                filter = normalizedFilter,
                authorizedRomTreeUri = authorizedRomTreeUri,
                visibleGames = visibleGames,
                source = source,
                cursorState = cursorState,
                excludedRecommendationIds = excludedIds,
                limit = limit - selectedRecommendations.size
            )
            excludedIds += selectedRecommendations.map { it.displayEntry.id }
        }
        if (selectedRecommendations.isEmpty() && afterRecommendationId != null && !cursorState.cursorFound) {
            return suggestionsForFilter(
                context = context,
                filter = filter,
                authorizedRomTreeUri = authorizedRomTreeUri,
                games = games,
                excludedRecommendationIds = excludedRecommendationIds,
                afterRecommendationId = null,
                limit = limit
            )
        }
        return selectedRecommendations
    }

    fun nextForFilter(
        context: Context,
        filter: String,
        authorizedRomTreeUri: String?,
        games: List<GameEntry>,
        excludedRecommendationIds: Set<String> = emptySet(),
        afterRecommendationId: String? = null
    ): GameRecommendation? =
        suggestionsForFilter(
            context = context,
            filter = filter,
            authorizedRomTreeUri = authorizedRomTreeUri,
            games = games,
            excludedRecommendationIds = excludedRecommendationIds,
            afterRecommendationId = afterRecommendationId,
            limit = 1
        ).firstOrNull()

    private fun suggestionsForSource(
        context: Context,
        filter: String,
        authorizedRomTreeUri: String?,
        visibleGames: List<GameEntry>,
        source: RecommendationSource,
        cursorState: RecommendationCursorState,
        excludedRecommendationIds: Set<String>,
        limit: Int
    ): List<GameRecommendation> {
        if (limit <= 0) return emptyList()
        Log.w(
            "RetroLauncher",
            "Recommendation request filter=$filter treePresent=${!authorizedRomTreeUri.isNullOrBlank()} sourcePlatform=${source.platform} romFolderHint=${source.romFolderHint}"
        )
        val availableRoms = runCatching { availableRomsForSource(context, authorizedRomTreeUri, source) }
            .onFailure { Log.w("RetroLauncher", "Recommendation folder scan failed for ${source.platform}", it) }
            .getOrDefault(emptyList())
        if (availableRoms.isEmpty()) {
            Log.w(
                "RetroLauncher",
                "Recommendation request found no ROMs for filter=$filter sourcePlatform=${source.platform} romFolderHint=${source.romFolderHint}"
            )
            return emptyList()
        }

        val templateGame = recommendationTemplateGame(visibleGames, source)
        val rules = loadSourceRules(context, source)
        var noLaunchEntryCount = 0
        var excludedCount = 0
        var wildcardCount = 0
        var curatedCount = 0
        val selectedRecommendations = mutableListOf<GameRecommendation>()
        val visibleRomUris = visibleGames.flatMapTo(mutableSetOf(), ::gameRomUriCandidates)
        val orderedRoms = availableRoms.sortedWith(
            compareBy<AvailableRom> { rom -> rom.displayName.lowercase() }
                .thenBy { rom -> rom.uriString.lowercase() }
        )
        Log.d(
            "RetroLauncher",
            "Recommendation scan filter=$filter platform=${source.platform} roms=${availableRoms.size} visibleGames=${visibleGames.size} rules=${rules.size}"
        )
        for (rom in orderedRoms) {
            if (selectedRecommendations.size >= limit) break
            if (source.platform == "MAME Arcade" && romFileExtension(rom.displayName) !in setOf("zip", "7z", "chd")) continue
            val recommendationId = fileBackedRecommendationIdFor(source, rom)
            if (!cursorState.hasReachedStart) {
                if (recommendationId == cursorState.afterRecommendationId) {
                    cursorState.cursorFound = true
                    cursorState.hasReachedStart = true
                }
                continue
            }
            if (rom.uriString in visibleRomUris) continue

            val matchingRule = bestMatchingRuleForRom(rom, rules)
            if (recommendationId in excludedRecommendationIds) {
                excludedCount += 1
                continue
            }
            val suggestionRule = matchingRule ?: wildcardRecommendationRuleFor(rom)
            val previewAssetPath = source.buildPreviewAssetPath(context, suggestionRule)
            val baseLaunchEntry = source.buildLaunchEntry(context, recommendationId, suggestionRule, rom, templateGame)
            if (baseLaunchEntry == null) {
                noLaunchEntryCount += 1
                continue
            }
            val scanSpec = explicitPlatformScanSpec(source.platform, baseLaunchEntry.packageName)
            if (scanSpec != null && !acceptsPlatformRom(context, rom, scanSpec)) continue
            val sourcedArtworkUri = if (
                previewAssetPath.isNullOrBlank() &&
                !source.platform.equals("MAME Arcade", ignoreCase = true)
            ) {
                resolveRecommendationImportedArtworkUri(
                    context = context,
                    recommendationId = recommendationId
                )
            } else {
                null
            }
            val seededArtworkUri = if (sourcedArtworkUri.isNullOrBlank()) {
                buildRecommendationArtworkUri(
                    context = context,
                    id = recommendationId,
                    platform = source.platform,
                    title = suggestionRule.title
                )
            } else {
                null
            }
            val launchEntry = baseLaunchEntry.withRecommendationArtworkUri(sourcedArtworkUri ?: seededArtworkUri)
            val displayReason = matchingRule
                ?.reason
                ?.ifBlank { launchEntry.subtitle }
                ?: WILDCARD_RECOMMENDATION_REASON
            if (matchingRule != null) {
                curatedCount += 1
            } else {
                wildcardCount += 1
            }
            Log.d(
                "RetroLauncher",
                "Recommendation selected filter=$filter sourcePlatform=${source.platform} title=${suggestionRule.title} rom=${rom.displayName} curated=${matchingRule != null} previewAsset=${!previewAssetPath.isNullOrBlank()} sourcedArtwork=${!sourcedArtworkUri.isNullOrBlank()}"
            )
            selectedRecommendations += GameRecommendation(
                displayEntry = launchEntry.copy(subtitle = displayReason),
                launchEntry = launchEntry,
                reason = displayReason,
                previewAssetPath = previewAssetPath,
                isCurated = matchingRule != null
            )
        }
        Log.d(
            "RetroLauncher",
            "Recommendation scan completed filter=$filter sourcePlatform=${source.platform} selected=${selectedRecommendations.size} curated=$curatedCount wildcard=$wildcardCount noLaunchEntry=$noLaunchEntryCount excluded=$excludedCount"
        )
        return selectedRecommendations
    }

    private fun loadSourceRules(context: Context, source: RecommendationSource): List<RecommendationRule> {
        val loaded = loadRules(context, source.priorityListResId)
        if (loaded.isNotEmpty() || source.platform != "MAME Arcade") return loaded
        return ArcadeGameCatalog.games.map { game ->
            RecommendationRule(id = game.rom, title = game.title, romFileName = "${game.rom}.zip",
                reason = "Classic arcade", acceptedRomSets = listOf(game.rom) + game.clones,
                preferredRomSet = game.rom, overlayAssetName = "${game.rom}.png")
        }
    }

    private fun loadRules(context: Context, @RawRes rawResId: Int?): List<RecommendationRule> {
        if (rawResId == null) return emptyList()
        synchronized(cachedRules) {
            cachedRules[rawResId]?.let { return it }
        }
        val rawText = context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
        val trimmed = rawText.trim()
        val loaded = if (trimmed.isBlank()) {
            emptyList()
        } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            loadRulesFromJson(trimmed)
        } else {
            loadRulesFromCsv(trimmed)
        }
        synchronized(cachedRules) {
            cachedRules[rawResId] = loaded
        }
        return loaded
    }
}

private fun arcadeOverlayDedupeKey(assetName: String): String =
    assetName.substringBeforeLast('.', assetName).lowercase()

private fun resolveRecommendationImportedArtworkUri(
    context: Context,
    recommendationId: String
): String? {
    val importedIconStore = ImportedIconStore(context)
    val cacheFileName = "${recommendationArtworkFileName(recommendationId)}-cover"
    return importedIconStore.existingBitmapUri(cacheFileName)
}

private fun resolveWildcardRecommendationArtworkUri(
    context: Context,
    recommendationId: String,
    launchEntry: GameEntry,
    title: String
): String? {
    resolveRecommendationImportedArtworkUri(
        context = context,
        recommendationId = recommendationId
    )?.let { return it }

    val importedIconStore = ImportedIconStore(context)
    val cacheFileName = "${recommendationArtworkFileName(recommendationId)}-cover"
    val downloadedUri = launchEntry.artworkSearchQueries(title)
        .firstNotNullOfOrNull { query ->
            importedIconStore.downloadFirstAvailableImage(
                fileName = cacheFileName,
                imageUrls = ArtworkSuggestionService.fetchCandidates(launchEntry, query).map(ArtworkCandidate::imageUrl)
            )
        }

    return downloadedUri
}

private fun recommendationArtworkSearchQuery(
    game: GameEntry,
    fallbackTitle: String
): String = game.artworkSearchQuery(fallbackTitle)

private fun buildRetroArchMameRecommendation(
    context: Context,
    id: String,
    rule: RecommendationRule,
    rom: AvailableRom
): GameEntry? {
    val packageName = preferredRetroArchPackage(context) ?: return null
    return GameEntry(
        id = id,
        title = rule.title,
        subtitle = "RetroArch • MAME Arcade",
        platform = "MAME Arcade",
        sourceType = GameSourceType.CUSTOM_INTENT,
        packageName = packageName,
        activityName = "com.retroarch.browser.retroactivity.RetroActivityFuture",
        intentAction = Intent.ACTION_MAIN,
        intentDataUri = rom.uriString,
        extrasJson = """{"ROM":"{filePath}","LIBRETRO":"/data/data/$packageName/cores/mame2003_plus_libretro_android.so"}"""
    )
}

private fun buildRetroArchRecommendation(
    context: Context,
    id: String,
    title: String,
    platform: String,
    rom: AvailableRom,
    coreSoFile: String
): GameEntry? {
    val packageName = preferredRetroArchPackage(context) ?: return null
    return GameEntry(
        id = id,
        title = title,
        subtitle = "RetroArch • $platform",
        platform = platform,
        sourceType = GameSourceType.CUSTOM_INTENT,
        packageName = packageName,
        activityName = "com.retroarch.browser.retroactivity.RetroActivityFuture",
        intentAction = Intent.ACTION_MAIN,
        intentDataUri = rom.uriString,
        extrasJson = """{"ROM":"{filePath}","LIBRETRO":"/data/data/$packageName/cores/$coreSoFile"}"""
    )
}

private fun buildRecommendationLaunchEntry(
    context: Context,
    id: String,
    title: String,
    platform: String,
    rom: AvailableRom,
    templateGame: GameEntry?,
    preferredTemplatePackages: List<String>,
    retroArchCoreSoFile: String?
): GameEntry? {
    templateGame
        ?.toRecommendationLaunchEntry(
            id = id,
            title = title,
            platform = platform,
            romUri = rom.uriString
        )
        ?.let { return it }

    preferredInstalledTemplate(context, preferredTemplatePackages)
        ?.toRecommendationLaunchEntry(
            id = id,
            title = title,
            platform = platform,
            romUri = rom.uriString
        )
        ?.let { return it }

    return retroArchCoreSoFile?.let { coreSoFile ->
        buildRetroArchRecommendation(
            context = context,
            id = id,
            title = title,
            platform = platform,
            rom = rom,
            coreSoFile = coreSoFile
        )
    }
}

private fun preferredInstalledTemplate(
    context: Context,
    packageNames: List<String>
): CustomIntentTemplate? {
    if (packageNames.isEmpty()) return null
    return packageNames.firstNotNullOfOrNull { packageName ->
        CustomIntentTemplates.all.firstOrNull { template ->
            template.packageName == packageName && isPackageInstalled(context, packageName)
        }
    }
}

private fun buildCitraRecommendation(
    context: Context,
    id: String,
    title: String,
    rom: AvailableRom
): GameEntry? {
    val packageName = "org.citra.emu"
    if (!isPackageInstalled(context, packageName)) return null
    return GameEntry(
        id = id,
        title = title,
        subtitle = "Citra • Nintendo 3DS",
        platform = "Nintendo 3DS",
        sourceType = GameSourceType.CUSTOM_INTENT,
        packageName = packageName,
        activityName = "org.citra.emu.ui.EmulationActivity",
        intentAction = Intent.ACTION_VIEW,
        intentDataUri = rom.uriString,
        extrasJson = """{"GamePath":"{filePath}"}"""
    )
}

private fun buildDolphinRecommendation(
    context: Context,
    id: String,
    title: String,
    platform: String,
    rom: AvailableRom
): GameEntry? {
    val packageName = preferredDolphinPackage(context) ?: return null
    val intentAction = if (packageName == "org.dolphinemu.mmjr") {
        Intent.ACTION_VIEW
    } else {
        Intent.ACTION_MAIN
    }
    return GameEntry(
        id = id,
        title = title,
        subtitle = "Dolphin • $platform",
        platform = platform,
        sourceType = GameSourceType.CUSTOM_INTENT,
        packageName = packageName,
        activityName = "org.dolphinemu.dolphinemu.ui.main.MainActivity",
        intentAction = intentAction,
        intentDataUri = rom.uriString,
        extrasJson = """{"AutoStartFile":"{filePath}"}"""
    )
}

private fun CustomIntentTemplate.toRecommendationLaunchEntry(
    id: String,
    title: String,
    platform: String,
    romUri: String
): GameEntry = GameEntry(
    id = id,
    title = title,
    subtitle = defaultSubtitle(platform),
    platform = platform,
    sourceType = GameSourceType.CUSTOM_INTENT,
    packageName = packageName.ifBlank { null },
    activityName = activityName.ifBlank { null },
    intentAction = action.ifBlank { null },
    intentDataUri = romUri,
    mimeType = mimeType.ifBlank { null },
    categoriesCsv = categoriesCsv.ifBlank { null },
    extrasJson = extrasJson.ifBlank { null }
)

private fun GameEntry.withRecommendationArtworkUri(uri: String?): GameEntry =
    if (uri.isNullOrBlank() || !imageUri.isNullOrBlank()) {
        this
    } else {
        copy(imageUri = uri)
    }

private fun recommendationTemplateGame(
    visibleGames: List<GameEntry>,
    source: RecommendationSource
): GameEntry? {
    return visibleGames.firstOrNull { game ->
        game.platform.equals(source.platform, ignoreCase = true) && game.recommendationTemplateTarget() != null
    }
}

private fun GameEntry.toRecommendationLaunchEntry(
    id: String,
    title: String,
    platform: String,
    romUri: String
): GameEntry? {
    val target = recommendationTemplateTarget() ?: return null
    return GameEntry(
        id = id,
        title = title,
        subtitle = subtitle.ifBlank { target.label },
        platform = platform,
        sourceType = target.sourceType,
        packageName = target.packageName,
        activityName = target.activityName,
        shortcutId = target.shortcutId,
        userSerial = target.userSerial,
        intentAction = target.intentAction,
        intentDataUri = romUri,
        mimeType = target.mimeType,
        categoriesCsv = target.categoriesCsv,
        extrasJson = target.extrasJson,
        authorizedTreeUri = authorizedTreeUri
    )
}

private fun GameEntry.recommendationTemplateTarget(): LaunchTarget? {
    val preferred = preferredTarget()
    if (preferred?.sourceType == GameSourceType.CUSTOM_INTENT) {
        return preferred
    }
    return allTargets().firstOrNull { target -> target.sourceType == GameSourceType.CUSTOM_INTENT }
}

private fun recommendationIdFor(
    source: RecommendationSource,
    rule: RecommendationRule,
    rom: AvailableRom
): String =
    "recommendation:${source.key}:${rule.recommendationKey() ?: rom.normalizedStems.firstOrNull().orEmpty()}"

private fun fileBackedRecommendationIdFor(
    source: RecommendationSource,
    rom: AvailableRom
): String =
    "recommendation:${source.key}:file:${availableRomSuggestionKey(rom)}"

private fun bestMatchingRuleForRom(
    rom: AvailableRom,
    rules: List<RecommendationRule>
): RecommendationRule? =
    rules.firstOrNull { rule -> bestMatchingRom(rule, listOf(rom)) != null }

private fun wildcardRecommendationIdFor(
    source: RecommendationSource,
    rom: AvailableRom
): String =
    "recommendation:${source.key}:wildcard:${availableRomSuggestionKey(rom)}"

private fun buildWildcardRecommendations(
    context: Context,
    source: RecommendationSource,
    templateGame: GameEntry?,
    availableRoms: List<AvailableRom>,
    excludedRecommendationIds: Set<String>,
    excludedRomUris: Set<String>,
    limit: Int
): List<GameRecommendation> {
    if (limit <= 0) return emptyList()
    val shuffledCandidates = availableRoms
        .filterNot { rom -> rom.uriString in excludedRomUris }
        .sortedBy { rom -> rom.uriString.lowercase() }
        .shuffled(Random(source.key.hashCode()))
    return buildList {
        for (rom in shuffledCandidates) {
            if (size >= limit) break
            val recommendationId = wildcardRecommendationIdFor(source, rom)
            if (recommendationId in excludedRecommendationIds) continue
            val rule = wildcardRecommendationRuleFor(rom)
            val previewAssetPath = source.buildPreviewAssetPath(context, rule)
            val baseLaunchEntry = source.buildLaunchEntry(context, recommendationId, rule, rom, templateGame)
                ?: continue
            val sourcedArtworkUri = if (previewAssetPath.isNullOrBlank()) {
                resolveWildcardRecommendationArtworkUri(
                    context = context,
                    recommendationId = recommendationId,
                    launchEntry = baseLaunchEntry,
                    title = rule.title
                )
            } else {
                null
            }
            val seededArtworkUri = if (previewAssetPath.isNullOrBlank() && sourcedArtworkUri.isNullOrBlank()) {
                buildRecommendationArtworkUri(
                    context = context,
                    id = recommendationId,
                    platform = source.platform,
                    title = rule.title
                )
            } else {
                null
            }
            val launchEntry = baseLaunchEntry.withRecommendationArtworkUri(sourcedArtworkUri ?: seededArtworkUri)
            add(
                GameRecommendation(
                    displayEntry = launchEntry.copy(subtitle = WILDCARD_RECOMMENDATION_REASON),
                    launchEntry = launchEntry,
                    reason = WILDCARD_RECOMMENDATION_REASON,
                    previewAssetPath = previewAssetPath,
                    isCurated = false
                )
            )
        }
    }
}

private fun availableRomSuggestionKey(rom: AvailableRom): String {
    val nameKey = rom.displayName.systemId().ifBlank { "rom" }
    val uriKey = rom.uriString.hashCode().toUInt().toString(16)
    return "$nameKey-$uriKey"
}

private fun wildcardRecommendationRuleFor(rom: AvailableRom): RecommendationRule {
    val romFileName = rom.displayName.trim()
    val romStem = romFileName.substringBeforeLast('.', romFileName).trim()
    return RecommendationRule(
        id = romStem.ifBlank { null },
        title = wildcardRecommendationTitleFor(romFileName),
        romFileName = romFileName,
        reason = WILDCARD_RECOMMENDATION_REASON,
        acceptedRomSets = listOfNotNull(romStem.takeIf { it.isNotBlank() }),
        acceptedFileNames = listOf(romFileName),
        preferredRomSet = romStem.takeIf { it.isNotBlank() }
    )
}

private fun wildcardRecommendationTitleFor(displayName: String): String {
    return normalizedArtworkTitle(displayName, stripFileExtension = true)
        ?: displayName.substringBeforeLast('.', displayName).trim().ifBlank { displayName }
}

private fun availableRomsForSource(
    context: Context,
    authorizedRomTreeUri: String?,
    source: RecommendationSource,
    forceRefresh: Boolean = false,
    cancellationCheck: () -> Unit = {}
): List<AvailableRom> {
    cancellationCheck()
    val cacheKey = listOf(
        authorizedRomTreeUri?.trim().orEmpty(),
        source.key
    ).joinToString(separator = "|")
    if (!forceRefresh) cachedAvailableRoms.get(cacheKey)?.let { return it }
    val directory = resolvePreferredRomFolder(
        context = context,
        authorizedRomTreeUri = authorizedRomTreeUri,
        subfolderHint = source.romFolderHint,
        platform = source.platform,
        cancellationCheck = cancellationCheck
    ) ?: return emptyList()
    // Listing failures propagate to the scan UI and must never be cached as an empty folder.
    val allFiles = listRomDocuments(context, directory, cancellationCheck)
    val candidateEntries = allFiles.filter { file ->
        if (source.includeDirectories) file.isDirectory else !file.isDirectory
    }
    val resolvedRoms = candidateEntries.mapNotNull { file ->
        cancellationCheck()
        val displayName = file.name
        if (!file.isDirectory && displayName.hasIgnoredRecommendationExtension()) return@mapNotNull null
        val normalizedStems = normalizeLooseStemCandidates(displayName)
        if (normalizedStems.isEmpty()) null else AvailableRom(
            displayName = displayName,
            uriString = file.uri.toString(),
            normalizedStems = normalizedStems
        )
    }
    Log.d(
        "RetroLauncher",
        "ROM scan folderHint=${source.romFolderHint} all=${allFiles.size} candidates=${candidateEntries.size} resolved=${resolvedRoms.size}"
    )
    cachedAvailableRoms.put(cacheKey, resolvedRoms)
    return resolvedRoms
}

private fun sourceHasAvailableRoms(
    context: Context,
    authorizedRomTreeUri: String?,
    source: RecommendationSource
): Boolean {
    val spec = explicitPlatformScanSpec(source.platform, "com.retroarch")
    return runCatching { availableRomsForSource(context, authorizedRomTreeUri, source) }
        .onFailure { Log.w("RetroLauncher", "ROM availability check failed for ${source.platform}", it) }
        .getOrDefault(emptyList())
        .any { rom ->
            when {
                spec != null -> acceptsPlatformRom(context, rom, spec)
                source.platform == "MAME Arcade" -> romFileExtension(rom.displayName) in setOf("zip", "7z", "chd")
                else -> true
            }
        }
}

private fun String.hasIgnoredRecommendationExtension(): Boolean =
    substringAfterLast('.', "")
        .lowercase()
        .let(ignoredRecommendationExtensions::contains)

private fun gamesMatchingFilter(games: List<GameEntry>, filter: String): List<GameEntry> {
    val platformFilter = GamePlatforms.filterLabel(filter)
    return if (platformFilter != null) {
        games.filter { game -> GamePlatforms.matchesFilter(game.platform, platformFilter) }
    } else {
        games.filter { game -> game.genre.equals(filter, ignoreCase = true) }
    }
}

private fun gameMatchesRecommendation(context: Context, game: GameEntry, rule: RecommendationRule): Boolean {
    if (rule.normalizedTitleCandidates().any { normalizedTitle ->
            titlesLooselyMatch(game.title, normalizedTitle)
        }) {
        return true
    }
    val currentRomStems = gameRomStems(context, game)
    if (currentRomStems.isEmpty()) return false
    return currentRomStems.any { currentRomStem ->
        rule.orderedRomMatchStems().any { desiredStem ->
            exactRomStemMatch(currentRomStem, desiredStem)
        }
    }
}

private fun bestMatchingRom(rule: RecommendationRule, availableRoms: List<AvailableRom>): AvailableRom? {
    val desiredStems = rule.orderedRomMatchStems()
    if (desiredStems.isEmpty()) return null
    return availableRoms
        .mapNotNull { rom ->
            rom.normalizedStems.flatMap { candidateStem ->
                desiredStems.mapIndexedNotNull { index, desiredStem ->
                    looseRomMatchScore(candidateStem, desiredStem)?.let { score ->
                        RomMatchCandidate(
                            rom = rom,
                            score = score,
                            desiredPriority = index,
                            lengthDelta = abs(candidateStem.length - desiredStem.length)
                        )
                    }
                }
            }.maxWithOrNull(
                compareByDescending<RomMatchCandidate> { it.score }
                    .thenBy { it.desiredPriority }
                    .thenBy { it.lengthDelta }
            )
        }
        .sortedWith(
            compareByDescending<RomMatchCandidate> { it.score }
                .thenBy { it.desiredPriority }
                .thenBy { it.lengthDelta }
                .thenBy { it.rom.displayName.length }
        )
        .firstOrNull()
        ?.rom
}

private fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0
    while (index < line.length) {
        val currentChar = line[index]
        when {
            currentChar == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index += 1
            }
            currentChar == '"' -> inQuotes = !inQuotes
            currentChar == ',' && !inQuotes -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(currentChar)
        }
        index += 1
    }
    values += current.toString()
    return values
}

private fun loadRulesFromCsv(rawText: String): List<RecommendationRule> {
    val rows = rawText.lineSequence()
        .map(String::trim)
        .filter { it.isNotBlank() }
        .toList()
    if (rows.isEmpty()) return emptyList()

    val headerCells = parseCsvLine(rows.first())
    val csvColumns = recommendationCsvColumns(headerCells) ?: RecommendationCsvColumns(
        titleIndex = 0,
        romIndex = 1,
        reasonIndex = 2
    )
    val dataRows = if (recommendationCsvColumns(headerCells) != null) rows.drop(1) else rows
    return dataRows.mapNotNull { line ->
        val cells = parseCsvLine(line)
        val title = cells.getOrNull(csvColumns.titleIndex)?.trim().orEmpty()
        val romFileName = cells.getOrNull(csvColumns.romIndex)?.trim().orEmpty()
        val reason = csvColumns.reasonIndex
            ?.let { index -> cells.getOrNull(index)?.trim().orEmpty() }
            .orEmpty()
        RecommendationRule(
            title = title,
            romFileName = romFileName,
            reason = reason
        ).takeIf { rule ->
            rule.title.isNotBlank() && rule.romFileName.isNotBlank()
        }
    }
}

private fun loadRulesFromJson(rawText: String): List<RecommendationRule> {
    val rootEntries = when {
        rawText.startsWith("[") -> JSONArray(rawText)
        else -> JSONObject(rawText).optJSONArray("entries") ?: JSONArray()
    }
    return buildList {
        repeat(rootEntries.length()) { index ->
            rootEntries.optJSONObject(index)
                ?.toRecommendationRule()
                ?.let { rule -> add(index to rule) }
        }
    }.sortedWith(
        compareByDescending<Pair<Int, RecommendationRule>> { it.second.priority ?: Int.MIN_VALUE }
            .thenBy { it.first }
    ).map { it.second }
}

private fun JSONObject.toRecommendationRule(): RecommendationRule? {
    val match = optJSONObject("match")
    val launch = optJSONObject("launch")
    val artwork = optJSONObject("artwork")

    val acceptedRomSets = buildList {
        addAll(match.optStringList("accepted_rom_sets"))
        optString("rom_set").ifBlank { null }?.let(::add)
    }.distinctBy { it.lowercase() }
    val acceptedFileNames = buildList {
        addAll(match.optStringList("accepted_file_names"))
        optString("rom_name").ifBlank { null }?.let(::add)
    }.distinctBy { it.lowercase() }
    val preferredRomSet = launch.optNonBlankString("preferred_rom_set") ?: optString("rom_set").ifBlank {
        acceptedRomSets.firstOrNull()
    }
    val romFileName = acceptedFileNames.firstOrNull()
        ?: preferredRomSet?.let { "$it.zip" }
        ?: acceptedRomSets.firstOrNull()?.let { "$it.zip" }
        ?: optString("rom_name").ifBlank { null }
        ?: optString("rom_set").ifBlank { null }?.let { "$it.zip" }
        ?: return null
    val title = optString("title").trim()
    if (title.isBlank()) return null

    return RecommendationRule(
        id = optString("id").ifBlank { null },
        title = title,
        romFileName = romFileName,
        reason = optString("description").ifBlank {
            optString("reason")
        },
        titleAliases = buildList {
            addAll(match.optStringList("title_aliases"))
            addAll(optStringList("aliases"))
        }.distinctBy { it.lowercase() },
        acceptedRomSets = acceptedRomSets,
        acceptedFileNames = acceptedFileNames,
        preferredRomSet = preferredRomSet,
        overlayAssetName = artwork.optNonBlankString("overlay_asset") ?: optString("overlay_asset").ifBlank { null },
        priority = optIntOrNull("priority")
    )
}

private fun JSONObject?.optStringList(key: String): List<String> =
    this?.optJSONArray(key).toTrimmedStringList()

private fun JSONArray?.toTrimmedStringList(): List<String> =
    if (this == null) {
        emptyList()
    } else {
        buildList {
            repeat(length()) { index ->
                optString(index)
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.let(::add)
            }
        }
    }

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) {
        optInt(key)
    } else {
        null
    }

private fun JSONObject?.optNonBlankString(key: String): String? =
    this?.optString(key)?.trim()?.takeIf(String::isNotBlank)

private fun buildRecommendationArtworkUri(
    context: Context,
    id: String,
    platform: String,
    title: String
): String? {
    val importedIconStore = ImportedIconStore(context)
    val cacheFileName = "${recommendationArtworkFileName(id)}-art"
    importedIconStore.existingBitmapUri(cacheFileName)?.let { return it }
    val bitmap = buildRecommendationArtworkBitmap(context, platform, title) ?: return null
    return importedIconStore.saveBitmap(cacheFileName, bitmap)
}

private fun recommendationArtworkFileName(id: String): String =
    id.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "recommendation-art" }

private fun buildRecommendationArtworkBitmap(
    context: Context,
    platform: String,
    title: String
): Bitmap? {
    val width = 1024
    val height = 1024
    val palette = recommendationArtworkPalette(platform)
    return runCatching {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                palette.topColor,
                palette.bottomColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val frameRect = RectF(
            width * 0.07f,
            height * 0.07f,
            width * 0.93f,
            height * 0.93f
        )
        canvas.drawRoundRect(
            frameRect,
            46f,
            46f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(34, 255, 255, 255)
            }
        )
        canvas.drawRoundRect(
            RectF(
                width * 0.11f,
                height * 0.11f,
                width * 0.89f,
                height * 0.57f
            ),
            36f,
            36f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(22, 255, 255, 255)
            }
        )

        drawRecommendationPlatformLogo(canvas, context, platform, width, height)

        val titleWidth = (width * 0.78f).toInt().coerceAtLeast(1)
        val titleLayout = StaticLayout.Builder
            .obtain(title.trim(), 0, title.trim().length, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 88f
                isFakeBoldText = true
                setShadowLayer(22f, 0f, 8f, android.graphics.Color.argb(92, 0, 0, 0))
            }, titleWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(3)
            .build()
        canvas.save()
        canvas.translate((width - titleWidth) / 2f, height * 0.61f)
        titleLayout.draw(canvas)
        canvas.restore()

        val platformLabel = platform.uppercase()
        val platformPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(214, 255, 255, 255)
            textSize = 34f
            letterSpacing = 0.08f
        }
        val platformWidth = platformPaint.measureText(platformLabel)
        canvas.drawText(
            platformLabel,
            ((width - platformWidth) / 2f).coerceAtLeast(0f),
            height * 0.90f,
            platformPaint
        )

        canvas.drawRoundRect(
            RectF(width * 0.18f, height * 0.86f, width * 0.82f, height * 0.865f),
            999f,
            999f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentColor
            }
        )
        bitmap
    }.getOrNull()
}

private fun drawRecommendationPlatformLogo(
    canvas: Canvas,
    context: Context,
    platform: String,
    width: Int,
    height: Int
) {
    val svg = PlatformSvgIcons.svgForPlatform(context.resources, platform)
    if (svg.isNullOrBlank()) {
        val fallbackText = platform.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        if (fallbackText.isBlank()) return
        val fallbackPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(228, 255, 255, 255)
            textSize = 260f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fallbackText, width / 2f, height * 0.38f, fallbackPaint)
        return
    }

    val picture = SVG.getFromString(svg).renderToPicture()
    val sourceWidth = picture.width.takeIf { it > 0 }?.toFloat() ?: width.toFloat()
    val sourceHeight = picture.height.takeIf { it > 0 }?.toFloat() ?: height.toFloat()
    val maxWidth = width * 0.62f
    val maxHeight = height * 0.30f
    val scale = minOf(maxWidth / sourceWidth, maxHeight / sourceHeight)
    val drawWidth = sourceWidth * scale
    val drawHeight = sourceHeight * scale
    val offsetX = (width - drawWidth) / 2f
    val offsetY = (height * 0.17f) + ((maxHeight - drawHeight) / 2f)

    canvas.save()
    canvas.translate(offsetX, offsetY)
    canvas.scale(scale, scale)
    canvas.drawPicture(picture)
    canvas.restore()
}

private data class RecommendationArtworkPalette(
    val topColor: Int,
    val bottomColor: Int,
    val accentColor: Int
)

private fun recommendationArtworkPalette(platform: String): RecommendationArtworkPalette {
    val palettes = listOf(
        RecommendationArtworkPalette(
            topColor = android.graphics.Color.parseColor("#102030"),
            bottomColor = android.graphics.Color.parseColor("#1F5F8B"),
            accentColor = android.graphics.Color.parseColor("#F9C74F")
        ),
        RecommendationArtworkPalette(
            topColor = android.graphics.Color.parseColor("#251431"),
            bottomColor = android.graphics.Color.parseColor("#7C3AED"),
            accentColor = android.graphics.Color.parseColor("#F97316")
        ),
        RecommendationArtworkPalette(
            topColor = android.graphics.Color.parseColor("#132A13"),
            bottomColor = android.graphics.Color.parseColor("#2D6A4F"),
            accentColor = android.graphics.Color.parseColor("#FFD166")
        ),
        RecommendationArtworkPalette(
            topColor = android.graphics.Color.parseColor("#2B1D0E"),
            bottomColor = android.graphics.Color.parseColor("#A44A1F"),
            accentColor = android.graphics.Color.parseColor("#F4D35E")
        ),
        RecommendationArtworkPalette(
            topColor = android.graphics.Color.parseColor("#1A1B41"),
            bottomColor = android.graphics.Color.parseColor("#4C5FD5"),
            accentColor = android.graphics.Color.parseColor("#72EFDD")
        )
    )
    val index = ((platform.lowercase().hashCode() and Int.MAX_VALUE) % palettes.size)
    return palettes[index]
}

private fun recommendationCsvColumns(headerCells: List<String>): RecommendationCsvColumns? {
    if (headerCells.isEmpty()) return null
    val normalizedHeaders = headerCells.map(::normalizeRecommendationHeader)
    val titleIndex = normalizedHeaders.indexOfFirst { header ->
        header == "title" || header.contains("gametitle")
    }
    val romIndex = normalizedHeaders.indexOfFirst { header ->
        header.contains("romfilename") || header == "filename"
    }
    if (titleIndex == -1 || romIndex == -1) return null
    val reasonIndex = normalizedHeaders.indexOfFirst { header ->
        header == "reason" || header.contains("mustplay") || header.contains("why")
    }.takeIf { it != -1 }
    return RecommendationCsvColumns(
        titleIndex = titleIndex,
        romIndex = romIndex,
        reasonIndex = reasonIndex
    )
}

private fun normalizeRecommendationHeader(raw: String?): String =
    raw
        ?.trim()
        ?.lowercase()
        ?.replace(Regex("[^a-z0-9]+"), "")
        .orEmpty()

private fun preferredRetroArchPackage(context: Context): String? =
    listOf("com.retroarch.aarch64", "com.retroarch").firstOrNull { packageName ->
        isPackageInstalled(context, packageName)
    }

private fun preferredDolphinPackage(context: Context): String? =
    listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr").firstOrNull { packageName ->
        isPackageInstalled(context, packageName)
    }

private fun isPackageInstalled(context: Context, packageName: String): Boolean = runCatching {
    val pm = context.packageManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0)
    }
}.isSuccess

private fun gameRomStems(context: Context, game: GameEntry): List<String> {
    val intentCandidates = sequenceOf(
        game.preferredTarget()?.intentDataUri,
        game.intentDataUri
    ) + game.allTargets().asSequence().map { it.intentDataUri }
    return intentCandidates
        .mapNotNull { uriString -> extractDisplayName(context, uriString) ?: uriString?.substringAfterLast('/') }
        .flatMap(::normalizeLooseStemCandidates)
        .distinct()
        .toList()
}

private fun gameRomUriCandidates(game: GameEntry): List<String> =
    (sequenceOf(
        game.preferredTarget()?.intentDataUri,
        game.intentDataUri
    ) + game.allTargets().asSequence().map { it.intentDataUri })
        .mapNotNull { uriString -> uriString?.trim()?.takeIf { it.isNotBlank() } }
        .distinct()
        .toList()

private fun extractDisplayName(context: Context, uriString: String?): String? {
    val parsed = uriString?.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    if (parsed.scheme != "content") {
        return parsed.lastPathSegment?.substringAfterLast('/')
    }
    val queried = runCatching {
        context.contentResolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
    }.getOrNull()
    return queried ?: parsed.lastPathSegment?.substringAfterLast('/')
}

private data class RomMatchCandidate(
    val rom: AvailableRom,
    val score: Int,
    val desiredPriority: Int,
    val lengthDelta: Int
)

private fun RecommendationRule.normalizedTitleCandidates(): List<String> =
    buildList {
        add(title)
        addAll(titleAliases)
    }.flatMap(::normalizeLooseKeyCandidates).distinct()

private fun RecommendationRule.orderedRomMatchStems(): List<String> =
    buildList {
        preferredRomSet?.let(::add)
        addAll(acceptedRomSets)
        addAll(acceptedFileNames)
        add(romFileName)
        id?.let(::add)
    }.flatMap(::normalizeLooseStemCandidates).distinct()

private fun RecommendationRule.previewAssetBaseNames(): List<String> =
    buildList {
        overlayAssetName?.let(::add)
        add(title)
        addAll(titleAliases)
        preferredRomSet?.let(::add)
        addAll(acceptedRomSets)
        addAll(acceptedFileNames.map { it.substringBeforeLast('.', it) })
        add(romFileName.substringBeforeLast('.', romFileName))
        id?.let(::add)
    }.map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

private fun RecommendationRule.recommendationKey(): String? =
    listOfNotNull(
        id,
        preferredRomSet,
        acceptedRomSets.firstOrNull(),
        romFileName.substringBeforeLast('.', romFileName),
        normalizeLooseKey(title)
    ).firstNotNullOfOrNull { candidate ->
        candidate.takeIf { it.isNotBlank() }?.systemId()
    }

private fun titlesLooselyMatch(rawTitle: String?, normalizedExpectedTitle: String): Boolean {
    return normalizeLooseKeyCandidates(rawTitle).any { normalizedTitle ->
        normalizedTitle == normalizedExpectedTitle
    }
}

private fun namesLooselyMatch(rawName: String?, normalizedExpectedName: String): Boolean {
    val normalizedName = normalizeLooseKey(rawName) ?: return false
    return normalizedName == normalizedExpectedName ||
        normalizedName.contains(normalizedExpectedName) ||
        normalizedExpectedName.contains(normalizedName)
}

private fun exactRomStemMatch(candidateStem: String, desiredStem: String): Boolean =
    candidateStem == desiredStem

private fun looseRomMatchScore(candidateStem: String, desiredStem: String): Int? {
    if (candidateStem == desiredStem) return 400
    if (candidateStem.startsWith(desiredStem)) return 300 - (candidateStem.length - desiredStem.length).coerceAtLeast(0)
    if (desiredStem.startsWith(candidateStem) && candidateStem.length >= 4) {
        return 260 - (desiredStem.length - candidateStem.length).coerceAtLeast(0)
    }
    if (desiredStem.length >= 4 && candidateStem.contains(desiredStem)) {
        return 220 - (candidateStem.length - desiredStem.length).coerceAtLeast(0)
    }
    return null
}

private fun normalizeLooseStem(rawName: String?): String? =
    normalizeLooseStemCandidates(rawName).firstOrNull()

private fun normalizeLooseStemCandidates(rawName: String?): List<String> {
    val stem = rawName?.substringBeforeLast('.', rawName ?: "") ?: return emptyList()
    return normalizeLooseKeyCandidates(stem, stripGroupedMetadata = true)
}

private fun normalizeLooseKey(raw: String?): String? =
    normalizeLooseKeyCandidates(raw).firstOrNull()

private fun normalizeLooseKeyCandidates(
    raw: String?,
    stripGroupedMetadata: Boolean = false
): List<String> =
    raw
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { value ->
            buildList {
                add(value)
                if (stripGroupedMetadata) {
                    val stripped = value
                        .replace(Regex("\\([^)]*\\)"), " ")
                        .replace(Regex("\\[[^]]*]"), " ")
                        .replace(Regex("""\{[^}]*\}"""), " ")
                        .trim()
                    if (stripped.isNotBlank()) {
                        add(stripped)
                    }
                }
            }
        }
        ?.flatMap { candidate ->
            val tokens = candidate
                .lowercase()
                .replace("&", " and ")
                .split(Regex("[^a-z0-9]+"))
                .filter { it.isNotBlank() }
            val filteredJoin = tokens.filterNot(::isLooseArticleToken)
                .joinToString(separator = "")
                .takeIf { it.isNotBlank() }
            val rawJoin = tokens.joinToString(separator = "")
                .takeIf { it.isNotBlank() }
            buildList {
                filteredJoin?.let(::add)
                rawJoin?.let(::add)
            }
        }
        ?.distinct()
        .orEmpty()

private fun isLooseArticleToken(token: String): Boolean =
    token == "the" || token == "a" || token == "an"

private fun String.systemId(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
