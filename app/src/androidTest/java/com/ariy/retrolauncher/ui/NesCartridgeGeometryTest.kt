package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.findNostalgicArtworkWindowBounds
import com.ariy.retrolauncher.model.NostalgicOverlays
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NesCartridgeGeometryTest {
    @Test fun fittedArtworkStartsAtTopAndUsesShellColorForRemainingSpace() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Nintendo Entertainment System"))
        val original = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false })
        val old = requireNotNull(findNostalgicArtworkWindowBounds(original))
        for ((w, h) in listOf(300 to 420, 885 to 1467, 300 to 350)) {
            val shell = original
            val well = requireNotNull(findNostalgicArtworkWindowBounds(shell))
            assertEquals(298, well.width)
            assertEquals(479, well.height)
            assertEquals(old, well)
            val artwork = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
            val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
            Canvas(face).apply {
                val rect = RectF(well.left.toFloat(), well.top.toFloat(), well.right.toFloat(), well.bottom.toFloat())
                drawRect(rect, Paint().apply { color = spec.artworkBackgroundColor })
                val scale = minOf(rect.width() / w, rect.height() / h)
                val destination = RectF(
                    rect.centerX() - w * scale / 2,
                    rect.top,
                    rect.centerX() + w * scale / 2,
                    rect.top + h * scale
                )
                drawBitmap(artwork, null, destination, Paint(Paint.FILTER_BITMAP_FLAG))
                drawBitmap(shell, 0f, 0f, null)
            }
            assertEquals(Color.CYAN, face.getPixel((well.left + well.right) / 2, well.top + 5))
            if (destinationHeight(w, h, well.width, well.height) < well.height - 1) {
                assertEquals(spec.artworkBackgroundColor,
                    face.getPixel((well.left + well.right) / 2, well.bottom - 5))
            }
            File(requireNotNull(context.externalCacheDir), "nes-fixed-$w-$h.png").outputStream().use {
                face.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            artwork.recycle()
            face.recycle()
        }
        original.recycle()
    }

    private fun destinationHeight(width: Int, height: Int, windowWidth: Int, windowHeight: Int): Float {
        val scale = minOf(windowWidth.toFloat() / width, windowHeight.toFloat() / height)
        return height * scale
    }
}