package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.StackedArtworkTile
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StackedLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fourColumnsUseTallTemplatesAndKeepLaunchAndEditActions() {
        var launched = 0
        var edited = 0
        val games = List(8) { i -> GameEntry(id = "stacked-$i", title = "Galaga", platform = "MAME Arcade",
            sourceType = GameSourceType.CUSTOM_INTENT, intentDataUri = "file:///galaga.zip") }
        compose.setContent {
            MaterialTheme {
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(games, key = { it.id }) { game ->
                        StackedArtworkTile(game, columns = 4, editMode = game.id == "stacked-1",
                            onLaunch = { launched++ }, onEdit = { edited++ }, modifier = Modifier.testTag(game.id))
                    }
                }
            }
        }
        val first = compose.onNodeWithTag("stacked-0").fetchSemanticsNode().boundsInRoot
        for(i in 1..3) {
            val bounds = compose.onNodeWithTag("stacked-$i").fetchSemanticsNode().boundsInRoot
            assertEquals(first.top, bounds.top, 1f)
            assertEquals(first.width, bounds.width, 1f)
            assertTrue(bounds.left > first.left)
        }
        assertEquals(0.46f, first.width/first.height, 0.01f)
        compose.onNodeWithTag("stacked-0").performClick()
        compose.onNodeWithTag("stacked-1").performClick()
        compose.runOnIdle { assertEquals(1, launched); assertEquals(1, edited) }
        compose.waitForIdle()
        val settled = compose.onNodeWithTag("stacked-0").fetchSemanticsNode().boundsInRoot
        assertEquals(first.height, settled.height, 1f)
    }
}