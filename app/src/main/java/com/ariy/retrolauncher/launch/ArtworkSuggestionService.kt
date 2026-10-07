package com.ariy.retrolauncher.launch

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.regex.Pattern
import org.json.JSONObject

data class ArtworkCandidate(
    val title: String,
    val source: String,
    val imageUrl: String
)

object ArtworkSuggestionService {
    fun fetchCandidates(query: String): List<ArtworkCandidate> {
        if (query.isBlank()) return emptyList()
        val duckCandidates = fetchDuckDuckGoCandidates(query)
        if (duckCandidates.isNotEmpty()) return duckCandidates
        return fetchGoogleFallbackCandidates(query)
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

    private fun fetchGoogleFallbackCandidates(query: String): List<ArtworkCandidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val searchUrl = "https://www.google.com/search?tbm=isch&q=$encoded"
        val html = getHtml(searchUrl) ?: return emptyList()
        val imageUrls = extractImageUrls(html)
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

    private fun extractImageUrls(html: String): List<String> {
        val regex = Pattern.compile("""https:\\/\\/[^"'\\]+""")
        val matches = regex.matcher(html)
        val found = linkedSetOf<String>()
        while (matches.find()) {
            val candidate = matches.group()
                .replace("\\u003d", "=")
                .replace("\\u0026", "&")
                .replace("\\/", "/")
            val looksLikeImage = candidate.contains("gstatic.com/images") ||
                candidate.contains("encrypted-tbn0.gstatic.com") ||
                candidate.endsWith(".jpg") ||
                candidate.endsWith(".jpeg") ||
                candidate.endsWith(".png") ||
                candidate.endsWith(".webp")
            if (looksLikeImage) {
                found += candidate
            }
        }
        return found.toList()
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
