package com.ariy.retrolauncher.launch

import org.jsoup.Jsoup
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Alternate, explicitly typed front covers. Never use search thumbnails or gameplay images. */
internal object NesPortraitArtworkCatalog {
    private const val ROOT = "https://gamesdb.launchbox-app.com"
    private val cache = ConcurrentHashMap<String, List<ArtworkCandidate>>()
    private fun title(value: String): String = normalizedArtworkTitle(value, stripFileExtension = true).orEmpty()
    internal fun key(value: String): String = title(value).lowercase()
        .replace(Regex("\\bplayers\\b"), "player").replace(Regex("[^a-z0-9]"), "")

    fun fetchCandidates(rom: String): List<ArtworkCandidate> {
        cache[rom]?.let { return it }
        return try {
            val query = title(rom).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
                .replace(Regex("(?i)\\bplayer$"), "players")
            val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
            val search = download("$ROOT/games/results/$encoded?searchText=$encoded")
            val pages = parseSearch(search, rom)
            pages.take(2).flatMap { parseDetails(download(it), rom) }.distinctBy { it.imageUrl }
                .also { cache[rom] = it }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parseSearch(html: String, rom: String): List<String> = Jsoup.parse(html, ROOT)
        .select("a[href^=/games/details/]").filter { card ->
            key(card.selectFirst("h3")?.text().orEmpty()) == key(rom) &&
                card.select("p").any { it.text().trim() == "Nintendo Entertainment System" }
        }.map { it.absUrl("href") }.distinct()

    internal fun parseDetails(html: String, rom: String): List<ArtworkCandidate> {
        val doc = Jsoup.parse(html, ROOT)
        // Rendered img.src is a small preview even when alt advertises original
        // dimensions. Nuxt's flattened payload links each preview to its full image.
        val originals = mutableMapOf<String, String>()
        runCatching {
            val payload = JSONArray(doc.selectFirst("script#__NUXT_DATA__")?.data().orEmpty())
            for (i in 0 until payload.length()) {
                val entry = payload.opt(i) as? JSONObject ?: continue
                fun referenced(name: String): String? {
                    val index = (entry.opt(name) as? Number)?.toInt() ?: return null
                    return payload.opt(index) as? String
                }
                val preview = referenced("imageFileName") ?: continue
                val full = referenced("fullGameImageFileName") ?: continue
                if (full.matches(Regex("[a-zA-Z0-9_-]+\\.(png|jpg|jpeg|webp)"))) originals[preview] = full
            }
        }
        if (key(doc.selectFirst("h1")?.text().orEmpty()) != key(rom)) return emptyList()
        val platform = doc.select("dt").firstOrNull { it.text().trim() == "Platform" }?.nextElementSibling()?.text()
        if (platform != "Nintendo Entertainment System") return emptyList()
        if ('[' in rom || ']' in rom) return emptyList()
        // Unknown publisher/prototype metadata cannot be verified against a bare game page title.
        val qualifiers = Regex("\\(([^)]*)\\)").findAll(rom).map { it.groupValues[1] }
        if (qualifiers.any { q -> !q.matches(Regex("(?i)(USA|Europe|Japan|World|En|Ja|Fr|De|Es|It|Rev(?:ision)?\\s*[0-9.]+)")) }) return emptyList()
        data class Image(val candidate: ArtworkCandidate, val rank: Int, val area: Long)
        return doc.select("img[alt][src]").mapNotNull { image ->
            val alt = image.attr("alt")
            val match = Regex("^(.+?) - (Box - Front(?: - Reconstructed)?|Fanart - Box - Front) \\(([^)]+)\\) - ([0-9]+)x([0-9]+)$").matchEntire(alt)
                ?: return@mapNotNull null
            if (key(match.groupValues[1]) != key(rom)) return@mapNotNull null
            val type = match.groupValues[2]
            val region = match.groupValues[3].replace("North America", "USA")
            val exactRegion = LibretroArtworkCatalog.regionRank("($region)", rom) == 0
            val neutralFanart = region == "World" && type.startsWith("Fanart")
            if (!exactRegion && !neutralFanart) return@mapNotNull null
            val width = match.groupValues[4].toIntOrNull() ?: return@mapNotNull null
            val height = match.groupValues[5].toIntOrNull() ?: return@mapNotNull null
            if (width < 400 || height < 500 || width.toFloat() / height !in 0.5f..0.85f) return@mapNotNull null
            val previewUrl = image.attr("src")
            val previewUri = runCatching { URI(previewUrl) }.getOrNull() ?: return@mapNotNull null
            if (previewUri.scheme != "https" || previewUri.host !in setOf("images.launchbox-app.com", "gamesdb-images.launchbox.gg") || previewUri.userInfo != null) return@mapNotNull null
            val original = originals[previewUrl.substringAfterLast('/')]
            val url = if (original != null) "https://images.launchbox-app.com/$original" else previewUrl
            val uri = runCatching { URI(url) }.getOrNull() ?: return@mapNotNull null
            if (uri.scheme != "https" || uri.host !in setOf("images.launchbox-app.com", "gamesdb-images.launchbox.gg") || uri.userInfo != null) return@mapNotNull null
            Image(ArtworkCandidate(match.groupValues[1], "LaunchBox $type ($region)", url),
                (if (exactRegion) 0 else 2) + (if (type.startsWith("Fanart")) 1 else 0), width.toLong() * height)
        }.sortedWith(compareBy<Image> { it.rank }.thenByDescending { it.area }).map { it.candidate }
    }

    private fun download(url: String): String = Jsoup.connect(url).userAgent("RetroLauncher/1.0 (NES front-cover lookup)")
        .timeout(15_000).maxBodySize(4 * 1024 * 1024).execute().body()
}