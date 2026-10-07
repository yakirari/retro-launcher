package net.rpcsx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.rpcsx.dialogs.AlertDialogQueue
import net.rpcsx.ui.navigation.AppNavHost
import net.rpcsx.utils.GeneralSettings
import net.rpcsx.utils.GitHub
import net.rpcsx.utils.RpcsxUpdater
import java.io.File
import java.security.MessageDigest
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var unregisterUsbEventListener: () -> Unit
    private var bootPathFromIntent: String? = null
    private var pendingBootPathAfterFirmwareInstall: String? = null
    private val bootLoopGuardPrefs by lazy {
        getSharedPreferences("rpcsx_boot_loop_guard", Context.MODE_PRIVATE)
    }
    private val installCachePrefs by lazy {
        getSharedPreferences("rpcsx_install_cache", Context.MODE_PRIVATE)
    }
    private val installFirmwareLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                PrecompilerService.start(this, PrecompilerServiceAction.InstallFirmware, uri)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bootPathFromIntent = intent.extractBootPath()

        GeneralSettings.init(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (!RPCSX.initialized) {
            Permission.PostNotifications.requestPermission(this)

            with(getSystemService(NOTIFICATION_SERVICE) as NotificationManager) {
                val channel = NotificationChannel(
                    "rpcsx-progress",
                    getString(R.string.installation_progress),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                createNotificationChannel(channel)
            }

            val romRoot = resolveRetroLauncherRomRoot(this)
                ?: "/storage/emulated/0/ROMs"
            val romDir = File(romRoot)
            romDir.mkdirs()
            File(romDir, "ps3").mkdirs()

            val runtimeRoot = resolveEmbeddedRuntimeRoot(this)
            val rootDir = File(runtimeRoot)
            rootDir.mkdirs()
            migrateEmbeddedRuntimeDataIfNeeded(legacyRoot = romDir, runtimeRoot = rootDir)
            ensureRpcsxMountDirs(rootDir)
            RPCSX.rootDirectory = rootDir.absolutePath
            if (!RPCSX.rootDirectory.endsWith("/")) {
                RPCSX.rootDirectory += "/"
            }

            lifecycleScope.launch {
                GameRepository.load()
            }

            FirmwareRepository.load()
            GitHub.initialize(this)

            val configuredLibrary = GeneralSettings["rpcsx_library"] as? String
            var rpcsxLibrary = configuredLibrary
            if (!isUsableEmbeddedLibraryPath(rpcsxLibrary)) {
                rpcsxLibrary = null
            }
            val rpcsxUpdateStatus = GeneralSettings["rpcsx_update_status"]
            val rpcsxPrevLibrary = GeneralSettings["rpcsx_prev_library"] as? String

            val nativeLibraryDir =
                packageManager.getApplicationInfo(packageName, 0).nativeLibraryDir
            RPCSX.nativeLibDirectory = nativeLibraryDir

            val coreCandidates = preferredEmbeddedCoreCandidates(
                filesDir = filesDir,
                configuredLibrary = rpcsxLibrary
            )
            Log.i("RPCSX Core", "Candidates: ${coreCandidates.joinToString()}")
            val selectedCore = coreCandidates.firstOrNull { candidate ->
                runCatching { RPCSX.openLibrary(candidate) }.getOrDefault(false)
            }
            if (selectedCore != null) {
                Log.i("RPCSX Core", "Selected core: $selectedCore")
                GeneralSettings["rpcsx_library"] = selectedCore
                GeneralSettings["rpcsx_installed_arch"] = RpcsxUpdater.getArch()
                GeneralSettings["rpcsx_update_status"] = true
                GeneralSettings.sync()
            } else if (rpcsxLibrary != null) {
                if (rpcsxUpdateStatus == false && rpcsxPrevLibrary != null) {
                    GeneralSettings["rpcsx_library"] = rpcsxPrevLibrary
                    GeneralSettings["rpcsx_installed_arch"] = GeneralSettings["rpcsx_prev_installed_arch"]
                    GeneralSettings["rpcsx_prev_installed_arch"] = null
                    GeneralSettings["rpcsx_prev_library"] = null
                    GeneralSettings["rpcsx_bad_version"] = RpcsxUpdater.getFileVersion(File(rpcsxLibrary))
                    GeneralSettings.sync()

                    File(rpcsxLibrary).delete()
                    rpcsxLibrary = rpcsxPrevLibrary

                    AlertDialogQueue.showDialog(
                        getString(R.string.failed_to_update_rpcsx),
                        getString(R.string.failed_to_load_new_version)
                    )
                } else if (rpcsxUpdateStatus == null) {
                    GeneralSettings["rpcsx_update_status"] = false
                    GeneralSettings.sync()
                }

                RPCSX.openLibrary(rpcsxLibrary)
            }

            if (RPCSX.activeLibrary.value != null) {
                RPCSX.instance.initialize(RPCSX.rootDirectory, UserRepository.getUserFromSettings())
                applyEmbeddedDisplayDefaults()
                logRuntimeParity("MainActivity")
                val gpuDriverPath = GeneralSettings["gpu_driver_path"] as? String
                val gpuDriverName = GeneralSettings["gpu_driver_name"] as? String

                if (gpuDriverPath != null && gpuDriverName != null) {
                    RPCSX.instance.setCustomDriver(gpuDriverPath, gpuDriverName, nativeLibraryDir)
                }

                lifecycleScope.launch {
                    UserRepository.load()
                }

                RPCSX.initialized = true

                thread {
                    RPCSX.instance.startMainThreadProcessor()
                }

                thread {
                    RPCSX.instance.processCompilationQueue()
                }

                GeneralSettings["rpcsx_update_status"] = true
                if (rpcsxPrevLibrary != null) {
                    if (rpcsxLibrary != rpcsxPrevLibrary) {
                        File(rpcsxPrevLibrary).delete()
                    }

                    GeneralSettings["rpcsx_prev_library"] = null
                    GeneralSettings["rpcsx_prev_installed_arch"] = null
                    GeneralSettings.sync()
                }
            }

            val updateFile = File(RPCSX.rootDirectory + "cache", "rpcsx-${BuildConfig.Version}.apk")
            if (updateFile.exists()) {
                updateFile.delete()
            }
        }

        setContent {
            RPCSXTheme {
                AppNavHost()
            }
        }

        lifecycleScope.launch {
            while (isActive) {
                val pendingBootPath = pendingBootPathAfterFirmwareInstall
                if (!pendingBootPath.isNullOrBlank() &&
                    FirmwareRepository.isUsable() &&
                    FirmwareRepository.progressChannel.value == null
                ) {
                    launchIntoEmulator(pendingBootPath)
                    pendingBootPathAfterFirmwareInstall = null
                    finish()
                }
                delay(1000)
            }
        }

        if (RPCSX.activeLibrary.value != null) {
            launchGameFromIntentIfRequested()
            unregisterUsbEventListener = listenUsbEvents(this)
        } else {
            unregisterUsbEventListener = {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterUsbEventListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bootPathFromIntent = intent.extractBootPath()
        launchGameFromIntentIfRequested()
    }

    private fun launchGameFromIntentIfRequested() {
        val bootPath = bootPathFromIntent?.takeIf { it.isNotBlank() } ?: return
        val pathFile = File(bootPath)
        val looksLikePkg = bootPath.trim().endsWith(".pkg", ignoreCase = true) ||
            (pathFile.isFile && pathFile.extension.equals("pkg", ignoreCase = true))
        val looksLikeRap = bootPath.trim().endsWith(".rap", ignoreCase = true) ||
            (pathFile.isFile && pathFile.extension.equals("rap", ignoreCase = true))
        if (looksLikePkg) {
            if (isPkgInstallCached(pathFile)) {
                val bootable = pathFile.parentFile?.let(::findBootablePathInDirectory)
                if (!bootable.isNullOrBlank()) {
                    launchIntoEmulator(bootable)
                    bootPathFromIntent = null
                    finish()
                    return
                }
            }
            installPkgAndRaps(pathFile)
            bootPathFromIntent = null
            AlertDialogQueue.showDialog(
                "Installing package",
                "Detected PKG file. Installing package before launch."
            )
            return
        }
        if (looksLikeRap) {
            thread(isDaemon = true) {
                installRapFiles(listOf(pathFile))
                GameRepository.queueRefresh()
            }
            bootPathFromIntent = null
            AlertDialogQueue.showDialog(
                "Installing license",
                "Detected RAP file. Installing license."
            )
            return
        }
        if (shouldThrottleAutoBoot(bootPath)) {
            bootPathFromIntent = null
            AlertDialogQueue.showDialog(
                "Auto-launch paused",
                "RPCSX detected repeated auto-launch attempts for this game after a crash. " +
                    "Open the game manually from the list when ready."
            )
            return
        }
        // Avoid crashy auto-boot path when firmware is not installed yet.
        if (!FirmwareRepository.isUsable() || FirmwareRepository.progressChannel.value != null) {
            if (!FirmwareRepository.isUsable() && FirmwareRepository.progressChannel.value == null) {
                val autoFirmwareUri = findAutoFirmwareUri(this)
                if (autoFirmwareUri != null) {
                    pendingBootPathAfterFirmwareInstall = bootPath
                    bootPathFromIntent = null
                    PrecompilerService.start(this, PrecompilerServiceAction.InstallFirmware, autoFirmwareUri)
                    AlertDialogQueue.showDialog(
                        getString(R.string.missing_firmware),
                        "Found local firmware in ps3 folder. Installing automatically, then launching the game."
                    )
                    return
                }
            } else if (FirmwareRepository.progressChannel.value != null) {
                pendingBootPathAfterFirmwareInstall = bootPath
                bootPathFromIntent = null
            }
            AlertDialogQueue.showDialog(
                getString(R.string.missing_firmware),
                getString(R.string.install_firmware_to_continue),
                onConfirm = { installFirmwareLauncher.launch("*/*") },
                confirmText = getString(R.string.install),
                dismissText = getString(R.string.close)
            )
            return
        }
        markAutoBootAttempt(bootPath)
        launchIntoEmulator(bootPath)
        bootPathFromIntent = null
        // When launched from Retro Launcher via boot intent, close this host activity
        // so exiting the emulator returns to Retro Launcher instead of RPCSX home.
        finish()
    }

    private fun launchIntoEmulator(bootPath: String) {
        val normalized = bootPath.trim()
        if (normalized.endsWith(".pkg", ignoreCase = true)) {
            val pkgFile = File(normalized)
            installPkgAndRaps(pkgFile)
            AlertDialogQueue.showDialog(
                "Installing package",
                "Detected PKG file. Installing package before launch."
            )
            return
        }
        if (normalized.endsWith(".rap", ignoreCase = true)) {
            val rapFile = File(normalized)
            thread(isDaemon = true) {
                installRapFiles(listOf(rapFile))
                GameRepository.queueRefresh()
            }
            AlertDialogQueue.showDialog(
                "Installing license",
                "Detected RAP file. Installing license."
            )
            return
        }
        val emulatorIntent = Intent(this, RPCSXActivity::class.java).apply {
            action = "rpcsx.intent.action.Emulator"
            putExtra("path", normalized)
            putExtra("rom_root", RPCSX.rootDirectory.trimEnd('/'))
        }
        startActivity(emulatorIntent)
    }

    private fun installPkgAndRaps(pkgFile: File) {
        val siblingRaps = pkgFile.parentFile
            ?.listFiles()
            ?.filter { it.isFile && it.extension.equals("rap", ignoreCase = true) }
            .orEmpty()
        val installProgress = ProgressRepository.create(this, getString(R.string.package_installation))
        thread(isDaemon = true) {
            val pkgDescriptor = runCatching {
                ParcelFileDescriptor.open(pkgFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }.getOrNull()
            if (pkgDescriptor == null) {
                ProgressRepository.onProgressEvent(installProgress, -1, 0)
                return@thread
            }
            val installed = runCatching {
                RPCSX.instance.install(pkgDescriptor.fd, installProgress)
            }.getOrDefault(false)
            runCatching { pkgDescriptor.close() }
            if (!installed) {
                ProgressRepository.onProgressEvent(installProgress, -1, 0)
                return@thread
            }
            cachePkgInstall(pkgFile)
            if (siblingRaps.isNotEmpty()) {
                Thread.sleep(1200)
                installRapFiles(siblingRaps)
                GameRepository.queueRefresh()
            }
            GameRepository.queueRefresh()
            Thread.sleep(1000)
            val installedBootable = findRecentlyInstalledGamePath()
            if (!installedBootable.isNullOrBlank()) {
                runOnUiThread {
                    clearAutoBootThrottle()
                    launchIntoEmulator(installedBootable)
                    finish()
                }
            }
        }
    }

    private fun installRapFiles(rapFiles: List<File>) {
        if (rapFiles.isEmpty()) return
        GameRepository.queueRefresh()
        Thread.sleep(500)
        val gamePaths = GameRepository.list()
            .map { it.info.path }
            .filter { it != "$" }
            .filter(::isRapInstallCandidatePath)
            .distinct()
        if (gamePaths.isEmpty()) return
        var installedAnyRap = false
        rapFiles.forEach { rapFile ->
            var successForAnyGame = false
            gamePaths.forEach { gamePath ->
                if (successForAnyGame) return@forEach
                if (isRapInstallCached(rapFile, gamePath)) {
                    successForAnyGame = true
                    return@forEach
                }
                val rapDescriptor = runCatching {
                    ParcelFileDescriptor.open(rapFile, ParcelFileDescriptor.MODE_READ_ONLY)
                }.getOrNull() ?: return@forEach
                val progress = ProgressRepository.create(
                    this,
                    getString(R.string.license_installation),
                    silent = true
                )
                val success = runCatching {
                    RPCSX.instance.installKey(rapDescriptor.fd, progress, gamePath)
                }.getOrDefault(false)
                runCatching { rapDescriptor.close() }
                if (!success) {
                    ProgressRepository.onProgressEvent(progress, -1, 0)
                } else {
                    // Refresh game flags and verify lock state before trusting the install result.
                    GameRepository.queueRefresh()
                    Thread.sleep(500)
                    val stillLocked = GameRepository.find(gamePath)?.hasFlag(GameFlag.Locked) == true
                    if (!stillLocked) {
                        successForAnyGame = true
                        installedAnyRap = true
                        cacheRapInstall(rapFile, gamePath)
                    }
                }
            }
        }
        if (installedAnyRap) {
            GameRepository.queueRefresh()
        }
    }

    private fun isRapInstallCandidatePath(path: String): Boolean {
        val normalized = path.trim()
        if (normalized.isBlank()) return false
        if (normalized.endsWith(".iso", ignoreCase = true)) return false
        val file = File(normalized)
        if (!file.exists()) return false
        if (file.isFile) return false
        val hddGameRoot = File(RPCSX.rootDirectory, "config/dev_hdd0/game").absolutePath
        return file.absolutePath.startsWith(hddGameRoot, ignoreCase = true)
    }

    private fun shouldThrottleAutoBoot(bootPath: String): Boolean {
        // Disabled: this caused false positives that blocked normal relaunch flows.
        return false
    }

    private fun markAutoBootAttempt(bootPath: String) {
        bootLoopGuardPrefs.edit()
            .putString(KEY_BOOT_PATH, bootPath)
            .putLong(KEY_BOOT_ATTEMPT_MS, System.currentTimeMillis())
            .apply()
    }

    private fun clearAutoBootThrottle() {
        bootLoopGuardPrefs.edit()
            .remove(KEY_BOOT_PATH)
            .remove(KEY_BOOT_ATTEMPT_MS)
            .apply()
    }

    private fun cachePkgInstall(pkgFile: File) {
        installCachePrefs.edit()
            .putLong(pkgCacheKey(pkgFile), System.currentTimeMillis())
            .apply()
    }

    private fun isPkgInstallCached(pkgFile: File): Boolean =
        installCachePrefs.contains(pkgCacheKey(pkgFile))

    private fun cacheRapInstall(rapFile: File, gamePath: String) {
        installCachePrefs.edit()
            .putLong(rapCacheKey(rapFile, gamePath), System.currentTimeMillis())
            .apply()
    }

    private fun isRapInstallCached(rapFile: File, gamePath: String): Boolean =
        installCachePrefs.contains(rapCacheKey(rapFile, gamePath))

    private fun pkgCacheKey(pkgFile: File): String =
        "pkg:${fileFingerprint(pkgFile)}"

    private fun rapCacheKey(rapFile: File, gamePath: String): String =
        "rap:${fileFingerprint(rapFile)}:${gamePath.trim()}"

    private fun fileFingerprint(file: File): String {
        val length = runCatching { file.length() }.getOrDefault(-1L)
        val modified = runCatching { file.lastModified() }.getOrDefault(0L)
        return "${file.absolutePath}|$length|$modified"
    }

    private fun logRuntimeParity(tagSuffix: String) {
        val settingsJson = runCatching { RPCSX.instance.settingsGet("") }.getOrDefault("")
        val settingsHash = sha256Hex(settingsJson)
        val settingsPreview = settingsJson.take(300)
        val corePath = RPCSX.activeLibrary.value ?: "<none>"
        val version = runCatching { RPCSX.instance.getVersion() }.getOrDefault("<unknown>")
        Log.i(
            "RPCSX Parity",
            "[$tagSuffix] core=$corePath root=${RPCSX.rootDirectory} version=$version settingsHash=$settingsHash settingsPreview=$settingsPreview"
        )
    }

    private fun applyEmbeddedDisplayDefaults() {
        val changed = runCatching {
            RPCSX.instance.settingsSet("Video@@Stretch To Display Area", "true")
        }.getOrDefault(false)
        Log.i("RPCSX Display", "Applied default Stretch To Display Area=true result=$changed")
    }

    private fun sha256Hex(text: String): String = runCatching {
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }.getOrDefault("<sha256-error>")

    companion object {
        private const val KEY_BOOT_PATH = "boot_path"
        private const val KEY_BOOT_ATTEMPT_MS = "boot_attempt_ms"
        private const val AUTO_BOOT_COOLDOWN_MS = 45_000L
    }
}

private fun ensureRpcsxMountDirs(rootDir: File) {
    val configRoot = File(rootDir, "config")
    if (!configRoot.exists()) {
        runCatching { configRoot.mkdirs() }
            .onFailure { Log.w("RPCSX Mount", "Failed to create ${configRoot.absolutePath}: ${it.message}") }
    }
    val requiredDirs = arrayOf(
        "dev_hdd0",
        "dev_hdd1",
        "dev_flash",
        "dev_usb000",
        "dev_bdvd"
    )
    requiredDirs.forEach { name ->
        val targetDir = File(configRoot, name)
        val legacyDir = File(rootDir, name)
        if (!targetDir.exists()) {
            if (legacyDir.exists() && legacyDir.isDirectory) {
                val migrated = runCatching { legacyDir.renameTo(targetDir) }.getOrDefault(false)
                if (!migrated) {
                    runCatching { targetDir.mkdirs() }
                        .onFailure { Log.w("RPCSX Mount", "Failed to create ${targetDir.absolutePath}: ${it.message}") }
                }
            } else {
                runCatching { targetDir.mkdirs() }
                    .onFailure { Log.w("RPCSX Mount", "Failed to create ${targetDir.absolutePath}: ${it.message}") }
            }
        }
        // Keep legacy dirs for backward compatibility with any older path assumptions.
        if (!legacyDir.exists()) {
            runCatching { legacyDir.mkdirs() }
                .onFailure { Log.w("RPCSX Mount", "Failed to create ${legacyDir.absolutePath}: ${it.message}") }
        }
    }
}

private fun isUsableEmbeddedLibraryPath(path: String?): Boolean {
    val value = path?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    // Embedded launcher must not reuse standalone app-private library paths.
    if (value.contains("/data/user/0/net.rpcsx/") ||
        value.contains("/data/data/net.rpcsx/") ||
        value.contains("/Android/data/net.rpcsx/")
    ) {
        return false
    }
    if (value.contains("_from_net.rpcsx_")) return false
    val file = File(value)
    return file.exists() && file.isFile && file.canRead()
}

private fun preferredEmbeddedCoreCandidates(
    filesDir: File,
    configuredLibrary: String?,
): List<String> {
    val candidates = linkedSetOf<String>()
    if (isUsableEmbeddedLibraryPath(configuredLibrary)) {
        candidates += configuredLibrary!!.trim()
    }
    filesDir.listFiles()
        ?.asSequence()
        ?.filter {
            it.isFile &&
                it.name.startsWith("librpcsx-android_") &&
                it.extension == "so" &&
                !it.name.contains("_from_net.rpcsx_")
        }
        ?.sortedByDescending { runCatching { it.lastModified() }.getOrDefault(0L) }
        ?.map { it.absolutePath }
        ?.filter { isUsableEmbeddedLibraryPath(it) }
        ?.forEach { candidates += it }
    return candidates.toList()
}

private fun Intent?.extractBootPath(): String? {
    val launchIntent = this ?: return null
    val direct = launchIntent.getStringExtra("path")
        ?.takeIf { it.isNotBlank() }
        ?: launchIntent.getStringExtra("game")?.takeIf { it.isNotBlank() }
    if (!direct.isNullOrBlank()) return normalizeIncomingBootPath(direct)
    val data = launchIntent.data?.toString()?.takeIf { it.isNotBlank() } ?: return null
    val raw = runCatching {
        val parsed = Uri.parse(data)
        if (parsed.scheme.equals("file", ignoreCase = true)) {
            parsed.path?.takeIf { it.isNotBlank() }
        } else {
            data
        }
    }.getOrDefault(data)
    return raw?.let(::normalizeIncomingBootPath)
}

private fun normalizeIncomingBootPath(path: String): String {
    val trimmed = path.trim()
    if (trimmed.isBlank()) return path
    val file = File(trimmed)
    if (!file.exists()) return trimmed
    if (file.isDirectory) {
        return findBootablePathInDirectory(file) ?: file.absolutePath
    }
    val name = file.name.lowercase()
    if (name.endsWith(".iso")) return file.absolutePath
    if (name == "eboot.bin") {
        val parent = file.parentFile ?: return file.absolutePath
        if (parent.name.equals("usrdir", ignoreCase = true)) {
            val ps3Game = parent.parentFile
            return ps3Game?.parentFile?.absolutePath ?: ps3Game?.absolutePath ?: parent.absolutePath
        }
        return parent.absolutePath
    }
    if (name == "param.sfo") {
        return file.parentFile?.parentFile?.absolutePath ?: file.parentFile?.absolutePath ?: file.absolutePath
    }
    if (name.endsWith(".pkg") || name.endsWith(".rap")) return file.absolutePath
    return file.absolutePath
}

private fun findRecentlyInstalledGamePath(
    rootDirectory: String = RPCSX.rootDirectory,
    withinMs: Long = 15 * 60 * 1000L
): String? {
    val gameRoot = File(rootDirectory, "config/dev_hdd0/game")
    if (!gameRoot.exists() || !gameRoot.isDirectory) return null
    val cutoff = System.currentTimeMillis() - withinMs
    val newestParam = gameRoot.walkTopDown()
        .maxDepth(4)
        .filter { it.isFile && it.name.equals("PARAM.SFO", ignoreCase = true) }
        .filter { runCatching { it.lastModified() >= cutoff }.getOrDefault(false) }
        .maxByOrNull { runCatching { it.lastModified() }.getOrDefault(0L) }
        ?: return null
    val ps3GameDir = newestParam.parentFile?.takeIf { it.name.equals("PS3_GAME", ignoreCase = true) }
        ?: return null
    return ps3GameDir.parentFile?.absolutePath ?: ps3GameDir.absolutePath
}

private fun findBootablePathInDirectory(directory: File): String? {
    if (!directory.exists() || !directory.isDirectory) return null

    val iso = directory.walkTopDown()
        .maxDepth(3)
        .firstOrNull { it.isFile && it.extension.equals("iso", ignoreCase = true) }
    if (iso != null) return iso.absolutePath

    val eboot = directory.walkTopDown()
        .maxDepth(6)
        .firstOrNull { it.isFile && it.name.equals("EBOOT.BIN", ignoreCase = true) }
    if (eboot != null) {
        val usrdir = eboot.parentFile
        val ps3Game = if (usrdir?.name.equals("USRDIR", ignoreCase = true)) usrdir.parentFile else null
        return ps3Game?.parentFile?.absolutePath ?: ps3Game?.absolutePath ?: directory.absolutePath
    }

    val param = directory.walkTopDown()
        .maxDepth(5)
        .firstOrNull { it.isFile && it.name.equals("PARAM.SFO", ignoreCase = true) }
    if (param != null) {
        val ps3Game = param.parentFile?.takeIf { it.name.equals("PS3_GAME", ignoreCase = true) }
        return ps3Game?.parentFile?.absolutePath ?: ps3Game?.absolutePath ?: directory.absolutePath
    }

    val pkg = directory.walkTopDown()
        .maxDepth(3)
        .firstOrNull { it.isFile && it.extension.equals("pkg", ignoreCase = true) }
    if (pkg != null) return pkg.absolutePath

    return null
}

private fun resolveRetroLauncherRomRoot(context: Context): String? {
    val uriString = context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE)
        .getString("authorized_rom_tree_uri", null)
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val treeUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        ?: runCatching { DocumentsContract.getDocumentId(treeUri) }.getOrNull()
        ?: return null
    val volume = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', "").trimStart('/')
    val basePath = if (volume.equals("primary", ignoreCase = true)) {
        "/storage/emulated/0"
    } else {
        "/storage/$volume"
    }
    val rawPath = if (relativePath.isBlank()) basePath else "$basePath/$relativePath"
    return normalizeRomRootPath(rawPath)
}

private fun normalizeRomRootPath(path: String): String {
    val normalized = path.trimEnd('/')
    val lower = normalized.lowercase()
    val knownSystemLeafNames = setOf(
        "3ds", "arcade", "atari2600", "dreamcast", "ds", "gc", "gb", "gba", "gbc",
        "genesis", "mame", "n64", "nds", "nes", "ngc", "ps1", "ps2", "ps3", "psp",
        "saturn", "scummvm", "snes", "switch", "vita", "wii", "wiiu"
    )
    val leaf = normalized.substringAfterLast('/', normalized)
    if (leaf.lowercase() in knownSystemLeafNames) {
        val parent = normalized.substringBeforeLast('/', "")
        if (parent.isNotBlank()) return parent
    }
    val romsMarker = "/roms/"
    val markerIndex = lower.indexOf(romsMarker)
    if (markerIndex >= 0) {
        return normalized.substring(0, markerIndex + romsMarker.length - 1)
    }
    return normalized
}

private fun resolveEmbeddedRuntimeRoot(context: Context): String {
    val appExternal = context.getExternalFilesDir(null)?.absolutePath
    if (!appExternal.isNullOrBlank()) return appExternal
    return resolveRetroLauncherRomRoot(context) ?: "/storage/emulated/0/ROMs"
}

private fun migrateEmbeddedRuntimeDataIfNeeded(legacyRoot: File, runtimeRoot: File) {
    if (legacyRoot.absolutePath == runtimeRoot.absolutePath) return
    if (!legacyRoot.exists() || !legacyRoot.isDirectory) return
    val entries = listOf("config", "games.json", "fw.json", "cache")
    entries.forEach { name ->
        val from = File(legacyRoot, name)
        if (!from.exists()) return@forEach
        val to = File(runtimeRoot, name)
        if (to.exists()) return@forEach
        runCatching {
            if (from.isDirectory) {
                from.copyRecursively(to, overwrite = false)
            } else {
                to.parentFile?.mkdirs()
                from.copyTo(to, overwrite = false)
            }
        }.onSuccess {
            Log.i("RPCSX Migration", "Migrated ${from.absolutePath} -> ${to.absolutePath}")
        }.onFailure {
            Log.w("RPCSX Migration", "Failed to migrate ${from.absolutePath}: ${it.message}")
        }
    }
}

private fun findAutoFirmwareUri(context: Context): Uri? {
    val romRoot = resolveRetroLauncherRomRoot(context) ?: "/storage/emulated/0/ROMs"
    val ps3Root = File(romRoot, "ps3")
    if (!ps3Root.exists() || !ps3Root.isDirectory) return null
    val directCandidates = listOf(
        File(ps3Root, "PS3UPDAT.PUP"),
        File(ps3Root, "PS3UPDAT.pup"),
        File(ps3Root, "PS3_UPDATE.PUP"),
        File(ps3Root, "firmware/PS3UPDAT.PUP"),
        File(ps3Root, "firmware/PS3UPDAT.pup")
    )
    directCandidates.firstOrNull { it.exists() && it.isFile && it.canRead() }?.let { return Uri.fromFile(it) }
    val scanned = ps3Root.walkTopDown()
        .maxDepth(3)
        .firstOrNull { file ->
            file.isFile &&
                file.canRead() &&
                file.extension.equals("pup", ignoreCase = true)
        }
    return scanned?.let(Uri::fromFile)
}
