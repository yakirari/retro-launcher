package com.ariy.retrolauncher.launch

import android.content.Context
import android.graphics.BitmapFactory
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GamePlatforms
import org.json.JSONObject

internal object NesBundledArtwork {
    const val MANIFEST = "nes_covers/index.json"
    data class Cover(val title: String, val path: String, val sha256: String)
    @Volatile private var cached: List<Cover>? = null

    internal fun parse(text: String): List<Cover> {
        val json = JSONObject(text)
        require(json.getInt("version") == 1)
        val rows = json.getJSONArray("covers")
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val path = row.getString("path")
            val hash = row.getString("sha256")
            require(path.matches(Regex("front/[a-z0-9_-]+\\.jpg")))
            require(hash.matches(Regex("[a-f0-9]{64}")))
            Cover(row.getString("romTitle"), path, hash)
        }
    }

    internal fun find(covers: List<Cover>, rom: String): Cover? {
        if (!Regex("\\((USA|Europe|Japan|World|Australia)(?:[, /][^)]*)?\\)", RegexOption.IGNORE_CASE).containsMatchIn(rom)) return null
        val entries = covers.map { LibretroArtworkCatalog.CatalogEntry(href = it.path, displayTitle = it.title,
            matchKey = LibretroArtworkCatalog.artworkMatchKey(it.title)) }
        val matches = LibretroArtworkCatalog.selectNesEntries(entries, rom, rom)
        val paths = matches.map { it.href }.distinct()
        return paths.singleOrNull()?.let { path -> covers.first { it.path == path } }
    }

    fun cover(context: Context, game: GameEntry): Cover? {
        if (GamePlatforms.normalizeOrNull(game.platform) != "Nintendo Entertainment System" || game.cartridgeLabelUri != null) return null
        val rom = game.intentDataUri?.let { android.net.Uri.decode(it).substringAfterLast('/') } ?: return null
        val covers = cached ?: synchronized(this) {
            cached ?: run {
                context.assets.open(MANIFEST).bufferedReader().use { parse(it.readText()) }
            }.also { cached = it }
        }
        return find(covers, rom)
    }

    fun save(context: Context, game: GameEntry, store: ImportedIconStore): String? {
        if (!game.imageUri.isNullOrBlank() && !NesArtworkPolicy.ownsAutomaticCover(game)) return null
        val cover = cover(context, game) ?: return null
        val name = "${game.id}-nes-bundled-v1-${cover.sha256}"
        store.existingBitmapUri(name)?.let { return it }
        val bitmap = context.assets.open("nes_covers/${cover.path}").use { BitmapFactory.decodeStream(it) } ?: return null
        return try { store.saveBitmap(name, bitmap) } finally { bitmap.recycle() }
    }
}