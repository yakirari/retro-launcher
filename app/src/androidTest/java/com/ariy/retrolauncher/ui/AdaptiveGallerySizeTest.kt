package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.GridNostalgicCard
import com.ariy.retrolauncher.RecommendationAddCard
import com.ariy.retrolauncher.ShelfstalgicItemLayout
import com.ariy.retrolauncher.ShelfstalgicSlot
import com.ariy.retrolauncher.StackedArtworkTile
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.GalleryTileSize
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdaptiveGallerySizeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectorShowsOnlyNamedSizesAndSelectsEachOne() {
        val selected = mutableStateOf(GalleryTileSize.MEDIUM)
        compose.setContent {
            MaterialTheme { GalleryTileSizeSelector(selected.value) { selected.value = it } }
        }
        compose.onNodeWithText("Medium").assertIsSelected()
        for (size in GalleryTileSize.entries) {
            compose.onNodeWithText(size.label).performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(size, selected.value) }
        }
        for (count in listOf("1", "2", "3", "4", "8")) compose.onNodeWithText(count).assertDoesNotExist()
    }

    @Test fun gridFitsCompleteRowsIncludingRecommendationsAndAddCard() = checkGrid(stacked = false)

    @Test fun stackedFitsCompleteCabinetRowsAndKeepsActions() = checkGrid(stacked = true)

    private fun checkGrid(stacked: Boolean) {
        val size = mutableStateOf(GalleryTileSize.MEDIUM)
        val viewport = mutableStateOf(360.dp to 240.dp)
        var sizing = GalleryGridSizing(1, 1f)
        var density = 1f
        var launches = 0
        var edits = 0
        val game = GameEntry(id = "sizing", title = "Galaga", platform = "MAME Arcade",
            sourceType = GameSourceType.CUSTOM_INTENT, intentDataUri = "file:///galaga.zip")
        compose.setContent {
            MaterialTheme {
                BoxWithConstraints(Modifier.size(viewport.value.first, viewport.value.second).testTag("viewport")) {
                    density = LocalDensity.current.density
                    val spacing = if (stacked) 4.dp else 10.dp
                    sizing = galleryGridSizing(maxWidth.value, maxHeight.value, size.value,
                        if (stacked) 0.46f else 1f, spacing.value, density)
                    LazyVerticalGrid(columns = GridCells.Fixed(sizing.columns), modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing)) {
                        items(100) { index ->
                            val tag = Modifier.testTag("tile-$index")
                            if (!stacked && index == 2) {
                                RecommendationAddCard(game, sizing.columns, "transparent",
                                    tileHeight = sizing.itemHeightDp.dp, onAdd = { launches++ }, modifier = tag)
                            } else if (stacked) {
                                StackedArtworkTile(game, sizing.columns, editMode = index == 1,
                                    tileHeight = sizing.itemHeightDp.dp, onLaunch = { launches++ },
                                    onEdit = { edits++ }, modifier = tag)
                            } else {
                                GridNostalgicCard(game, sizing.columns, editMode = index == 1,
                                    tileBackgroundMode = "transparent", normalizeCanvasSize = true,
                                    tileHeight = sizing.itemHeightDp.dp, isRecommendation = index == 0,
                                    onLaunch = { launches++ }, onEdit = { edits++ }, modifier = tag)
                            }
                        }
                    }
                }
            }
        }
        for (dimensions in listOf(360.dp to 240.dp, 240.dp to 360.dp, 320.dp to 130.dp, 120.dp to 360.dp)) {
            for (choice in GalleryTileSize.entries) {
                compose.runOnIdle { viewport.value = dimensions; size.value = choice }
                compose.waitForIdle()
                val bounds = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
                val expectedHeight = sizing.itemHeightDp * density
                if (choice == GalleryTileSize.LARGE) {
                    val first = compose.onNodeWithTag("tile-0").fetchSemanticsNode().boundsInRoot
                    val second = compose.onNodeWithTag("tile-1").fetchSemanticsNode().boundsInRoot
                    assertEquals(first.top, second.top, 0.5f)
                    assertTrue(second.left > first.left)
                }
                val required = sizing.columns * choice.minimumVisibleItems
                for (index in 0 until required) {
                    val tile = compose.onNodeWithTag("tile-$index").fetchSemanticsNode().boundsInRoot
                    assertEquals("$stacked / $choice / $dimensions / $index", expectedHeight, tile.height, 1f)
                    assertTrue(tile.top >= bounds.top - 0.5f)
                    assertTrue(tile.bottom <= bounds.bottom + 0.5f)
                    assertTrue(tile.left >= bounds.left - 0.5f)
                    assertTrue(tile.right <= bounds.right + 0.5f)
                }
            }
        }
        compose.onNodeWithTag("tile-0").performClick()
        compose.onNodeWithTag("tile-1").performClick()
        compose.runOnIdle { assertEquals(1, launches); assertEquals(1, edits) }
    }

    @Test fun centeredShelfFitsCompleteSlotsAtBothEndsForAllSizes() {
        val size = mutableStateOf(GalleryTileSize.MEDIUM)
        val atEnd = mutableStateOf(false)
        val viewport = mutableStateOf(360.dp to 240.dp)
        var density = 1f
        var slotWidth = 1f
        var artworkHeight = 1f
        compose.setContent {
            MaterialTheme {
                BoxWithConstraints(Modifier.size(viewport.value.first, viewport.value.second).testTag("viewport")) {
                    density = LocalDensity.current.density
                    slotWidth = shelfSlotWidth(maxWidth.value, maxHeight.value, size.value, density)
                    artworkHeight = shelfArtworkHeight(maxHeight.value, size.value)
                    val state = rememberLazyListState()
                    LaunchedEffect(size.value, atEnd.value, viewport.value) {
                        state.scrollToItem(if (atEnd.value) 19 else 0)
                    }
                    LazyRow(state = state, modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = (maxWidth - slotWidth.dp) / 2f),
                        horizontalArrangement = Arrangement.spacedBy(SHELF_ITEM_SPACING_DP.dp)) {
                        items(20) { index ->
                            ShelfstalgicSlot(
                                ShelfstalgicItemLayout(slotWidth.dp, (slotWidth - 16f).coerceAtLeast(1f).dp,
                                    false, 8.dp, 8.dp, 1f, 0f, 1f),
                                artworkHeight.dp, Modifier.testTag("slot-$index")
                            ) { Box(Modifier.height(artworkHeight.dp)) }
                        }
                    }
                }
            }
        }
        for (dimensions in listOf(360.dp to 240.dp, 240.dp to 360.dp, 320.dp to 130.dp)) {
            for (choice in GalleryTileSize.entries) {
                for (end in listOf(false, true)) {
                    compose.runOnIdle { viewport.value = dimensions; size.value = choice; atEnd.value = end }
                    compose.waitForIdle()
                    val bounds = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
                    val indices = if (end) (20 - choice.minimumVisibleItems until 20)
                        else (0 until choice.minimumVisibleItems)
                    for (index in indices) {
                        val tile = compose.onNodeWithTag("slot-$index").fetchSemanticsNode().boundsInRoot
                        assertEquals("$choice / $dimensions / end=$end / $index", slotWidth * density, tile.width, 1f)
                        assertEquals((artworkHeight + 16f) * density, tile.height, 1f)
                        assertTrue(tile.left >= bounds.left - 0.5f && tile.right <= bounds.right + 0.5f)
                        assertTrue(tile.top >= bounds.top - 0.5f && tile.bottom <= bounds.bottom + 0.5f)
                    }
                }
            }
        }
    }
}
