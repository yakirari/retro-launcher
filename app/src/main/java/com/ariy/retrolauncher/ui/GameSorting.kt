package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import java.util.Locale

internal enum class GameSortOrder(val label: String) {
    ALPHABETICAL("Alphabetical (A–Z)"),
    YEAR_ASCENDING("Year (oldest first)"),
    YEAR_DESCENDING("Year (newest first)"),
    POPULARITY("Popularity (most played)");

    companion object {
        fun fromStored(raw: String?): GameSortOrder = entries.firstOrNull { it.name == raw } ?: ALPHABETICAL
    }
}

/** Played/unplayed groups always precede the selected sort; no guessed public ratings. */
internal fun sortLibraryGames(games: List<GameEntry>, order: GameSortOrder): List<GameEntry> {
    val byTitle = compareBy<GameEntry> { curatedDisplayTitle(it.libraryDisplayTitle()).lowercase(Locale.ROOT) }
    val selected = when (order) {
        GameSortOrder.ALPHABETICAL -> byTitle
        GameSortOrder.YEAR_ASCENDING -> compareBy<GameEntry> { it.libraryReleaseYear() == null }
            .thenBy { it.libraryReleaseYear() }.then(byTitle)
        GameSortOrder.YEAR_DESCENDING -> compareBy<GameEntry> { it.libraryReleaseYear() == null }
            .thenByDescending { it.libraryReleaseYear() }.then(byTitle)
        GameSortOrder.POPULARITY -> compareByDescending<GameEntry> { it.playCount.coerceAtLeast(0) }
            .thenByDescending { it.lastPlayedAtEpochMillis ?: Long.MIN_VALUE }.then(byTitle)
    }
    return games.sortedWith(compareByDescending<GameEntry> { it.lastPlayedAtEpochMillis != null || it.playCount > 0 }.then(selected))
}