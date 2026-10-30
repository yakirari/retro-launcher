package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.findNostalgicArtworkWindowBounds
import com.ariy.retrolauncher.launch.NesLabelScans
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.NostalgicOverlays
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NesLabelArtworkTest {
    @Test fun reviewedScanDownloadsCropsAndCachesWithoutARomOrLibraryMutation(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scans = context.assets.open(NesLabelScans.MANIFEST).bufferedReader().use { NesLabelScans.parse(it.readText()) }
        val scan = scans.values.single()
        val root = File(context.cacheDir, "nes-source-review-test").apply { mkdirs() }
        val label = requireNotNull(NesCartridgeLabelStore.loadScan(root, "review", scan))
        assertTrue(label.width in 884..886)
        assertTrue(label.height in 1466..1468)
        val cached = requireNotNull(NesCartridgeLabelStore.loadScan(root, "review", scan))
        assertTrue(label.sameAs(cached))
        val game = GameEntry(title = "Manual", platform = "Nintendo Entertainment System",
            sourceType = GameSourceType.CUSTOM_INTENT, cartridgeLabelUri = "content://manual/label")
        assertNull(NesCartridgeLabelStore.load(context, game))
        assertNull(NesCartridgeLabelStore.load(context, game.copy(cartridgeLabelUri = null)))
        val spec = requireNotNull(NostalgicOverlays.specForPlatform(game.platform))
        val shell = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false })
        val bounds = requireNotNull(findNostalgicArtworkWindowBounds(shell))
        val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
        val well = RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
        val scale = minOf(well.width() / label.width, well.height() / label.height)
        Canvas(face).apply {
            drawRect(well, Paint().apply { color = spec.artworkBackgroundColor })
            drawBitmap(label, null, RectF(well.centerX() - label.width * scale / 2,
                well.centerY() - label.height * scale / 2, well.centerX() + label.width * scale / 2,
                well.centerY() + label.height * scale / 2), Paint(Paint.FILTER_BITMAP_FLAG))
            drawBitmap(shell, 0f, 0f, null)
        }
        assertNotEquals(Color.TRANSPARENT, face.getPixel(well.centerX().toInt(), well.centerY().toInt()))
        File(requireNotNull(context.externalCacheDir), "nes-real-label-review.png").outputStream().use {
            face.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}