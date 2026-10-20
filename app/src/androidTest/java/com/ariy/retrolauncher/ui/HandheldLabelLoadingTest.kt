package com.ariy.retrolauncher.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.NostalgicOverlays
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HandheldLabelLoadingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun delayedStickerNeverFlashesCoverOrPreviousGameAndRevisitUsesSettledArt() {
        val spec = requireNotNull(NostalgicOverlays.specForPlatform("Game Boy Advance"))
        val prefix = UUID.randomUUID().toString()
        fun game(name: String) = GameEntry(title = name, platform = "Game Boy Advance",
            sourceType = GameSourceType.CUSTOM_INTENT, intentDataUri = "content://test/$prefix/$name.gba")
        fun image(color: Int) = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val cover = image(Color.RED).asImageBitmap()
        val first = game("first")
        val second = game("second")
        val current = mutableStateOf(first)
        val gate = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        val requests = AtomicInteger()
        val shown = AtomicInteger()
        val placeholder = 0xFFF0EDE4.toInt()
        compose.setContent {
            val label = rememberHandheldLabel(current.value, spec, cover, false) { _, entry ->
                if (entry.title == "first") {
                    if (requests.incrementAndGet() == 1) gate.await() else CompletableDeferred<Unit>().await()
                    image(Color.BLUE)
                } else {
                    gate2.await()
                    image(Color.GREEN)
                }
            }
            SideEffect { shown.set(requireNotNull(label).asAndroidBitmap().getPixel(320, 100)) }
        }
        compose.runOnIdle { assertEquals(placeholder, shown.get()); gate.complete(Unit) }
        compose.waitUntil(5000) { shown.get() == Color.BLUE }
        compose.runOnIdle { current.value = second }
        compose.runOnIdle { assertEquals(placeholder, shown.get()); gate2.complete(Unit) }
        compose.waitUntil(5000) { shown.get() == Color.GREEN }
        compose.runOnIdle { current.value = first }
        compose.runOnIdle { assertEquals("Revisited sticker remains while lookup refreshes", Color.BLUE, shown.get()) }
    }
}