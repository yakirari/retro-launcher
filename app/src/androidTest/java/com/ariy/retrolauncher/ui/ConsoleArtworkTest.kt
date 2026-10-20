package com.ariy.retrolauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.TransparentRegionBounds
import com.ariy.retrolauncher.buildNostalgicArtworkMaskBitmap
import com.ariy.retrolauncher.findNostalgicArtworkWindowBounds
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.NostalgicOverlays
import com.ariy.retrolauncher.recolorNostalgicOverlayBitmap
import com.ariy.retrolauncher.resolveArtworkSourceUri
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlin.math.min
import kotlin.math.roundToInt
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ConsoleArtworkTest {
    @Test fun jewelCaseRetainsCoverTabsOverInsertedArtwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (platform in listOf("Sony PlayStation", "Sega Dreamcast")) {
            val spec = requireNotNull(NostalgicOverlays.specForPlatform(platform))
            val shell = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
                BitmapFactory.Options().apply { inScaled = false })
            assertEquals(0, Color.alpha(shell.getPixel(450, 350)))
            // Four semicircular cover tabs and the two right-side retaining clips.
            for ((x, y) in listOf(312 to 100, 720 to 100, 312 to 674, 720 to 674,
                    808 to 250, 808 to 500)) {
                assertTrue("$platform must keep the original retaining detail at $x,$y",
                    Color.alpha(shell.getPixel(x, y)) > 200)
            }
            val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
            Canvas(face).apply {
                drawColor(Color.MAGENTA)
                drawBitmap(shell, 0f, 0f, null)
            }
            assertEquals(Color.MAGENTA, face.getPixel(450, 350))
            assertNotEquals(Color.MAGENTA, face.getPixel(312, 100))
            File(requireNotNull(context.externalCacheDir), "$platform-jewel-case-review.png")
                .outputStream().use { face.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun exactCatalogCanReplaceAnAutomaticPhysicalProductPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val candidates = com.ariy.retrolauncher.launch.LibretroArtworkCatalog.fetchCandidates(
            "Nintendo GameCube", "Eternal Darkness - Sanity's Requiem", "Eternal Darkness (USA).rvz")
        assertTrue("Exact flat cover catalog should be available", candidates.isNotEmpty())
        val saved = com.ariy.retrolauncher.launch.ImportedIconStore(context).downloadBestAspectImage(
            "console-source-verification", candidates.map { it.imageUrl }, 0.70f, minimumShortEdge = 240)
        assertNotNull("Exact cover scan should download", saved)
    }

    @Test fun consoleTemplatesKeepFullArtworkAndMaskUsesSameCoordinates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val saved = JSONArray(context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE).getString("games", "[]"))
        val games = (0 until saved.length()).map { GameEntry.fromJson(saved.getJSONObject(it)) }
        for (platform in listOf("Nintendo GameCube", "Nintendo 64", "Sony PlayStation 2")) {
            val spec = requireNotNull(NostalgicOverlays.specForPlatform(platform))
            assertTrue(spec.fitCompleteArtwork)
            assertFalse(spec.applyVintageArtwork)
            assertEquals(0f, spec.preTrimTopFraction)
            assertEquals(1f, spec.preTrimHeightFraction)
            val source = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
                BitmapFactory.Options().apply { inScaled = false })
            val detected = requireNotNull(findNostalgicArtworkWindowBounds(source))
            val bounds = if (spec.detectArtworkPlacementFromTransparency) detected else TransparentRegionBounds(
                (source.width * spec.artworkLeftFraction).roundToInt(),
                (source.height * spec.artworkTopFraction).roundToInt(),
                (source.width * (spec.artworkLeftFraction + spec.artworkWidthFraction)).roundToInt(),
                (source.height * (spec.artworkTopFraction + spec.artworkHeightFraction)).roundToInt(), 0)
            assertEquals(0, Color.alpha(source.getPixel((bounds.left + bounds.right)/2, (bounds.top + bounds.bottom)/2)))
            if (platform == "Nintendo GameCube") assertEquals(0, Color.alpha(source.getPixel(450, 151)))
            if (platform == "Sony PlayStation 2") {
                assertEquals("No baked E rating", 0, Color.alpha(source.getPixel(240, 655)))
                assertEquals("No baked platform heading", 0, Color.alpha(source.getPixel(400, 60)))
                val mask = buildNostalgicArtworkMaskBitmap(source, bounds)
                assertEquals(bounds.width, mask.width)
                assertEquals(bounds.height, mask.height)
                assertEquals(Color.WHITE, mask.getPixel(mask.width/2, mask.height/2))
            }
            val verifiedCatalogCover = if (platform == "Nintendo GameCube")
                File(context.filesDir, "shortcut-icons/console-source-verification.png") else null
            val n64Sticker = if (platform == "Nintendo 64") games.firstOrNull { it.platform == platform &&
                it.title.contains("Donkey Kong") }?.let { runBlocking { N64CartridgeLabelStore.load(context, it) } } else null
            val artwork = n64Sticker ?: verifiedCatalogCover?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                ?: games.firstOrNull { it.platform == platform && it.imageUri != null &&
                    (platform != "Nintendo 64" || it.imageUri.contains("console-catalog-v2")) }?.imageUri?.let { uri ->
                context.contentResolver.openInputStream(resolveArtworkSourceUri(uri))?.use { BitmapFactory.decodeStream(it) }
            } ?: Bitmap.createBitmap(350, 500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
            val face = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Canvas(face).apply {
                val well = RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
                drawRect(well, Paint().apply { color = Color.BLACK })
                val scale = min(well.width()/artwork.width, well.height()/artwork.height)
                val w = artwork.width*scale; val h = artwork.height*scale
                drawBitmap(artwork, null, RectF(well.centerX()-w/2, well.centerY()-h/2,
                    well.centerX()+w/2, well.centerY()+h/2), Paint(Paint.FILTER_BITMAP_FLAG))
                drawBitmap(recolorNostalgicOverlayBitmap(source, Color.BLACK), 0f, 0f, null)
            }
            File(requireNotNull(context.externalCacheDir), "$platform-console-review.png").outputStream().use {
                face.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun n64CartridgeUsesExactRomPortraitSticker() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Nintendo 64"))
        val shell = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false })
        val bounds = requireNotNull(findNostalgicArtworkWindowBounds(shell))
        assertTrue("Real N64 sticker well is portrait", bounds.width.toFloat() / bounds.height in 0.8f..0.95f)
        val games = JSONArray(context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE).getString("games", "[]"))
        val entries = (0 until games.length()).map { GameEntry.fromJson(games.getJSONObject(it)) }
            .filter { it.platform == "Nintendo 64" }
        if (entries.isNotEmpty()) {
            var matched = 0
            for (entry in entries) {
                val sticker = N64CartridgeLabelStore.load(context, entry)
                android.util.Log.i("ConsoleArtworkTest", "N64 ${entry.title}: ${sticker?.let { "${it.width}x${it.height}" } ?: "no exact label"}")
                if (sticker != null) {
                    val reviewedUsaScan = !entry.title.contains("Harvest Moon") && !entry.title.contains("Shindou")
                    if (reviewedUsaScan) {
                        assertTrue("Reviewed originals must upgrade the 74px cache", sticker.width >= 300 && sticker.height >= 350)
                    } else { assertEquals(74, sticker.width); assertEquals(86, sticker.height) }
                    matched++
                }
            }
            assertTrue("Device library should have verified real sticker matches", matched > 0)
        }
        val cover = Bitmap.createBitmap(bounds.width, bounds.height, Bitmap.Config.ARGB_8888)
        Canvas(cover).apply {
            drawColor(Color.GREEN)
        }
        val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
        Canvas(face).apply { drawBitmap(cover, bounds.left.toFloat(), bounds.top.toFloat(), null); drawBitmap(shell, 0f, 0f, null) }
        assertEquals(Color.GREEN, face.getPixel((bounds.left+bounds.right)/2, (bounds.top+bounds.bottom)/2))
    }

    @Test fun scummVmPlaceholderHasExactCoverAndManualImagesAreNotIcons() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val candidates = com.ariy.retrolauncher.launch.LibretroArtworkCatalog.fetchCandidates("ScummVM",
            "Beneath a Steel Sky (v0.0288 Floppy/DOS)")
        assertTrue(candidates.isNotEmpty())
        val store = com.ariy.retrolauncher.launch.ImportedIconStore(context)
        val cover = store.downloadBestAspectImage("scummvm-source-verification", candidates.map { it.imageUrl }, 0.7f)
        assertNotNull(cover)
        val manual = GameEntry(id = "scummvm-source-verification", title = "Beneath a Steel Sky", platform = "ScummVM", imageUri = cover,
            sourceType = com.ariy.retrolauncher.model.GameSourceType.SHORTCUT)
        assertFalse("A real cover must never be treated as the generic placeholder", store.isLegacyScummVmPlaceholder(manual))
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("ScummVM"))
        val shell = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId, BitmapFactory.Options().apply { inScaled = false })
        val bounds = requireNotNull(findNostalgicArtworkWindowBounds(shell))
        assertTrue("Floppy window, not outer canvas", bounds.width in 350..700 && bounds.height in 180..400)
        val art = context.contentResolver.openInputStream(resolveArtworkSourceUri(requireNotNull(cover)))!!.use { BitmapFactory.decodeStream(it) }
        val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
        Canvas(face).apply {
            drawBitmap(art, null, RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            drawBitmap(recolorNostalgicOverlayBitmap(shell, Color.BLACK), 0f, 0f, null)
        }
        File(requireNotNull(context.externalCacheDir), "ScummVM-console-review.png").outputStream().use { face.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun startupRepairsLegacyScummVmIconsWithoutChangingLaunchTargets() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val json = JSONArray(context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE).getString("games", "[]"))
        val original = (0 until json.length()).map { GameEntry.fromJson(json.getJSONObject(it)) }
            .filter { com.ariy.retrolauncher.launch.ImportedIconStore(context).isLegacyScummVmPlaceholder(it) }
        if (original.isEmpty()) return@runBlocking
        val model = java.util.concurrent.atomic.AtomicReference<com.ariy.retrolauncher.LauncherViewModel>()
        val owner = androidx.lifecycle.ViewModelStore()
        instrumentation.runOnMainSync {
            val vm = com.ariy.retrolauncher.LauncherViewModel(context.applicationContext as android.app.Application)
            owner.put("artwork-regression", vm)
            model.set(vm)
        }
        try {
            val repaired = kotlinx.coroutines.withTimeout(120_000) {
                model.get().games.first { games -> original.all { before ->
                    games.firstOrNull { it.id == before.id }?.imageUri?.contains("scummvm-cover-v1") == true
                } }
            }
            for (before in original) {
                val after = repaired.first { it.id == before.id }
                assertEquals(before.shortcutId, after.shortcutId)
                assertEquals(before.extrasJson, after.extrasJson)
                assertEquals(before.intentDataUri, after.intentDataUri)
            }
        } finally { instrumentation.runOnMainSync { owner.clear() } }
    }
}