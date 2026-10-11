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
    val entries: List<GameEntry>, val discovered: Int, val skipped: Int = 0, val notice: String? = null,
    val deferred: Boolean = false
)

/** Sequential local discovery only; no artwork/network work before the import commits. */
internal object LibraryScanner {
    val platforms: List<String> = GamePlatforms.values.filter {
        GameRecommendations.supportsPlatformScan(it) || preferredRomFolderHintForPlatform(it) != null
    }

    suspend fun scan(
        context: Context, root: String, platform: String, installedPackages: Set<String>, games: List<GameEntry>,
        scannedArcadeFolders: MutableSet<String>,
        chooseEmulator: suspend (LibraryEmulatorChoice) -> GameSystemEmulatorOption?,
        linkScummVm: suspend () -> List<GameEntry> = { emptyList() },
        onReadingGame: (String) -> Unit = {}
    ): PlatformLibraryBatch {
        val job = currentCoroutineContext()
        val check = RomScanCancellationCheck(job)
        if (platform == "ScummVM") {
            if (ScummVmGames.PACKAGE !in installedPackages) return PlatformLibraryBatch(emptyList(), 0)
            val candidates = GameRecommendations.scanImportableScummVmFolders(
                context, root, emptyList(), check, allowUnrecognizedRootFallback = false)
            val linked = candidates.filter { !it.registeredTarget.isNullOrBlank() }
            // Android may redact shortcut intents. Keep their exact opaque IDs/profile,
            // rather than inventing a registered target from their titles or folder names.
            val shared = com.ariy.retrolauncher.launch.InstalledTargets(context.applicationContext as android.app.Application)
                .loadShortcuts(ScummVmGames.PACKAGE).map { shortcut ->
                    GameEntry(title = shortcut.title, subtitle = "ScummVM", platform = platform,
                        sourceType = GameSourceType.SHORTCUT, packageName = shortcut.packageName,
                        shortcutId = shortcut.shortcutId, userSerial = shortcut.userSerial)
                }
            val templates = CustomIntentTemplates.all
            val choices = LibrarySetupPolicy.standaloneChoices(buildGameSystemEmulatorOptions(templates),
                templates, setOf(platform), installedPackages)
            val preferred = games.firstOrNull { it.platform == platform && !it.needsLaunchSetup }?.packageName
            val option = LibrarySetupPolicy.chooseStandalone(choices, templates, preferred) {
                chooseEmulator(LibraryEmulatorChoice("ScummVM", choices))
            } ?: return PlatformLibraryBatch(emptyList(), candidates.size, candidates.size,
                "ScummVM skipped — no emulator selected.", deferred = true)
            val draft = resolveEmulatorSelectionDraft(templates, option) ?: return PlatformLibraryBatch(emptyList(), 0)
            val entries = linked.map { candidate ->
                check()
                onReadingGame(candidate.title)
                GameEntry(title = candidate.title, subtitle = draft.subtitle, platform = platform,
                    sourceType = GameSourceType.CUSTOM_INTENT, packageName = draft.packageName,
                    activityName = draft.activityName.ifBlank { null }, intentAction = draft.action,
                    intentDataUri = candidate.folderUri, authorizedTreeUri = root,
                    extrasJson = ScummVmGames.extrasForTarget(requireNotNull(candidate.registeredTarget), draft.extras))
            } + shared
            // First setup opens ScummVM's own registered-game interface automatically.
            // Its private full list is not readable; an exported config is the bulk path.
            val selected = if (LibrarySetupPolicy.needsScummVmLink(entries.size, games)) linkScummVm() else emptyList()
            val unlinked = candidates.size - linked.size
            return PlatformLibraryBatch(entries + selected, candidates.size + shared.size + selected.size,
                unlinked, if (unlinked > 0 || (entries + selected).isEmpty())
                    "ScummVM folders need registered targets. Use the ScummVM game picker or import its exported config in setup; selecting one game does not expose the private full library." else null,
                deferred = (entries + selected).isEmpty())
        }
        // Auto-discovery must not classify a sibling platform's .bin/.iso/.zip files
        // as every other platform when the manual picker resolver falls back to root.
        val directory = resolvePreferredRomFolder(context, root, preferredRomFolderHintForPlatform(platform), platform,
            allowUnrecognizedRootFallback = false, cancellationCheck = check)
            ?: return PlatformLibraryBatch(emptyList(), 0)
        val arcade = platform in setOf("MAME Arcade", "Final Burn Neo")
        if (arcade && directory.uri.toString() in scannedArcadeFolders) return PlatformLibraryBatch(emptyList(), 0)
        if (!GameRecommendations.supportsPlatformScan(platform)) {
            val count = listRomDocuments(context, directory, check).count {
                it.isDirectory || it.name.substringAfterLast('.', "").lowercase() !in setOf("txt", "png", "jpg", "webp")
            }
            return PlatformLibraryBatch(emptyList(), count, count, if (count > 0)
                "$platform folder detected, but this platform has no verified bulk ROM scanner. Use Add games to choose an individual supported file / launch contract or an Android-published game shortcut. No games from this folder were imported automatically."
            else null)
        }
        if (listRomDocuments(context, directory, check).isEmpty()) return PlatformLibraryBatch(emptyList(), 0)
        val templates = CustomIntentTemplates.all
        val folderName = android.provider.DocumentsContract.getDocumentId(directory.uri)
            .substringAfterLast('/').substringAfterLast(':')
        val sharedArcade = arcade && folderName.equals("arcade", ignoreCase = true)
        val candidatePlatforms = if (sharedArcade) setOf("MAME Arcade", "Final Burn Neo") else setOf(platform)
        val preferred = games.firstOrNull { it.platform in candidatePlatforms && !it.needsLaunchSetup }
            ?.let { it.preferredTarget() ?: it.primaryTarget() }?.packageName
        val choices = LibrarySetupPolicy.standaloneChoices(
            buildGameSystemEmulatorOptions(templates), templates, candidatePlatforms, installedPackages
        )
        val option = LibrarySetupPolicy.chooseStandalone(choices, templates, preferred) {
            chooseEmulator(LibraryEmulatorChoice(folderName, choices))
        }
        if (option == null) {
            if (arcade) scannedArcadeFolders += directory.uri.toString()
            return PlatformLibraryBatch(emptyList(), 0, notice =
                "$platform skipped — no emulator selected. Use Add games to choose this platform, configure its emulator and import games later.", deferred = true)
        }
        val entryPlatform = option.platform ?: platform
        val draft = resolveEmulatorSelectionDraft(templates, option)
        // RetroArch is explicitly chosen with a named core preset. Private core
        // availability cannot be inspected; setup explains the install requirement.
        if (draft == null) return PlatformLibraryBatch(emptyList(), 0, notice =
            "$platform skipped — emulator preset needs setup. Use Add games to configure it first.", deferred = true)
        fun entry(title: String, uri: String, scummTarget: String? = null): GameEntry {
            check()
            onReadingGame(title)
            val extras = when {
                scummTarget != null -> ScummVmGames.extrasForTarget(scummTarget, draft.extras)
                draft.packageName == "dev.eden.eden_emulator" -> mergeEdenLaunchConfigIntoExtras(
                    draft.extras, inferEdenTitleIdFromUriString(context, uri).orEmpty(), draft.edenResolutionSetup
                )
                else -> draft.extras
            }
            return GameEntry(
                title = title, subtitle = draft.subtitle, platform = entryPlatform,
                sourceType = GameSourceType.CUSTOM_INTENT, packageName = draft.packageName.ifBlank { null },
                activityName = draft.activityName.ifBlank { null }, intentAction = draft.action.ifBlank { null },
                intentDataUri = uri, mimeType = draft.mimeType.ifBlank { null },
                categoriesCsv = draft.categories.ifBlank { null }, extrasJson = extras.ifBlank { null },
                authorizedTreeUri = root, needsLaunchSetup = false
            )
        }
        // Pass no library snapshot to discovery: repeat scans still discover every candidate.
        // Identity-based merge happens against fresh state on the main thread at commit time.
        val batch = when (platform) {
            "MAME Arcade" -> {
                val candidates = GameRecommendations.scanImportableMameRoms(context, root, emptyList(), check)
                PlatformLibraryBatch(candidates.map { entry(it.title, it.romUri) }, candidates.size)
            }
            else -> {
                val candidates = GameRecommendations.scanImportablePlatformRoms(
                    context, root, emptyList(), platform, draft.packageName, option.retroArchSystemId, check
                )
                PlatformLibraryBatch(candidates.map { entry(it.title, it.romUri) }, candidates.size)
            }
        }
        if (arcade) scannedArcadeFolders += directory.uri.toString()
        return batch
    }
}