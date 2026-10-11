package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry

/** Saved cover state is the durable queue: unfinished entries survive process death. */
internal fun missingArtworkToResume(
    games: List<GameEntry>,
    inFlightIds: Set<String>,
    retryAfterMillis: Map<String, Long>,
    nowMillis: Long
): List<GameEntry> = games.filter { game ->
    game.imageUri.isNullOrBlank() && game.title.isNotBlank() &&
        game.id !in inFlightIds && nowMillis >= (retryAfterMillis[game.id] ?: 0L)
}