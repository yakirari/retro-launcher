package com.ariy.retrolauncher.launch

import org.junit.Assert.*
import org.junit.Test

class ArtworkFallbackTest {
    private fun candidate(url: String) = ArtworkCandidate("Game", "Catalog", url)

    @Test fun failedCatalogFallsThroughToNextSource() {
        val batches = sequenceOf(listOf(candidate("broken")), listOf(candidate("working")))
        val attempted = mutableListOf<String>()
        val result = firstSuccessfulArtworkBatch(batches) {
            val url = it.single().imageUrl
            attempted += url
            if (url == "working") "saved" else null
        }
        assertEquals("saved", result)
        assertEquals(listOf("broken", "working"), attempted)
    }

    @Test fun successfulCatalogDoesNotFetchLaterSources() {
        val batches = sequence {
            yield(listOf(candidate("working")))
            error("Unnecessary network source was requested")
        }
        assertEquals("saved", firstSuccessfulArtworkBatch(batches) { "saved" })
    }

    @Test fun skipsEmptyBatchesAndDuplicateUrlsAcrossSources() {
        val batches = sequenceOf(emptyList(), listOf(candidate("broken")),
            listOf(candidate("broken"), candidate("working")))
        val attempted = mutableListOf<String>()
        firstSuccessfulArtworkBatch(batches) {
            attempted += it.map(ArtworkCandidate::imageUrl)
            if (it.any { candidate -> candidate.imageUrl == "working" }) "saved" else null
        }
        assertEquals(listOf("broken", "working"), attempted)
    }

    @Test fun allFailedSourcesReturnNull() {
        assertNull(firstSuccessfulArtworkBatch<String>(sequenceOf(listOf(candidate("broken")))) { null })
    }
}