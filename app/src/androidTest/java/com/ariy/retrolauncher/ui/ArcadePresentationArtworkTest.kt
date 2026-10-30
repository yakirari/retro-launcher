package com.ariy.retrolauncher.ui

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.launch.ArcadePresentationArtStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class ArcadePresentationArtworkTest {
    @Test fun everyPackagedFrontMatchesItsManifestForAllLayouts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rows = context.assets.open("arcade_presentations/index.json").bufferedReader().use {
            JSONObject(it.readText()).getJSONArray("mappings")
        }
        assertEquals(381, rows.length())
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val asset = row.getString("asset")
            val path = requireNotNull(ArcadePresentationArtStore.path(context, asset))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val bytes = context.assets.open(path).use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            assertEquals(row.optInt("frontWidth", 460), bounds.outWidth)
            assertEquals(row.optInt("frontHeight", 1000), bounds.outHeight)
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals("arcade_presentations/${row.getString("front")}", path)
            assertEquals("Supplied front bytes: $asset", row.getString("frontSha256"), hash)
        }
        assertNull(ArcadePresentationArtStore.path(context, "unknown.png"))
    }

    @Test fun obsoleteCabinetAssetsAreNotPackaged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (folder in listOf("full_cabinets", "cabinets_mame", "overlay_mame",
            "arcade_presentations/lit", "arcade_presentations/front-transparent")) {
            assertTrue("Obsolete artwork: $folder", context.assets.list(folder).isNullOrEmpty())
        }
    }

    @Test fun everyFrontHasTransparentSurroundAndUnchangedCabinetPixels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (name in requireNotNull(context.assets.list("arcade_presentations/front"))) {
            val path = "arcade_presentations/front/$name"
            val original = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            val rendered = requireNotNull(decodeSampledArtworkAsset(context, path))
            assertEquals(original.width, rendered.width)
            assertEquals(original.height, rendered.height)
            val before = IntArray(original.width * original.height)
            val after = IntArray(before.size)
            original.getPixels(before, 0, original.width, 0, 0, original.width, original.height)
            rendered.getPixels(after, 0, rendered.width, 0, 0, rendered.width, rendered.height)
            val background = arcadeFrontBackgroundMask(before, original.width, original.height)
            assertTrue("No surround removed: $name", background.any { it })
            assertEquals("Top-left surround: $name", 0, after[0] ushr 24)
            for (i in before.indices) {
                if (background[i]) assertEquals("Surround alpha: $name", 0, after[i] ushr 24)
                else assertEquals("Cabinet altered: $name at $i", before[i], after[i])
            }
            original.recycle()
            rendered.recycle()
        }
    }
}