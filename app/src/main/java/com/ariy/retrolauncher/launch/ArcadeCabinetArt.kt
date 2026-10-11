package com.ariy.retrolauncher.launch

import android.content.Context
import org.json.JSONObject
import java.util.Locale

internal data class ArcadeCabinetArt(val asset: String, val kind: String, val notes: String)

internal fun parseArcadeCabinetArt(json: String): Map<String, ArcadeCabinetArt> {
    val rows = JSONObject(json).getJSONArray("mappings")
    return buildMap {
        repeat(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            if (!row.isNull("asset")) {
                val asset = row.getString("asset")
                require(asset.matches(Regex("[a-z0-9_]+\\.png"))) { "Invalid cabinet asset" }
                put(row.getString("rom").lowercase(Locale.ROOT), ArcadeCabinetArt(asset, row.getString("kind"), row.optString("notes")))
            }
        }
    }
}

internal object ArcadeCabinetArtStore {
    @Volatile private var cached: Map<String, ArcadeCabinetArt>? = null

    fun find(context: Context, raw: String?): ArcadeCabinetArt? {
        if (raw.isNullOrBlank()) return null
        val mappings = cached ?: synchronized(this) {
            cached ?: runCatching {
                context.assets.open("arcade-metadata/cabinet-art.json").bufferedReader().use { parseArcadeCabinetArt(it.readText()) }
            }.getOrDefault(emptyMap()).also { cached = it }
        }
        val stem = raw.trim().lowercase(Locale.ROOT).replace(Regex("\\.(zip|7z|chd)$"), "")
            .replace(Regex(" \\([0-9]+\\)$"), "")
        return mappings[stem] ?: ArcadeGameCatalog.find(raw)?.rom?.let(mappings::get)
    }
}