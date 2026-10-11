package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.json.JSONObject
import java.util.Locale

/** Discovery is limited to GameNative-published shortcuts, not its private library database. */
internal object GameNativeScan {
    const val PACKAGE = "app.gamenative"
    const val ACTION = "app.gamenative.LAUNCH_GAME"
    private val sources = setOf("STEAM", "GOG", "EPIC", "AMAZON", "CUSTOM_GAME")
    data class Key(val appId: Int, val source: String, val userSerial: Long)

    /** Opaque Android-managed game shortcuts work in the profile that published them. */
    fun isPublishedGameShortcut(option: InstalledShortcutOption): Boolean =
        option.packageName == PACKAGE && option.shortcutId.matches(Regex("game_[1-9][0-9]*")) &&
            (option.intentAction == null || option.intentAction == ACTION) &&
            (option.extrasJson == null || key(option.extrasJson, option.userSerial) != null)

    fun key(extras: String?, userSerial: Long = 0): Key? = runCatching {
        val json = JSONObject(extras.orEmpty())
        val appId = json.optString("app_id").toIntOrNull()?.takeIf { it > 0 } ?: return null
        val source = json.optString("game_source").uppercase(Locale.ROOT).takeIf { it in sources } ?: return null
        Key(appId, source, userSerial)
    }.getOrNull()

    fun select(shortcuts: List<InstalledShortcutOption>, games: List<GameEntry>, collection: String? = null): List<GameEntry> {
        val existing = games.flatMap { game -> game.allTargets().mapNotNull { target ->
            if (target.packageName == PACKAGE) key(target.extrasJson, target.userSerial ?: 0) else null
        } }.toSet()
        val existingShortcuts = games.flatMap { it.allTargets() }.filter { it.packageName == PACKAGE }
            .map { it.shortcutId to (it.userSerial ?: 0) }.toSet()
        return shortcuts.asSequence()
            // LauncherApps deliberately strips intents/extras. Trust the publisher's game shortcut ID,
            // then let startShortcut execute the opaque Android-managed payload without guessing a store.
            .filter { it.userSerial == 0L && isPublishedGameShortcut(it) }
            .filterNot { (it.shortcutId to it.userSerial) in existingShortcuts || key(it.extrasJson, it.userSerial) in existing }
            .distinctBy { it.shortcutId to it.userSerial }
            .map { option ->
                GameEntry(title = option.title, subtitle = "GameNative",
                    platform = com.ariy.retrolauncher.model.PcGameStores.fromExtras(option.extrasJson) ?: "GameNative",
                    genre = collection?.takeIf(String::isNotBlank), sourceType = GameSourceType.SHORTCUT,
                    packageName = PACKAGE, shortcutId = option.shortcutId, userSerial = option.userSerial,
                    intentAction = option.intentAction, extrasJson = option.extrasJson)
            }.sortedBy { it.title.lowercase(Locale.ROOT) }.toList()
    }
}