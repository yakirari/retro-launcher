package com.ariy.retrolauncher

/** A reset invalidates callbacks as well as workers, even inside NonCancellable cleanup. */
internal class ResetGeneration {
    @Volatile var current: Long = 0
        private set
    @Volatile var resetting: Boolean = false
        private set

    fun accepts(generation: Long): Boolean = !resetting && generation == current

    @Synchronized fun begin(): Boolean {
        if (resetting) return false
        resetting = true
        current++
        return true
    }

    fun finish() { resetting = false }
}