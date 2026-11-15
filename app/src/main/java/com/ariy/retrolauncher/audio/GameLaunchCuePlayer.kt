package com.ariy.retrolauncher.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RawRes
import com.ariy.retrolauncher.R
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.LaunchTarget

internal object GameLaunchCueCatalog {
    @RawRes
    fun resourceFor(game: GameEntry, target: LaunchTarget? = null): Int? {
        val rawPlatform = game.platform?.trim()
        val platform = GamePlatforms.normalizeOrNull(rawPlatform)
            ?: GamePlatforms.inferFromPackageName(target?.packageName)

        return when (platform) {
            "Nintendo GameCube", "Nintendo Wii" -> R.raw.cue_01_nintendo_gamecube_wii
            "Nintendo 3DS" -> R.raw.cue_02_nintendo_3ds
            "Nintendo DS" -> R.raw.cue_03_nintendo_ds
            "Nintendo Entertainment System" -> R.raw.cue_04_nes
            "Super Nintendo" -> R.raw.cue_05_super_nintendo_snes
            "Nintendo 64" -> R.raw.cue_06_nintendo_64
            "Game Boy", "Game Boy Color" -> R.raw.cue_07_gameboy_gbc
            "Game Boy Advance" -> R.raw.cue_08_gameboy_advance_gba
            "Sony PlayStation" -> R.raw.cue_09_playstation_1
            "Sony PlayStation 2" -> R.raw.cue_10_playstation_2
            "Sony PlayStation Portable" -> R.raw.cue_11_psp
            "Sony PlayStation Vita" -> R.raw.cue_12_ps_vita
            "Sega Dreamcast" -> R.raw.cue_13_dreamcast
            "Sega Genesis / Mega Drive" -> R.raw.cue_14_sega_genesis_megadrive
            "PC Engine / TurboGrafx-16" -> R.raw.cue_15_pc_engine_turbografx16
            "Neo Geo" -> R.raw.cue_16_neo_geo
            "Neo Geo Pocket", "Neo Geo Pocket Color" -> R.raw.cue_17_neo_geo_pocket_color
            "MAME Arcade", "Final Burn Neo" -> R.raw.cue_18_arcade_coin_insert
            "DOS Games", "ScummVM" -> R.raw.cue_19_dos_scummvm
            "PC (Windows)", "Steam", "GOG", "Epic Games", "Amazon Games",
            "GameNative", "GameHub", "GameHub Lite" -> R.raw.cue_20_pc_game_shortcut
            else -> when {
                rawPlatform.equals("Arcade", ignoreCase = true) -> R.raw.cue_18_arcade_coin_insert
                target?.packageName == "org.scummvm.scummvm" -> R.raw.cue_19_dos_scummvm
                target?.packageName in PC_GAME_HUB_PACKAGES -> R.raw.cue_20_pc_game_shortcut
                else -> null
            }
        }
    }

    private val PC_GAME_HUB_PACKAGES = setOf(
        "app.gamenative",
        "com.xiaoji.egggame",
        "gamehub.lite"
    )
}

internal class GameLaunchCuePlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var fadeRunnable: Runnable? = null

    fun play(game: GameEntry, target: LaunchTarget? = game.preferredTarget() ?: game.primaryTarget()) {
        stop()
        val resourceId = GameLaunchCueCatalog.resourceFor(game, target) ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(::onAudioFocusChange, handler)
            .build()

        val focusResult = try {
            audioManager.requestAudioFocus(request)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not request audio focus for launch cue.", error)
            return
        }
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Audio focus was not granted for launch cue.")
            return
        }
        focusRequest = request

        val mediaPlayer = try {
            MediaPlayer.create(appContext, resourceId)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not prepare launch cue resource $resourceId.", error)
            abandonAudioFocus()
            return
        }
        if (mediaPlayer == null) {
            Log.w(TAG, "Could not prepare launch cue resource $resourceId.")
            abandonAudioFocus()
            return
        }

        player = mediaPlayer
        try {
            mediaPlayer.setAudioAttributes(audioAttributes)
            mediaPlayer.isLooping = false
            mediaPlayer.setOnCompletionListener(::finish)
            mediaPlayer.setOnErrorListener { failedPlayer, what, extra ->
                Log.w(TAG, "Launch cue playback failed (what=$what, extra=$extra).")
                finish(failedPlayer)
                true
            }
            mediaPlayer.start()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not start launch cue resource $resourceId.", error)
            finish(mediaPlayer)
        }
    }

    fun release() {
        stop()
    }

    private fun onAudioFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                fadeRunnable?.let(handler::removeCallbacks)
                fadeRunnable = null
                player?.setVolume(1f, 1f)
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> fadeOut()
        }
    }

    private fun fadeOut() {
        val activePlayer = player ?: return
        fadeRunnable?.let(handler::removeCallbacks)
        val fadeStartedAt = SystemClock.uptimeMillis()
        val fade = object : Runnable {
            override fun run() {
                if (player !== activePlayer) return
                val progress = ((SystemClock.uptimeMillis() - fadeStartedAt) / FADE_OUT_MILLIS.toFloat())
                    .coerceIn(0f, 1f)
                val volume = 1f - progress
                activePlayer.setVolume(volume, volume)
                if (progress >= 1f) {
                    finish(activePlayer)
                } else {
                    handler.postDelayed(this, FADE_STEP_MILLIS)
                }
            }
        }
        fadeRunnable = fade
        handler.post(fade)
    }

    private fun finish(finishedPlayer: MediaPlayer) {
        if (player !== finishedPlayer) return
        player = null
        fadeRunnable?.let(handler::removeCallbacks)
        fadeRunnable = null
        finishedPlayer.release()
        abandonAudioFocus()
    }

    private fun stop() {
        fadeRunnable?.let(handler::removeCallbacks)
        fadeRunnable = null
        player?.release()
        player = null
        abandonAudioFocus()
    }

    private fun abandonAudioFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        audioManager.abandonAudioFocusRequest(request)
    }

    private companion object {
        const val TAG = "GameLaunchCue"
        const val FADE_OUT_MILLIS = 150L
        const val FADE_STEP_MILLIS = 25L
    }
}
