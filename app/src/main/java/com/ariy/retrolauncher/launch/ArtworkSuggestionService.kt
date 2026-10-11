package com.ariy.retrolauncher.launch

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.regex.Pattern
import org.json.JSONObject
import com.ariy.retrolauncher.model.GameEntry

data class ArtworkCandidate(
    val title: String,
    val source: String,
    val imageUrl: String
)

object ArtworkSuggestionService {
    fun fetchCandidates(game: GameEntry, query: String): List<ArtworkCandidate> =
        fetchCandidateBatches(game, listOf(query)).firstOrNull { it.isNotEmpty() }.orEmpty()

    internal fun fetchCandidateBatches(game: GameEntry, queries: List<String>): Sequence<List<ArtworkCandidate>> = sequence {
        val searchQueries = queries.filter(String::isNotBlank).distinct()
        if (searchQueries.isEmpty()) return@sequence
        val scummVm = com.ariy.retrolauncher.model.GamePlatforms.normalizeOrNull(game.platform) == "ScummVM"
        val digital = game.platform in com.ariy.retrolauncher.model.PcGameStores.platforms || game.platform == "PC (Windows)"
        val catalogTitle = if (scummVm) {
            ScummVmGames.artworkTitle(game.title)
        } else if (digital) {
            SteamStoreArtworkCatalog.catalogTitle(game.title)
        } else if (com.ariy.retrolauncher.model.GamePlatforms.normalizeOrNull(game.platform) == "Sony PlayStation Portable") {
            pspArtworkTitle(game.title)
        } else normalizedArtworkTitle(game.title, stripFileExtension = true)
            ?: normalizedArtworkTitle(searchQueries.first(), stripFileExtension = true)
        if (catalogTitle != null) {
            if (digital) yield(SteamStoreArtworkCatalog.fetchCandidates(catalogTitle))
            if (!game.platform.equals("Nintendo Switch", ignoreCase = true) &&
                !game.platform.equals("Switch", ignoreCase = true)
            ) {
                yield(EdenBoxartCatalog.fetchCandidates(game.platform, catalogTitle))
            }
            yield(LibretroArtworkCatalog.fetchCandidates(game.platform, catalogTitle))
            if (game.platform.equals("Nintendo Switch", ignoreCase = true) ||
                game.platform.equals("Switch", ignoreCase = true)
            ) {
                yield(SteamStoreArtworkCatalog.fetchCandidates(catalogTitle))
            }
        }
        // A missing exact ScummVM cover is preferable to a generic image for a short engine key.
        if (!scummVm) for (query in searchQueries) yieldAll(fetchWebCandidateBatches(query))
    }

    fun fetchCandidates(query: String): List<ArtworkCandidate> =
        fetchWebCandidateBatches(query).firstOrNull { it.isNotEmpty() }.orEmpty()

    private fun fetchWebCandidateBatches(query: String): Sequence<List<ArtworkCandidate>> = sequence {
        if (query.isBlank()) return@sequence
        val duckCandidates = fetchDuckDuckGoCandidates(query)
            .filterNot { isPhysicalProductSearchResult(it.title) }
            .filter { isRelevantArtworkTitle(query, it.title) }
            .distinctBy(ArtworkCandidate::imageUrl)
        yield(duckCandidates.take(18))

        val bingCandidates = fetchBingFallbackCandidates(query)
            .distinctBy(ArtworkCandidate::imageUrl)
        yield(bingCandidates.take(6))

        yield(fetchGoogleFallbackCandidates(query)
            .distinctBy(ArtworkCandidate::imageUrl)
            .take(6))
    }

    internal fun isRelevantArtworkTitle(query: String, resultTitle: String): Boolean {
        val contextTerms = setOf(
            "art", "artwork", "game", "illustration", "key", "nintendo", "official", "poster",
            "promotional", "screenshot", "switch", "video", "wallpaper", "playstation", "portable", "psp"
        )
        val ignoredTerms = setOf(
            "art", "artwork", "box", "case", "cover", "front", "game", "image", "illustration",
            "key", "nintendo", "official", "png", "poster", "promotional", "scan", "switch", "the",
            "video", "wallpaper", "playstation", "portable", "psp"
        )
        val positiveQuery = query.replace(Regex("""(?:^|\s)-(?:"[^"]*"|\S+)"""), " ")
        val queryTerms = positiveQuery.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() && (it.length > 1 || it.all(Char::isDigit)) && it !in ignoredTerms }
            .toSet()
        if (queryTerms.isEmpty()) return true
        val resultTerms = resultTitle.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .toSet()
        val hasTitleMatch = queryTerms.all { it in resultTerms }
        if (!hasTitleMatch) return false
        val shortAmbiguousTitle = queryTerms.all { it.length <= 3 }
        return !shortAmbiguousTitle || positiveQuery.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .any { it in contextTerms && it in resultTerms }
    }

    internal fun isPhysicalProductSearchResult(title: String): Boolean =
        Regex(
            """(?i)\b(?:sealed|unopened|unboxing|for sale|listing|empty case|case only|physical copy|console bundle|lot of \d+|complete in box|\bcib\b)\b"""
        ).containsMatchIn(title)

    internal fun extractImageUrlsFromHtml(html: String): List<String> {
        val decoded = html
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("&amp;", "&")
            .replace("&#x2F;", "/")
            .replace("&#x3A;", ":")
            .replace("&#x3D;", "=")
            .replace("&quot;", "\"")

        val preferred = linkedSetOf<String>()
        val patterns = listOf(
            Regex("""(?i)(?:imgurl|murl|image_url|imageUrl|ou|src)\\s*[:=]\\s*[\"']?(https?://[^\"'\\s>]+)"""),
            Regex("""(?i)<img[^>]+src=[\"'](https?://[^\"']+)[\"']""")
        )
        patterns.forEach { pattern ->
            pattern.findAll(decoded).forEach { match ->
                val candidate = cleanImageCandidate(match.groupValues.lastOrNull().orEmpty())
                if (candidate != null) {
                    preferred += candidate
                }
            }
        }
        if (preferred.isNotEmpty()) return preferred.toList()

        val found = linkedSetOf<String>()
        var scanIndex = 0
        while (scanIndex < decoded.length) {
            val httpIndex = decoded.indexOf("http", startIndex = scanIndex)
            if (httpIndex < 0) break
            var endIndex = httpIndex + "http".length
            while (endIndex < decoded.length) {
                val char = decoded[endIndex]
                val stopsUrl = char == '"' || char == '\'' || char == ',' || char == ' ' || char == '<' ||
                    char == '>' || char == ')' || char == ']' || char == '}' || char == ';' ||
                    char == '\n' || char == '\r' || char == '\t'
                if (stopsUrl) break
                endIndex += 1
            }
            val candidate = cleanImageCandidate(decoded.substring(httpIndex, endIndex))
            if (candidate != null) {
                found += candidate
            }
            scanIndex = endIndex + 1
        }
        return found.toList()
    }

    private fun fetchDuckDuckGoCandidates(query: String): List<ArtworkCandidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val vqd = fetchDuckVqd(encoded) ?: return emptyList()
        val apiUrl = "https://duckduckgo.com/i.js?l=us-en&o=json&p=1&q=$encoded&vqd=$vqd"
        val payload = getJson(apiUrl, "https://duckduckgo.com/?q=$encoded&iax=images&ia=images")
            ?: return emptyList()
        val results = runCatching { payload.getJSONArray("results") }.getOrNull() ?: return emptyList()
        return buildList {
            repeat(results.length()) { index ->
                val result = runCatching { results.getJSONObject(index) }.getOrNull() ?: return@repeat
                val imageUrl = result.optString("image").ifBlank { null } ?: return@repeat
                val title = result.optString("title").ifBlank { query }
                add(
                    ArtworkCandidate(
                        title = title,
                        source = "DuckDuckGo Images",
                        imageUrl = imageUrl
                    )
                )
            }
        }
    }

    private fun fetchDuckVqd(encodedQuery: String): String? {
        val html = getHtml("https://duckduckgo.com/?q=$encodedQuery&iax=images&ia=images") ?: return null
        val patterns = listOf(
            Pattern.compile("vqd=\\\\?\"([^\"]+)\\\\?\""),
            Pattern.compile("vqd='([^']+)'"),
            Pattern.compile("vqd=\"([^\"]+)\"")
        )
        patterns.forEach { regex ->
            val match = regex.matcher(html)
            if (match.find()) {
                return match.group(1)
            }
        }
        return null
    }

    private fun fetchBingFallbackCandidates(query: String): List<ArtworkCandidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val searchUrl = "https://www.bing.com/images/search?q=$encoded&form=HDRSC2"
        val html = getHtml(searchUrl) ?: return emptyList()
        val imageUrls = extractImageUrlsFromHtml(html)
        return buildList {
            imageUrls.take(6).forEach { imageUrl ->
                add(
                    ArtworkCandidate(
                        title = query,
                        source = "Bing Images",
                        imageUrl = imageUrl
                    )
                )
            }
        }
    }

    private fun fetchGoogleFallbackCandidates(query: String): List<ArtworkCandidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val searchUrl = "https://www.google.com/search?tbm=isch&q=$encoded"
        val html = getHtml(searchUrl) ?: return emptyList()
        val imageUrls = extractImageUrlsFromHtml(html)
        return buildList {
            imageUrls.take(6).forEach { imageUrl ->
                add(
                    ArtworkCandidate(
                        title = query,
                        source = "Google Images",
                        imageUrl = imageUrl
                    )
                )
            }
        }
    }

    private fun getJson(url: String, referer: String): JSONObject? = runCatching {
        openConnection(url, referer)
            .inputStream
            .bufferedReader()
            .use { JSONObject(it.readText()) }
    }.getOrNull()

    private fun getHtml(url: String): String? = runCatching {
        openConnection(url).inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun cleanImageCandidate(raw: String): String? {
        val candidate = raw
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x2F;", "/")
            .replace("&#x3A;", ":")
            .replace("&#x3D;", "=")
            .trim()
            .trim('"', '\'')
            .takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return null

        if (candidate.contains("bing.com/sa/simg") ||
            candidate.contains("facebook_sharing") ||
            candidate.contains("microsoft.com") ||
            candidate.contains("google.com/images/branding") ||
            candidate.contains("/googlelogo") ||
            candidate.contains("/logo.") ||
            candidate.contains("/favicon.")) {
            return null
        }

        val extension = candidate.substringAfterLast('.', "").substringBefore('?', "").lowercase()
        if (extension !in setOf("jpg", "jpeg", "png", "webp", "gif", "avif")) {
            return null
        }
        return candidate
    }

    private fun openConnection(url: String, referer: String? = null): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            doInput = true
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 Chrome/123.0 Safari/537.36")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            if (!referer.isNullOrBlank()) {
                setRequestProperty("Referer", referer)
            }
        }
}
