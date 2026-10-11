package com.ariy.retrolauncher

import android.content.Context
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import com.ariy.retrolauncher.launch.GameRecommendations
import com.ariy.retrolauncher.launch.ScummVmGames
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import kotlinx.coroutines.currentCoroutineContext

internal data class PlatformLibraryBatch(
    val entries: List<GameEntry>, val discovered: Int, val skipped: Int = 0, val notice: String? = null
)

/** Sequential local discovery only; no artwork/network work before the import commits. */
internal object LibraryScanner {
    val platforms: List<String> = GamePlatforms.values.filter {
        GameRecommendations.supportsPlatformScan(it) || preferredRomFolderHintForPlatform(it) != null
    }

    suspend fun scan(
        context: Context, root: String, platform: String, installedPackages: Set<String>, games: List<GameEntry>
    ): PlatformLibraryBatch {
        val job = currentCoroutineContext()
        val check = RomScanCancellationCheck(job)
        // Auto-discovery must not classify a sibling platform's .bin/.iso/.zip files
        // as every other platform when the manual picker resolver falls back to root.
        val directory = resolvePreferredRomFolder(context, root, preferredRomFolderHintForPlatform(platform), platform,
            allowUnrecognizedRootFallback = false, cancellationCheck = check)
            ?: return PlatformLibraryBatch(emptyList(), 0)
        if (!GameRecommendations.supportsPlatformScan(platform)) {
            val count = listRomDocuments(context, directory, check).count {
                it.isDirectory || it.name.substringAfterLast('.', "").lowercase() !in setOf("txt", "png", "jpg", "webp")
            }
            return PlatformLibraryBatch(emptyList(), count, count, if (count > 0)
                "$platform folder detected, but this platform has no verified bulk ROM scanner. Use Add games to choose an individual supported file / launch contract or an Android-published game shortcut. No games from this folder were imported automatically."
            else null)
        }
        val templates = CustomIntentTemplates.all
        val preferred = games.firstOrNull { it.platform == platform && !it.needsLaunchSetup }
            ?.let { it.preferredTarget() ?: it.primaryTarget() }?.packageName
        val option = LibrarySetupPolicy.selectStandalone(
            buildGameSystemEmulatorOptions(templates), templates, platform, installedPackages, preferred
        )
        val draft = resolveEmulatorSelectionDraft(templates, option)
        // Private RetroArch core files cannot be enumerated by another app. Deliberately
        // require the existing manual system/core flow rather than inventing a default.
        val setupNotice = if (draft == null) {
            "$platform: no verified standalone preset available. Imported ROMs need emulator setup; use tile menu → Edit / Launch targets to choose a preset and, for RetroArch, a system/core."
        } else null
        fun entry(title: String, uri: String, scummTarget: String? = null): GameEntry {
            check()
            val extras = when {
                scummTarget != null -> ScummVmGames.extrasForTarget(scummTarget, draft?.extras.orEmpty())
                draft?.packageName == "dev.eden.eden_emulator" -> mergeEdenLaunchConfigIntoExtras(
                    draft.extras, inferEdenTitleIdFromUriString(context, uri).orEmpty(), draft.edenResolutionSetup
                )
                else -> draft?.extras
            }
            return GameEntry(
                title = title, subtitle = draft?.subtitle ?: "Needs emulator setup", platform = platform,
                sourceType = GameSourceType.CUSTOM_INTENT, packageName = draft?.packageName?.ifBlank { null },
                activityName = draft?.activityName?.ifBlank { null }, intentAction = draft?.action?.ifBlank { null },
                intentDataUri = uri, mimeType = draft?.mimeType?.ifBlank { null },
                categoriesCsv = draft?.categories?.ifBlank { null }, extrasJson = extras?.ifBlank { null },
                authorizedTreeUri = root, needsLaunchSetup = draft == null
            )
        }
        // Pass no library snapshot to discovery: repeat scans still discover every candidate.
        // Identity-based merge happens against fresh state on the main thread at commit time.
        return when (platform) {
            "ScummVM" -> {
                val candidates = GameRecommendations.scanImportableScummVmFolders(context, root, emptyList(), check)
                val linked = candidates.filter { !it.registeredTarget.isNullOrBlank() }
                val entries = linked.map { entry(it.title, it.folderUri, it.registeredTarget) }
                val unlinked = candidates.size - linked.size
                PlatformLibraryBatch(entries, candidates.size, unlinked, if (unlinked > 0)
                    "$unlinked ScummVM folders were not linked. Register and authorize games inside ScummVM, then use Add games → ScummVM → Choose registered game or import an exported scummvm.ini. Private folders are not silently registered."
                else setupNotice.takeIf { entries.isNotEmpty() })
            }
            "MAME Arcade" -> {
                val candidates = GameRecommendations.scanImportableMameRoms(context, root, emptyList(), check)
                PlatformLibraryBatch(candidates.map { entry(it.title, it.romUri) }, candidates.size,
                    notice = setupNotice.takeIf { candidates.isNotEmpty() })
            }
            else -> {
                val candidates = GameRecommendations.scanImportablePlatformRoms(
                    context, root, emptyList(), platform, draft?.packageName, option?.retroArchSystemId, check
                )
                PlatformLibraryBatch(candidates.map { entry(it.title, it.romUri) }, candidates.size,
                    notice = setupNotice.takeIf { candidates.isNotEmpty() })
            }
        }
    }
}