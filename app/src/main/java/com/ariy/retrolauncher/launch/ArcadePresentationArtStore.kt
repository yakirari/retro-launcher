package com.ariy.retrolauncher.launch

import android.content.Context
import org.json.JSONObject

/** The supplied front designs are shared by grid, stacked, shelf and recommendation previews. */
object ArcadePresentationArtStore {
    @Volatile private var entries: Map<String, String>? = null

    fun path(context: Context, originalAsset: String): String? {
        val rows = entries ?: synchronized(this) {
            entries ?: run {
                val json = context.assets.open("arcade_presentations/index.json").bufferedReader().use { JSONObject(it.readText()) }
                val array = json.getJSONArray("mappings")
                buildMap {
                    for (i in 0 until array.length()) {
                        val row = array.getJSONObject(i)
                        val front = row.getString("front")
                        if (front.matches(Regex("front/[a-z0-9_]+\\.png"))) {
                            put(row.getString("asset"), front)
                        }
                    }
                }
            }.also { entries = it }
        }
        val row = rows[originalAsset] ?: return null
        return "arcade_presentations/$row"
    }
}