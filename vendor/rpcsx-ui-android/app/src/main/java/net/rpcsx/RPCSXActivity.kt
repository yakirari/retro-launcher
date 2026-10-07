package net.rpcsx

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import net.rpcsx.databinding.ActivityRpcs3Binding
import net.rpcsx.dialogs.AlertDialogQueue
import net.rpcsx.overlay.State
import net.rpcsx.utils.GeneralSettings
import net.rpcsx.utils.InputBindingPrefs
import java.io.File
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlin.math.abs

class RPCSXActivity : Activity() {
    private lateinit var binding: ActivityRpcs3Binding
    private lateinit var unregisterUsbEventListener: () -> Unit
    private var gamePadState: State = State()
    private var usesAxisL2 = false
    private var usesAxisR2 = false
    private var bootThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var startupWatchdogPosted = false
    private val inputBindings by lazy { InputBindingPrefs.loadBindings() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneralSettings.init(this)
        binding = ActivityRpcs3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        unregisterUsbEventListener = listenUsbEvents(this)
        enableFullScreenImmersive()

        binding.oscToggle.setOnClickListener {
            binding.padOverlay.isInvisible = !binding.padOverlay.isInvisible
            binding.oscToggle.setImageResource(if (binding.padOverlay.isInvisible) R.drawable.ic_osc_off else R.drawable.ic_show_osc)
        }

        val gamePath = intent.getStringExtra("path")!!
        RPCSX.lastPlayedGame = gamePath
        ensureRuntimeInitialized()
        scheduleStartupWatchdog(gamePath)

        bootThread = thread {
            if (RPCSX.getState() != EmulatorState.Stopped) {
                val state = RPCSX.getState()
                Log.w("RPCSX State", state.name)

                if (state == EmulatorState.Paused && RPCSX.activeGame.value == gamePath) {
                    RPCSX.instance.resume()
                    return@thread
                }

                if (RPCSX.getState() != EmulatorState.Stopping && RPCSX.getState() != EmulatorState.Stopped) {
                    RPCSX.instance.kill()

                    while (RPCSX.getState() != EmulatorState.Stopped) {
                        Thread.sleep(300)
                        if (Thread.interrupted()) {
                            return@thread
                        }
                    }
                }
            }

            Log.w("RPCSX State", RPCSX.getState().name)
            RPCSX.activeGame.value = gamePath

            val bootResult = RPCSX.boot(gamePath)
            if (bootResult != BootResult.NoErrors) {
                Log.e("RPCSX Boot", "Boot failed for path=$gamePath with result=${bootResult.name}")
                runOnUiThread {
                    AlertDialogQueue.showDialog(
                        getString(R.string.failed_to_boot),
                        getString(R.string.error_with_msg, bootResult.name)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        val latestState = runCatching { RPCSX.getState() }.getOrDefault(EmulatorState.Stopped)
        if (latestState == EmulatorState.Stopped || latestState == EmulatorState.Stopping) {
            RPCSX.state.value = EmulatorState.Stopped
            RPCSX.activeGame.value = null
        } else {
            RPCSX.state.value = EmulatorState.Paused
        }
        unregisterUsbEventListener()
        bootThread?.interrupt()
        bootThread?.join()
    }

    override fun onBackPressed() {
        val state = runCatching { RPCSX.getState() }.getOrDefault(EmulatorState.Stopped)
        if (state != EmulatorState.Stopped && state != EmulatorState.Stopping) {
            runCatching { RPCSX.instance.kill() }
        }
        finish()
    }

    private fun scheduleStartupWatchdog(gamePath: String) {
        if (startupWatchdogPosted) return
        startupWatchdogPosted = true
        mainHandler.postDelayed({
            val state = runCatching { RPCSX.getState() }.getOrDefault(EmulatorState.Stopped)
            val activeGame = RPCSX.activeGame.value
            val looksStuck = activeGame == gamePath &&
                (state == EmulatorState.Stopped || state == EmulatorState.Ready || state == EmulatorState.Stopping)
            if (!isFinishing && !isDestroyed && looksStuck) {
                Log.e("RPCSX Boot", "Startup watchdog triggered: state=$state path=$gamePath")
                AlertDialogQueue.showDialog(
                    getString(R.string.failed_to_boot),
                    "Emulator stayed in state $state after launch."
                )
                finish()
            }
        }, 8000L)
    }


    private fun keyCodeToPadBit(keyCode: Int): Pair<Int, Int> {
        val event = inputBindings[keyCode] ?: Pair(0, 0)
        
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            if (usesAxisR2) return Pair(0, 0) else return event
        }
        
        if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            if (usesAxisL2) return Pair(0, 0) else return event
        }
        
        return event
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || (event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD)) == 0 || event.repeatCount != 0) {
            return super.onKeyDown(keyCode, event)
        }
        val padBit = keyCodeToPadBit(keyCode)
        if (padBit.first == 0) {
            return super.onKeyDown(keyCode, event)
        }

        gamePadState.digital[padBit.second] = gamePadState.digital[padBit.second] or padBit.first
        sendGamepadData()
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null || event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD) == 0) {
            return super.onKeyUp(keyCode, event)
        }

        val padBit = keyCodeToPadBit(keyCode)
        if (padBit.first == 0) {
            return super.onKeyUp(keyCode, event)
        }

        gamePadState.digital[padBit.second] =
            gamePadState.digital[padBit.second] and padBit.first.inv()
        sendGamepadData()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null || event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK || event.action != MotionEvent.ACTION_MOVE) {
            return super.onGenericMotionEvent(event)
        }

        if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0.1) {
            gamePadState.digital[1] =
                gamePadState.digital[1] or Digital2Flags.CELL_PAD_CTRL_L2.bit
            usesAxisL2 = true
        } else if (usesAxisL2) {
            usesAxisL2 = false
            gamePadState.digital[1] =
                gamePadState.digital[1] and Digital2Flags.CELL_PAD_CTRL_L2.bit.inv()
        }

        if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0.1) {
            gamePadState.digital[1] =
                gamePadState.digital[1] or Digital2Flags.CELL_PAD_CTRL_R2.bit
            usesAxisR2 = true
        } else if (usesAxisR2) {
            usesAxisR2 = false
            gamePadState.digital[1] =
                gamePadState.digital[1] and Digital2Flags.CELL_PAD_CTRL_R2.bit.inv()
        }

        val dpadX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val dpadY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        gamePadState.digital[0] =
            gamePadState.digital[0] and (Digital1Flags.CELL_PAD_CTRL_LEFT.bit or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit or Digital1Flags.CELL_PAD_CTRL_UP.bit or Digital1Flags.CELL_PAD_CTRL_DOWN.bit).inv()
        if (abs(dpadX) > 0.1f) {
            if (dpadX < 0) {
                gamePadState.digital[0] =
                    gamePadState.digital[0] or Digital1Flags.CELL_PAD_CTRL_LEFT.bit
            } else {
                gamePadState.digital[0] =
                    gamePadState.digital[0] or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit
            }
        }

        if (abs(dpadY) > 0.1f) {
            if (dpadY < 0) {
                gamePadState.digital[0] =
                    gamePadState.digital[0] or Digital1Flags.CELL_PAD_CTRL_UP.bit
            } else {
                gamePadState.digital[0] =
                    gamePadState.digital[0] or Digital1Flags.CELL_PAD_CTRL_DOWN.bit
            }
        }

        gamePadState.leftStickX = (event.getAxisValue(MotionEvent.AXIS_X) * 127 + 128).toInt()
        gamePadState.leftStickY = (event.getAxisValue(MotionEvent.AXIS_Y) * 127 + 128).toInt()
        gamePadState.rightStickX = (event.getAxisValue(MotionEvent.AXIS_Z) * 127 + 128).toInt()
        gamePadState.rightStickY = (event.getAxisValue(MotionEvent.AXIS_RZ) * 127 + 128).toInt()

        sendGamepadData()
        return true
    }

    private fun sendGamepadData() {
        RPCSX.instance.overlayPadData(
            gamePadState.digital[0],
            gamePadState.digital[1],
            gamePadState.leftStickX,
            gamePadState.leftStickY,
            gamePadState.rightStickX,
            gamePadState.rightStickY
        )
    }

    private fun enableFullScreenImmersive() {
        with(window) {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            val insetsController = WindowInsetsControllerCompat(this, decorView)
            insetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        applyInsetsToPadOverlay()
    }

    private fun ensureRuntimeInitialized() {
        if (RPCSX.initialized && RPCSX.activeLibrary.value != null) {
            applyEmbeddedDisplayDefaults()
            return
        }

        val romRoot = intent.getStringExtra("rom_root")
            ?: (getExternalFilesDir(null)?.absolutePath ?: "/storage/emulated/0/ROMs")
        val rootDir = File(romRoot)
        rootDir.mkdirs()
        File(rootDir, "ps3").mkdirs()
        ensureRpcsxMountDirs(rootDir)
        RPCSX.rootDirectory = rootDir.absolutePath
        if (!RPCSX.rootDirectory.endsWith("/")) {
            RPCSX.rootDirectory += "/"
        }

        val nativeLibraryDir = packageManager.getApplicationInfo(packageName, 0).nativeLibraryDir
        RPCSX.nativeLibDirectory = nativeLibraryDir
        val configuredLibrary = GeneralSettings["rpcsx_library"] as? String

        val coreCandidates = preferredCoreCandidatesForActivity(
            filesDir = filesDir,
            configuredLibrary = configuredLibrary
        )
        Log.i("RPCSX Core", "Activity candidates: ${coreCandidates.joinToString()}")
        val openedLibrary = coreCandidates.firstOrNull { candidate ->
            runCatching { RPCSX.openLibrary(candidate) }.getOrDefault(false)
        }?.also { Log.i("RPCSX Core", "Activity selected core: $it") } != null

        if (!openedLibrary || RPCSX.activeLibrary.value == null) {
            Log.e("RPCSX Boot", "No usable RPCSX core in activity process")
            AlertDialogQueue.showDialog(
                getString(R.string.failed_to_boot),
                "No usable RPCSX core found."
            )
            finish()
            return
        }

        RPCSX.instance.initialize(RPCSX.rootDirectory, UserRepository.getUserFromSettings())
        applyEmbeddedDisplayDefaults()
        logRuntimeParity("RPCSXActivity")
        thread { RPCSX.instance.startMainThreadProcessor() }
        RPCSX.initialized = true
    }

    private fun applyEmbeddedDisplayDefaults() {
        val changed = runCatching {
            RPCSX.instance.settingsSet("Video@@Stretch To Display Area", "true")
        }.getOrDefault(false)
        Log.i("RPCSX Display", "Applied default Stretch To Display Area=true result=$changed")
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

    private fun sha256Hex(text: String): String = runCatching {
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }.getOrDefault("<sha256-error>")

    private fun preferredCoreCandidatesForActivity(
        filesDir: File,
        configuredLibrary: String?
    ): List<String> {
        val candidates = linkedSetOf<String>()
        if (!configuredLibrary.isNullOrBlank()) {
            val configuredFile = File(configuredLibrary.trim())
            if (configuredFile.exists() &&
                configuredFile.isFile &&
                configuredFile.canRead() &&
                !configuredFile.name.contains("_from_net.rpcsx_")
            ) {
                candidates += configuredFile.absolutePath
            }
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
            ?.forEach { candidates += it.absolutePath }
        return candidates.toList()
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
            if (!legacyDir.exists()) {
                runCatching { legacyDir.mkdirs() }
                    .onFailure { Log.w("RPCSX Mount", "Failed to create ${legacyDir.absolutePath}: ${it.message}") }
            }
        }
    }

    private fun applyInsetsToPadOverlay() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.padOverlay) { view, windowInsets ->
            // I don't think we need `displayCutout` insets here as well
            // Since there is hardly any overlay overlapping with it
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableFullScreenImmersive()
    }
}
