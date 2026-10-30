package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.ariy.retrolauncher.launch.ImportedIconStore
import org.junit.Assert.*
import org.junit.Test

/** Offline policy fixture plus an explicitly opt-in live-library migration check. */
class NesLibraryRepairTest {
    @Test fun bundledSnesLabelsAreLocalNormalizedAndManualSafe(): Unit {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = com.ariy.retrolauncher.launch.SnesCartridgeScans
        val json = context.assets.open(catalog.MANIFEST).bufferedReader().use { org.json.JSONObject(it.readText()) }
        val rows = json.getJSONArray("labels")
        assertTrue(rows.length() > 0)
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val game = com.ariy.retrolauncher.model.GameEntry(id = "snes-local-test", title = "Short display name",
                platform = "Super Nintendo", sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
                intentDataUri = "file:///roms/${row.getString("romTitle")}.sfc")
            val path = requireNotNull(catalog.path(context, game)) { row.getString("romTitle") }
            assertEquals("snes_labels/${row.getString("path")}", path)
            val bytes = context.assets.open(path).use { it.readBytes() }
            assertEquals(row.getString("sha256"), java.security.MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") { "%02x".format(it) })
            val bitmap = requireNotNull(decodeSampledArtworkAsset(context, path, 768))
            try {
                assertEquals(row.getInt("width"), bitmap.width)
                assertEquals(row.getInt("height"), bitmap.height)
                assertTrue(maxOf(bitmap.width, bitmap.height) <= 768)
                assertTrue(bitmap.width > bitmap.height)
            } finally { bitmap.recycle() }
            assertNull(catalog.path(context, game.copy(imageUri = "content://manual-cover")))
            assertNull(catalog.path(context, game.copy(imageUri = "file:///manual.png")))
            assertNull(catalog.path(context, game.copy(cartridgeLabelUri = "content://manual-label")))
            assertEquals(path, catalog.path(context, game.copy(imageUri = "file:///icons/${game.id}-autofill.png?v=123")))
            assertNull(catalog.path(context, game.copy(platform = "Nintendo Entertainment System")))
        }
        val spec = requireNotNull(com.ariy.retrolauncher.model.NostalgicOverlays.specForPlatform("Super Nintendo"))
        assertTrue(spec.fitCompleteArtwork)
        assertFalse(spec.applyVintageArtwork)
    }
    @Test fun bundledFrontsAreNormalizedAndAvailableWithoutNetwork(): Unit {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = context.assets.open(com.ariy.retrolauncher.launch.NesBundledArtwork.MANIFEST)
            .bufferedReader().use { org.json.JSONObject(it.readText()) }
        val rows = json.getJSONArray("covers")
        assertTrue(rows.length() > 0)
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val bytes = context.assets.open("nes_covers/${row.getString("path")}").use { it.readBytes() }
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(row.getString("sha256"), digest)
            val image = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            try {
                assertEquals(row.getInt("width"), image.width)
                assertEquals(row.getInt("height"), image.height)
                assertTrue(maxOf(image.width, image.height) <= 768)
            } finally { image.recycle() }
        }
        val row = rows.getJSONObject(0)
        val game = com.ariy.retrolauncher.model.GameEntry(id = "nes-bundled-test", title = "Unrelated shortened tile title",
            platform = "Nintendo Entertainment System", sourceType = com.ariy.retrolauncher.model.GameSourceType.CUSTOM_INTENT,
            intentDataUri = "file:///roms/${row.getString("romTitle")}.nes")
        val store = ImportedIconStore(context)
        val saved = requireNotNull(com.ariy.retrolauncher.launch.NesBundledArtwork.save(context, game, store))
        try {
            assertTrue(saved.contains("nes-bundled-v1"))
            assertNull(com.ariy.retrolauncher.launch.NesBundledArtwork.save(context, game.copy(imageUri = "content://manual-cover"), store))
            assertNull(com.ariy.retrolauncher.launch.NesBundledArtwork.save(context, game.copy(cartridgeLabelUri = "content://manual-label"), store))
            assertNull(com.ariy.retrolauncher.launch.NesBundledArtwork.cover(context,
                game.copy(intentDataUri = game.intentDataUri!!.replace("(USA)", "(Japan)"))))
        } finally { java.io.File(android.net.Uri.parse(saved).path!!).delete() }
    }
    @Test fun alternateCatalogFindsTypedPortraitForLiveWideEntries(): Unit = kotlinx.coroutines.runBlocking {
        org.junit.Assume.assumeTrue("Live library/network test requires explicit opt-in",
            InstrumentationRegistry.getArguments().getString("verifyLiveNesPortraits") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("retro_launcher", android.content.Context.MODE_PRIVATE)
        fun stored(): List<com.ariy.retrolauncher.model.GameEntry> {
            val array = org.json.JSONArray(preferences.getString("games", "[]"))
            return (0 until array.length()).map { com.ariy.retrolauncher.model.GameEntry.fromJson(array.getJSONObject(it)) }
        }
        val before = stored().filter { com.ariy.retrolauncher.launch.NesArtworkPolicy.ownsAutomaticCover(it) }
        val expected = before.filter { game ->
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(com.ariy.retrolauncher.resolveArtworkSourceUri(game.imageUri!!).path, options)
            if (options.outWidth.toFloat() / options.outHeight in 0.5f..0.85f) false else {
                val rom = android.net.Uri.decode(game.intentDataUri ?: "").substringAfterLast('/')
                com.ariy.retrolauncher.launch.NesPortraitArtworkCatalog.fetchCandidates(rom).isNotEmpty()
            }
        }
        if (expected.isEmpty()) {
            assertTrue("No eligible alternatives or previous portrait migrations found",
                stored().any { it.imageUri?.contains("nes-portrait-v2") == true })
            return@runBlocking
        }
        for (game in expected) {
            val rom = android.net.Uri.decode(game.intentDataUri ?: "").substringAfterLast('/')
            val candidates = com.ariy.retrolauncher.launch.NesPortraitArtworkCatalog.fetchCandidates(rom)
            val candidate = candidates.first()
            val connection = java.net.URL(candidate.imageUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10_000; connection.readTimeout = 20_000
            try {
                assertEquals("${game.title}: ${candidate.imageUrl}", 200, connection.responseCode)
                val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                assertNotNull("${game.title}: image decoding", bitmap)
                bitmap!!.recycle()
                val probeName = "nes-download-probe-${game.id}"
                val probe = ImportedIconStore(context).downloadNesCatalogCover(probeName,
                    candidates.map { it.imageUrl }, game.imageUri) {}
                assertNotNull("${game.title}: bounded download/save", probe)
                java.io.File(android.net.Uri.parse(probe).path!!).delete()
            } finally { connection.disconnect() }
        }
        val persisted = kotlinx.coroutines.CompletableDeferred<Unit>()
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "games" && expected.all { old -> stored().firstOrNull { it.id == old.id }?.imageUri?.contains("nes-portrait-v2") == true }) persisted.complete(Unit)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        // This explicit live-source verification must not inherit yesterday's failed
        // lookup backoff; normal startup retains its 24-hour retry protection.
        assertTrue(context.getSharedPreferences("nes-portrait-v2", android.content.Context.MODE_PRIVATE).edit().clear().commit())
        val owner = androidx.lifecycle.ViewModelStore()
        try {
            instrumentation.runOnMainSync { owner.put("nes-portrait-test", com.ariy.retrolauncher.LauncherViewModel(context.applicationContext as android.app.Application)) }
            kotlinx.coroutines.withTimeout(240_000) { persisted.await() }
            assertTrue(preferences.edit().commit())
            for (old in expected) {
                val after = stored().first { it.id == old.id }
                assertEquals(old, after.copy(imageUri = old.imageUri))
            }
        } finally {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            instrumentation.runOnMainSync { owner.clear() }
        }
    }
    @Test fun genericDownloadRejectsThumbnailsAndRetainsOriginal(): Unit {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ImportedIconStore(context)
        val name = "nes-policy-test-${java.util.UUID.randomUUID()}"
        val originalBitmap = Bitmap.createBitmap(512, 700, Bitmap.Config.ARGB_8888)
        val tiny = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val good = Bitmap.createBitmap(512, 700, Bitmap.Config.ARGB_8888)
        var oldFile: java.io.File? = null
        var newFile: java.io.File? = null
        try {
            val original = requireNotNull(store.saveBitmap(name, originalBitmap))
            oldFile = java.io.File(android.net.Uri.parse(original).path!!)
            val bytes = oldFile.readBytes()
            assertNull(store.downloadNesCatalogCover("$name-new", emptyList(), original) {})
            var attempts = 0
            val saved = requireNotNull(store.downloadNesCatalogCover("$name-new", listOf("tiny", "good"), original,
                load = { attempts++; if (it == "tiny") tiny else good }) {})
            assertEquals(2, attempts)
            assertTrue(tiny.isRecycled)
            assertTrue(good.isRecycled)
            newFile = java.io.File(android.net.Uri.parse(saved).path!!)
            assertTrue(newFile.isFile)
            assertNotEquals(original, saved)
            assertArrayEquals(bytes, oldFile.readBytes())
        } finally {
            oldFile?.delete(); newFile?.delete()
            originalBitmap.recycle(); tiny.recycle(); good.recycle()
        }
    }
}