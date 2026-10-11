package com.ariy.retrolauncher.ui

internal object FilterNavigation {
    fun adjacent(current: String, options: List<String>, direction: Int): String? {
        if (options.isEmpty()) return null
        val index = options.indexOfFirst { it.equals(current, ignoreCase = true) }
        if (index < 0) return if (direction < 0) options.last() else options.first()
        return options[(index + if (direction < 0) options.size - 1 else 1) % options.size]
    }

    /** A worker's temporary empty loading snapshot is not an empty platform. */
    fun shouldSkipEmpty(ready: Boolean, itemCount: Int, libraryCount: Int, isAll: Boolean): Boolean =
        ready && itemCount == 0 && libraryCount > 0 && !isAll
}