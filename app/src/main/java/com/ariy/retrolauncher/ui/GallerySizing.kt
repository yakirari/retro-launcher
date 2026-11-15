package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.model.GalleryTileSize
import com.ariy.retrolauncher.model.GamePlatforms
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class GalleryGridSizing(val columns: Int, val itemHeightDp: Float)

internal fun usesSquareAppIconTile(platform: String?): Boolean {
    val normalized = GamePlatforms.normalizeOrNull(platform)
    return normalized == GamePlatforms.ANDROID || normalized == GamePlatforms.EMULATORS
}

private val threeColumnStackedLargePlatforms = setOf(
    "mamearcade",
    "finalburnneo",
    "gameboy",
    "gameboycolor",
    "dos",
    "dosgames",
    "scummvm",
    "switch",
    "nintendoswitch",
    "nintendogamecube",
    "gc",
    "psp",
    "sonyplaystationportable",
    "gg",
    "gamegear",
    "segagamegear"
)

internal fun usesThreeColumnStackedLarge(platform: String?): Boolean {
    val normalized = GamePlatforms.normalizeOrNull(platform)
    if (normalized == "MAME Arcade" || normalized == "Final Burn Neo" ||
        normalized == "Game Boy" || normalized == "Game Boy Color" || normalized == "DOS Games" ||
        normalized == "ScummVM" || normalized == "Nintendo Switch" || normalized == "Nintendo GameCube" ||
        normalized == "Sony PlayStation Portable") {
        return true
    }
    val platformKey = platform?.filter(Char::isLetterOrDigit)?.lowercase().orEmpty()
    return platformKey in threeColumnStackedLargePlatforms
}

/** Round in physical pixels so all requested rows, including their gaps, fit exactly. */
internal fun galleryGridSizing(
    widthDp: Float,
    heightDp: Float,
    size: GalleryTileSize,
    aspectRatio: Float = 1f,
    spacingDp: Float = 10f,
    density: Float = 1f,
    maximumColumns: Int? = null,
    minimumColumns: Int? = null
): GalleryGridSizing {
    require(widthDp.isFinite() && heightDp.isFinite())
    require(aspectRatio.isFinite() && aspectRatio > 0f)
    require(spacingDp.isFinite() && spacingDp >= 0f)
    require(density.isFinite() && density > 0f)
    require(maximumColumns == null || maximumColumns > 0)
    require(minimumColumns == null || minimumColumns > 0)
    val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
    val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)
    val spacingPx = (spacingDp * density).roundToInt()
    val rows = size.minimumVisibleItems
    val rowHeightPx = ((heightPx - spacingPx * (rows - 1)) / rows).coerceAtLeast(1)
    val availableColumns = ((widthPx + spacingPx) / (1 + spacingPx)).coerceAtLeast(1)
    val maxColumns = minOf(availableColumns, maximumColumns ?: availableColumns)
    val minColumns = if (size == GalleryTileSize.LARGE) 2 else 1
    val requestedMinimumColumns = minOf(minimumColumns ?: minColumns, maxColumns)
    val columns = ceil((widthPx + spacingPx) / (rowHeightPx * aspectRatio + spacingPx))
        .toInt().coerceIn(requestedMinimumColumns, maxColumns)
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
