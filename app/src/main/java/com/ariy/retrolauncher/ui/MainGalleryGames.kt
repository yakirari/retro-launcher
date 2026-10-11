package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GameSourceType

/** The mixed main gallery is for games; standalone apps remain available in Android. */
internal fun mainGalleryGames(games: List<GameEntry>, filter: String): List<GameEntry> =
    if (filter.equals(GameFilters.ALL, ignoreCase = true)) games.filterNot { it.sourceType == GameSourceType.ACTIVITY }
    else games