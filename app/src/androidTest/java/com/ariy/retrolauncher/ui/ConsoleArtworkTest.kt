package com.ariy.retrolauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.R
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
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ConsoleArtworkTest {
    @Test fun snesChoosesRegionalShellByBestLabelFit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val usa = requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo", 2.24f))
        val japan = requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo", 2.88f))
        assertEquals(R.drawable.nostalgic_overlay_snes, usa.drawableResourceId)
        assertEquals(R.drawable.nostalgic_overlay_snes_japan, japan.drawableResourceId)
        assertEquals(usa.drawableResourceId, NostalgicOverlays.specForArtwork("Super Nintendo", null)?.drawableResourceId)
        assertTrue(usa.fitCompleteArtwork && japan.fitCompleteArtwork)
        assertFalse(usa.applyVintageArtwork || japan.applyVintageArtwork)

        for ((spec, expectedRatio) in listOf(usa to (469f / 223f), japan to (648f / 201f))) {
            val shell = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
                BitmapFactory.Options().apply { inScaled = false })
            val bounds = requireNotNull(findNostalgicArtworkWindowBounds(shell))
            assertEquals(expectedRatio, bounds.width.toFloat() / bounds.height, 0.02f)
            assertEquals(bounds.left / shell.width.toFloat(), spec.artworkLeftFraction, 0.002f)
            assertEquals(bounds.top / shell.height.toFloat(), spec.artworkTopFraction, 0.002f)
            shell.recycle()
        }

        val labels = JSONObject(context.assets.open(com.ariy.retrolauncher.launch.SnesCartridgeScans.MANIFEST)
            .bufferedReader().use { it.readText() }).getJSONArray("labels")
        for (index in 0 until labels.length()) {
            val row = labels.getJSONObject(index)
            val ratio = row.getInt("width").toFloat() / row.getInt("height")
            val selected = requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo", ratio))
            val usaFit = minOf(ratio / (469f / 223f), (469f / 223f) / ratio)
            val japanFit = minOf(ratio / (648f / 201f), (648f / 201f) / ratio)
            val expected = if (japanFit > usaFit) R.drawable.nostalgic_overlay_snes_japan
                else R.drawable.nostalgic_overlay_snes
            assertEquals(row.getString("romTitle"), expected, selected.drawableResourceId)
            if (row.getString("romTitle").contains("(USA)")) {
                assertEquals(R.drawable.nostalgic_overlay_snes, selected.drawableResourceId)
            } else if (row.getString("romTitle").contains("(Japan)")) {
                assertEquals(R.drawable.nostalgic_overlay_snes_japan, selected.drawableResourceId)
            }
        }
    }

    @Test fun snesUsesBundledCartridgeLabelsWithoutNetworkOrCoverMutation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Super Nintendo"))
        assertTrue(spec.fitCompleteArtwork)
        assertFalse(spec.applyVintageArtwork)
        val json = context.assets.open(com.ariy.retrolauncher.launch.SnesCartridgeScans.MANIFEST)
            .bufferedReader().use { org.json.JSONObject(it.readText()) }
        val rows = json.getJSONArray("labels")
        assertTrue(rows.length() > 0)
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val bytes = context.assets.open("snes_labels/${row.getString("path")}").use { it.readBytes() }
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(row.getString("sha256"), hash)
            val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            try {
                assertEquals(row.getInt("width"), bitmap.width)
                assertEquals(row.getInt("height"), bitmap.height)
                assertTrue(maxOf(bitmap.width, bitmap.height) <= 4096)
            } finally { bitmap.recycle() }
            val game = GameEntry(id = "snes-scan-test", title = "Shortened tile title", platform = "Super Nintendo",
                sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
                imageUri = "file:///private/snes-scan-test-autofill.png#revision",
                intentDataUri = "file:///roms/${row.getString("romTitle")}.sfc")
            val originalCover = game.imageUri
            assertEquals("snes_labels/${row.getString("path")}", com.ariy.retrolauncher.launch.SnesCartridgeScans.path(context, game))
            val regionlessName = row.getString("romTitle").substringBefore(" (")
                .lowercase().replace(Regex("[^a-z0-9]"), "")
            val regionlessGame = game.copy(intentDataUri = "file:///roms/$regionlessName.zip")
            val fallbackPath = requireNotNull(com.ariy.retrolauncher.launch.SnesCartridgeScans.path(context, regionlessGame)) {
                "Missing regionless fallback for ${row.getString("romTitle")}: $regionlessName"
            }
            assertTrue(fallbackPath.startsWith("snes_labels/labels/"))
            assertEquals("file:///roms/$regionlessName.zip", regionlessGame.intentDataUri)
            assertEquals(originalCover, game.imageUri)
            assertNull(com.ariy.retrolauncher.launch.SnesCartridgeScans.path(context, game.copy(imageUri = "content://manual-cover")))
            assertNull(com.ariy.retrolauncher.launch.SnesCartridgeScans.path(context, game.copy(cartridgeLabelUri = "content://manual-label")))
        }
    }

    @Test fun suppliedRegionalLabelsUseTheirIntendedFixedCartridges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = com.ariy.retrolauncher.launch.SnesCartridgeScans
        val labels = JSONObject(context.assets.open(catalog.MANIFEST)
            .bufferedReader().use { it.readText() }).getJSONArray("labels")
        val rows = (0 until labels.length()).map { labels.getJSONObject(it) }
            .filter { it.getString("sourcePack") == "game-labels-batch2-final" }
        assertEquals(61, rows.size)
        var usaCount = 0
        var japanCount = 0
        for (row in rows) {
            val path = "snes_labels/${row.getString("path")}"
            val bytes = context.assets.open(path).use { it.readBytes() }
            assertEquals(row.getString("sha256"), java.security.MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") { "%02x".format(it) })
            val image = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            val spec = requireNotNull(NostalgicOverlays.specForArtwork("Super Nintendo",
                image.width.toFloat() / image.height))
            val japan = row.getString("templateRegion") == "Japan"
            if (japan) japanCount++ else usaCount++
            assertEquals(if (japan) R.drawable.nostalgic_overlay_snes_japan else R.drawable.nostalgic_overlay_snes,
                spec.drawableResourceId)
            assertFalse(row.getBoolean("authenticatedOriginalScan"))
            image.recycle()
            val game = GameEntry(id = "restored-snes-test", title = "Ignored tile title", platform = "Super Nintendo",
                sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
                intentDataUri = "file:///roms/${row.getString("romTitle")}.zip")
            assertEquals(path, catalog.path(context, game))
            assertNotNull(catalog.path(context, game.copy(
                intentDataUri = "file:///roms/${row.getString("romTitle").substringBefore(" (")}.zip")))
            assertNull(catalog.path(context, game.copy(cartridgeLabelUri = "content://manual-label")))
            assertNull(catalog.path(context, game.copy(imageUri = "content://manual-cover")))
        }
        assertEquals(44, usaCount)
        assertEquals(14, japanCount)
        val duplicateNamedMarioRpg = GameEntry(
            id = "mario-rpg-duplicate-test",
            title = "supermariorpglegendofthesevenstars",
            platform = "Super Nintendo",
            sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
            imageUri = "file:///data/user/0/com.ariy.retrolauncher/files/mario-rpg-duplicate-test-autofill.png",
            intentDataUri = "content://com.android.externalstorage.documents/tree/roms/document/roms%2FSNES%2Fsupermariorpglegendofthesevenstars%20(2).sfc"
        )
        assertEquals("snes_labels/labels/supermariorpglegendofthesevenstars-japan.jpg",
            catalog.path(context, duplicateNamedMarioRpg))
        val chrono = GameEntry(id = "chrono-test", title = "chronotrigger", platform = "Super Nintendo",
            sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
            intentDataUri = "file:///roms/chronotrigger.zip")
        assertEquals("snes_labels/labels/chronotrigger-usa.jpg", catalog.path(context, chrono))
        for (title in listOf("megaman10", "megaman12", "megaman13")) {
            val game = GameEntry(id = "$title-test", title = "Ignored tile title", platform = "Super Nintendo",
                sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
                intentDataUri = "file:///roms/$title.sfc")
            assertEquals("snes_labels/labels/$title.jpg", catalog.path(context, game))
        }
    }

    @Test fun snesSpecialShellColorsPreserveGeometryTransparencyAndLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = com.ariy.retrolauncher.launch.SnesCartridgeScans
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Super Nintendo"))
        val original = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false })
        val snapshot = original.copy(Bitmap.Config.ARGB_8888, false)
        val neutral = recolorNostalgicOverlayBitmap(original, Color.BLACK)
        val well = requireNotNull(findNostalgicArtworkWindowBounds(original))
        for ((rom, expectedColor) in listOf("Doom (USA).sfc" to 0xFFB52027.toInt(),
            "Killer Instinct (USA).sfc" to 0xFF202124.toInt())) {
            val game = GameEntry(id = "snes-color-test", title = "Ignored display title", platform = "Super Nintendo",
                sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
                intentDataUri = "file:///roms/${Uri.encode(rom)}")
            assertEquals(expectedColor, catalog.shellColor(game))
            val shell = tintSnesCartridgeShell(original, expectedColor)
            assertEquals(original.width, shell.width)
            assertEquals(original.height, shell.height)
            val pixels = IntArray(shell.width * shell.height)
            val neutralPixels = IntArray(pixels.size)
            shell.getPixels(pixels, 0, shell.width, 0, 0, shell.width, shell.height)
            neutral.getPixels(neutralPixels, 0, neutral.width, 0, 0, neutral.width, neutral.height)
            for (index in pixels.indices) {
                assertEquals(Color.alpha(neutralPixels[index]), Color.alpha(pixels[index]))
            }
            val plastic = shell.getPixel(480, 500)
            if (rom.startsWith("Doom")) {
                assertTrue(Color.red(plastic) > 3 * Color.green(plastic))
            } else {
                assertTrue(Color.red(plastic) < 60 && Color.green(plastic) < 60 && Color.blue(plastic) < 60)
            }
            assertNotEquals(shell.getPixel(480, 500), shell.getPixel(480, 450))
            val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
            Canvas(face).apply {
                drawRect(well.left.toFloat(), well.top.toFloat(), well.right.toFloat(), well.bottom.toFloat(),
                    Paint().apply { color = Color.CYAN })
                drawBitmap(shell, 0f, 0f, null)
            }
            assertEquals(Color.CYAN, face.getPixel((well.left + well.right) / 2, (well.top + well.bottom) / 2))
            val label = context.assets.open(requireNotNull(catalog.path(context, game)))
                .use { requireNotNull(BitmapFactory.decodeStream(it)) }
            val scale = min(well.width.toFloat() / label.width, well.height.toFloat() / label.height)
            val x = well.left + (well.width - label.width * scale) / 2
            val y = well.top + (well.height - label.height * scale) / 2
            face.eraseColor(Color.TRANSPARENT)
            Canvas(face).apply {
                drawRect(well.left.toFloat(), well.top.toFloat(), well.right.toFloat(), well.bottom.toFloat(),
                    Paint().apply { color = spec.artworkBackgroundColor })
                drawBitmap(label, null, RectF(x, y, x + label.width * scale, y + label.height * scale),
                    Paint(Paint.FILTER_BITMAP_FLAG))
                drawBitmap(shell, 0f, 0f, null)
            }
            File(requireNotNull(context.externalCacheDir), "${rom.substringBefore(" (")}-snes-shell-color.png")
                .outputStream().use { assertTrue(face.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            face.recycle()
            label.recycle()
            shell.recycle()
        }
        assertTrue(original.sameAs(snapshot))
        neutral.recycle()
        snapshot.recycle()
        original.recycle()
    }

    @Test fun nesStickerFitsTheActualOpeningAndPreservesPlasticTrim() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Nintendo Entertainment System"))
        val shell = BitmapFactory.decodeResource(context.resources, spec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false })
        val bounds = requireNotNull(findNostalgicArtworkWindowBounds(shell))
        assertTrue(spec.fitCompleteArtwork)
        assertFalse(spec.applyVintageArtwork)
        assertTrue(spec.clipArtworkToTransparencyMask)
        // The actual alpha opening, not an independently rounded rectangle, defines the label edge.
        val mask = buildNostalgicArtworkMaskBitmap(shell, bounds)
        assertEquals(bounds.width, mask.width)
        assertEquals(bounds.height, mask.height)
        assertEquals(Color.WHITE, mask.getPixel(mask.width / 2, mask.height / 2))
        val artwork = Bitmap.createBitmap(300, 420, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
            Canvas(this).apply {
                drawRect(0f, 0f, 300f, 24f, Paint().apply { color = Color.RED })
                drawRect(0f, 396f, 300f, 420f, Paint().apply { color = Color.BLUE })
            }
        }
        val face = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
        val well = RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
        val scale = min(well.width() / artwork.width, well.height() / artwork.height)
        val w = artwork.width * scale
        val h = artwork.height * scale
        val fitted = RectF(well.centerX() - w / 2, well.centerY() - h / 2,
            well.centerX() + w / 2, well.centerY() + h / 2)
        Canvas(face).apply {
            drawRect(well, Paint().apply { color = spec.artworkBackgroundColor })
            drawBitmap(artwork, null, fitted, Paint(Paint.FILTER_BITMAP_FLAG))
            drawBitmap(recolorNostalgicOverlayBitmap(shell, Color.BLACK), 0f, 0f, null)
        }
        assertEquals(Color.RED, face.getPixel(well.centerX().roundToInt(), (fitted.top + 10 * scale).roundToInt()))
        assertEquals(Color.BLUE, face.getPixel(well.centerX().roundToInt(), (fitted.bottom - 10 * scale).roundToInt()))
        File(requireNotNull(context.externalCacheDir), "nes-sticker-placement-review.png")
            .outputStream().use { face.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

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