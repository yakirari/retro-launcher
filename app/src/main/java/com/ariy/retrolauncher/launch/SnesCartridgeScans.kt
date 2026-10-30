package com.ariy.retrolauncher.launch

import android.content.Context
import android.net.Uri
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.NostalgicOverlays
import org.json.JSONObject

internal object SnesCartridgeScans {
    const val MANIFEST = "snes_labels/index.json"
    data class Label(
        val title: String,
        val path: String,
        val width: Int,
        val height: Int,
        val sourcePack: String = ""
    )
    @Volatile private var cached: List<Label>? = null

    internal fun parse(text: String): List<Label> {
        val json = JSONObject(text)
        require(json.getInt("version") == 1)
        val rows = json.getJSONArray("labels")
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val path = row.getString("path")
            require(path.matches(Regex("labels/[a-z0-9_-]+\\.(?:jpg|png)")))
            val width = row.getInt("width")
            val height = row.getInt("height")
            require(width > 0 && height > 0)
            Label(row.getString("romTitle"), path, width, height, row.optString("sourcePack"))
        }
    }

    internal fun find(labels: List<Label>, rom: String): Label? {
        val hasRegion = Regex("\\((USA|Europe|Japan|World|Australia)(?:[, /][^)]*)?\\)", RegexOption.IGNORE_CASE).containsMatchIn(rom)
        val entries = labels.map { LibretroArtworkCatalog.CatalogEntry(href = it.path, displayTitle = it.title,
            matchKey = LibretroArtworkCatalog.artworkMatchKey(it.title)) }
        val paths = LibretroArtworkCatalog.selectNesEntries(entries, rom, rom).map { it.href }.distinct()
        paths.singleOrNull()?.let { path -> return labels.first { it.path == path } }
        if (hasRegion) return null
        val matches = labels.filter { it.path in paths }.distinctBy { it.path }
        if (matches.groupBy { it.title.lowercase() }.any { it.value.size > 1 }) return null
        // Unknown region permits a presentation fallback, not a claim about the ROM's release.
        return matches.sortedBy { it.path }.maxByOrNull { label ->
            val aspect = label.width.toFloat() / label.height
            requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo", aspect)).artworkFitScore(aspect)
        }
    }

    internal fun findForRom(labels: List<Label>, rom: String): Label? {
        find(labels, rom)?.let { return it }
        val withoutDuplicateSuffix = rom.replace(Regex("""\s+\(\d+\)(?=\.[^.]+$)"""), "")
        return withoutDuplicateSuffix.takeIf { it != rom }?.let { find(labels, it) }
    }

    internal fun findWithFallback(primary: List<Label>, fallback: List<Label>, rom: String): Label? =
        findForRom(primary, rom) ?: findForRom(fallback, rom)

    internal fun findWithFallback(labels: List<Label>, rom: String): Label? =
        findWithFallback(
            labels.filter { it.sourcePack !in setOf("game-labels-batch2", "game-labels-batch2-final", "supplied-png-pack") },
            labels.filter { it.sourcePack in setOf("game-labels-batch2", "game-labels-batch2-final", "supplied-png-pack") },
            rom
        )

    fun shellColor(game: GameEntry): Int? {
        if (GamePlatforms.normalizeOrNull(game.platform) != "Super Nintendo") return null
        val title = game.intentDataUri?.takeIf { it.isNotBlank() }
            ?.let { Uri.decode(it).substringAfterLast('/') } ?: game.title
        return when (LibretroArtworkCatalog.artworkMatchKey(title)) {
            "doom" -> 0xFFB52027.toInt()
            "killerinstinct" -> 0xFF202124.toInt()
            else -> null
        }
    }

    fun path(context: Context, game: GameEntry): String? {
        if (GamePlatforms.normalizeOrNull(game.platform) != "Super Nintendo" || game.cartridgeLabelUri != null) return null
        val manualCover = game.imageUri?.takeIf { it.isNotBlank() }?.let { source ->
            val uri = Uri.parse(source)
            uri.scheme != "file" || uri.lastPathSegment != "${game.id}-autofill.png"
        } ?: false
        if (manualCover) return null
        val rom = game.intentDataUri?.let { Uri.decode(it).substringAfterLast('/') } ?: return null
        val labels = cached ?: synchronized(this) {
            cached ?: run { context.assets.open(MANIFEST).bufferedReader().use { parse(it.readText()) } }
                .also { cached = it }
        }
        return findWithFallback(labels, rom)?.let { "snes_labels/${it.path}" }
    }
}