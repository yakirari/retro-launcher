package com.ariy.retrolauncher

import android.app.ActivityManager
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
import com.ariy.retrolauncher.data.GameRepository
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import com.ariy.retrolauncher.launch.ArtworkCandidate
import com.ariy.retrolauncher.launch.ArtworkSuggestionService
import com.ariy.retrolauncher.launch.GameRecommendation
import com.ariy.retrolauncher.launch.GameRecommendations
import com.ariy.retrolauncher.launch.TimedCache
import com.ariy.retrolauncher.launch.firstSuccessfulArtworkBatch
import com.ariy.retrolauncher.launch.ImportedIconStore
import com.ariy.retrolauncher.launch.InstalledActivityOption
import com.ariy.retrolauncher.launch.InstalledShortcutOption
import com.ariy.retrolauncher.launch.InstalledTargets
import com.ariy.retrolauncher.launch.ScummVmGames
import com.ariy.retrolauncher.launch.detectScummVmFolder
import com.ariy.retrolauncher.launch.arcadeOverlayAssetBaseNames
import com.ariy.retrolauncher.launch.defaultSubtitle
import com.ariy.retrolauncher.launch.arcadeRomShortNameForArtworkSearch
import com.ariy.retrolauncher.launch.findMatchingArcadeOverlayAssetName
import com.ariy.retrolauncher.launch.isGenericArcadeFallbackAssetName
import com.ariy.retrolauncher.launch.artworkSearchQueries
import com.ariy.retrolauncher.launch.artworkSearchQuery
import com.ariy.retrolauncher.launch.preferredArtworkAspectRatio
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.LaunchTarget
import com.ariy.retrolauncher.model.toJsonString
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val installedTargets = InstalledTargets(application)
    private val importedIconStore = ImportedIconStore(application)
    private val resetGeneration = ResetGeneration()
    private val workerGeneration = ThreadLocal<Long?>()
    private var workerJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    private var workerScope = CoroutineScope(viewModelScope.coroutineContext + workerJob)
    private val _resetting = MutableStateFlow(false)
    val resetting: StateFlow<Boolean> = _resetting.asStateFlow()
    private val _resetNonce = MutableStateFlow(0L)
    val resetNonce: StateFlow<Long> = _resetNonce.asStateFlow()
    private val _resetFailures = MutableStateFlow<List<String>>(emptyList())
    val resetFailures: StateFlow<List<String>> = _resetFailures.asStateFlow()
    fun dismissResetFailures() { _resetFailures.value = emptyList() }

    fun acceptsSession(nonce: Long): Boolean = resetGeneration.accepts(nonce)

    private fun canWrite(): Boolean = resetGeneration.accepts(workerGeneration.get() ?: resetGeneration.current)

    private fun launchWorker(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val generation = workerGeneration.get() ?: resetGeneration.current
        return workerScope.launch(context + workerGeneration.asContextElement(generation)) {
            if (resetGeneration.accepts(generation)) block()
        }
    }

    /** Called only by the explicit confirmation button. No ROM/shared-file deletion. */
    fun resetLauncher() {
        if (!resetGeneration.begin()) return
        _resetFailures.value = emptyList()
        _resetting.value = true
        val retiringWorkers = workerJob
        retiringWorkers.cancel()
        viewModelScope.launch {
            withContext(kotlinx.coroutines.NonCancellable) {
                // Wait for synchronous downloads/icon writes and NonCancellable finally blocks.
                // Deletion happens AFTER draining, so no old private files can be recreated late.
                retiringWorkers.join()
                val failures = withContext(Dispatchers.IO) {
                    com.ariy.retrolauncher.data.LauncherResetStorage(getApplication()).clear()
                }
                _games.value = emptyList()
                _setupCompleted.value = false
                _authorizedRomTreeUri.value = null
                _gridColumns.value = 2
                _genreFilter.value = GameFilters.ALL
                _libraryScan.value = LibraryScanProgress()
                _shortcutOptions.value = emptyList()
                _activityOptions.value = emptyList()
                _artworkSuggestions.value = emptyMap()
                _artworkLoading.value = emptySet()
                _artworkPending.value = emptySet()
                _activeArtworkBackfillPlatforms.value = emptySet()
                synchronized(pendingArtworkAutofillIds) { pendingArtworkAutofillIds.clear() }
                synchronized(activeArtworkBackfillPlatformIds) { activeArtworkBackfillPlatformIds.clear() }
                artworkRetryAfterMillis.clear()
                artworkSuggestionCache.clear()
                GameRecommendations.invalidateRomInventory()
                pendingRetroArchCleanupPackage = null
                installedRefreshRequested = false
                importSharedGamesRequested = false
                installedRefreshJob = null
                hubArtworkJob = null
                libraryScanJob = null
                artworkResumeJob = null
                switchDuplicateReconciliationJob = null
                workerJob = SupervisorJob(viewModelScope.coroutineContext[Job])
                workerScope = CoroutineScope(viewModelScope.coroutineContext + workerJob)
                _resetNonce.value = resetGeneration.current
                _resetFailures.value = failures
                _statusMessage.value = if (failures.isEmpty()) {
                    "Launcher reset. ROMs and installed apps were not deleted. Choose your ROM folder again to import games."
                } else {
                    "Reset incomplete: ${failures.joinToString("; ")}. Some saved data/access may remain; retry Reset launcher. ROMs and installed apps were not deleted."
                }
                resetGeneration.finish()
                _resetting.value = false
                // Discovery is allowed, but welcome must not silently restore old hub games.
                refreshInstalledTargets(importSharedGames = false)
            }
        }
    }

    private val _setupCompleted = MutableStateFlow(repository.loadSetupCompleted())
    val setupCompleted: StateFlow<Boolean> = _setupCompleted.asStateFlow()
    private val _libraryScan = MutableStateFlow(LibraryScanProgress())
    internal val libraryScan: StateFlow<LibraryScanProgress> = _libraryScan.asStateFlow()
    private var libraryScanJob: Job? = null
    private var pendingLibraryEmulator: kotlinx.coroutines.CompletableDeferred<GameSystemEmulatorOption?>? = null
    private var pendingLibraryScummVm: kotlinx.coroutines.CompletableDeferred<List<GameEntry>>? = null

    internal fun completeLibraryScummVm(entries: List<GameEntry>) {
        if (!canWrite()) return
        pendingLibraryScummVm?.complete(entries)
    }

    private suspend fun awaitLibraryScummVm(): List<GameEntry> {
        val answer = kotlinx.coroutines.CompletableDeferred<List<GameEntry>>()
        try {
            withContext(Dispatchers.Main.immediate) {
                pendingLibraryScummVm = answer
                _libraryScan.update { it.copy(stage = LibraryScanStage.LINKING_SCUMMVM, scummVmSetupRequired = true) }
            }
            return answer.await()
        } finally {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main.immediate) {
                if (pendingLibraryScummVm === answer) {
                    pendingLibraryScummVm = null
                    _libraryScan.update { it.copy(stage = LibraryScanStage.SCANNING, scummVmSetupRequired = false) }
                }
            }
        }
    }

    internal fun chooseLibraryEmulator(key: String?) {
        val choice = _libraryScan.value.emulatorChoice ?: return
        val option = if (key == null) null else choice.options.firstOrNull { it.key == key } ?: return
        pendingLibraryEmulator?.complete(option)
    }

    private suspend fun awaitLibraryEmulator(choice: LibraryEmulatorChoice): GameSystemEmulatorOption? {
        val answer = kotlinx.coroutines.CompletableDeferred<GameSystemEmulatorOption?>()
        try {
            withContext(Dispatchers.Main.immediate) {
                pendingLibraryEmulator = answer
                _libraryScan.update { it.copy(stage = LibraryScanStage.CHOOSING_EMULATOR, emulatorChoice = choice) }
            }
            return answer.await()
        } finally {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main.immediate) {
                if (pendingLibraryEmulator === answer) {
                    pendingLibraryEmulator = null
                    _libraryScan.update { it.copy(stage = LibraryScanStage.SCANNING, emulatorChoice = null) }
                }
            }
        }
    }
    private var installedRefreshJob: Job? = null
    @Volatile private var installedRefreshRequested = false
    @Volatile private var importSharedGamesRequested = false
    private var hubArtworkJob: Job? = null

    private val _games = MutableStateFlow(cleanupLoadedGames(repository.loadGames()))
    val games: StateFlow<List<GameEntry>> = _games.asStateFlow()

    private val _gridColumns = MutableStateFlow(repository.loadGridColumns())
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _authorizedRomTreeUri = MutableStateFlow(repository.loadAuthorizedRomTreeUri())
    val authorizedRomTreeUri: StateFlow<String?> = _authorizedRomTreeUri.asStateFlow()

    private val _genreFilter = MutableStateFlow(repository.loadGenreFilter())
    val genreFilter: StateFlow<String> = _genreFilter.asStateFlow()

    private val _shortcutOptions = MutableStateFlow<List<InstalledShortcutOption>>(emptyList())
    val shortcutOptions: StateFlow<List<InstalledShortcutOption>> = _shortcutOptions.asStateFlow()

    private val _activityOptions = MutableStateFlow<List<InstalledActivityOption>>(emptyList())
    val activityOptions: StateFlow<List<InstalledActivityOption>> = _activityOptions.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _artworkSuggestions = MutableStateFlow<Map<String, List<ArtworkCandidate>>>(emptyMap())
    val artworkSuggestions: StateFlow<Map<String, List<ArtworkCandidate>>> = _artworkSuggestions.asStateFlow()
    private val artworkSuggestionCache = TimedCache<String, Pair<String, List<ArtworkCandidate>>>(128, 15 * 60_000L)

    private val _artworkLoading = MutableStateFlow<Set<String>>(emptySet())
    val artworkLoading: StateFlow<Set<String>> = _artworkLoading.asStateFlow()
    private val _artworkPending = MutableStateFlow<Set<String>>(emptySet())
    val artworkPending: StateFlow<Set<String>> = _artworkPending.asStateFlow()
    private val _activeArtworkBackfillPlatforms = MutableStateFlow<Set<String>>(emptySet())
    val activeArtworkBackfillPlatforms: StateFlow<Set<String>> = _activeArtworkBackfillPlatforms.asStateFlow()
    private val pendingArtworkAutofillIds = mutableSetOf<String>()
    private val artworkAutofillSemaphore = Semaphore(BACKGROUND_ARTWORK_AUTOFILL_CONCURRENCY)
    private val activeArtworkBackfillPlatformIds = mutableSetOf<String>()

    private val _launcherResumeNonce = MutableStateFlow(0)
    val launcherResumeNonce: StateFlow<Int> = _launcherResumeNonce.asStateFlow()

    private var pendingRetroArchCleanupPackage: String? = null
    private var switchDuplicateReconciliationJob: Job? = null
    private var artworkResumeJob: Job? = null
    private val artworkRetryAfterMillis = ConcurrentHashMap<String, Long>()

    init {
        restoreAuthorizedRomTreeUriIfNeeded()
        persistCleanedGamesIfNeeded()
        val arcadeDeduped = dedupeArcadeGameEntries(_games.value).map(ScummVmGames::repairTitle)
            .map(com.ariy.retrolauncher.model.PcGameStores::classify)
            .map { com.ariy.retrolauncher.launch.classifyStandaloneActivity(it) }
        if (arcadeDeduped != _games.value) persistGames(arcadeDeduped)
        scheduleSwitchDuplicateReconciliation()
        refreshInstalledTargets(importSharedGames = _setupCompleted.value)
        prefetchMameFallbackArtworkIfNeeded()
    }

    fun refreshInstalledTargets(importSharedGames: Boolean = true) {
        if (!canWrite()) return
        importSharedGamesRequested = importSharedGamesRequested || importSharedGames
        if (installedRefreshJob?.isActive == true) {
            installedRefreshRequested = true
            return
        }
        installedRefreshJob = launchWorker(Dispatchers.IO) {
            do {
            installedRefreshRequested = false
            val shouldImportSharedGames = importSharedGamesRequested
            importSharedGamesRequested = false
            try {
            _isDefaultLauncher.value = installedTargets.isDefaultLauncher()
            val activities = installedTargets.loadActivities()
            _activityOptions.value = activities
            val additions = com.ariy.retrolauncher.launch.installedEmulatorLaunchers(activities, _games.value)
            updateGamesFromBackground { current ->
                val stillMissing = com.ariy.retrolauncher.launch.installedEmulatorLaunchers(activities, current)
                    .map { it.packageName to it.userSerial }.toSet()
                current + additions.filter { (it.packageName to it.userSerial) in stillMissing }
            }
            // Publish launchable tiles before slower icon and shortcut discovery work.
            additions.forEach { entry ->
                currentCoroutineContext().ensureActive()
                val icon = runCatching { installedTargets.importActivityIcon(entry) }.getOrNull()
                if (icon != null) updateGamesFromBackground { current -> current.map {
                    if (it.id == entry.id && it.imageUri.isNullOrBlank()) it.copy(imageUri = icon) else it
                } }
            }
            val shortcuts = installedTargets.loadShortcuts()
            _shortcutOptions.value = shortcuts
            (if (shouldImportSharedGames) LibrarySetupPolicy.missingHubShortcuts(shortcuts, _games.value)
                else emptyList()).forEach { option ->
                currentCoroutineContext().ensureActive()
                try { importShortcut(option, autofill = false) }
                catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    setStatus("Could not import shared shortcut ${option.title}. Return to Home to retry: ${error.message ?: error.javaClass.simpleName}")
                }
            }
            // Optional portrait upgrades begin only after all accessible shared games appear.
            if (hubArtworkJob?.isActive != true) hubArtworkJob = launchWorker(Dispatchers.IO) {
                _games.value.filter { it.platform in com.ariy.retrolauncher.model.PcGameStores.platforms }
                    .forEach { game ->
                        if (game.imageUri.isNullOrBlank()) autoFillArtworkIfMissing(game.id, game.title)
                        else upgradeDigitalPortrait(game)
                    }
            }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                setStatus("Installed target refresh failed. Return to Home to retry: ${error.message ?: error.javaClass.simpleName}")
            }
            } while (installedRefreshRequested)
        }
    }

    fun completeSetup() {
        if (!canWrite()) return
        repository.saveSetupCompleted()
        _setupCompleted.value = true
        refreshInstalledTargets()
    }

    fun cancelLibraryScan() { libraryScanJob?.cancel() }

    fun hasSelectedRomRoot(): Boolean = !repository.loadSelectedRomRoot().isNullOrBlank()

    /** Must be called with the picker result, never a normalized permission ancestor. */
    fun authorizeAndScanLibrary(uri: String) {
        if (!canWrite()) return
        if (libraryScanJob?.isActive == true) {
            setStatus("Cancel the current library scan before choosing another root.")
            return
        }
        repository.saveSelectedRomRoot(uri)
        setAuthorizedRomTreeUri(uri)
        completeSetup()
        scanLibrary()
    }

    fun scanLibrary() {
        if (!canWrite()) return
        if (libraryScanJob?.isActive == true) return
        val root = repository.loadSelectedRomRoot()
        if (root.isNullOrBlank()) {
            setStatus("Choose your ROM root once before scanning. Existing authorization is not used as a broader scan root.")
            return
        }
        _libraryScan.value = LibraryScanProgress(running = true, totalPlatforms = LibraryScanner.platforms.size)
        libraryScanJob = launchWorker(Dispatchers.IO) {
            val artworkIds = linkedSetOf<String>()
            try {
                val app = getApplication<Application>()
                val directory = DocumentFile.fromTreeUri(app, Uri.parse(root))
                    ?: error("The selected ROM root is no longer accessible. Choose it again.")
                // Fail explicitly on revoked grants/provider failure instead of claiming
                // a successful empty scan when platform discovery cannot read the root.
                val scanContext = currentCoroutineContext()
                listRomDocuments(app, directory, RomScanCancellationCheck(scanContext))
                _libraryScan.update { it.copy(stage = LibraryScanStage.FINDING_EMULATORS) }
                // Only current-profile packages can safely be used for custom emulator intents.
                val packages = CustomIntentTemplates.all.map { it.packageName }.filter { packageName ->
                    runCatching { app.packageManager.getApplicationInfo(packageName, 0) }.isSuccess
                }.toSet()
                val scannedArcadeFolders = mutableSetOf<String>()
                for (platform in LibraryScanner.platforms) {
                    currentCoroutineContext().ensureActive()
                    _libraryScan.update { it.copy(platform = platform, stage = LibraryScanStage.SCANNING, currentGame = "") }
                    try {
                        var lastGameUpdate = 0L
                        val batch = LibraryScanner.scan(app, root, platform, packages, _games.value,
                            scannedArcadeFolders, ::awaitLibraryEmulator, ::awaitLibraryScummVm) { title ->
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastGameUpdate >= 100L) {
                                _libraryScan.update { it.copy(currentGame = title) }
                                lastGameUpdate = now
                            }
                        }
                        currentCoroutineContext().ensureActive()
                        _libraryScan.update { it.copy(discovered = it.discovered + batch.discovered,
                            stage = LibraryScanStage.ADDING,
                            skipped = it.skipped + batch.skipped,
                            deferredPlatforms = if (batch.deferred) it.deferredPlatforms + platform else it.deferredPlatforms,
                            messages = (it.messages + listOfNotNull(batch.notice)).takeLast(64)) }
                        for (chunk in batch.entries.chunked(64)) {
                            currentCoroutineContext().ensureActive()
                            val committed = commitImportChunk(chunk)
                            val changed = committed.results.filter { it.outcome != UpsertOutcome.DUPLICATE }
                            artworkIds += changed.map { it.gameId }
                            val unconfiguredIds = committed.games.filter { it.needsLaunchSetup }.map { it.id }.toSet()
                            _libraryScan.update { it.copy(
                                currentGame = chunk.last().title,
                                imported = it.imported + changed.size,
                                skipped = it.skipped + committed.results.size - changed.size,
                                unconfigured = it.unconfigured + committed.results.count { result -> result.gameId in unconfiguredIds }
                            ) }
                            delay(16) // Let a frame/progress update render between durable batches.
                        }
                    } catch (error: CancellationException) { throw error
                    } catch (error: Exception) {
                        _libraryScan.update { it.copy(errors = it.errors + 1,
                            messages = (it.messages + "$platform: ${error.message ?: error.javaClass.simpleName}. Retry Scan library or reselect the folder if access was revoked.").takeLast(64)) }
                    }
                    _libraryScan.update { it.copy(completedPlatforms = it.completedPlatforms + 1) }
                }
                _libraryScan.update { it.finished().copy(messages = it.messages +
                    "Local import complete. Only recognized platform folders and supported formats are detected; unrecognized layouts need Add games or a supported folder name. Artwork is optional and runs afterward; offline games remain imported. Retry missing artwork from Options → Artwork.") }
            } catch (error: CancellationException) {
                _libraryScan.update { it.finished(cancelled = true).copy(messages = it.messages +
                    "Cancelled. Already committed games are kept; Scan library safely retries the rest.") }
                throw error
            } catch (error: Exception) {
                _libraryScan.update { it.finished().copy(errors = it.errors + 1, messages = it.messages +
                    "Library scan failed: ${error.message ?: error.javaClass.simpleName}. Choose the ROM root again or retry.") }
            } finally {
                // Imports remain durable on cancellation. Existing bounded autofill queues
                // preserve custom artwork and never gate completion on network availability.
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main.immediate) {
                    if (!canWrite()) return@withContext
                    enqueueImportedArtwork(artworkIds)
                    scheduleSwitchDuplicateReconciliation()
                }
            }
        }
    }

    fun setAuthorizedRomTreeUri(uri: String?, statusOverride: String? = null) {
        if (!canWrite()) return
        GameRecommendations.invalidateRomInventory()
        val normalized = normalizeAuthorizedRomTreeUri(uri?.ifBlank { null })
        _authorizedRomTreeUri.value = normalized
        repository.saveAuthorizedRomTreeUri(normalized)
        setStatus(
            statusOverride ?: if (normalized == null) {
                "ROM folder authorization cleared."
            } else {
                "ROM folder authorized."
            }
        )
    }

    fun setGridColumns(columns: Int) {
        if (!canWrite()) return
        if (columns !in setOf(1, 2, 3, 4, 8)) return
        _gridColumns.value = columns
        repository.saveGridColumns(columns)
    }

    fun setGenreFilter(genre: String) {
        if (!canWrite()) return
        val normalized = GameFilters.normalizeFilter(genre)
        _genreFilter.value = normalized
        repository.saveGenreFilter(normalized)
    }

    fun addShortcut(option: InstalledShortcutOption) {
        launchWorker(Dispatchers.IO) { importShortcut(option) }
    }

    private suspend fun importShortcut(option: InstalledShortcutOption, autofill: Boolean = true) {
        val entryId = UUID.randomUUID().toString()
        val inferredIntentDataUri = option.resolvedIntentDataUri()
        val entry = GameEntry(
            id = entryId,
            title = option.title,
            subtitle = option.sourceLabel,
            platform = com.ariy.retrolauncher.model.PcGameStores.forShortcut(option)
                ?: "ScummVM".takeIf { option.packageName == ScummVmGames.PACKAGE }
                ?: inferPlatformFromIntentDataUri(inferredIntentDataUri),
            imageUri = runCatching { installedTargets.importShortcutIcon(
                packageName = option.packageName,
                shortcutId = option.shortcutId,
                userSerial = option.userSerial,
                targetId = entryId
            ) }.getOrNull(),
            sourceType = GameSourceType.SHORTCUT,
            packageName = option.packageName,
            shortcutId = option.shortcutId,
            userSerial = option.userSerial,
            intentAction = option.intentAction,
            intentDataUri = inferredIntentDataUri,
            mimeType = option.mimeType,
            categoriesCsv = option.categoriesCsv,
            extrasJson = option.extrasJson
        )
        withContext(Dispatchers.Main.immediate) { upsertOrMerge(
            entry = entry,
            target = LaunchTarget(
                sourceType = GameSourceType.SHORTCUT,
                label = option.sourceLabel,
                packageName = option.packageName,
                shortcutId = option.shortcutId,
                userSerial = option.userSerial,
                intentAction = option.intentAction,
                intentDataUri = inferredIntentDataUri,
                mimeType = option.mimeType,
                categoriesCsv = option.categoriesCsv,
                extrasJson = option.extrasJson
            ),
            autofill = autofill
        ) }
        if (autofill) launchWorker(Dispatchers.IO) {
            _games.value.firstOrNull { it.packageName == option.packageName && it.shortcutId == option.shortcutId &&
                it.userSerial == option.userSerial }?.let { upgradeDigitalPortrait(it) }
        }
    }

    private suspend fun upgradeDigitalPortrait(game: GameEntry) {
        if (game.platform !in com.ariy.retrolauncher.model.PcGameStores.platforms || game.imageUri.isNullOrBlank()) return
        val generated = resolveArtworkSourceUri(game.imageUri).lastPathSegment == "${game.id}-autofill.png"
        if (!generated && !installedTargets.isImportedShortcutIcon(game)) return
        if (SystemClock.elapsedRealtime() < (artworkRetryAfterMillis[game.id] ?: 0L)) return
        if (!queueArtworkAutofill(game.id)) return
        try {
            artworkRetryAfterMillis[game.id] = SystemClock.elapsedRealtime() + 5 * 60_000L
            val portrait = com.ariy.retrolauncher.launch.SteamStoreArtworkCatalog.fetchCandidates(game.title)
                .firstOrNull { it.source.endsWith("portrait") } ?: return
            val saved = importedIconStore.downloadImage("${game.id}-digital-portrait", portrait.imageUrl) ?: return
            updateGamesFromBackground { current -> current.map { it.withAutofilledArtwork(game, saved) } }
            artworkRetryAfterMillis.remove(game.id)
        } finally {
            finishArtworkAutofill(game.id, false)
        }
    }

    fun importPublishedShortcut(packageName: String, shortcutId: String, userSerial: Long) {
        launchWorker {
            val option = withContext(Dispatchers.IO) {
                installedTargets.loadShortcuts(packageName).firstOrNull {
                    it.shortcutId == shortcutId && it.userSerial == userSerial
                }
            }
            if (option != null) addShortcut(option)
            else _statusMessage.value = "Shortcut not available yet. Open Add games → Shortcuts after returning from the game app."
        }
    }

    fun addActivity(option: InstalledActivityOption) {
        val entry = GameEntry(
            title = option.title,
            subtitle = option.sourceLabel,
            platform = com.ariy.retrolauncher.launch.standaloneActivityPlatform(option.packageName),
            sourceType = GameSourceType.ACTIVITY,
            packageName = option.packageName,
            activityName = option.activityName,
            userSerial = option.userSerial
        )
        upsertOrMerge(
            entry = entry,
            target = LaunchTarget(
                sourceType = GameSourceType.ACTIVITY,
                label = option.sourceLabel,
                packageName = option.packageName,
                activityName = option.activityName,
                userSerial = option.userSerial
            )
        )
    }

    fun addCustom(entry: GameEntry) {
        val normalizedEntry = entry.copy(
            genre = normalizeCollection(entry.genre),
            platform = resolvePlatform(entry)
        )
        upsertOrMerge(
            entry = normalizedEntry,
            target = LaunchTarget(
                sourceType = GameSourceType.CUSTOM_INTENT,
                label = normalizedEntry.subtitle,
                packageName = normalizedEntry.packageName,
                activityName = normalizedEntry.activityName,
                intentAction = normalizedEntry.intentAction,
                intentDataUri = normalizedEntry.intentDataUri,
                mimeType = normalizedEntry.mimeType,
                categoriesCsv = normalizedEntry.categoriesCsv,
                extrasJson = normalizedEntry.extrasJson
            )
        )
    }

    fun addCustomEntries(entries: List<GameEntry>, refreshPlatform: String? = null) {
        if (!canWrite()) return
        if (libraryScanJob?.isActive == true) {
            setStatus("A library import is running. Wait for it or cancel it before scanning another platform.")
            return
        }
        if (entries.isEmpty()) {
            val platformLabel = refreshPlatform?.takeIf { it.isNotBlank() } ?: "titles"
            setStatus("No new $platformLabel were found in the ROM folder.")
            return
        }

        val platform = refreshPlatform ?: entries.first().platform.orEmpty()
        _libraryScan.value = LibraryScanProgress(running = true, stage = LibraryScanStage.ADDING,
            platform = platform, totalPlatforms = 1, discovered = entries.size)
        libraryScanJob = launchWorker(Dispatchers.IO) {
            val artworkIds = linkedSetOf<String>()
            var added = 0
            var merged = 0
            try {
                for (chunk in entries.chunked(64)) {
                    val committed = commitImportChunk(chunk)
                    val results = committed.results
                    added += results.count { it.outcome == UpsertOutcome.ADDED }
                    merged += results.count { it.outcome == UpsertOutcome.MERGED }
                    artworkIds += results.filter { it.outcome != UpsertOutcome.DUPLICATE }.map { it.gameId }
                    _libraryScan.update { it.copy(currentGame = chunk.last().title,
                        imported = added + merged, skipped = it.skipped + results.count { result -> result.outcome == UpsertOutcome.DUPLICATE }) }
                    setStatus("Adding $platform: ${added + merged} added / updated, ${_libraryScan.value.skipped} already present…")
                    delay(16)
                }
                _libraryScan.update { it.finished().copy(completedPlatforms = 1) }
                setStatus("Added $added titles and updated $merged existing tiles.")
            } catch (error: CancellationException) {
                _libraryScan.update { it.finished(cancelled = true) }
                setStatus("Import cancelled. Already added games are kept.")
                throw error
            } catch (error: Exception) {
                _libraryScan.update { it.finished().copy(errors = 1) }
                setStatus("Import failed; already added games are kept. ${error.message ?: error.javaClass.simpleName}")
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main.immediate) {
                    if (canWrite()) {
                        enqueueImportedArtwork(artworkIds)
                        scheduleSwitchDuplicateReconciliation()
                    }
                }
            }
        }
    }

    /** Never overwrite edits/artwork arriving while an import is being prepared. */
    private suspend fun commitImportChunk(entries: List<GameEntry>): PreparedGameImport {
        while (true) {
            currentCoroutineContext().ensureActive()
            val snapshot = _games.value
            val prepared = withContext(Dispatchers.Default) {
                val job = currentCoroutineContext()
                val batch = prepareGameImport(snapshot, entries,
                    normalize = { it.copy(genre = normalizeCollection(it.genre), platform = resolvePlatform(it)) },
                    resolveEntryPlatform = ::resolvePlatform, checkpoint = { job.ensureActive() })
                val unique = dedupeArcadeGameEntries(batch.games).map(ScummVmGames::repairTitle)
                    .map(com.ariy.retrolauncher.model.PcGameStores::classify)
                batch.copy(games = unique) to repository.serializeGames(unique)
            }
            val committed = withContext(Dispatchers.Main.immediate) {
                currentCoroutineContext().ensureActive()
                if (!canWrite()) throw CancellationException("Launcher reset")
                if (_games.value !== snapshot) false else {
                    repository.saveSerializedGames(prepared.second)
                    _games.value = prepared.first.games
                    true
                }
            }
            if (committed) return prepared.first
            kotlinx.coroutines.yield()
        }
    }

    /** One worker drains imported IDs; no hundreds-of-coroutines/pending-state burst. */
    private fun enqueueImportedArtwork(ids: Set<String>) {
        if (ids.isEmpty()) return
        launchWorker(Dispatchers.IO) {
            val imported = _games.value.associateBy { it.id }
            ids.forEach { id ->
                currentCoroutineContext().ensureActive()
                imported[id]?.let { game ->
                    autoFillArtworkIfMissingInternal(game.id, game.title, publishLoadingState = false, cacheSuggestions = false)
                }
            }
        }
    }

    fun fillMissingArtworkForPlatform(platform: String?, refreshAll: Boolean = false) {
        if (!canWrite()) return
        val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return
        val started = synchronized(activeArtworkBackfillPlatformIds) {
            if (normalizedPlatform in activeArtworkBackfillPlatformIds) {
                false
            } else {
                activeArtworkBackfillPlatformIds += normalizedPlatform
                _activeArtworkBackfillPlatforms.update { it + normalizedPlatform }
                true
            }
        }
        if (!started) return
        launchWorker(Dispatchers.IO) {
            try {
                val platformGames = _games.value.filter { game ->
                    GamePlatforms.normalizeOrNull(game.platform) == normalizedPlatform
                }
                val refreshableIds = platformGames.map { it.id }.toSet()
                if (refreshableIds.isEmpty()) return@launchWorker

                _games.value
                    .asSequence()
                    .filter { game -> GamePlatforms.normalizeOrNull(game.platform) == normalizedPlatform }
                    .filter { game -> if (refreshAll) true else game.imageUri.isNullOrBlank() }
                    .forEach { game ->
                        // Queue only the item being processed; cancellation must not strand later IDs.
                        if (!queueArtworkAutofill(game.id)) return@forEach
                        autoFillArtworkIfMissingInternal(
                            id = game.id,
                            title = game.title,
                            publishLoadingState = true,
                            cacheSuggestions = false,
                            alreadyQueued = true,
                            replaceExistingArtwork = refreshAll
                        )
                    }
            } finally {
                synchronized(activeArtworkBackfillPlatformIds) {
                    activeArtworkBackfillPlatformIds -= normalizedPlatform
                }
                _activeArtworkBackfillPlatforms.update { it - normalizedPlatform }
            }
        }
    }

    fun updateGame(entry: GameEntry) {
        val normalizedEntry = entry.copy(
            genre = normalizeCollection(entry.genre),
            platform = resolvePlatform(entry),
            needsLaunchSetup = entry.needsLaunchSetup && entry.packageName.isNullOrBlank()
        )
        val updated = _games.value.map { if (it.id == normalizedEntry.id) normalizedEntry else it }
        persistGames(updated)
    }

    fun removeGame(id: String) {
        persistGames(_games.value.filterNot { it.id == id })
    }

    fun moveGame(fromId: String, toId: String) {
        if (fromId == toId) return
        val current = _games.value.toMutableList()
        val fromIndex = current.indexOfFirst { it.id == fromId }
        val toIndex = current.indexOfFirst { it.id == toId }
        if (fromIndex == -1 || toIndex == -1) return
        val moving = current.removeAt(fromIndex)
        current.add(toIndex, moving)
        persistGames(current)
    }

    fun updateGameImage(id: String, uri: Uri?) {
        val updated = _games.value.map { game ->
            if (game.id == id) game.copy(imageUri = uri?.toString()?.let(::stampArtworkUri)) else game
        }
        persistGames(updated)
    }

    fun importImageFromUrl(id: String, imageUrl: String) {
        launchWorker(Dispatchers.IO) {
            val savedUri = importedIconStore.downloadImage(id, imageUrl.trim())
            if (savedUri != null) {
                updateGamesFromBackground { games ->
                    games.map { game ->
                        if (game.id == id) game.copy(imageUri = stampArtworkUri(resolveArtworkSourceUri(savedUri).toString())) else game
                    }
                }
                setStatus("Artwork updated.")
            } else {
                setStatus("Could not download image from URL.")
            }
        }
    }

    fun loadArtworkSuggestions(id: String, title: String) {
        launchWorker(Dispatchers.IO) {
            _artworkLoading.update { it + id }
            try {
                val queryKey = artworkSuggestionQueryKey(id, title)
                val suggestions = fetchArtworkSuggestionsFor(id, title)
                cacheArtworkSuggestions(id, queryKey, suggestions)
                _artworkSuggestions.update { it + (id to suggestions) }
                if (suggestions.isEmpty()) setStatus("No artwork suggestions found.")
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                Log.w("RetroLauncher", "Artwork lookup failed for id='$id'", error)
                setStatus("Artwork lookup failed.")
            } finally {
                _artworkLoading.update { it - id }
            }
        }
    }

    fun importTopArtworkSuggestion(id: String, title: String) {
        launchWorker(Dispatchers.IO) {
            _artworkLoading.update { it + id }
            try {
                val queryKey = artworkSuggestionQueryKey(id, title)
                val suggestions = cachedArtworkSuggestions(id, queryKey)
                    ?: fetchArtworkSuggestionsFor(id, title).also { fetched ->
                        cacheArtworkSuggestions(id, queryKey, fetched)
                    }
                _artworkSuggestions.update { it + (id to suggestions) }
                val game = _games.value.firstOrNull { it.id == id } ?: return@launchWorker
                val savedUri = downloadArtworkWithFallback(game, title, suggestions, fileName = id)
                if (savedUri != null) {
                    updateGamesFromBackground { games ->
                        games.map { game ->
                            if (game.id == id) game.copy(imageUri = stampArtworkUri(resolveArtworkSourceUri(savedUri).toString())) else game
                        }
                    }
                    setStatus("Artwork updated from suggestions.")
                } else {
                    setStatus("Could not download any suggested image.")
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                Log.w("RetroLauncher", "Top artwork import failed for id='$id'", error)
                setStatus("Artwork lookup failed.")
            } finally {
                _artworkLoading.update { it - id }
            }
        }
    }

    fun launchTarget(game: GameEntry, target: LaunchTarget): Boolean {
        if (game.needsLaunchSetup && target.packageName.isNullOrBlank()) {
            setStatus("Needs emulator setup. Open this tile's menu → Edit / Launch targets, select an installed emulator preset and system/core, then save. Or install a supported standalone emulator and retry Options → Scan library.")
            return false
        }
        val app = getApplication<Application>()
        val normalizedGame = normalizeRetroArchPlatformIfNeeded(game)
        var launchedPackageName: String? = null
        val launched = when (target.sourceType) {
            GameSourceType.SHORTCUT -> {
                val packageName = target.packageName ?: return false.also { setStatus("Shortcut package missing.") }
                val shortcutId = target.shortcutId ?: return false.also { setStatus("Shortcut id missing.") }
                val userSerial = target.userSerial ?: return false.also { setStatus("Shortcut user missing.") }
                launchedPackageName = packageName
                installedTargets.startShortcut(packageName, shortcutId, userSerial)
            }

            GameSourceType.ACTIVITY -> {
                val packageName = target.packageName ?: return false.also { setStatus("Activity package missing.") }
                launchedPackageName = packageName
                if (target.hasExplicitLaunchPayload()) {
                    launchPayloadActivity(app, game, target, ::setStatus)
                } else {
                    val activityName = target.activityName ?: return false.also { setStatus("Activity name missing.") }
                    val userSerial = target.userSerial ?: 0L
                    installedTargets.startActivity(ComponentName(packageName, activityName), userSerial)
                }
            }

            GameSourceType.CUSTOM_INTENT -> runCatching {
                val effectiveTarget = normalizedGame.normalizeRetroArchTargetForLaunch(app, target)
                launchedPackageName = effectiveTarget.packageName
                val baseIntent = normalizedGame.buildIntent(app, effectiveTarget).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (effectiveTarget.packageName.isRetroArchPackage()) {
                    startRetroArchIntentWithFallbacks(app, effectiveTarget, baseIntent)
                } else {
                    runCatching {
                        startIntentWithFileUriSupport(app, effectiveTarget.packageName, baseIntent)
                    }.recoverCatching { firstError ->
                        val fallback = fallbackToLauncherComponentIntent(app, effectiveTarget.packageName, baseIntent)
                            ?: fallbackToPackageOnlyIntent(effectiveTarget, baseIntent)
                            ?: throw firstError
                        startIntentWithFileUriSupport(app, effectiveTarget.packageName, fallback)
                    }
                }
                true
            }.onFailure { error ->
                setStatus("Launch failed for ${normalizedGame.title}: ${error.message ?: error.javaClass.simpleName}")
            }.getOrDefault(false)
        }

        if (!launched) {
            if (_statusMessage.value == null) {
                setStatus("Launch failed for ${normalizedGame.title}.")
            }
            pendingRetroArchCleanupPackage = null
        } else {
            markGamePlayed(normalizedGame.id)
            pendingRetroArchCleanupPackage = launchedPackageName
                ?.takeIf { it.isRetroArchPackage() && normalizedGame.platform.supportsRetroArchCleanupOnHome() }
        }
        return launched
    }

    fun onLauncherResumed() {
        if (!canWrite()) return
        refreshInstalledTargets(importSharedGames = _setupCompleted.value)
        _launcherResumeNonce.value = _launcherResumeNonce.value + 1
        if (artworkResumeJob?.isActive != true) {
            artworkResumeJob = launchWorker {
                while (true) {
                    resumeMissingArtworkIfOnline()
                    delay(30_000)
                }
            }
        }
        val packageName = pendingRetroArchCleanupPackage ?: return
        pendingRetroArchCleanupPackage = null
        launchWorker(Dispatchers.IO) {
            delay(250)
            runCatching {
                getApplication<Application>()
                    .getSystemService(ActivityManager::class.java)
                    ?.killBackgroundProcesses(packageName)
                Log.d("RetroLauncher", "Requested RetroArch cleanup for pkg='$packageName' after launcher resume")
            }.onFailure { error ->
                Log.w("RetroLauncher", "RetroArch cleanup request failed for pkg='$packageName'", error)
            }
        }
    }

    fun onLauncherPaused() {
        artworkResumeJob?.cancel()
        artworkResumeJob = null
    }

    private fun resumeMissingArtworkIfOnline() {
        if (_libraryScan.value.running) return
        val connectivity = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
            ?: return
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return
        val inFlight = synchronized(pendingArtworkAutofillIds) { pendingArtworkAutofillIds.toSet() }
        missingArtworkToResume(_games.value, inFlight, artworkRetryAfterMillis, SystemClock.elapsedRealtime())
            .forEach { game -> autoFillArtworkIfMissing(game.id, game.title) }
    }

    fun preferredLaunchTarget(game: GameEntry): LaunchTarget =
        game.preferredTarget() ?: game.primaryTarget()

    fun setPreferredTarget(gameId: String, target: LaunchTarget) {
        val updated = _games.value.map { game ->
            if (
                game.id == gameId &&
                (GamePlatforms.normalizeOrNull(game.platform) != "Nintendo Switch" || game.allTargets().size <= 1)
            ) game.promoteTarget(target) else game
        }
        persistGames(updated)
    }

    fun removeTarget(gameId: String, target: LaunchTarget) {
        val updated = _games.value.mapNotNull { game ->
            if (game.id != gameId) {
                game
            } else {
                val remainingTargets = game.allTargets().filterNot { it.stableKey() == target.stableKey() }
                when {
                    remainingTargets.isEmpty() -> {
                        setStatus("A game tile needs at least one launch target.")
                        game
                    }

                    remainingTargets.size == game.allTargets().size -> game

                    else -> {
                        val newPrimary = remainingTargets.first()
                        val newPreferredKey = game.preferredTargetKey
                            ?.takeIf { it != target.stableKey() && remainingTargets.any { remaining -> remaining.stableKey() == it } }
                            ?: newPrimary.stableKey()
                        game.copy(
                            sourceType = newPrimary.sourceType,
                            subtitle = game.subtitle.ifBlank { newPrimary.label },
                            packageName = newPrimary.packageName,
                            activityName = newPrimary.activityName,
                            shortcutId = newPrimary.shortcutId,
                            userSerial = newPrimary.userSerial,
                            intentAction = newPrimary.intentAction,
                            intentDataUri = newPrimary.intentDataUri,
                            mimeType = newPrimary.mimeType,
                            categoriesCsv = newPrimary.categoriesCsv,
                            extrasJson = newPrimary.extrasJson,
                            alternateTargetsJson = remainingTargets.drop(1).takeIf { it.isNotEmpty() }?.toJsonString(),
                            preferredTargetKey = newPreferredKey
                        )
                    }
                }
            }
        }
        persistGames(updated)
    }

    fun launch(game: GameEntry): Boolean {
        val preferred = preferredLaunchTarget(game)
        return launchGroupedTarget(game, preferred)
    }

    fun launchGroupedTarget(game: GameEntry, requestedTarget: LaunchTarget): Boolean {
        val targets = game.allTargets()
        if (GamePlatforms.normalizeOrNull(game.platform) != "Nintendo Switch" || targets.size <= 1) {
            setPreferredTarget(game.id, requestedTarget)
            return launchTarget(game, requestedTarget)
        }

        val basePreferred = preferredLaunchTarget(game)
        val orderedTargets = listOf(requestedTarget, basePreferred) + targets
        val distinctTargets = orderedTargets.distinctBy(LaunchTarget::stableKey)
        var baseTargetFailed = false
        for ((index, target) in distinctTargets.withIndex()) {
            _statusMessage.value = null
            if (launchTarget(game, target)) {
                if (target.stableKey() == basePreferred.stableKey() || baseTargetFailed) {
                    setPreferredTarget(game.id, target)
                    if (baseTargetFailed && target.stableKey() != basePreferred.stableKey()) {
                        setStatus("Default Switch game file failed; switched the default to another grouped file.")
                    }
                } else {
                    setStatus("Launched another grouped Switch file; the preferred default remains unchanged.")
                }
                return true
            }
            if (target.stableKey() == basePreferred.stableKey()) baseTargetFailed = true
            if (index < distinctTargets.lastIndex) _statusMessage.value = null
        }
        return false
    }

    fun addRecommendation(recommendation: GameRecommendation) {
        val wasAlreadyAdded = _games.value.any { it.id == recommendation.launchEntry.id }
        val updatedGames = (_games.value + recommendation.launchEntry).distinctBy { it.id }
        persistGames(updatedGames, synchronously = true)
        if (!wasAlreadyAdded && recommendation.launchEntry.platform.supportsMameFallbackArtwork()) {
            importTopArtworkSuggestion(recommendation.launchEntry.id, recommendation.launchEntry.title)
        } else {
            autoFillArtworkIfMissing(recommendation.launchEntry.id, recommendation.launchEntry.title)
        }
    }

    fun playRecommendation(recommendation: GameRecommendation): Boolean =
        launch(recommendation.launchEntry)

    fun acceptRecommendation(recommendation: GameRecommendation): Boolean {
        addRecommendation(recommendation)
        return launch(recommendation.launchEntry)
    }

    fun verifyScummVmConfig(gameFolderUri: String, gameId: String): Boolean {
        val folder = gameFolderUri.trim()
        val id = gameId.trim().lowercase()
        if (folder.isBlank()) {
            setStatus("ScummVM verify failed: choose a game folder.")
            return false
        }
        if (id.isBlank()) {
            setStatus("ScummVM verify failed: enter a game id.")
            return false
        }
        val extras = JSONObject().apply {
            put(SCUMMVM_EXTRA_GAME_ID, id)
        }.toString()
        val target = LaunchTarget(
            sourceType = GameSourceType.CUSTOM_INTENT,
            label = "ScummVM verify",
            packageName = "org.scummvm.scummvm",
            intentAction = Intent.ACTION_MAIN,
            intentDataUri = folder,
            extrasJson = extras
        )
        val probe = GameEntry(
            title = "ScummVM verify",
            sourceType = GameSourceType.CUSTOM_INTENT,
            packageName = target.packageName,
            intentAction = target.intentAction,
            intentDataUri = target.intentDataUri,
            extrasJson = extras
        )
        val launched = launchTarget(probe, target)
        if (launched) {
            setStatus("ScummVM verify launched. If the game starts, this ID and folder are valid.")
        }
        return launched
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun templates() = CustomIntentTemplates.all

    private fun upsertOrMerge(entry: GameEntry, target: LaunchTarget, autofill: Boolean = true) {
        val normalizedTitle = entry.title.trim().lowercase()
        val normalizedPlatform = resolvePlatform(entry)
        val current = _games.value.toMutableList()
        val existingIndex = current.indexOfFirst { existing ->
            existing.title.trim().lowercase() == normalizedTitle &&
                resolvePlatform(existing) == normalizedPlatform
        }
        val result = upsertOrMergeInPlace(
            current = current,
            entry = entry.copy(platform = normalizedPlatform),
            target = target,
            resolveEntryPlatform = ::resolvePlatform
        )
        if (result.outcome == UpsertOutcome.DUPLICATE) return
        persistGames(current)
        if (autofill) autoFillArtworkIfMissing(result.gameId, result.title)
        scheduleSwitchDuplicateReconciliation()
    }

    private fun persistGames(games: List<GameEntry>, synchronously: Boolean = false) {
        if (!canWrite()) return
        val uniqueGames = dedupeArcadeGameEntries(games).map(ScummVmGames::repairTitle)
            .map(com.ariy.retrolauncher.model.PcGameStores::classify)
        _games.value = uniqueGames
        if (synchronously) {
            repository.saveGamesSync(uniqueGames)
        } else {
            launchWorker(Dispatchers.Default) {
                val payload = repository.serializeGames(uniqueGames)
                withContext(Dispatchers.Main.immediate) {
                    // A newer edit/import owns persistence if this snapshot is stale.
                    if (canWrite() && _games.value === uniqueGames) repository.saveSerializedGames(payload)
                }
            }
        }
    }

    // All background read/modify/write commits share the main thread with user edits.
    private suspend fun updateGamesFromBackground(transform: (List<GameEntry>) -> List<GameEntry>) {
        withContext(Dispatchers.Main.immediate) {
            currentCoroutineContext().ensureActive()
            if (!canWrite()) return@withContext
            val current = _games.value
            val updated = transform(current)
            if (updated != current) persistGames(updated)
        }
    }

    private fun markGamePlayed(gameId: String) {
        val playedAt = System.currentTimeMillis()
        val updated = _games.value.map { game ->
            if (game.id == gameId) game.copy(
                lastPlayedAtEpochMillis = playedAt,
                playCount = game.playCount.coerceIn(0, Long.MAX_VALUE - 1) + 1
            ) else game
        }
        if (updated == _games.value) return
        persistGames(updated, synchronously = true)
    }

    private fun normalizeRetroArchPlatformIfNeeded(game: GameEntry): GameEntry {
        if (!game.isRetroArchPreset()) return game
        val resolved = resolvePlatform(game) ?: return game
        if (resolved.equals(game.platform, ignoreCase = true)) return game
        val normalized = game.copy(platform = resolved)
        val updated = _games.value.map { existing ->
            if (existing.id == game.id) normalized else existing
        }
        persistGames(updated)
        return normalized
    }

    fun setStatus(message: String) {
        if (!canWrite()) return
        _statusMessage.value = message
    }

    private fun autoFillArtworkIfMissing(id: String, title: String) {
        if (!queueArtworkAutofill(id)) return
        launchWorker(Dispatchers.IO) {
            autoFillArtworkIfMissingInternal(
                id = id,
                title = title,
                publishLoadingState = true,
                cacheSuggestions = true,
                alreadyQueued = true
            )
        }
    }

    private suspend fun autoFillArtworkIfMissingInternal(
        id: String,
        title: String,
        publishLoadingState: Boolean,
        cacheSuggestions: Boolean,
        alreadyQueued: Boolean = false,
        replaceExistingArtwork: Boolean = false
    ) {
        if (!alreadyQueued && !queueArtworkAutofill(id)) return
        try {
            if (title.isBlank()) return
            val current = _games.value.firstOrNull { it.id == id } ?: return
            if (!replaceExistingArtwork && !current.imageUri.isNullOrBlank()) return
            if (current.platform.supportsMameFallbackArtwork()) {
                val availableAssets = runCatching {
                    getApplication<Application>().assets.list("overlay_mame")
                        ?.filterNot { it.contains("Zone.Identifier", ignoreCase = true) }
                        ?.associateBy { it.lowercase() }
                        .orEmpty()
                }.getOrDefault(emptyMap())
                if (availableAssets.isNotEmpty() && !current.needsMameFallbackArtwork(availableAssets)) {
                    return
                }
            }
            artworkAutofillSemaphore.withPermit {
                markArtworkAutofillStarted(id, publishLoadingState)
                val latest = _games.value.firstOrNull { it.id == id } ?: return@withPermit
                if (!replaceExistingArtwork && !latest.imageUri.isNullOrBlank()) return@withPermit
                artworkRetryAfterMillis[id] = SystemClock.elapsedRealtime() + 5 * 60_000L
                val searchTitle = latest.title
                val queryKey = artworkSuggestionQueryKey(id, searchTitle)
                val suggestions = (if (cacheSuggestions) cachedArtworkSuggestions(id, queryKey) else null)
                    ?: fetchArtworkSuggestionsFor(id, searchTitle).also { fetched ->
                        if (cacheSuggestions) {
                            cacheArtworkSuggestions(id, queryKey, fetched)
                        }
                    }
                if (cacheSuggestions) {
                    _artworkSuggestions.update { it + (id to suggestions) }
                }
                val savedUri = downloadArtworkWithFallback(latest, searchTitle, suggestions, fileName = "$id-autofill")
                if (!savedUri.isNullOrBlank()) {
                    updateGamesFromBackground { games ->
                        games.map { game ->
                            game.withAutofilledArtwork(latest, stampArtworkUri(resolveArtworkSourceUri(savedUri).toString()))
                        }
                    }
                    artworkRetryAfterMillis.remove(id)
                }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                artworkRetryAfterMillis.remove(id)
                throw error
            }
            Log.w("RetroLauncher", "Artwork autofill failed for id='$id'", error)
        } finally {
            finishArtworkAutofill(id, publishLoadingState)
        }
    }

    private fun queueArtworkAutofill(id: String): Boolean {
        if (!canWrite()) return false
        val queued = synchronized(pendingArtworkAutofillIds) {
            if (id in pendingArtworkAutofillIds) {
                false
            } else {
                pendingArtworkAutofillIds += id
                true
            }
        }
        if (queued) {
            _artworkPending.update { it + id }
        }
        return queued
    }

    private fun markArtworkAutofillStarted(id: String, publishLoadingState: Boolean) {
        _artworkPending.update { it - id }
        if (publishLoadingState) {
            _artworkLoading.update { it + id }
        }
    }

    private fun finishArtworkAutofill(id: String, publishLoadingState: Boolean) {
        synchronized(pendingArtworkAutofillIds) {
            pendingArtworkAutofillIds -= id
        }
        _artworkPending.update { it - id }
        if (publishLoadingState) {
            _artworkLoading.update { it - id }
        }
    }

    private fun prefetchMameFallbackArtworkIfNeeded() {
        launchWorker(Dispatchers.IO) {
            val availableAssets = runCatching {
                getApplication<Application>().assets.list("overlay_mame")
                    ?.filterNot { it.contains("Zone.Identifier", ignoreCase = true) }
                    ?.associateBy { it.lowercase() }
                    .orEmpty()
            }.getOrDefault(emptyMap())
            if (!hasGenericMameFallbackAsset(availableAssets)) return@launchWorker
            _games.value
                .filter { it.needsMameFallbackArtwork(availableAssets) }
                .forEach { game ->
                    autoFillArtworkIfMissing(game.id, game.title)
                }
        }
    }

    private fun artworkSearchQueryFor(id: String, fallbackTitle: String): String {
        val game = _games.value.firstOrNull { it.id == id } ?: return fallbackTitle
        return game.artworkSearchQuery(fallbackTitle)
    }

    private fun artworkSearchQueriesFor(id: String, fallbackTitle: String): List<String> {
        val game = _games.value.firstOrNull { it.id == id } ?: return listOf(fallbackTitle)
        return game.artworkSearchQueries(fallbackTitle)
    }

    private fun fetchArtworkSuggestionsFor(id: String, fallbackTitle: String): List<ArtworkCandidate> {
        val game = _games.value.firstOrNull { it.id == id } ?: return emptyList()
        val searchGame = game.copy(title = fallbackTitle.ifBlank { game.title })
        return ArtworkSuggestionService.fetchCandidateBatches(searchGame, searchGame.artworkSearchQueries(searchGame.title))
            .firstOrNull { it.isNotEmpty() }.orEmpty().distinctBy(ArtworkCandidate::imageUrl).take(24)
    }

    private suspend fun downloadArtworkWithFallback(
        game: GameEntry,
        title: String,
        suggestions: List<ArtworkCandidate>,
        fileName: String
    ): String? {
        val coroutineContext = currentCoroutineContext()
        val searchGame = game.copy(title = title.ifBlank { game.title })
        val batches = sequence {
            yield(suggestions)
            yieldAll(ArtworkSuggestionService.fetchCandidateBatches(searchGame, searchGame.artworkSearchQueries(searchGame.title)))
        }
        return firstSuccessfulArtworkBatch(batches) { candidates ->
            coroutineContext.ensureActive()
            // Web results are ranked matches, not interchangeable editions from an exact
            // catalog. Do not replace the first valid match with a lower-ranked image
            // merely because its shape fits the tile better.
            val saved = if (candidates.any(ArtworkSuggestionService::isWebCandidate)) {
                importedIconStore.downloadFirstAvailableImage(fileName, candidates.map(ArtworkCandidate::imageUrl),
                    maxAttempts = 6, cancellationCheck = { coroutineContext.ensureActive() })
            } else importedIconStore.downloadBestAspectImage(
                fileName = fileName,
                imageUrls = candidates.map(ArtworkCandidate::imageUrl),
                targetAspectRatio = preferredArtworkAspectRatio(game.platform),
                cancellationCheck = { coroutineContext.ensureActive() }
            )
            // A download fallback must not replace the primary Find images results/cache.
            saved
        }
    }

    private fun artworkSuggestionQueryKey(id: String, title: String): String =
        artworkSearchQueriesFor(id, title).joinToString(separator = "\u001f")

    private fun cachedArtworkSuggestions(id: String, queryKey: String): List<ArtworkCandidate>? =
        artworkSuggestionCache.get(id)
            ?.takeIf { (cachedKey, _) -> cachedKey == queryKey }
            ?.second

    private fun cacheArtworkSuggestions(id: String, queryKey: String, suggestions: List<ArtworkCandidate>) {
        if (suggestions.isNotEmpty()) artworkSuggestionCache.put(id, queryKey to suggestions)
    }

    private fun persistCleanedGamesIfNeeded() {
        val loaded = repository.loadGames()
        if (loaded != _games.value) {
            repository.saveGames(_games.value)
        }
    }

    private fun scheduleSwitchDuplicateReconciliation() {
        if (!canWrite()) return
        switchDuplicateReconciliationJob?.cancel()
        switchDuplicateReconciliationJob = launchWorker {
            while (true) {
                val snapshot = _games.value
                val titleIdsByGameId = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    snapshot.mapNotNull { game ->
                        if (GamePlatforms.normalizeOrNull(game.platform) != "Nintendo Switch") {
                            return@mapNotNull null
                        }
                        val targetTitleIds = game.allTargets().mapNotNull { target ->
                            val rawUri = target.intentDataUri?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
                                ?: return@mapNotNull null
                            runCatching { inferSwitchTitleIdFromPackageUri(context, uri) }
                                .getOrNull()
                                ?.let { target.stableKey() to it }
                        }
                        game.id to targetTitleIds.toMap()
                    }.toMap()
                }

                if (_games.value != snapshot) continue
                val deduped = dedupeSwitchGameEntries(snapshot, titleIdsByGameId)
                if (deduped != snapshot) persistGames(deduped)
                return@launchWorker
            }
        }
    }

    private fun cleanupLoadedGames(entries: List<GameEntry>): List<GameEntry> {
        val templateLabels = CustomIntentTemplates.all.map { it.label.lowercase() }.toSet()
        val helperTitles = setOf(
            "emulated platform",
            "suggested rom path",
            "suggested platform rom",
            "suggested platfrom rom"
        )
        return entries
            .filterNot { game ->
            val title = game.title.trim()
            if (title.isBlank()) return@filterNot true
            val normalized = title.lowercase()
            val normalizedWithoutPreset = normalized.removeSuffix(" preset")
            val templateArtifact = normalized in templateLabels || normalizedWithoutPreset in templateLabels
            val helperArtifact = normalized in helperTitles
            val knownLeftover = normalized == "dolphin mmjr2"
            (templateArtifact || helperArtifact || knownLeftover) && game.imageUri.isNullOrBlank()
        }
            .flatMap { game -> splitCrossPlatformRetroArchEntry(game) }
            .map { game ->
                val resolvedPlatform = resolvePlatform(game)
                val migratedSubtitle = migrateGeneratedSubtitle(game, resolvedPlatform)
                if (game.isRetroArchPreset()) {
                    val preferred = game.preferredTarget()
                    val coreHint = inferRetroArchPlatformFromExtras(game).orEmpty()
                    val pathHint = inferPlatformFromExtrasPath(game).orEmpty()
                    val uriHint = inferPlatformFromEntry(game).orEmpty()
                    Log.d(
                        "RetroLauncherPlatform",
                        "title='${game.title}' stored='${game.platform.orEmpty()}' resolved='${resolvedPlatform.orEmpty()}' coreHint='$coreHint' pathHint='$pathHint' uriHint='$uriHint' pkg='${(preferred?.packageName ?: game.packageName).orEmpty()}' uri='${(preferred?.intentDataUri ?: game.intentDataUri).orEmpty()}'"
                    )
                }
                game.copy(
                    subtitle = migratedSubtitle,
                    platform = resolvedPlatform
                )
            }
    }

    private fun migrateGeneratedSubtitle(game: GameEntry, resolvedPlatform: String?): String {
        if (game.sourceType != GameSourceType.CUSTOM_INTENT) return game.subtitle
        val currentSubtitle = game.subtitle.takeIf { it.isNotBlank() } ?: return game.subtitle
        val template = CustomIntentTemplates.matchingTemplate(game.packageName, game.activityName) ?: return game.subtitle
        if (!currentSubtitle.equals(template.description, ignoreCase = true)) return game.subtitle
        return template.defaultSubtitle(
            platform = resolvedPlatform ?: game.platform,
            retroArchSystem = resolvedPlatform ?: game.platform
        )
    }

    private fun restoreAuthorizedRomTreeUriIfNeeded() {
        // Explicit welcome/reset state must never recover a revoked/failed-to-release old grant.
        if (!_setupCompleted.value) return
        val app = getApplication<Application>()
        val current = _authorizedRomTreeUri.value
        if (current != null && app.hasPersistedReadPermissionForTree(current)) return
        val restored = app.findRestorableRomTreeUri() ?: return
        _authorizedRomTreeUri.value = restored
        repository.saveAuthorizedRomTreeUri(restored)
    }

}

private fun splitCrossPlatformRetroArchEntry(entry: GameEntry): List<GameEntry> {
    if (!entry.isRetroArchPreset()) return listOf(entry)
    val targetsWithPlatform = entry.allTargets().map { target ->
        val probe = entry.copy(
            subtitle = target.label.ifBlank { entry.subtitle },
            sourceType = target.sourceType,
            packageName = target.packageName,
            activityName = target.activityName,
            shortcutId = target.shortcutId,
            userSerial = target.userSerial,
            intentAction = target.intentAction,
            intentDataUri = target.intentDataUri,
            mimeType = target.mimeType,
            categoriesCsv = target.categoriesCsv,
            extrasJson = target.extrasJson,
            alternateTargetsJson = null,
            preferredTargetKey = null
        )
        target to resolvePlatform(probe)
    }
    val distinctPlatforms = targetsWithPlatform.mapNotNull { it.second }.distinct()
    if (distinctPlatforms.size <= 1) return listOf(entry)

    val originalPreferredKey = entry.preferredTargetKey
    return targetsWithPlatform
        .groupBy { it.second }
        .map { (platform, grouped) ->
            val targets = grouped.map { it.first }
            val primary = targets.firstOrNull { it.stableKey() == originalPreferredKey } ?: targets.first()
            val alternates = targets.filterNot { it.stableKey() == primary.stableKey() }
            entry.copy(
                id = if (primary.stableKey() == entry.primaryTarget().stableKey()) entry.id else UUID.randomUUID().toString(),
                subtitle = primary.label.ifBlank { entry.subtitle },
                platform = platform,
                sourceType = primary.sourceType,
                packageName = primary.packageName,
                activityName = primary.activityName,
                shortcutId = primary.shortcutId,
                userSerial = primary.userSerial,
                intentAction = primary.intentAction,
                intentDataUri = primary.intentDataUri,
                mimeType = primary.mimeType,
                categoriesCsv = primary.categoriesCsv,
                extrasJson = primary.extrasJson,
                alternateTargetsJson = alternates.takeIf { it.isNotEmpty() }?.toJsonString(),
                preferredTargetKey = primary.stableKey()
            )
        }
}

private fun resolvePlatform(entry: GameEntry): String? {
    if (entry.sourceType == GameSourceType.ACTIVITY) return com.ariy.retrolauncher.launch.standaloneActivityPlatform(entry.packageName)
    if (entry.packageName == "app.gamenative") return com.ariy.retrolauncher.model.PcGameStores.fromExtras(entry.extrasJson)
        ?: entry.platform?.takeIf { it in com.ariy.retrolauncher.model.PcGameStores.platforms }
        ?: "GameNative"
    val storedPlatform = GamePlatforms.normalizeOrNull(entry.platform)
    val retroArchCorePlatform = inferRetroArchPlatformFromExtras(entry)
    val inferredFromExtrasPath = inferPlatformFromExtrasPath(entry)
    val inferredFromEntry = inferPlatformFromEntry(entry)
    if (entry.isRetroArchPreset()) {
        // For RetroArch, prefer ROM path inference first to recover from stale core metadata.
        if (!inferredFromExtrasPath.isNullOrBlank()) return inferredFromExtrasPath
        if (!inferredFromEntry.isNullOrBlank()) return inferredFromEntry
        val inferredFromSubtitle = inferRetroArchPlatformFromSubtitle(entry)
        if (!inferredFromSubtitle.isNullOrBlank()) return inferredFromSubtitle
        if (!storedPlatform.isNullOrBlank()) return storedPlatform
        if (!retroArchCorePlatform.isNullOrBlank()) return retroArchCorePlatform
    }
    // For single-system launchers like Dolphin, prefer ROM/data URI inference over package name
    // so Wii content is not collapsed into the GameCube package fallback.
    if (!inferredFromEntry.isNullOrBlank()) return inferredFromEntry
    val packageCandidates = sequenceOf(entry.packageName) + entry.orderedTargets().asSequence().map { it.packageName }
    val packageInferred = packageCandidates
        .mapNotNull { GamePlatforms.inferFromPackageName(it) }
        .firstOrNull()
    if (!packageInferred.isNullOrBlank()) return packageInferred
    return inferredFromExtrasPath ?: retroArchCorePlatform ?: storedPlatform
}

private fun inferPlatformFromEntry(entry: GameEntry): String? {
    val uriCandidates = sequenceOf(entry.intentDataUri) + entry.orderedTargets().asSequence().map { it.intentDataUri }
    val inferredFromUri = uriCandidates
        .mapNotNull { inferPlatformFromIntentDataUri(it) }
        .firstOrNull()
    if (!inferredFromUri.isNullOrBlank()) return inferredFromUri
    val packageCandidates = sequenceOf(entry.packageName) + entry.orderedTargets().asSequence().map { it.packageName }
    return packageCandidates
        .mapNotNull { GamePlatforms.inferFromPackageName(it) }
        .firstOrNull()
}

private fun inferRetroArchPlatformFromExtras(entry: GameEntry): String? {
    val retroArchCoreTokenToPlatform = mapOf(
        "dosbox" to "DOS Games",
        "mupen64plus" to "Nintendo 64",
        "mgba" to "Game Boy Advance",
        "melonds" to "Nintendo DS",
        "yabasanshiro" to "Sega Saturn",
        "beetle_psx" to "Sony PlayStation",
        "pcsx" to "Sony PlayStation",
        "ppsspp" to "Sony PlayStation Portable",
        "genesis_plus_gx" to "Sega Genesis / Mega Drive",
        "flycast" to "Sega Dreamcast",
        "mame" to "MAME Arcade",
        "fbneo" to "Final Burn Neo",
        "snes9x" to "Super Nintendo",
        "mesen" to "Nintendo Entertainment System"
    )
    val extrasCandidates = sequenceOf(entry.extrasJson) + entry.orderedTargets().asSequence().map { it.extrasJson }
    return extrasCandidates
        .mapNotNull { raw ->
            val json = runCatching { JSONObject(raw.orEmpty()) }.getOrNull() ?: return@mapNotNull null
            val libretro = json.optString("LIBRETRO").ifBlank { return@mapNotNull null }.lowercase()
            val tokenMatch = retroArchCoreTokenToPlatform.entries.firstOrNull { (token, _) ->
                libretro.contains(token)
            }?.value
            tokenMatch?.let { GamePlatforms.normalizeOrNull(it) } ?: GamePlatforms.inferFromUriString(libretro)
        }
        .firstOrNull()
}

private fun inferRetroArchPlatformFromSubtitle(entry: GameEntry): String? {
    val subtitleCandidates = sequenceOf(entry.subtitle) + entry.orderedTargets().asSequence().map { it.label }
    return subtitleCandidates
        .mapNotNull { subtitle ->
            val normalized = subtitle?.trim().orEmpty()
            when {
                normalized.contains("RetroArch", ignoreCase = true) &&
                    normalized.contains("Game Boy Color", ignoreCase = true) -> "Game Boy Color"
                normalized.contains("RetroArch", ignoreCase = true) &&
                    normalized.contains("Game Boy Advance", ignoreCase = true) -> "Game Boy Advance"
                normalized.contains("RetroArch", ignoreCase = true) &&
                    normalized.contains("Game Boy", ignoreCase = true) -> "Game Boy"
                else -> null
            }?.let(GamePlatforms::normalizeOrNull)
        }
        .firstOrNull()
}

private fun inferPlatformFromExtrasPath(entry: GameEntry): String? {
    val extrasCandidates = sequenceOf(entry.extrasJson) + entry.orderedTargets().asSequence().map { it.extrasJson }
    return extrasCandidates
        .mapNotNull { raw ->
            val json = runCatching { JSONObject(raw.orEmpty()) }.getOrNull() ?: return@mapNotNull null
            val candidates = mutableListOf<String>()
            val directKeys = listOf("ROM", "rom", "path", "PATH", "cmd", "CMD", "CONTENT", "content")
            directKeys.forEach { key ->
                val value = json.optString(key).takeIf { it.isNotBlank() }
                if (value != null) candidates += value
            }
            val iterator = json.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val value = json.optString(key).takeIf { it.isNotBlank() }
                if (value != null) candidates += value
            }
            candidates
                .asSequence()
                .mapNotNull { candidate -> GamePlatforms.inferFromUriString(candidate) }
                .firstOrNull()
        }
        .firstOrNull()
}

private fun GameEntry.orderedTargets(): List<LaunchTarget> {
    val preferred = preferredTarget()
    val all = allTargets()
    return if (preferred == null) all else listOf(preferred) + all.filterNot { it.stableKey() == preferred.stableKey() }
}

private fun normalizeCollection(raw: String?): String? =
    raw?.trim()?.takeIf { it.isNotBlank() }

private fun inferPlatformFromIntentDataUri(intentDataUri: String?): String? =
    GamePlatforms.inferFromUriString(intentDataUri)

private fun GameEntry.isRetroArchPreset(): Boolean {
    if (packageName.isRetroArchPackage()) return true
    return allTargets().any { it.packageName.isRetroArchPackage() }
}

private fun String?.supportsRetroArchCleanupOnHome(): Boolean =
    this.equals("MAME Arcade", ignoreCase = true) ||
        this.equals("Final Burn Neo", ignoreCase = true)

private fun hasGenericMameFallbackAsset(availableAssets: Map<String, String>): Boolean =
    listOf("mame.png", "mame.jpg", "mame.jpeg").any { availableAssets.containsKey(it) }

private fun GameEntry.needsMameFallbackArtwork(availableAssets: Map<String, String>): Boolean {
    if (!imageUri.isNullOrBlank()) return false
    if (!platform.supportsMameFallbackArtwork()) return false
    val assetBaseNames = arcadeOverlayAssetBaseNames()
        .ifEmpty { arcadeRomShortNameForArtworkSearch()?.let(::listOf).orEmpty() }
    if (assetBaseNames.isEmpty()) return false
    val matchedAsset = findMatchingArcadeOverlayAssetName(availableAssets, assetBaseNames)
    return matchedAsset == null || isGenericArcadeFallbackAssetName(matchedAsset)
}

private fun String?.supportsMameFallbackArtwork(): Boolean =
    this.equals("MAME Arcade", ignoreCase = true) ||
        this.equals("Final Burn Neo", ignoreCase = true)

private fun GameEntry.normalizeRetroArchTargetForLaunch(
    app: Application,
    target: LaunchTarget
): LaunchTarget {
    val requestedPackage = target.packageName?.takeIf { it.isNotBlank() } ?: return target
    if (!requestedPackage.isRetroArchPackage()) return target
    val resolvedPlatform = resolvePlatform(this)
        ?: GamePlatforms.normalizeOrNull(platform)
        ?: inferPlatformFromIntentDataUri(target.intentDataUri ?: intentDataUri)
    val preferredPackage = when (GamePlatforms.normalizeOrNull(resolvedPlatform)) {
        "MAME Arcade" -> "com.retroarch.aarch64"
        "Final Burn Neo" -> "com.retroarch.aarch64"
        "DOS Games" -> "com.retroarch.aarch64"
        else -> requestedPackage
    }.takeIf(app::isPackageInstalled) ?: requestedPackage
    val normalizedActivity = target.activityName
        ?.takeIf { it.isNotBlank() }
        ?.let { canonicalActivityName(preferredPackage, it) }
        ?: retroArchActivityName(preferredPackage)
    return target.copy(
        packageName = preferredPackage,
        activityName = normalizedActivity
    )
}

private fun GameEntry.buildIntent(app: Application, target: LaunchTarget): Intent {
    val effectiveTarget = normalizeRetroArchTargetForLaunch(app, target)
    val isRetroArchTarget = effectiveTarget.packageName.isRetroArchPackage()
    val isScummVmTarget = effectiveTarget.packageName.isScummVmPackage()
    val isDolphinTarget = effectiveTarget.packageName.isDolphinPackage()
    val extrasObject = effectiveTarget.extrasJson
        ?.takeIf { it.isNotBlank() }
        ?.let(::JSONObject)
    val edenTitleId = extrasObject
        ?.optString(EDEN_EXTRA_TITLE_ID)
        ?.let(::normalizeSwitchBaseTitleId)
    val edenResolutionSetup = extrasObject
        ?.takeIf { it.has(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP) }
        ?.optInt(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP, Int.MIN_VALUE)
        ?.takeIf { it != Int.MIN_VALUE }
    val isEdenCustomConfigLaunch = effectiveTarget.packageName.isEdenPackage() &&
        !edenTitleId.isNullOrBlank() &&
        edenResolutionSetup != null
    if (isEdenCustomConfigLaunch) {
        extrasObject?.put(EDEN_EXTRA_TITLE_ID, edenTitleId)
        extrasObject?.put(
            EDEN_EXTRA_CUSTOM_SETTINGS,
            buildEdenCustomSettings(edenResolutionSetup)
        )
    }
    extrasObject?.remove(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP)
    val registeredScummVmTarget = ScummVmGames.registeredTarget(effectiveTarget.extrasJson)
    extrasObject?.remove(ScummVmGames.REGISTERED_TARGET)
    val action = effectiveTarget.intentAction?.takeIf { it.isNotBlank() }
        ?.let { existingAction ->
            if (isEdenCustomConfigLaunch) EDEN_CUSTOM_CONFIG_ACTION else existingAction
        }
        ?: EDEN_CUSTOM_CONFIG_ACTION.takeIf { isEdenCustomConfigLaunch }
        ?: Intent.ACTION_MAIN.takeIf { isRetroArchTarget }
        ?: effectiveTarget.intentDataUri?.takeIf { it.isNotBlank() }?.let { Intent.ACTION_VIEW }
        ?: Intent.ACTION_MAIN
    val intent = when {
        isScummVmTarget -> {
            val packageName = effectiveTarget.packageName ?: "org.scummvm.scummvm"
            app.packageManager.getLaunchIntentForPackage(packageName)
                ?.apply {
                    // Keep the command payload path/id, but launch through exported entry activity.
                    `package` = null
                }
                ?: Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName(packageName, "$packageName.SplashActivity")
                )
        }
        !effectiveTarget.activityName.isNullOrBlank() && !effectiveTarget.packageName.isNullOrBlank() ->
            Intent(action).setComponent(ComponentName(effectiveTarget.packageName, effectiveTarget.activityName))

        !effectiveTarget.packageName.isNullOrBlank() && action == Intent.ACTION_MAIN ->
            app.packageManager.getLaunchIntentForPackage(effectiveTarget.packageName)?.apply {
                this.action = action
            } ?: Intent(action).setPackage(effectiveTarget.packageName)

        !effectiveTarget.packageName.isNullOrBlank() ->
            Intent(action).setPackage(effectiveTarget.packageName)

        else -> Intent(action)
    }

    val activeAuthorizedTreeUri = authorizedTreeUri ?: app.globalAuthorizedRomTreeUri()
    val normalizedData = effectiveTarget.intentDataUri
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?.normalizeAgainstAuthorizedTree(activeAuthorizedTreeUri)
    val sourceData = normalizedData?.let { uri ->
        if (effectiveTarget.packageName.needsLegacyFileUriLaunch()) {
            uri.toFileUriIfExternalStorageDocument()
        } else {
            uri
        }
    }
    val resolvedRetroArchPlatform = if (effectiveTarget.packageName.isRetroArchPackage()) {
        resolvePlatform(this)
            ?: GamePlatforms.normalizeOrNull(platform)
            ?: inferPlatformFromIntentDataUri(effectiveTarget.intentDataUri ?: intentDataUri)
    } else {
        null
    }
    val resolvedFilePath = sourceData
        .toLaunchFilePath()
        ?.let { rawPath ->
            rawPath
        }
        ?.let { rawPath ->
            maybeExtractLaunchPath(
                app = app,
                sourceUri = normalizedData,
                originalPath = rawPath,
                packageName = effectiveTarget.packageName,
                platform = resolvedRetroArchPlatform
            )
        }
        ?.let { rawPath ->
            if (effectiveTarget.packageName.isRedreamPackage()) normalizeRedreamBootPath(rawPath) else rawPath
        }
        .orEmpty()
    validateLaunchPayload(effectiveTarget.packageName, resolvedFilePath)
    val data = when {
        resolvedFilePath.isBlank() -> sourceData
        sourceData == null -> Uri.fromFile(File(resolvedFilePath))
        effectiveTarget.packageName.needsLegacyFileUriLaunch() -> Uri.fromFile(File(resolvedFilePath))
        else -> sourceData
    }
    val usesDolphinAutoStart = isDolphinTarget &&
        extrasObject?.has("AutoStartFile") == true &&
        resolvedFilePath.isNotBlank()
    val attachDataToIntent = !effectiveTarget.packageName.isCitraPackage() &&
        !effectiveTarget.packageName.isRetroArchPackage() &&
        !effectiveTarget.packageName.isScummVmPackage() &&
        !isEdenCustomConfigLaunch &&
        !usesDolphinAutoStart
    val resolvedMimeType = effectiveTarget.mimeType?.takeIf { it.isNotBlank() }
        ?: inferLaunchMimeType(effectiveTarget.packageName, data)

    if (attachDataToIntent && data != null && !resolvedMimeType.isNullOrBlank()) {
        intent.setDataAndType(data, resolvedMimeType)
    } else if (attachDataToIntent && data != null) {
        intent.data = data
    } else if (!resolvedMimeType.isNullOrBlank()) {
        intent.type = resolvedMimeType
    }

    if (attachDataToIntent && data != null && data.scheme == "content") {
        val clipData = ClipData.newUri(app.contentResolver, title.ifBlank { "game" }, data)
        activeAuthorizedTreeUri
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?.takeIf { treeUri -> data.isDescendantOfTree(treeUri) }
            ?.let { treeUri -> clipData.addItem(ClipData.Item(treeUri)) }
        intent.clipData = clipData
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
    }

    effectiveTarget.categoriesCsv
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.forEach(intent::addCategory)

    if (effectiveTarget.packageName.isRetroArchPackage()) {
        applyRetroArchCoreOverride(
            extrasObject = extrasObject,
            packageName = effectiveTarget.packageName,
            platform = resolvedRetroArchPlatform
        )
        applyRetroArchConfigOverride(
            extrasObject = extrasObject,
            packageName = effectiveTarget.packageName
        )
        applyRetroArchEnvironmentOverride(
            app = app,
            extrasObject = extrasObject,
            packageName = effectiveTarget.packageName
        )
    }
    val explicitScummVmGameId = extrasObject
        ?.optString(SCUMMVM_EXTRA_GAME_ID)
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        if (!explicitScummVmGameId.isNullOrBlank()) {
            extrasObject?.remove(SCUMMVM_EXTRA_GAME_ID)
        }

    extrasObject
        ?.keys()
        ?.forEach { key ->
            val jsonValue = extrasObject.get(key)
            val dataUriString = data?.toString().orEmpty()
            val resolvedValue = when {
                key == "AutoStartFile" && isDolphinTarget && resolvedFilePath.isNotBlank() -> resolvedFilePath
                jsonValue is String -> jsonValue
                    .replace("{dataUri}", dataUriString)
                    .replace("{filePath}", resolvedFilePath)
                    .let { value ->
                        if (
                            key == "AutoStartFile" &&
                            isDolphinTarget &&
                            value.startsWith("content://") &&
                            resolvedFilePath.isNotBlank()
                        ) {
                            resolvedFilePath
                        } else {
                            value
                        }
                    }
                else -> jsonValue
            }
            when (jsonValue) {
                is Boolean -> intent.putExtra(key, resolvedValue as Boolean)
                is Int -> intent.putExtra(key, resolvedValue as Int)
                is Long -> intent.putExtra(key, resolvedValue as Long)
                is Double -> intent.putExtra(key, resolvedValue as Double)
                else -> intent.putExtra(key, resolvedValue.toString())
            }
        }

    if (isScummVmTarget) {
        val sourceUri = data ?: target.intentDataUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val folderPath = sourceUri.toLaunchFilePath()
            ?: sourceUri.toScummVmLaunchPath().orEmpty()
        val detectedGameId = if (registeredScummVmTarget == null && sourceUri?.scheme == "content") {
            runCatching { detectScummVmFolder(app, sourceUri) }.getOrNull()
        } else null
        val requestedId = explicitScummVmGameId ?: detectedGameId
        // Android passes the URI payload as ONE native argument. A folder/--path command is not supported.
        // Only a target from the registered-game picker/config or a published shortcut is safe to autostart.
        val publishedTarget = if (registeredScummVmTarget == null) {
            InstalledTargets(app).loadShortcuts().firstOrNull { shortcut ->
                shortcut.packageName == ScummVmGames.PACKAGE &&
                    shortcut.intentDataUri?.let { Uri.parse(it).schemeSpecificPart } == requestedId
            }?.intentDataUri?.let { Uri.parse(it).schemeSpecificPart }
        } else null
        val gameId = registeredScummVmTarget ?: publishedTarget
            ?: throw IllegalArgumentException("Add the game in ScummVM first, grant ScummVM access to its folder, then use 'Choose registered game' or import its exported scummvm.ini. A folder marker alone is not a registered launch target.")
        intent.data = Uri.fromParts("scummvm", gameId, null)
        Log.d(
            "RetroLauncher",
            "ScummVM launch mode='${if (gameId.isNullOrBlank()) "ui" else "id"}' folder='$folderPath' gameId='${gameId.orEmpty()}' explicit='${explicitScummVmGameId.orEmpty()}' detected='${detectedGameId.orEmpty()}' data='${intent.data}'"
        )
    }

    if (target.packageName.isRetroArchPackage()) {
        Log.d(
            "RetroLauncher",
            "RetroArch launch pkg='${target.packageName.orEmpty()}' activity='${target.activityName.orEmpty()}' platform='${resolvedRetroArchPlatform.orEmpty()}' rom='${intent.getStringExtra("ROM").orEmpty()}' libretro='${intent.getStringExtra("LIBRETRO").orEmpty()}' config='${intent.getStringExtra("CONFIGFILE").orEmpty()}'"
        )
    } else if (isDolphinTarget) {
        Log.d(
            "RetroLauncher",
            "Dolphin launch pkg='${target.packageName.orEmpty()}' activity='${target.activityName.orEmpty()}' data='${intent.data}' autoStart='${intent.getStringExtra("AutoStartFile").orEmpty()}' resolvedFile='${resolvedFilePath}' source='${normalizedData}'"
        )
    } else if (target.packageName?.startsWith("com.explusalpha.") == true) {
        Log.d(
            "RetroLauncher",
            "ExPlusAlpha launch pkg='${target.packageName}' activity='${target.activityName.orEmpty()}' data='${intent.data}' type='${intent.type.orEmpty()}' resolvedFile='${resolvedFilePath}' source='${normalizedData}'"
        )
    }

    return intent
}

private fun applyRetroArchCoreOverride(
    extrasObject: JSONObject?,
    packageName: String?,
    platform: String?
) {
    if (extrasObject == null) return
    val pkg = packageName?.takeIf { it.isNotBlank() } ?: return
    val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return
    val coreSoFile = when (normalizedPlatform) {
        "DOS Games" -> "dosbox_pure_libretro_android.so"
        "Nintendo Entertainment System" -> "mesen_libretro_android.so"
        "Super Nintendo" -> "snes9x_libretro_android.so"
        "Game Boy" -> "gambatte_libretro_android.so"
        "Game Boy Color" -> "gambatte_libretro_android.so"
        "Game Boy Advance" -> "mgba_libretro_android.so"
        "Sega Saturn" -> "yabasanshiro_libretro_android.so"
        "MAME Arcade" -> "mame2003_plus_libretro_android.so"
        "Nintendo 64" -> "mupen64plus_next_gles3_libretro_android.so"
        "Nintendo DS" -> "melonds_libretro_android.so"
        "Sony PlayStation" -> "beetle_psx_hw_libretro_android.so"
        "Sony PlayStation Portable" -> "ppsspp_libretro_android.so"
        "Sega Genesis / Mega Drive" -> "genesis_plus_gx_libretro_android.so"
        "Sega Dreamcast" -> "flycast_libretro_android.so"
        "Final Burn Neo" -> "fbneo_libretro_android.so"
        else -> null
    }
    val expected = coreSoFile?.let { "/data/data/$pkg/cores/$it" } ?: return
    val current = extrasObject.optString("LIBRETRO").orEmpty()
    if (!current.equals(expected, ignoreCase = true)) {
        extrasObject.put("LIBRETRO", expected)
    }
}

private fun applyRetroArchConfigOverride(
    extrasObject: JSONObject?,
    packageName: String?
) {
    if (extrasObject == null) return
    val pkg = packageName?.takeIf { it.isNotBlank() } ?: return
    val expected = "/storage/emulated/0/Android/data/$pkg/files/retroarch.cfg"
    val current = extrasObject.optString("CONFIGFILE").orEmpty()
    if (!current.equals(expected, ignoreCase = true)) {
        extrasObject.put("CONFIGFILE", expected)
    }
}

private fun applyRetroArchEnvironmentOverride(
    app: Application,
    extrasObject: JSONObject?,
    packageName: String?
) {
    if (extrasObject == null) return
    val pkg = packageName?.takeIf { it.isNotBlank() } ?: return
    val appInfo = runCatching {
        app.packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
    }.getOrNull() ?: return

    val sdCardPath = Environment.getExternalStorageDirectory().absolutePath
    val externalFilesPath = "$sdCardPath/Android/data/$pkg/files"
    val imeId = runCatching {
        Settings.Secure.getString(app.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }.getOrNull().orEmpty()

    applyRetroArchStringExtra(extrasObject, "DATADIR", appInfo.dataDir.orEmpty())
    applyRetroArchStringExtra(extrasObject, "APK", appInfo.sourceDir.orEmpty())
    applyRetroArchStringExtra(extrasObject, "SDCARD", sdCardPath)
    applyRetroArchStringExtra(extrasObject, "EXTERNAL", externalFilesPath)
    if (imeId.isNotBlank()) {
        applyRetroArchStringExtra(extrasObject, "IME", imeId)
    }
}

private fun applyRetroArchStringExtra(
    extrasObject: JSONObject,
    key: String,
    expected: String
) {
    if (expected.isBlank()) return
    val current = extrasObject.optString(key).orEmpty()
    if (!current.equals(expected, ignoreCase = true)) {
        extrasObject.put(key, expected)
    }
}

private fun maybeExtractLaunchPath(
    app: Application,
    sourceUri: Uri?,
    originalPath: String,
    packageName: String?,
    platform: String?
): String {
    return maybeExtractRetroArchLaunchPath(
        app = app,
        sourceUri = sourceUri,
        originalPath = originalPath,
        packageName = packageName,
        platform = platform
    )?.let { extracted ->
        if (!extracted.equals(originalPath, ignoreCase = true)) return extracted
        maybeExtractStandaloneLaunchPath(
            app = app,
            sourceUri = sourceUri,
            originalPath = originalPath,
            packageName = packageName
        ) ?: extracted
    } ?: maybeExtractStandaloneLaunchPath(
        app = app,
        sourceUri = sourceUri,
        originalPath = originalPath,
        packageName = packageName
    ) ?: originalPath
}

private fun maybeExtractRetroArchLaunchPath(
    app: Application,
    sourceUri: Uri?,
    originalPath: String,
    packageName: String?,
    platform: String?
): String {
    if (!packageName.isRetroArchPackage()) return originalPath
    val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return originalPath
    if (!originalPath.endsWith(".zip", ignoreCase = true)) return originalPath
    return when (normalizedPlatform) {
        "Super Nintendo" -> extractRomFromZipToCache(
            app = app,
            sourceUri = sourceUri,
            originalPath = originalPath,
            cacheFolder = "retroarch-snes",
            romExtensions = setOf(".sfc", ".smc", ".swc", ".fig"),
            fallbackExtension = ".sfc"
        )
        "Game Boy" -> extractRomFromZipToCache(
            app = app,
            sourceUri = sourceUri,
            originalPath = originalPath,
            cacheFolder = "retroarch-gb",
            romExtensions = setOf(".gb", ".sgb"),
            fallbackExtension = ".gb"
        )
        "Game Boy Color" -> extractRomFromZipToCache(
            app = app,
            sourceUri = sourceUri,
            originalPath = originalPath,
            cacheFolder = "retroarch-gbc",
            romExtensions = setOf(".gbc", ".cgb", ".gb"),
            fallbackExtension = ".gbc"
        )
        else -> originalPath
    } ?: originalPath
}

private fun maybeExtractStandaloneLaunchPath(
    app: Application,
    sourceUri: Uri?,
    originalPath: String,
    packageName: String?
): String? {
    return when (packageName) {
        "com.explusalpha.GbcEmu" -> resolveGbcEmuLaunchPath(
            app = app,
            sourceUri = sourceUri,
            originalPath = originalPath
        )
        else -> null
    }
}

private data class ResolvedLaunchSource(
    val path: String,
    val uri: Uri?
)

private fun resolveGbcEmuLaunchPath(
    app: Application,
    sourceUri: Uri?,
    originalPath: String
): String? {
    val resolvedSource = resolveGbcEmuSource(app, sourceUri, originalPath)
    val launchPath = if (resolvedSource.path.endsWith(".zip", ignoreCase = true)) {
        extractRomFromZipToSiblingStorage(
            app = app,
            sourceUri = resolvedSource.uri,
            originalPath = resolvedSource.path,
            romExtensions = setOf(".gbc", ".cgb", ".gb", ".sgb"),
            fallbackExtension = ".gbc"
        )
    } else {
        resolvedSource.path
    } ?: return null
    return launchPath.takeIf { !it.equals(originalPath, ignoreCase = true) }
}

private fun resolveGbcEmuSource(
    app: Application,
    sourceUri: Uri?,
    originalPath: String
): ResolvedLaunchSource {
    val original = ResolvedLaunchSource(originalPath, sourceUri)
    val baseName = File(originalPath).name.removeGbcSaveStateSuffix() ?: return original
    val allowedExtensions = listOf(".gbc", ".cgb", ".gb", ".sgb", ".zip")

    val documentSibling = findSiblingDocumentLaunchSource(
        app = app,
        sourceUri = sourceUri,
        romBaseName = baseName,
        allowedExtensions = allowedExtensions
    )
    if (documentSibling != null) return documentSibling

    val fileSibling = findSiblingFileLaunchSource(
        originalPath = originalPath,
        romBaseName = baseName,
        allowedExtensions = allowedExtensions
    )
    return fileSibling ?: original
}

private fun String.removeGbcSaveStateSuffix(): String? {
    val trimmed = trim()
    if (!trimmed.endsWith(".gqs", ignoreCase = true)) return null
    return trimmed
        .replace(Regex("""(?i)\.[0-9a-f]{2}\.gqs$"""), "")
        .replace(Regex("""(?i)\.gqs$"""), "")
        .takeIf { it.isNotBlank() }
}

private fun findSiblingDocumentLaunchSource(
    app: Application,
    sourceUri: Uri?,
    romBaseName: String,
    allowedExtensions: List<String>
): ResolvedLaunchSource? {
    val source = sourceUri?.takeIf { it.scheme == "content" } ?: return null
    val authority = source.authority ?: return null
    if (authority != "com.android.externalstorage.documents") return null
    val documentId = runCatching { DocumentsContract.getDocumentId(source) }.getOrNull() ?: return null
    val root = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
    val parentRelativePath = relativePath.substringBeforeLast('/', "")
    val parentDocumentId = if (parentRelativePath.isBlank()) root else "$root:$parentRelativePath"
    val parentTreeUri = runCatching {
        DocumentsContract.buildTreeDocumentUri(authority, parentDocumentId)
    }.getOrNull() ?: return null
    val parentDir = runCatching { DocumentFile.fromTreeUri(app, parentTreeUri) }.getOrNull() ?: return null
    val candidate = runCatching { parentDir.listFiles().asSequence() }
        .getOrDefault(emptySequence())
        .filterNot(DocumentFile::isDirectory)
        .sortedBy { file ->
            val extension = allowedExtensions.indexOfFirst { ext ->
                file.name?.endsWith(ext, ignoreCase = true) == true
            }
            if (extension == -1) Int.MAX_VALUE else extension
        }
        .firstOrNull { file ->
            val name = file.name.orEmpty()
            allowedExtensions.any { ext -> name.endsWith(ext, ignoreCase = true) } &&
                name.substringBeforeLast('.', name).equals(romBaseName, ignoreCase = true)
        } ?: return null
    val path = candidate.uri.toExternalStoragePathFromDocumentUri()
        ?: candidate.uri.toFileUriIfExternalStorageDocument().path?.let(Uri::decode)
        ?: return null
    return ResolvedLaunchSource(path, candidate.uri)
}

private fun findSiblingFileLaunchSource(
    originalPath: String,
    romBaseName: String,
    allowedExtensions: List<String>
): ResolvedLaunchSource? {
    val sourceFile = File(originalPath)
    val parent = sourceFile.parentFile ?: return null
    val candidate = parent.listFiles()
        ?.asSequence()
        ?.filter(File::isFile)
        ?.sortedBy { file ->
            val extension = allowedExtensions.indexOfFirst { ext ->
                file.name.endsWith(ext, ignoreCase = true)
            }
            if (extension == -1) Int.MAX_VALUE else extension
        }
        ?.firstOrNull { file ->
            allowedExtensions.any { ext -> file.name.endsWith(ext, ignoreCase = true) } &&
                file.name.substringBeforeLast('.', file.name).equals(romBaseName, ignoreCase = true)
        } ?: return null
    return ResolvedLaunchSource(candidate.absolutePath, Uri.fromFile(candidate))
}

private fun extractRomFromZipToSiblingStorage(
    app: Application,
    sourceUri: Uri?,
    originalPath: String,
    romExtensions: Set<String>,
    fallbackExtension: String
): String? {
    val sourceStream = runCatching {
        when {
            sourceUri?.scheme == "content" -> app.contentResolver.openInputStream(sourceUri)
            else -> File(originalPath).inputStream()
        }
    }.getOrNull() ?: return null

    return sourceStream.use { input ->
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            val entry = generateSequence { zip.nextEntry }
                .firstOrNull { candidate ->
                    !candidate.isDirectory && romExtensions.any { candidate.name.endsWith(it, ignoreCase = true) }
                } ?: return null
            val extension = romExtensions.firstOrNull { entry.name.endsWith(it, ignoreCase = true) } ?: fallbackExtension
            val extractedName = "${File(originalPath).nameWithoutExtension}.__rl$extension"

            val siblingUri = findOrCreateSiblingDocumentUri(
                app = app,
                sourceUri = sourceUri,
                displayName = extractedName
            )
            if (siblingUri != null) {
                app.contentResolver.openOutputStream(siblingUri, "wt")?.use { output ->
                    zip.copyTo(output)
                } ?: return null
                return siblingUri.toExternalStoragePathFromDocumentUri()
                    ?: siblingUri.toFileUriIfExternalStorageDocument().path?.let(Uri::decode)
            }

            val parent = File(originalPath).parentFile ?: return null
            val extractedFile = File(parent, extractedName)
            FileOutputStream(extractedFile).use { output -> zip.copyTo(output) }
            extractedFile.absolutePath
        }
    }
}

private fun findOrCreateSiblingDocumentUri(
    app: Application,
    sourceUri: Uri?,
    displayName: String
): Uri? {
    val source = sourceUri?.takeIf { it.scheme == "content" } ?: return null
    val authority = source.authority ?: return null
    if (authority != "com.android.externalstorage.documents") return null
    val documentId = runCatching { DocumentsContract.getDocumentId(source) }.getOrNull() ?: return null
    val root = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
    val parentRelativePath = relativePath.substringBeforeLast('/', "")
    val parentDocumentId = if (parentRelativePath.isBlank()) root else "$root:$parentRelativePath"
    val parentTreeUri = runCatching {
        DocumentsContract.buildTreeDocumentUri(authority, parentDocumentId)
    }.getOrNull() ?: return null
    val parentDir = runCatching { DocumentFile.fromTreeUri(app, parentTreeUri) }.getOrNull() ?: return null
    val existing = runCatching { parentDir.listFiles().asSequence() }
        .getOrDefault(emptySequence())
        .firstOrNull { file -> !file.isDirectory && file.name == displayName }
    if (existing != null) return existing.uri
    return runCatching {
        parentDir.createFile("application/octet-stream", displayName)?.uri
    }.getOrNull()
}

private fun extractRomFromZipToCache(
    app: Application,
    sourceUri: Uri?,
    originalPath: String,
    cacheFolder: String,
    romExtensions: Set<String>,
    fallbackExtension: String
): String? {
    val cacheRoot = File(app.cacheDir, cacheFolder).apply { mkdirs() }
    val zipKey = originalPath.lowercase().hashCode().toString(16)
    val sourceStream = runCatching {
        when {
            sourceUri?.scheme == "content" -> app.contentResolver.openInputStream(sourceUri)
            else -> File(originalPath).inputStream()
        }
    }.getOrNull() ?: return null

    return sourceStream.use { input ->
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { entry ->
                    !entry.isDirectory && romExtensions.any { entry.name.endsWith(it, ignoreCase = true) }
                }
                ?.let { entry ->
                    val extension = romExtensions.firstOrNull { entry.name.endsWith(it, ignoreCase = true) } ?: fallbackExtension
                    val extracted = File(cacheRoot, "${zipKey}${extension}")
                    FileOutputStream(extracted).use { output -> zip.copyTo(output) }
                    extracted.absolutePath
                }
        }
    }
}

private fun Uri?.toScummVmLaunchPath(): String? {
    val source = this ?: return null
    if (source.scheme == "content") {
        return Uri.decode(source.toString()).takeIf { it.isNotBlank() }
    }
    return source.toLaunchFilePath()
}

private fun String?.isRetroArchPackage(): Boolean = this == "com.retroarch" || this == "com.retroarch.aarch64"

private fun String?.isPpssppPackage(): Boolean = this == "org.ppsspp.ppsspp"

private fun String?.isCitraPackage(): Boolean = this == "org.citra.emu"

private fun String?.isDolphinPackage(): Boolean =
    this == "org.dolphinemu.dolphinemu" || this == "org.dolphinemu.mmjr"

private fun String?.isRedreamPackage(): Boolean = this == "io.recompiled.redream"

private fun validateLaunchPayload(packageName: String?, resolvedFilePath: String) {
    val lowerName = File(resolvedFilePath).name.lowercase()
    when {
        packageName.isDolphinPackage() && resolvedFilePath.isNotBlank() -> {
            if (
                lowerName.endsWith(".wux") ||
                lowerName.endsWith(".wua") ||
                lowerName.endsWith(".wud") ||
                lowerName.endsWith(".rpx")
            ) {
                throw IllegalArgumentException(
                    "Dolphin only supports GameCube/Wii files. '$lowerName' is a Wii U dump, so use the GameCube Wind Waker or a Wii U emulator instead."
                )
            }
        }

        packageName.isRedreamPackage() && resolvedFilePath.isNotBlank() -> {
            val isSupported = lowerName.endsWith(".cdi") ||
                lowerName.endsWith(".chd") ||
                lowerName.endsWith(".cue") ||
                lowerName.endsWith(".gdi")
            if (!isSupported) {
                throw IllegalArgumentException(
                    "Redream direct launch needs a .gdi, .cue, .chd, or .cdi file. If this is a multi-track dump, pick the descriptor file instead of a raw track like .bin."
                )
            }
        }
    }
}

private fun String?.isDrasticPackage(): Boolean = this == "com.dsemu.drastic"

private fun String?.isScummVmPackage(): Boolean = this == "org.scummvm.scummvm"

private fun String?.isEdenPackage(): Boolean = this == EDEN_PACKAGE_NAME

private fun buildEdenCustomSettings(resolutionSetup: Int): String =
    buildString {
        appendLine("[Renderer]")
        appendLine("resolution_setup\\use_global = false")
        appendLine("resolution_setup\\default = false")
        append("resolution_setup = ")
        append(resolutionSetup)
        appendLine()
    }

private const val SCUMMVM_EXTRA_GAME_ID = "scummvmGameId"
private const val EDEN_PACKAGE_NAME = "dev.eden.eden_emulator"
private const val EDEN_CUSTOM_CONFIG_ACTION = "dev.eden.eden_emulator.LAUNCH_WITH_CUSTOM_CONFIG"
private const val EDEN_EXTRA_TITLE_ID = "title_id"
private const val EDEN_EXTRA_CUSTOM_SETTINGS = "custom_settings"
private const val RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP = "_retroLauncherEdenResolutionSetup"
private const val BACKGROUND_ARTWORK_AUTOFILL_CONCURRENCY = 3

private fun normalizeRedreamBootPath(rawPath: String): String {
    val trimmed = rawPath.trim()
    if (trimmed.isBlank()) return rawPath
    val candidate = File(trimmed)
    val lowerName = candidate.name.lowercase()
    if (
        lowerName.endsWith(".cdi") ||
        lowerName.endsWith(".chd") ||
        lowerName.endsWith(".cue") ||
        lowerName.endsWith(".gdi")
    ) {
        return trimmed
    }
    return resolveDreamcastDescriptor(candidate)?.absolutePath ?: trimmed
}

private fun resolveDreamcastDescriptor(trackFile: File): File? {
    val parent = trackFile.parentFile ?: return null
    val descriptors = parent.listFiles()
        ?.asSequence()
        ?.filter { it.isFile }
        ?.filter { file ->
            val lowerName = file.name.lowercase()
            lowerName.endsWith(".gdi") || lowerName.endsWith(".cue")
        }
        ?.toList()
        .orEmpty()
    if (descriptors.isEmpty()) return null

    val exactStemMatch = descriptors.firstOrNull { descriptor ->
        descriptor.name.substringBeforeLast('.', descriptor.name)
            .equals(trackFile.name.substringBeforeLast('.', trackFile.name), ignoreCase = true)
    }
    if (exactStemMatch != null) return exactStemMatch

    val referencedMatch = descriptors.firstOrNull { descriptor ->
        descriptorReferencesDreamcastTrack(descriptor, trackFile.name)
    }
    if (referencedMatch != null) return referencedMatch

    return descriptors.singleOrNull { it.name.endsWith(".gdi", ignoreCase = true) }
        ?: descriptors.singleOrNull { it.name.endsWith(".cue", ignoreCase = true) }
}

private fun descriptorReferencesDreamcastTrack(descriptor: File, trackName: String): Boolean {
    val normalizedTrack = trackName.trim().lowercase()
    if (normalizedTrack.isBlank()) return false
    return runCatching {
        descriptor.useLines { lines ->
            lines.any { rawLine ->
                val line = rawLine.trim().lowercase()
                line.contains(normalizedTrack)
            }
        }
    }.getOrDefault(false)
}

private fun String?.needsLegacyFileUriLaunch(): Boolean {
    val packageName = this ?: return false
    return packageName in setOf(
        "org.ppsspp.ppsspp",
        "org.citra.emu",
        "com.dsemu.drastic",
        "com.PceEmu",
        "com.explusalpha.NesEmu",
        "com.explusalpha.NeoEmu",
        "com.github.stenzek.duckstation",
        "io.recompiled.redream",
        "com.flycast.emulator",
        "com.explusalpha.GbcEmu",
        "com.explusalpha.GbaEmu",
        "com.explusalpha.Snes9xPlus",
        "com.explusalpha.NgpEmu",
        "com.explusalpha.MdEmu",
        "xyz.aethersx2.android"
    )
}

private fun inferLaunchMimeType(packageName: String?, data: Uri?): String? {
    if (packageName?.startsWith("com.explusalpha.") != true && packageName != "com.PceEmu") return null
    val path = data?.toString().orEmpty()
    return "application/zip".takeIf { path.endsWith(".zip", ignoreCase = true) }
}

private fun LaunchTarget.hasExplicitLaunchPayload(): Boolean =
    !intentAction.isNullOrBlank() ||
        !intentDataUri.isNullOrBlank() ||
        !mimeType.isNullOrBlank() ||
        !categoriesCsv.isNullOrBlank() ||
        !extrasJson.isNullOrBlank()

private fun launchPayloadActivity(
    app: Application,
    game: GameEntry,
    target: LaunchTarget,
    onError: (String) -> Unit
): Boolean {
    grantUriPermissionsIfNeeded(app, game, target)
    val explicitAttempt = runCatching {
        app.startActivity(game.buildIntent(app, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }
    if (explicitAttempt.isSuccess) {
        return true
    }
    val explicitError = explicitAttempt.exceptionOrNull()
        ?.let { it.message ?: it.javaClass.simpleName }
        ?: "unknown explicit activity error"

    val fallbackTarget = target.copy(activityName = null)
    grantUriPermissionsIfNeeded(app, game, fallbackTarget)
    return runCatching {
        app.startActivity(game.buildIntent(app, fallbackTarget).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.onFailure { error ->
        val fallbackError = error.message ?: error.javaClass.simpleName
        onError(
            "Launch failed for ${game.title}: explicit=${target.activityName ?: "<none>"} -> $explicitError; " +
                "fallback=${target.packageName ?: "<none>"} -> $fallbackError"
        )
    }.getOrDefault(false)
}

private fun grantUriPermissionsIfNeeded(app: Application, game: GameEntry, target: LaunchTarget) {
    val packageName = target.packageName?.takeIf { it.isNotBlank() } ?: return
    target.intentDataUri
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?.normalizeAgainstAuthorizedTree(game.authorizedTreeUri)
        ?.let { uri ->
            runCatching {
                app.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    (game.authorizedTreeUri ?: app.globalAuthorizedRomTreeUri())
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?.takeIf { treeUri ->
            target.intentDataUri
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?.normalizeAgainstAuthorizedTree(game.authorizedTreeUri ?: app.globalAuthorizedRomTreeUri())
                ?.isDescendantOfTree(treeUri) == true
        }
        ?.let { treeUri ->
            runCatching {
                app.grantUriPermission(
                    packageName,
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            }
        }
}

private fun Application.globalAuthorizedRomTreeUri(): String? =
    getSharedPreferences("retro_launcher", android.content.Context.MODE_PRIVATE)
        .getString("authorized_rom_tree_uri", null)
        ?.ifBlank { null }

private fun Application.findRestorableRomTreeUri(): String? {
    return contentResolver.persistedUriPermissions
        .asSequence()
        .filter { it.isReadPermission }
        .map { it.uri }
        .firstOrNull { uri ->
            uri.authority == "com.android.externalstorage.documents" &&
                runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() != null
        }
        ?.toString()
}

private fun Application.hasPersistedReadPermissionForTree(uriString: String?): Boolean {
    val parsed = uriString?.takeIf { it.isNotBlank() }?.let {
        runCatching { Uri.parse(it) }.getOrNull()
    } ?: return false
    val parsedTreeId = runCatching { DocumentsContract.getTreeDocumentId(parsed) }
        .getOrElse { runCatching { DocumentsContract.getDocumentId(parsed) }.getOrNull() }
    return contentResolver.persistedUriPermissions.any { permission ->
        if (!permission.isReadPermission) return@any false
        if (permission.uri == parsed || permission.uri.toString() == parsed.toString()) return@any true
        if (permission.uri.authority != parsed.authority) return@any false
        val permissionTreeId = runCatching { DocumentsContract.getTreeDocumentId(permission.uri) }
            .getOrElse { runCatching { DocumentsContract.getDocumentId(permission.uri) }.getOrNull() }
        permissionTreeId != null && permissionTreeId == parsedTreeId
    }
}

private fun startIntentWithFileUriSupport(app: Application, packageName: String?, intent: Intent) {
    if (packageName.needsLegacyFileUriLaunch() && intent.data?.scheme == "file") {
        val previousPolicy = StrictMode.getVmPolicy()
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
        try {
            app.startActivity(intent)
        } finally {
            StrictMode.setVmPolicy(previousPolicy)
        }
    } else {
        app.startActivity(intent)
    }
}

private fun fallbackToLauncherComponentIntent(
    app: Application,
    packageName: String?,
    baseIntent: Intent
): Intent? {
    val pkg = packageName?.takeIf { it.isNotBlank() } ?: return null
    if (baseIntent.action != Intent.ACTION_VIEW || baseIntent.data == null) return null
    val launcherComponent = app.packageManager.getLaunchIntentForPackage(pkg)?.component ?: return null
    return Intent(baseIntent).apply {
        component = launcherComponent
        `package` = null
    }
}

private fun fallbackToPackageOnlyIntent(target: LaunchTarget, baseIntent: Intent): Intent? {
    if (target.activityName.isNullOrBlank()) return null
    val pkg = target.packageName?.takeIf { it.isNotBlank() } ?: return null
    return Intent(baseIntent).apply {
        component = null
        setPackage(pkg)
    }
}

private fun startRetroArchIntentWithFallbacks(
    app: Application,
    target: LaunchTarget,
    baseIntent: Intent
) {
    val packageName = target.packageName?.takeIf { it.isNotBlank() } ?: run {
        app.startActivity(baseIntent)
        return
    }
    val primaryActivity = target.activityName
        ?.takeIf { it.isNotBlank() }
        ?.let { canonicalActivityName(packageName, it) }
        ?: retroArchActivityName(packageName)
    val secondaryActivity = retroArchActivityName(packageName)

    val primaryIntent = Intent(baseIntent).apply {
        component = ComponentName(packageName, primaryActivity)
        `package` = null
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    if (shouldUseRetroArchTwoStageLaunch(packageName, primaryIntent)) {
        startRetroArchTwoStageLaunch(app, packageName, primaryIntent)
        return
    }
    try {
        startIntentWithFileUriSupport(app, packageName, primaryIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Try one canonical fallback activity.
    }

    if (!primaryActivity.equals(secondaryActivity, ignoreCase = true)) {
        val secondaryIntent = Intent(baseIntent).apply {
            component = ComponentName(packageName, secondaryActivity)
            `package` = null
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            startIntentWithFileUriSupport(app, packageName, secondaryIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to package fallback.
        }
    }

    val packageOnlyIntent = Intent(baseIntent).apply {
        component = null
        setPackage(packageName)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    startIntentWithFileUriSupport(app, packageName, packageOnlyIntent)
}

private fun shouldUseRetroArchTwoStageLaunch(packageName: String, launchIntent: Intent): Boolean {
    val rom = launchIntent.getStringExtra("ROM").orEmpty()
    val libretro = launchIntent.getStringExtra("LIBRETRO").orEmpty()
    val shouldUse = packageName == "com.retroarch" &&
        libretro.contains("/data/user/0/$packageName/cores/", ignoreCase = true) &&
        rom.contains("/retroarch-snes/", ignoreCase = true)
    if (packageName.isRetroArchPackage()) {
        Log.d(
            "RetroLauncher",
            "RetroArch two-stage decision pkg='$packageName' shouldUse='$shouldUse' rom='$rom' libretro='$libretro'"
        )
    }
    return shouldUse
}

private fun startRetroArchTwoStageLaunch(
    app: Application,
    packageName: String,
    finalIntent: Intent
) {
    val bootstrapIntent = Intent(finalIntent).apply {
        component = ComponentName(packageName, retroArchActivityName(packageName))
        `package` = null
        removeExtra("ROM")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val contentIntent = Intent(finalIntent).apply {
        component = ComponentName(packageName, retroArchActivityName(packageName))
        `package` = null
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    Log.d(
        "RetroLauncher",
        "RetroArch two-stage launch pkg='$packageName' bootstrap='${bootstrapIntent.component}' bootstrapRom='${bootstrapIntent.getStringExtra("ROM").orEmpty()}' contentRom='${contentIntent.getStringExtra("ROM").orEmpty()}' libretro='${contentIntent.getStringExtra("LIBRETRO").orEmpty()}'"
    )
    startIntentWithFileUriSupport(app, packageName, bootstrapIntent)
    Handler(Looper.getMainLooper()).postDelayed({
        Log.d(
            "RetroLauncher",
            "RetroArch two-stage content handoff pkg='$packageName' target='${contentIntent.component}' rom='${contentIntent.getStringExtra("ROM").orEmpty()}' libretro='${contentIntent.getStringExtra("LIBRETRO").orEmpty()}'"
        )
        runCatching {
            startIntentWithFileUriSupport(app, packageName, contentIntent)
        }.onFailure { error ->
            Log.w("RetroLauncher", "RetroArch two-stage content handoff failed", error)
        }
    }, 1200L)
}

private fun canonicalActivityName(packageName: String, activityName: String): String {
    val expanded = if (activityName.startsWith('.')) "$packageName$activityName" else activityName
    return if (
        packageName.isRetroArchPackage() &&
        expanded.endsWith(".browser.retroactivity.RetroActivityFuture", ignoreCase = true)
    ) {
        retroArchActivityName(packageName)
    } else {
        expanded
    }
}

private fun retroArchActivityName(packageName: String): String =
    if (packageName.isRetroArchPackage()) {
        "com.retroarch.browser.retroactivity.RetroActivityFuture"
    } else {
        "$packageName.browser.retroactivity.RetroActivityFuture"
    }

private fun Application.isPackageInstalled(packageName: String): Boolean = runCatching {
    packageManager.getPackageInfo(packageName, 0)
}.isSuccess

private fun GameEntry.promoteTarget(target: LaunchTarget): GameEntry {
    val currentTargets = allTargets()
    if (currentTargets.none { it.stableKey() == target.stableKey() }) return this
    val reordered = listOf(target) + currentTargets.filterNot { it.stableKey() == target.stableKey() }
    val primary = reordered.first()
    return copy(
        sourceType = primary.sourceType,
        subtitle = primary.label.ifBlank { subtitle },
        packageName = primary.packageName,
        activityName = primary.activityName,
        shortcutId = primary.shortcutId,
        userSerial = primary.userSerial,
        intentAction = primary.intentAction,
        intentDataUri = primary.intentDataUri,
        mimeType = primary.mimeType,
        categoriesCsv = primary.categoriesCsv,
        extrasJson = primary.extrasJson,
        alternateTargetsJson = reordered.drop(1).takeIf { it.isNotEmpty() }?.toJsonString(),
        preferredTargetKey = primary.stableKey()
    )
}

private fun InstalledShortcutOption.resolvedIntentDataUri(): String? =
    intentDataUri ?: shortcutId.takeIf(::looksLikeLaunchPayload)

private fun looksLikeLaunchPayload(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isBlank()) return false
    return normalized.startsWith("content://") ||
        normalized.startsWith("file://") ||
        normalized.startsWith("/") ||
        normalized.contains("%3A") ||
        normalized.contains("%2F") ||
        normalized.contains(".nsp", ignoreCase = true) ||
        normalized.contains(".xci", ignoreCase = true) ||
        normalized.contains(".gcm", ignoreCase = true) ||
        normalized.contains(".gcz", ignoreCase = true) ||
        normalized.contains(".iso", ignoreCase = true) ||
        normalized.contains(".wbfs", ignoreCase = true) ||
        normalized.contains(".rvz", ignoreCase = true) ||
        normalized.contains(".wia", ignoreCase = true) ||
        normalized.contains(".ciso", ignoreCase = true) ||
        normalized.contains(".dol", ignoreCase = true) ||
        normalized.contains(".elf", ignoreCase = true)
}

private fun Uri.normalizeAgainstAuthorizedTree(treeUriString: String?): Uri {
    val treeUri = treeUriString?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return this
    val authorityMatches = authority == treeUri.authority
    if (!authorityMatches) return this
    val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull() ?: return this
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return this
    val normalizedDocumentId = documentId.rewriteForTree(treeDocumentId)
    if (!normalizedDocumentId.isDescendantOf(treeDocumentId)) return this
    return runCatching {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, normalizedDocumentId)
    }.getOrDefault(this)
}

private fun Uri.isDescendantOfTree(treeUri: Uri): Boolean {
    if (authority != treeUri.authority) return false
    val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull() ?: return false
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return false
    return documentId.isDescendantOf(treeDocumentId)
}

private fun String.isDescendantOf(treeDocumentId: String): Boolean {
    val documentRoot = substringBefore(':', missingDelimiterValue = this)
    val documentPath = substringAfter(':', missingDelimiterValue = "")
    val treeRoot = treeDocumentId.substringBefore(':', missingDelimiterValue = treeDocumentId)
    val treePath = treeDocumentId.substringAfter(':', missingDelimiterValue = "")
    if (documentRoot != treeRoot) return false
    if (treePath.isBlank()) return true
    if (documentPath.isBlank()) return false
    return documentPath.equals(treePath, ignoreCase = true) ||
        documentPath.startsWith("$treePath/", ignoreCase = true)
}

private fun normalizeAuthorizedRomTreeUri(uriString: String?): String? {
    val raw = uriString?.takeIf { it.isNotBlank() } ?: return null
    val parsed = runCatching { Uri.parse(raw) }.getOrNull() ?: return raw
    val authority = parsed.authority ?: return raw
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(parsed) }
        .getOrElse { runCatching { DocumentsContract.getDocumentId(parsed) }.getOrNull() }
        ?: return raw
    val volume = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val path = documentId.substringAfter(':', "")
    if (path.isBlank()) return raw
    val leaf = path.substringAfterLast('/', path)
    val leafLooksLikeFile = leaf.contains('.') && leaf != "." && leaf != ".."
    if (!leafLooksLikeFile) return raw
    val parentPath = path.substringBeforeLast('/', "")
    val parentDocumentId = if (parentPath.isBlank()) volume else "$volume:$parentPath"
    return runCatching {
        DocumentsContract.buildTreeDocumentUri(authority, parentDocumentId).toString()
    }.getOrDefault(raw)
}

private fun String.rewriteForTree(treeDocumentId: String): String {
    val documentRoot = substringBefore(':', missingDelimiterValue = this)
    val documentPath = substringAfter(':', missingDelimiterValue = "")
    val treeRoot = treeDocumentId.substringBefore(':', missingDelimiterValue = treeDocumentId)
    val treePath = treeDocumentId.substringAfter(':', missingDelimiterValue = "")
    if (documentRoot == treeRoot) return this
    if (documentPath.isBlank() || treePath.isBlank()) return this
    if (!documentPath.startsWith(treePath, ignoreCase = true)) return this
    return "$treeRoot:$documentPath"
}

private fun Uri.toFileUriIfExternalStorageDocument(): Uri {
    if (scheme != "content") return this
    if (authority != "com.android.externalstorage.documents") return this
    val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull()
        ?: extractDocumentIdFromUriPath()
        ?: return this
    val root = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "").trimStart('/')
    if (relativePath.isBlank()) return this
    val basePath = if (root.equals("primary", ignoreCase = true)) {
        "/storage/emulated/0"
    } else {
        "/storage/$root"
    }
    return Uri.parse("file://$basePath/$relativePath")
}

private fun Uri?.toLaunchFilePath(): String? {
    val source = this ?: return null
    return when (source.scheme) {
        "file" -> source.path?.let(Uri::decode)
        "content" -> {
            source.toExternalStoragePathFromDocumentUri()
                ?: source.toFileUriIfExternalStorageDocument().path?.let(Uri::decode)
        }
        else -> source.path?.let(Uri::decode)
    }?.takeIf { it.isNotBlank() }
}

private fun Uri.toExternalStoragePathFromDocumentUri(): String? {
    if (scheme != "content") return null
    if (authority != "com.android.externalstorage.documents") return null
    val documentId = runCatching { DocumentsContract.getDocumentId(this) }.getOrNull()
        ?: runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull()
        ?: extractDocumentIdFromUriPath()
        ?: extractTreeDocumentIdFromUriPath()
        ?: return null
    val root = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "").trimStart('/')
    val basePath = if (root.equals("primary", ignoreCase = true)) {
        "/storage/emulated/0"
    } else {
        "/storage/$root"
    }
    return if (relativePath.isBlank()) basePath else "$basePath/$relativePath"
}

private fun Uri.extractDocumentIdFromUriPath(): String? {
    val encodedPath = encodedPath ?: return null
    val marker = "/document/"
    val index = encodedPath.indexOf(marker)
    if (index == -1) return null
    val encodedId = encodedPath.substring(index + marker.length)
    if (encodedId.isBlank()) return null
    return Uri.decode(encodedId).takeIf { it.contains(':') }
}

private fun Uri.extractTreeDocumentIdFromUriPath(): String? {
    val encodedPath = encodedPath ?: return null
    val marker = "/tree/"
    val index = encodedPath.indexOf(marker)
    if (index == -1) return null
    val encodedId = encodedPath.substring(index + marker.length).substringBefore('/')
    if (encodedId.isBlank()) return null
    return Uri.decode(encodedId).takeIf { it.contains(':') }
}
