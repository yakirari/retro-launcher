package com.ariy.retrolauncher.launch

import com.ariy.retrolauncher.model.GamePlatforms
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Direct title-name lookup in Eden's public Switch box-art collection. */
internal object EdenBoxartCatalog {
    private const val IMAGE_ROOT = "https://raw.githubusercontent.com/eden-emulator/boxart/master/img/"
    private val cache = ConcurrentHashMap<String, List<ArtworkCandidate>>()

    fun fetchCandidates(platform: String?, rawTitle: String): List<ArtworkCandidate> {
        if (GamePlatforms.normalizeOrNull(platform) != "Nintendo Switch") return emptyList()
        val slugs = filenameSlugs(rawTitle)
        if (slugs.isEmpty()) return emptyList()
        val cacheKey = slugs.first()
        return cache.computeIfAbsent(cacheKey) {
            slugs.asSequence()
                .map { slug -> "$IMAGE_ROOT$slug.png" }
                .firstOrNull(::isAvailableImage)
                ?.let { imageUrl ->
                    listOf(
                        ArtworkCandidate(
                            title = rawTitle.trim(),
                            source = "Eden Switch box-art catalog",
                            imageUrl = imageUrl
                        )
                    )
                }
                .orEmpty()
        }
    }

    internal fun filenameSlugs(rawTitle: String): List<String> {
        val normalized = normalizedArtworkTitle(rawTitle, stripFileExtension = true).orEmpty()
            .lowercase()
            .replace("&", " and ")
            .replace(Regex("['’]"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        if (normalized.isBlank()) return emptyList()
        return listOf(normalized, normalized.replace("-", "")).distinct()
    }

    private fun isAvailableImage(imageUrl: String): Boolean = runCatching {
        val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 4_000
            readTimeout = 4_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RetroLauncher/1.0 (Eden artwork lookup)")
        }
        try {
            connection.responseCode in 200..299 &&
                connection.contentType.orEmpty().startsWith("image/", ignoreCase = true)
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)
}