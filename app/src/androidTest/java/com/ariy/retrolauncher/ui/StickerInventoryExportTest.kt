package com.ariy.retrolauncher.ui

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.data.GameRepository
import com.ariy.retrolauncher.launch.SnesCartridgeScans
import com.ariy.retrolauncher.model.NostalgicOverlays
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit, read-only library audit; never copies ROMs or changes stored artwork. */
class StickerInventoryExportTest {
    @Test fun exportDeviceStickerCoverage(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("exportStickerInventory") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rows = JSONArray()
        for (game in GameRepository(context).loadGames().filter {
            it.platform in setOf("Nintendo Entertainment System", "Super Nintendo")
        }) {
            val snesPath = SnesCartridgeScans.path(context, game)
            val sticker = if (game.cartridgeLabelUri != null) null else if (game.platform == "Super Nintendo") {
                snesPath?.let { decodeSampledArtworkAsset(context, it, 768) }
            } else NesCartridgeLabelStore.load(context, game)
            val spec = NostalgicOverlays.specForArtwork(game.platform,
                sticker?.let { it.width.toFloat() / it.height })
            rows.put(JSONObject().apply {
                put("id", game.id)
                put("title", game.title)
                put("platform", game.platform)
                put("romFilename", game.intentDataUri?.let { Uri.decode(it).substringAfterLast('/') })
                put("imageUri", game.imageUri ?: JSONObject.NULL)
                put("status", when {
                    game.cartridgeLabelUri != null -> "manual-sticker"
                    sticker != null -> "verified-sticker"
                    else -> "needs-sticker"
                })
                put("stickerAsset", snesPath ?: JSONObject.NULL)
                if (sticker != null) {
                    put("stickerWidth", sticker.width)
                    put("stickerHeight", sticker.height)
                    put("windowAspect", spec?.let {
                        it.aspectRatio * it.artworkWidthFraction / it.artworkHeightFraction
                    })
                }
            })
        }
        File(context.cacheDir, "nes-snes-sticker-inventory.json").writeText(
            JSONObject().put("version", 1).put("games", rows).toString(2))
    }
}
