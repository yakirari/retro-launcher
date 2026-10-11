package com.ariy.retrolauncher

import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Test

class PreparedGameImportTest {
    private fun entry(index: Int) = GameEntry(title = "Game $index", platform = "MAME Arcade",
        sourceType = GameSourceType.CUSTOM_INTENT, packageName = "mame",
        intentDataUri = "content://roms/$index.zip")

    @Test fun thousandItemImportUsesSnapshotsAndRescanIsIdempotent() {
        val original = listOf(entry(0).copy(title = "My title", imageUri = "file:///custom.png", playCount = 5))
        val incoming = (0..999).map(::entry)
        var library = original
        var added = 0
        incoming.chunked(64).forEach {
            val batch = prepareGameImport(library, it)
            library = batch.games
            added += batch.results.count { result -> result.outcome == UpsertOutcome.ADDED }
        }
        assertEquals(999, added)
        assertEquals(1000, library.size)
        assertEquals(original.first(), library.first())
        assertEquals(1, original.size)
        val repeated = prepareGameImport(library, incoming)
        assertTrue(repeated.results.all { it.outcome == UpsertOutcome.DUPLICATE })
        assertEquals(library, repeated.games)
    }

    @Test fun cancellationDuringPreparationCannotMutateSnapshot() {
        val original = listOf(entry(0))
        var visited = 0
        try {
            prepareGameImport(original, (1..100).map(::entry), checkpoint = {
                if (++visited == 8) throw kotlinx.coroutines.CancellationException("cancel")
            })
            fail("Expected cancellation")
        } catch (_: kotlinx.coroutines.CancellationException) {
            assertEquals(listOf(entry(0)).map { it.title }, original.map { it.title })
            assertEquals(1, original.size)
        }
    }
}