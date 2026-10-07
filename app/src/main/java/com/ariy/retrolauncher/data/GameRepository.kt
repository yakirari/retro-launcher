package com.ariy.retrolauncher.data

import android.content.Context
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import org.json.JSONArray

class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE)

    fun loadGames(): List<GameEntry> {
        val raw = prefs.getString(KEY_GAMES, null) ?: return emptyList()
        val json = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until json.length()) {
                val entry = runCatching {
                    GameEntry.fromJson(json.getJSONObject(index))
                }.getOrNull()
                if (entry != null) {
                    add(entry)
                }
            }
        }
    }

    fun saveGames(games: List<GameEntry>) {
        val payload = JSONArray().apply {
            games.forEach { put(it.toJson()) }
        }
        prefs.edit().putString(KEY_GAMES, payload.toString()).apply()
    }

    fun saveGamesSync(games: List<GameEntry>): Boolean {
        val payload = JSONArray().apply {
            games.forEach { put(it.toJson()) }
        }
        return prefs.edit().putString(KEY_GAMES, payload.toString()).commit()
    }

    fun loadGridColumns(): Int = prefs.getInt(KEY_GRID_COLUMNS, 4).takeIf { it in setOf(1, 2, 3, 4, 8) } ?: 4

    fun saveGridColumns(columns: Int) {
        prefs.edit().putInt(KEY_GRID_COLUMNS, columns).apply()
    }

    fun loadAuthorizedRomTreeUri(): String? =
        prefs.getString(KEY_AUTHORIZED_ROM_TREE_URI, null)?.ifBlank { null }

    fun saveAuthorizedRomTreeUri(uri: String?) {
        prefs.edit().putString(KEY_AUTHORIZED_ROM_TREE_URI, uri).apply()
    }

    fun loadGenreFilter(): String =
        GameFilters.normalizeFilter(prefs.getString(KEY_GENRE_FILTER, GameFilters.ALL))

    fun saveGenreFilter(genre: String) {
        prefs.edit().putString(KEY_GENRE_FILTER, GameFilters.normalizeFilter(genre)).apply()
    }

    private companion object {
        const val KEY_GAMES = "games"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_AUTHORIZED_ROM_TREE_URI = "authorized_rom_tree_uri"
        const val KEY_GENRE_FILTER = "genre_filter"
    }
}
