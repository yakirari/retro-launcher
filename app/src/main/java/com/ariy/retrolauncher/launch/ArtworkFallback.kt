package com.ariy.retrolauncher.launch

/** Fetch subsequent sources lazily, only when every image in the preceding batch failed. */
internal fun <T : Any> firstSuccessfulArtworkBatch(
    batches: Sequence<List<ArtworkCandidate>>,
    download: (List<ArtworkCandidate>) -> T?
): T? {
    val attempted = mutableSetOf<String>()
    for (batch in batches) {
        val candidates = batch.filter { it.imageUrl.isNotBlank() && attempted.add(it.imageUrl) }
        if (candidates.isEmpty()) continue
        download(candidates)?.let { return it }
    }
    return null
}