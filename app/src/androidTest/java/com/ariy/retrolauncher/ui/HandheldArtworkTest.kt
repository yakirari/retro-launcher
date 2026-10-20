package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.model.HandheldCartridgeSettings
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.NostalgicOverlays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HandheldArtworkTest {
    /** Optional, read-only integration check against a real, authorized ROM on the device. */
    @Test fun exactHostedStickerIsVisibleThroughShell() {
        val uri = InstrumentationRegistry.getArguments().getString("cartridgeRomUri")
        val platform = InstrumentationRegistry.getArguments().getString("cartridgePlatform") ?: "Game Boy Advance"
        org.junit.Assume.assumeTrue("Supply cartridgeRomUri for hosted-art integration", uri != null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val game = GameEntry(title = "Exact ROM test", platform = platform, sourceType = GameSourceType.CUSTOM_INTENT,
            intentDataUri = uri)
        val sticker = requireNotNull(runBlocking(Dispatchers.IO) { CartridgeLabelStore.load(context, game) })
        assertTrue(sticker.width.toFloat() / sticker.height in
            if (platform == "Game Boy Advance") 1.6f..2.3f else 0.85f..1.4f)
        val spec = requireNotNull(NostalgicOverlays.specForPlatform(platform))
        val neutral = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false })
        val shell = tintHandheldShell(neutral, handheldArtworkColor(sticker) ?: Color.GRAY)
        val face = Bitmap.createBitmap(neutral.width, neutral.height, Bitmap.Config.ARGB_8888)
        val label = buildHandheldLabel(spec, sticker, true)
        val well = RectF(neutral.width * spec.artworkLeftFraction, neutral.height * spec.artworkTopFraction,
            neutral.width * (spec.artworkLeftFraction + spec.artworkWidthFraction),
            neutral.height * (spec.artworkTopFraction + spec.artworkHeightFraction))
        val reference = Bitmap.createBitmap(neutral.width, neutral.height, Bitmap.Config.ARGB_8888)
        Canvas(reference).drawBitmap(label, null, well, Paint(Paint.FILTER_BITMAP_FLAG))
        Canvas(face).apply {
            drawBitmap(label, null, well, Paint(Paint.FILTER_BITMAP_FLAG))
            drawBitmap(shell, 0f, 0f, null)
        }
        assertEquals("Shell must not conceal sticker", 0,
            Color.alpha(shell.getPixel(well.centerX().toInt(), well.centerY().toInt())))
        // Compare at the same scale/filter: tiny printed details change when downsampled.
        for (y in well.top.toInt()+2 until well.bottom.toInt()-2)
            for (x in well.left.toInt()+2 until well.right.toInt()-2) {
            assertEquals("Visible label at $x,$y", reference.getPixel(x, y), face.getPixel(x, y))
        }
        File(requireNotNull(context.externalCacheDir), "${platform.replace(' ', '-')}-real-sticker-review.png").outputStream().use {
            face.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val cached = requireNotNull(runBlocking(Dispatchers.IO) { CartridgeLabelStore.load(context, game) })
        assertEquals(sticker.width, cached.width)
        assertEquals(sticker.height, cached.height)
    }

    @Test fun shellTintKeepsAlphaAndSourcesUnchanged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (platform in listOf("Game Boy", "Game Boy Color", "Game Boy Advance")) {
            val spec = requireNotNull(NostalgicOverlays.specForPlatform(platform))
            assertFalse(spec.detectArtworkPlacementFromTransparency)
            assertFalse(spec.applyVintageArtwork)
            val source = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
                BitmapFactory.Options().apply { inScaled = false })
            val original = IntArray(source.width*source.height)
            source.getPixels(original, 0, source.width, 0, 0, source.width, source.height)
            val blue = tintHandheldShell(source, HandheldCartridgeSettings.shellColor(platform, "blue"))
            val red = tintHandheldShell(source, HandheldCartridgeSettings.shellColor(platform, "red"))
            val changed = IntArray(original.size)
            blue.getPixels(changed, 0, source.width, 0, 0, source.width, source.height)
            assertTrue(original.indices.all { Color.alpha(original[it]) == Color.alpha(changed[it]) })
            assertEquals(Color.TRANSPARENT, blue.getPixel(source.width/2,
                (source.height*(spec.artworkTopFraction+spec.artworkHeightFraction/2)).toInt()))
            assertNotEquals(blue.getPixel(source.width/2, 20), red.getPixel(source.width/2, 20))
            val after = IntArray(original.size)
            source.getPixels(after, 0, source.width, 0, 0, source.width, source.height)
            assertArrayEquals(original, after)
        }
    }

    @Test fun completeCoverFitsAndCustomLabelDoesNotGetExtraPrinting() {
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Game Boy Advance"))
        val cover = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cover)
        canvas.drawColor(Color.RED)
        val paint = Paint().apply { color = Color.BLUE }
        canvas.drawRect(0f, 185f, 100f, 200f, paint)
        val label = buildHandheldLabel(spec, cover, false)
        val pixels = IntArray(label.width*label.height)
        label.getPixels(pixels, 0, label.width, 0, 0, label.width, label.height)
        assertTrue("Top of cover retained", pixels.any { it == Color.RED })
        assertTrue("Bottom of cover retained", pixels.any { it == Color.BLUE })
        val complete = Bitmap.createBitmap(640, label.height, Bitmap.Config.ARGB_8888)
        complete.eraseColor(Color.GREEN)
        val custom = buildHandheldLabel(spec, complete, true)
        assertEquals(Color.GREEN, custom.getPixel(320, 5))
        assertEquals(Color.GREEN, custom.getPixel(320, custom.height-5))
        assertEquals(Color.RED, cover.getPixel(0, 0))
    }

    @Test fun coverUsesEntireStickerHeightWithoutGeneratedText() {
        for (platform in listOf("Game Boy", "Game Boy Color", "Game Boy Advance")) {
            val spec = requireNotNull(NostalgicOverlays.specForPlatform(platform))
            val cover = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            cover.eraseColor(Color.RED)
            val paint = Paint().apply { color = Color.BLUE }
            Canvas(cover).drawRect(0f, 190f, 100f, 200f, paint)
            val label = buildHandheldLabel(spec, cover, false)
            assertEquals("$platform top printing is not obscured", Color.RED,
                label.getPixel(label.width / 2, 0))
            assertEquals("$platform bottom printing is not obscured", Color.BLUE,
                label.getPixel(label.width / 2, label.height - 1))
            // A full-window sticker must remain full-window, not an inset cover with headings.
            val full = Bitmap.createBitmap(label.width, label.height, Bitmap.Config.ARGB_8888)
            full.eraseColor(Color.GREEN)
            val fitted = buildHandheldLabel(spec, full, false)
            val pixels = IntArray(fitted.width * fitted.height)
            fitted.getPixels(pixels, 0, fitted.width, 0, 0, fitted.width, fitted.height)
            assertTrue("$platform no generated title, platform heading or bands", pixels.all { it == Color.GREEN })
        }
    }

    @Test fun autoColorFollowsArtworkIgnoresWhiteMarginsAndHandlesMonochrome() {
        fun swatch(color: Int): Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            Canvas(this).drawRect(20f, 20f, 80f, 80f, Paint().apply { this.color = color })
        }
        val red = requireNotNull(handheldArtworkColor(swatch(Color.RED)))
        val blue = requireNotNull(handheldArtworkColor(swatch(Color.BLUE)))
        assertNotEquals(red, blue)
        assertTrue(Color.red(red) > Color.blue(red))
        assertTrue(Color.blue(blue) > Color.red(blue))
        assertEquals(red, handheldArtworkColor(swatch(Color.RED)))
        assertNull(handheldArtworkColor(swatch(Color.BLACK)))
        assertNull(handheldArtworkColor(swatch(Color.WHITE)))
        assertNull(handheldArtworkColor(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)))
    }

    @Test fun createPaletteReviewSheet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sheet = Bitmap.createBitmap(1440, 1220, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet).apply { drawColor(Color.rgb(26, 32, 43)) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cover = Bitmap.createBitmap(240, 340, Bitmap.Config.ARGB_8888)
        Canvas(cover).apply {
            drawColor(Color.rgb(27, 119, 146))
            paint.color = Color.rgb(237, 180, 73)
            drawCircle(120f, 140f, 78f, paint)
            paint.color = Color.WHITE; paint.textSize = 28f
            drawText("STAR TRAIL", 35f, 270f, paint)
            paint.textSize = 16f; drawText("ARTWORK DEMO", 47f, 304f, paint)
        }
        listOf("Game Boy", "Game Boy Color", "Game Boy Advance").forEachIndexed { row, platform ->
            val spec = requireNotNull(NostalgicOverlays.specForPlatform(platform))
            val source = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
                BitmapFactory.Options().apply { inScaled = false })
            listOf<String?>(null, "red", "blue", "green").forEachIndexed { column, color ->
                val shell = tintHandheldShell(source, if (color == null) {
                    handheldArtworkColor(cover) ?: HandheldCartridgeSettings.shellColor(platform, null)
                } else HandheldCartridgeSettings.shellColor(platform, color))
                val face = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                Canvas(face).apply {
                    val label = buildHandheldLabel(spec, cover, false)
                    drawBitmap(label, null, RectF(source.width*spec.artworkLeftFraction,
                        source.height*spec.artworkTopFraction,
                        source.width*(spec.artworkLeftFraction+spec.artworkWidthFraction),
                        source.height*(spec.artworkTopFraction+spec.artworkHeightFraction)), paint)
                    drawBitmap(shell, 0f, 0f, paint)
                }
                val width = if (row == 2) 330f else 296f
                val height = width/spec.aspectRatio
                val x = column*360f + (360-width)/2
                val y = row*400f + 48f + (340-height)/2
                canvas.drawBitmap(face, null, RectF(x,y,x+width,y+height), paint)
                paint.color = Color.WHITE; paint.textSize = 17f
                canvas.drawText("$platform · ${color ?: "Auto"}", column*360f+24, row*400f+28, paint)
            }
        }
        File(requireNotNull(context.externalCacheDir), "handheld-review.png").outputStream().use {
            assertTrue(sheet.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}