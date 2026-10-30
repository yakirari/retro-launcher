package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.model.GalleryTileSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class GalleryGridSizing(val columns: Int, val itemHeightDp: Float)

/** Round in physical pixels so all requested rows, including their gaps, fit exactly. */
internal fun galleryGridSizing(
    widthDp: Float,
    heightDp: Float,
    size: GalleryTileSize,
    aspectRatio: Float = 1f,
    spacingDp: Float = 10f,
    density: Float = 1f
): GalleryGridSizing {
    require(widthDp.isFinite() && heightDp.isFinite())
    require(aspectRatio.isFinite() && aspectRatio > 0f)
    require(spacingDp.isFinite() && spacingDp >= 0f)
    require(density.isFinite() && density > 0f)
    val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
    val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)
    val spacingPx = (spacingDp * density).roundToInt()
    val rows = size.minimumVisibleItems
    val rowHeightPx = ((heightPx - spacingPx * (rows - 1)) / rows).coerceAtLeast(1)
    val maxColumns = ((widthPx + spacingPx) / (1 + spacingPx)).coerceAtLeast(1)
    val minColumns = if (size == GalleryTileSize.LARGE) 2 else 1
    val columns = ceil((widthPx + spacingPx) / (rowHeightPx * aspectRatio + spacingPx))
        .toInt().coerceIn(minColumns.coerceAtMost(maxColumns), maxColumns)
    val cellWidthPx = ((widthPx - spacingPx * (columns - 1)) / columns).coerceAtLeast(1)
    val itemHeightPx = floor(cellWidthPx / aspectRatio).toInt().coerceIn(1, rowHeightPx)
    return GalleryGridSizing(columns, itemHeightPx / density)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryTileSizeSelector(size: GalleryTileSize, onSizeSelected: (GalleryTileSize) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GalleryTileSize.entries.forEach { option ->
            FilterChip(selected = size == option, onClick = { onSizeSelected(option) },
                label = { Text(option.label) })
        }
    }
}
