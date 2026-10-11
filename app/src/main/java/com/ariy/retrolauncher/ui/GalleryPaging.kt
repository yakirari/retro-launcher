package com.ariy.retrolauncher.ui

/** A growing prefix keeps lazy item indices and keys stable while pages arrive ahead of scrolling. */
internal object GalleryPaging {
    const val PAGE_SIZE = 48
    const val PREFETCH_DISTANCE = 12

    fun initialCount(total: Int): Int = total.coerceIn(0, PAGE_SIZE)

    fun countThrough(total: Int, current: Int, requiredIndex: Int): Int {
        if (total <= 0) return 0
        val index = requiredIndex.coerceIn(0, total - 1)
        val pageEnd = ((index.toLong() / PAGE_SIZE + 1) * PAGE_SIZE).coerceAtMost(total.toLong()).toInt()
        return maxOf(current.coerceIn(0, total), pageEnd)
    }

    fun nextCount(total: Int, current: Int, lastVisibleIndex: Int): Int {
        val loaded = current.coerceIn(0, total.coerceAtLeast(0))
        if (loaded == 0) return initialCount(total)
        if (lastVisibleIndex < loaded - PREFETCH_DISTANCE || loaded == total) return loaded
        return countThrough(total, loaded, loaded)
    }
}