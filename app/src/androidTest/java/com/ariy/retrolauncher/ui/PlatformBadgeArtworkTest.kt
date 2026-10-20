package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.model.PlatformSvgIcons
import com.caverock.androidsvg.SVG
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformBadgeArtworkTest {
    @Test fun redesignedBadgesRenderWithoutFontsOrOpaqueBackgrounds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val platforms = listOf("Sony PlayStation Vita", "Sega Saturn", "PC Engine / TurboGrafx-16")
        val preview = Bitmap.createBitmap(720, 320, Bitmap.Config.ARGB_8888)
        val previewCanvas = Canvas(preview).apply { drawColor(Color.rgb(31, 41, 55)) }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15f }
        platforms.forEachIndexed { index, platform ->
            val source = requireNotNull(PlatformSvgIcons.svgForPlatform(context.resources, platform))
            assertFalse("Lettering must not depend on installed fonts", source.contains("<text"))
            assertFalse("No embedded raster images", source.contains("<image"))
            val picture = SVG.getFromString(source).renderToPicture()
            for (size in listOf(132, 72)) {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val scale = minOf(size.toFloat() / picture.width, size.toFloat() / picture.height)
                canvas.translate((size - picture.width * scale) / 2f, (size - picture.height * scale) / 2f)
                canvas.scale(scale, scale)
                canvas.drawPicture(picture)
                val pixels = IntArray(size * size)
                bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
                val visible = pixels.indices.filter { Color.alpha(pixels[it]) > 32 }
                assertTrue("$platform must remain visible at $size pixels", visible.size > size * size / 30)
                val left = visible.minOf { it % size }
                val right = visible.maxOf { it % size }
                assertTrue("$platform must use the badge width", right - left > size * 0.8f)
                assertTrue("$platform must keep transparent outer padding", left > 0 && right < size - 1)
                assertTrue("$platform background must stay transparent", Color.alpha(pixels.first()) == 0)
                assertTrue("$platform should render white", visible.all { Color.red(pixels[it]) > 240 &&
                    Color.green(pixels[it]) > 240 && Color.blue(pixels[it]) > 240 })
                previewCanvas.drawBitmap(bitmap, index * 240f + (240 - size) / 2f,
                    if (size == 132) 46f else 208f, null)
                bitmap.recycle()
            }
            val label = platform.removePrefix("Sony ").substringBefore(" / ")
            previewCanvas.drawText(label, index * 240f + 16f, 26f, labelPaint)
        }
        File(requireNotNull(context.externalCacheDir), "platform-badge-review.png").outputStream().use {
            assertTrue(preview.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        preview.recycle()
    }
}