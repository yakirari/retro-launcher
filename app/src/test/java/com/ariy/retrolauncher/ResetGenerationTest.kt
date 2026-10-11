package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GlobalGallerySettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class ResetGenerationTest {
    @Test fun resetRejectsOldWorkersAndCallbacksEvenAfterNewSessionStarts() {
        val guard = ResetGeneration()
        val old = guard.current
        assertTrue(guard.accepts(old))
        assertTrue(guard.begin())
        assertFalse(guard.begin())
        assertFalse(guard.accepts(old))
        assertFalse(guard.accepts(guard.current))
        guard.finish()
        assertFalse(guard.accepts(old))
        assertTrue(guard.accepts(guard.current))
        assertTrue(guard.begin())
        assertEquals(2L, guard.current)
    }

    @Test fun cancelledScanCannotCommitOrScheduleArtworkFromNonCancellableFinally() = runBlocking {
        val guard = ResetGeneration()
        val generation = guard.current
        val started = CompletableDeferred<Unit>()
        var committed = false
        var scheduledArtwork = false
        val worker = launch(Dispatchers.Default) {
            try {
                started.complete(Unit)
                CompletableDeferred<Unit>().await()
            } finally {
                withContext(NonCancellable) {
                    if (guard.accepts(generation)) {
                        committed = true
                        scheduledArtwork = true
                    }
                }
            }
        }
        started.await()
        guard.begin()
        worker.cancelAndJoin()
        guard.finish()
        assertFalse(committed)
        assertFalse(scheduledArtwork)
        assertFalse(guard.accepts(generation))
    }

    @Test fun clearingPreferencesReloadsDefaultGalleryNotLegacyDisplayState() {
        val defaults = GlobalGallerySettings.fromPreferenceValues(emptyMap<String, Any>())
        assertEquals(GlobalGallerySettings.GRID, defaults.layoutMode)
        assertEquals(2, defaults.gridTileCount)
        assertEquals(2, defaults.shelfTileCount)
        assertEquals("transparent", defaults.tileBackgroundMode)
        assertTrue(defaults.showGameCaptions)
    }
}