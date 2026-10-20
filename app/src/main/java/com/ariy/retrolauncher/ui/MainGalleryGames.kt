package com.ariy.retrolauncher.ui

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.GameSourceType

/** The mixed gallery is for games; app and emulator tiles retain their dedicated galleries. */
internal fun mainGalleryGames(games: List<GameEntry>, filter: String): List<GameEntry> =
    if (filter.equals(GameFilters.ALL, ignoreCase = true)) games.filterNot {
        it.sourceType == GameSourceType.ACTIVITY || it.platform.equals(GamePlatforms.EMULATORS, ignoreCase = true)
    }
    else games