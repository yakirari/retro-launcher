package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry

/** Apply a background result only if its original title/platform/artwork are still current. */
internal fun GameEntry.withAutofilledArtwork(expected: GameEntry, imageUri: String): GameEntry =
    if (id == expected.id && title == expected.title && platform == expected.platform &&
        this.imageUri == expected.imageUri
    ) copy(imageUri = imageUri) else this