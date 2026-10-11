package com.ariy.retrolauncher.launch

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/** Exact-name fallback using Steam's public store search and app-details endpoints. */
internal object SteamStoreArtworkCatalog {
    private val cache = ConcurrentHashMap<String, List<ArtworkCandidate>>()

    fun fetchCandidates(rawTitle: String): List<ArtworkCandidate> {
        val title = catalogTitle(rawTitle)
        val key = matchKey(title)
        if (key.isBlank()) return emptyList()
        return cache[key] ?: lookupExactTitle(title, key).also { candidates ->
            // An offline/transient failure is not a permanent catalog miss.
            if (candidates.isNotEmpty()) cache[key] = candidates
        }
    }

    internal fun catalogTitle(rawTitle: String): String =
        rawTitle.trim().replace(Regex("""(?i)\.(?:exe|lnk|url)$"""), "")
            .replace(Regex("""\s+(?:US|EU|EUR|JP)$"""), "").trim()

    internal fun exactAppId(searchJson: String, expectedTitleKey: String): String? = runCatching {
        val items = JSONObject(searchJson).optJSONArray("items") ?: return null
        (0 until items.length())
            .asSequence()
            .mapNotNull { index -> runCatching { items.getJSONObject(index) }.getOrNull() }
            .firstOrNull { item -> matchKey(item.optString("name")) == expectedTitleKey }
            ?.opt("id")
            ?.toString()
            ?.takeIf(String::isNotBlank)
    }.getOrNull()

    internal fun appHeaderImage(detailsJson: String, appId: String): String? = runCatching {
        val app = JSONObject(detailsJson).optJSONObject(appId) ?: return null
        if (!app.optBoolean("success")) return null
        val imageUrl = app.optJSONObject("data")?.optString("header_image")
            ?.takeIf { it.startsWith("https://", ignoreCase = true) }
        imageUrl
    }.getOrNull()

    private fun lookupExactTitle(title: String, titleKey: String): List<ArtworkCandidate> = runCatching {
        val encodedTitle = URLEncoder.encode(title, Charsets.UTF_8.name())
        val searchJson = getText(
            "https://store.steampowered.com/api/storesearch/?term=$encodedTitle&l=english&cc=us"
        ) ?: return emptyList()
        val appId = exactAppId(searchJson, titleKey) ?: return emptyList()
        val detailsJson = getText(
            "https://store.steampowered.com/api/appdetails?appids=$appId&cc=us&l=en"
        ) ?: return emptyList()
        val imageUrl = appHeaderImage(detailsJson, appId) ?: return emptyList()
        val portrait = portraitImageUrls(appId).firstOrNull(::isAvailableImage)
        if (portrait != null) listOf(
            ArtworkCandidate(title = title, source = "Steam Store · exact title · portrait", imageUrl = portrait)
        ) else listOf(ArtworkCandidate(
                title = title,
                source = "Steam Store · exact title",
                imageUrl = imageUrl
            ))
    }.getOrDefault(emptyList())

    internal fun portraitImageUrls(appId: String): List<String> {
        if (appId.isBlank() || !appId.all(Char::isDigit)) return emptyList()
        val root = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/"
        return listOf("${root}library_600x900.jpg", "${root}portrait.png")
    }

    private fun isAvailableImage(url: String): Boolean = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 6_000
            readTimeout = 8_000
            instanceFollowRedirects = true
        }
        try {
            connection.responseCode in 200..299 &&
                connection.contentType.orEmpty().startsWith("image/", ignoreCase = true)
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)

    private fun matchKey(raw: String): String = raw.lowercase().replace(Regex("[^a-z0-9]+"), "")

    private fun getText(url: String): String? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RetroLauncher/1.0 (game artwork lookup)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}