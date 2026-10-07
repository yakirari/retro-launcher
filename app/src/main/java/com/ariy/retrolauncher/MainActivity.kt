package com.ariy.retrolauncher

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Process
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.util.LruCache
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ariy.retrolauncher.launch.CustomIntentTemplate
import com.ariy.retrolauncher.launch.ArtworkCandidate
import com.ariy.retrolauncher.launch.GameRecommendation
import com.ariy.retrolauncher.launch.GameRecommendations
import com.ariy.retrolauncher.launch.InstalledActivityOption
import com.ariy.retrolauncher.launch.InstalledShortcutOption
import com.ariy.retrolauncher.launch.arcadeOverlayAssetBaseNames
import com.ariy.retrolauncher.launch.defaultSubtitle
import com.ariy.retrolauncher.launch.findMatchingArcadeOverlayAssetName
import com.ariy.retrolauncher.launch.isGenericArcadeFallbackAssetName
import com.ariy.retrolauncher.launch.artworkSearchQuery
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.GameFilters
import com.ariy.retrolauncher.model.GamePlatforms
import com.ariy.retrolauncher.model.NostalgicOverlaySpec
import com.ariy.retrolauncher.model.NostalgicOverlays
import com.ariy.retrolauncher.model.PlatformSvgIcons
import com.ariy.retrolauncher.model.PlatformTopTen
import com.ariy.retrolauncher.model.GameSourceType
import com.ariy.retrolauncher.model.LaunchTarget
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.input.ImeAction
import org.json.JSONObject
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val AppBackgroundColor = Color(0xFF0D1117)
private const val APP_BACKGROUND_COLOR_ARGB = 0xFF0D1117.toInt()
private const val FEATURED_GALLERY_CARD_ASPECT_RATIO = 2.5f / 3.5f
private const val NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO = 960f / 768f
private const val MAME_CABINET_CANVAS_ASPECT_RATIO = 960f / 768f
private const val GALLERYSTALGIC_CONTENT_HEIGHT_FRACTION = 0.8f
private const val GALLERYSTALGIC_CONTENT_MAX_WIDTH_FRACTION = 0.86f
private const val MAME_CABINET_CANVAS_WIDTH = 960
private const val MAME_CABINET_CANVAS_HEIGHT = 768
private const val MAME_CABINET_TARGET_MAX_WIDTH = 760f
private const val MAME_CABINET_TARGET_MAX_HEIGHT = 700f
private const val MAME_CABINET_BOTTOM_PADDING = 18f
private const val TILE_BITMAP_CACHE_BYTES = 48 * 1024 * 1024
private const val TILE_ARTWORK_MAX_DIMENSION = 1024
private const val GALLERY_ARTWORK_PREFETCH_BEHIND = 2
private const val GALLERY_ARTWORK_PREFETCH_AHEAD = 6
private const val NOSTALGIC_OVERLAY_TRANSPARENT_ALPHA_THRESHOLD = 16
private const val NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD = 24
private const val MIN_NOSTALGIC_ARTWORK_WINDOW_PIXELS = 4_096
private const val RECOMMENDATION_FILTER_CACHE_TREE_URI_PREF_KEY = "recommendation_filter_cache_tree_uri"
private const val RECOMMENDATION_FILTER_CACHE_VALUES_PREF_KEY = "recommendation_filter_cache_values"
private const val RECOMMENDATION_FILTER_CACHE_SUPPORTED_PREF_KEY = "recommendation_filter_cache_supported"

private data class NostalgicArtworkPlacement(
    val leftFraction: Float,
    val topFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val cornerRadiusFraction: Float
)

private data class NostalgicOverlayRenderData(
    val overlayBitmap: ImageBitmap,
    val aspectRatio: Float,
    val occupiedWidthFraction: Float,
    val artworkPlacement: NostalgicArtworkPlacement?,
    val artworkMaskBitmap: ImageBitmap?
)

private data class MameCabinetRenderData(
    val bitmap: ImageBitmap,
    val aspectRatio: Float,
    val occupiedWidthFraction: Float
)

private data class RenderedBitmapData(
    val bitmap: Bitmap,
    val offsetX: Int,
    val offsetY: Int
)

private data class SameNostalgicLayoutMetrics(
    val targetAspectRatio: Float,
    val occupiedWidthFraction: Float
)

private data class RecommendationUiState(
    val suggestions: List<GameRecommendation> = emptyList(),
    val isLoading: Boolean = false
)

private data class RecommendationTileUi(
    val entry: GameEntry,
    val recommendation: GameRecommendation? = null,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
    val canSkip: Boolean = false,
    val badgeLabel: String = "Recommended"
)

private data class RecommendationAddTileUi(
    val entry: GameEntry,
    val platformFilter: String?,
    val collectionFilter: String? = null
)

private data class RecommendationTileActions(
    val onPlay: () -> Unit,
    val onAdd: () -> Unit,
    val onSkip: (() -> Unit)? = null
)

private enum class RecommendationActionKey {
    Play,
    Add,
    Skip
}

private data class RecommendationActionOption(
    val key: RecommendationActionKey,
    val onClick: () -> Unit
)

private fun RecommendationTileActions.options(): List<RecommendationActionOption> = buildList {
    add(RecommendationActionOption(RecommendationActionKey.Play, onPlay))
    add(RecommendationActionOption(RecommendationActionKey.Add, onAdd))
    onSkip?.let { add(RecommendationActionOption(RecommendationActionKey.Skip, it)) }
}

private fun isRecommendationActivateKey(key: Key): Boolean =
    key == Key.Enter ||
        key == Key.NumPadEnter ||
        key == Key.DirectionCenter ||
        key == Key.Spacebar ||
        key == Key.ButtonA

private data class TransparentRegionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val pixelCount: Int,
    val touchesEdge: Boolean = false
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top
}

private data class MameFallbackScreenRegion(
    val bounds: android.graphics.Rect,
    val clearOverlayWindow: Boolean
)

private data class MameCabinetForeground(
    val bitmap: Bitmap,
    val sourceBounds: android.graphics.Rect
)

private data class MameCabinetLayout(
    val drawLeft: Float,
    val drawTop: Float,
    val drawWidth: Float,
    val drawHeight: Float,
    val scale: Float
)

private data class LauncherBackgroundMotion(
    val scale: Float,
    val panXFraction: Float,
    val panYFraction: Float
)

private data class ShelfstalgicItemLayout(
    val slotWidth: Dp,
    val width: Dp,
    val compactMode: Boolean,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val alpha: Float,
    val translationY: Float,
    val zIndex: Float
)

private val recentTileBitmapCache = object : LruCache<String, Bitmap>(TILE_BITMAP_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            value.allocationByteCount
        } else {
            value.byteCount
        }
}

private val nostalgicOverlayRenderCache = java.util.concurrent.ConcurrentHashMap<String, NostalgicOverlayRenderData>()
private val sameNostalgicOverlayAspectRatioCache = java.util.concurrent.ConcurrentHashMap<Int, Float>()
private val sameNostalgicMameAspectRatioCache = java.util.concurrent.ConcurrentHashMap<String, Float>()
private val sameNostalgicOverlayOccupiedWidthCache = java.util.concurrent.ConcurrentHashMap<Int, Float>()
private val sameNostalgicMameOccupiedWidthCache = java.util.concurrent.ConcurrentHashMap<String, Float>()
private val platformBadgeBitmapCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
private const val LAUNCHER_LAYOUT_MODE_GRID = "grid"
private const val LAUNCHER_LAYOUT_MODE_GRIDSTALGIC = "gridstalgic"
private const val LAUNCHER_LAYOUT_MODE_GALLERY = "gallery"
private const val LAUNCHER_LAYOUT_MODE_NOSTALGIC = "nostalgic"
private const val LAUNCHER_LAYOUT_MODE_SHELFSTALGIC = "shelfstalgic"
private const val LAUNCHER_LAYOUT_MODE_SAMESTALGIC = "samestalgic"
private const val LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC = "gallerystalgic"
private const val LAUNCHER_BACKGROUND_NONE = "none"
private const val LAUNCHER_BACKGROUND_CUSTOM = "custom"
private const val LAUNCHER_BACKGROUND_PREF_KEY = "launcher_background"
private const val LAUNCHER_BACKGROUND_CUSTOM_URI_PREF_KEY = "launcher_background_custom_uri"
private const val LAUNCHER_BACKGROUND_ANIMATION_PREF_KEY = "launcher_background_animation"
private const val LAUNCHER_BACKGROUND_BLUR_PREF_KEY = "launcher_background_blur"
private const val SHOW_TOP_10_BADGES_PREF_KEY = "show_top_10_badges"
private const val LAUNCHER_TILE_BACKGROUND_MODE_NORMAL = "normal"
private const val LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT = "transparent"
private const val LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY = "glossy"
private const val LAUNCHER_TILE_BACKGROUND_PREF_KEY = "launcher_tile_background"
private const val TRANSPARENT_TILE_BACKGROUND_ALPHA_SCALE = 0.55f
private const val LAUNCHER_BACKGROUND_STATIC_MAX_DIMENSION = 1_920
private const val LAUNCHER_BACKGROUND_ANIMATED_MAX_DIMENSION = 3_072
private const val LAUNCHER_BACKGROUND_ANIMATION_SCALE = 2.03f
private val LauncherBackgroundScrimColor = Color(0x8C0D1117)
private val LauncherBackgroundBlurRadius = 8.dp
private const val LAUNCHER_BACKGROUND_ANIMATION_SEGMENT_MILLIS = 11_000
private const val LAUNCHER_BACKGROUND_START_STEPS_PER_SEGMENT = 32
private const val LAUNCHER_BACKGROUND_START_STEP_DELTA = 11

private fun normalizeLauncherLayoutMode(raw: String?): String = when {
    raw.equals(LAUNCHER_LAYOUT_MODE_GRIDSTALGIC, ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_GRIDSTALGIC
    raw.equals(LAUNCHER_LAYOUT_MODE_GALLERY, ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_GALLERY
    raw.equals(LAUNCHER_LAYOUT_MODE_NOSTALGIC, ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_NOSTALGIC
    raw.equals(LAUNCHER_LAYOUT_MODE_SHELFSTALGIC, ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_SHELFSTALGIC
    raw.equals(LAUNCHER_LAYOUT_MODE_SAMESTALGIC, ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_SAMESTALGIC
    raw.equals(LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC, ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC
    raw.equals("boxart", ignoreCase = true) -> LAUNCHER_LAYOUT_MODE_NOSTALGIC
    else -> LAUNCHER_LAYOUT_MODE_GRID
}

private fun isGalleryLayoutMode(mode: String?): Boolean =
    mode.equals(LAUNCHER_LAYOUT_MODE_GALLERY, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_NOSTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_SHELFSTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_SAMESTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC, ignoreCase = true)

private fun isNostalgicLayoutMode(mode: String?): Boolean =
    mode.equals(LAUNCHER_LAYOUT_MODE_NOSTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_SHELFSTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_SAMESTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC, ignoreCase = true)

private fun isShelfNostalgicLayoutMode(mode: String?): Boolean =
    mode.equals(LAUNCHER_LAYOUT_MODE_SHELFSTALGIC, ignoreCase = true)

private fun usesSameNostalgicCanvas(mode: String?): Boolean =
    mode.equals(LAUNCHER_LAYOUT_MODE_SAMESTALGIC, ignoreCase = true) ||
        mode.equals(LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC, ignoreCase = true)

private fun showsNostalgicGalleryInfo(mode: String?): Boolean =
    mode.equals(LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC, ignoreCase = true)

private fun showsGridNostalgicTiles(mode: String?): Boolean =
    mode.equals(LAUNCHER_LAYOUT_MODE_GRIDSTALGIC, ignoreCase = true)

private fun availableLauncherBackgroundAssets(context: Context): List<String> =
    runCatching {
        context.assets.list("backgrounds")
            ?.filter { fileName ->
                fileName.endsWith(".png", ignoreCase = true) ||
                    fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) ||
                    fileName.endsWith(".webp", ignoreCase = true)
            }
            ?.sortedBy { it.lowercase() }
            .orEmpty()
    }.getOrDefault(emptyList())

private fun normalizeLauncherBackground(
    raw: String?,
    availableBackgrounds: List<String>,
    hasCustomBackground: Boolean = false
): String {
    val normalized = raw?.trim().orEmpty()
    return when {
        normalized.isBlank() -> LAUNCHER_BACKGROUND_NONE
        normalized.equals(LAUNCHER_BACKGROUND_NONE, ignoreCase = true) -> LAUNCHER_BACKGROUND_NONE
        normalized.equals(LAUNCHER_BACKGROUND_CUSTOM, ignoreCase = true) && hasCustomBackground ->
            LAUNCHER_BACKGROUND_CUSTOM
        availableBackgrounds.any { it.equals(normalized, ignoreCase = true) } ->
            availableBackgrounds.first { it.equals(normalized, ignoreCase = true) }
        else -> LAUNCHER_BACKGROUND_NONE
    }
}

private fun normalizeLauncherTileBackgroundMode(raw: String?): String = when {
    raw.equals(LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT, ignoreCase = true) ->
        LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT
    raw.equals(LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY, ignoreCase = true) ->
        LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY
    else -> LAUNCHER_TILE_BACKGROUND_MODE_NORMAL
}

private fun usesTransparentTileBackgroundMode(mode: String?): Boolean =
    normalizeLauncherTileBackgroundMode(mode) != LAUNCHER_TILE_BACKGROUND_MODE_NORMAL

private fun isGlossyTileBackgroundMode(mode: String?): Boolean =
    normalizeLauncherTileBackgroundMode(mode) == LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY

private fun isTransparentTileBackgroundMode(mode: String?): Boolean =
    normalizeLauncherTileBackgroundMode(mode) == LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT

private fun resolvedTileBackgroundAlpha(mode: String?, alpha: Float): Float =
    when {
        isTransparentTileBackgroundMode(mode) -> alpha * TRANSPARENT_TILE_BACKGROUND_ALPHA_SCALE
        usesTransparentTileBackgroundMode(mode) -> alpha
        else -> 1f
    }

private fun tileSurfaceColor(
    mode: String?,
    normalColor: Color,
    translucentAlpha: Float
): Color =
    if (usesTransparentTileBackgroundMode(mode)) {
        normalColor.copy(alpha = resolvedTileBackgroundAlpha(mode, translucentAlpha))
    } else {
        normalColor
    }

private fun tileBackdropColor(
    mode: String?,
    baseColor: Color,
    normalAlpha: Float,
    translucentAlpha: Float
): Color =
    baseColor.copy(
        alpha = if (usesTransparentTileBackgroundMode(mode)) {
            resolvedTileBackgroundAlpha(mode, translucentAlpha)
        } else {
            normalAlpha
        }
    )

private fun Modifier.glossyTileOverlay(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        this.drawWithCache {
            val topGlow = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height
            )
            val diagonalHighlight = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                start = Offset(x = size.width * 0.04f, y = 0f),
                end = Offset(x = size.width * 0.96f, y = size.height)
            )
            val bottomShade = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x12000000),
                    Color(0x22000000)
                ),
                startY = size.height * 0.56f,
                endY = size.height
            )
            val rimHeight = 1.5.dp.toPx().coerceAtLeast(1f)
            onDrawWithContent {
                drawContent()
                drawRect(brush = bottomShade)
                drawRect(brush = topGlow)
                drawRect(brush = diagonalHighlight)
                drawRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset.Zero,
                    size = Size(width = size.width, height = rimHeight)
                )
            }
        }
    }

private fun launcherBackgroundLabel(assetName: String): String =
    assetName.substringBeforeLast('.')
        .replace(Regex("([A-Za-z])([0-9])"), "$1 $2")
        .uppercase()

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<LauncherViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onLauncherResumed()
    }
}

private data class RecommendationFilterScanState(
    val filters: Set<String> = emptySet(),
    val completed: Int = 0,
    val total: Int = 0
) {
    val isLoading: Boolean
        get() = total > 0 && completed < total

    val progressPercent: Int
        get() = when {
            total <= 0 -> 0
            completed <= 0 -> 0
            completed >= total -> 100
            else -> (completed * 100) / total
        }
}

private data class CachedRecommendationPlatformFilters(
    val filters: Set<String> = emptySet(),
    val isPresent: Boolean = false
)

private fun loadCachedRecommendationPlatformFilters(
    prefs: SharedPreferences,
    authorizedRomTreeUri: String?,
    supportedFilters: List<String>
): CachedRecommendationPlatformFilters {
    val normalizedTreeUri = authorizedRomTreeUri?.trim()?.takeIf(String::isNotBlank)
        ?: return CachedRecommendationPlatformFilters()
    val cachedTreeUri = prefs.getString(RECOMMENDATION_FILTER_CACHE_TREE_URI_PREF_KEY, null)
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: return CachedRecommendationPlatformFilters()
    if (cachedTreeUri != normalizedTreeUri) {
        return CachedRecommendationPlatformFilters()
    }
    val cachedSupportedFilters = prefs.getStringSet(RECOMMENDATION_FILTER_CACHE_SUPPORTED_PREF_KEY, null)
        ?.map { filter -> filter.lowercase() }
        ?.toSet()
        ?: return CachedRecommendationPlatformFilters()
    val currentSupportedFilters = supportedFilters.map { filter -> filter.lowercase() }.toSet()
    if (cachedSupportedFilters != currentSupportedFilters) {
        return CachedRecommendationPlatformFilters()
    }
    val cachedFilters = prefs.getStringSet(RECOMMENDATION_FILTER_CACHE_VALUES_PREF_KEY, emptySet()).orEmpty()
    val resolvedFilters = supportedFilters.filter { supportedFilter ->
        cachedFilters.any { cachedFilter -> cachedFilter.equals(supportedFilter, ignoreCase = true) }
    }.toSet()
    return CachedRecommendationPlatformFilters(
        filters = resolvedFilters,
        isPresent = true
    )
}

private fun saveCachedRecommendationPlatformFilters(
    prefs: SharedPreferences,
    authorizedRomTreeUri: String?,
    supportedFilters: List<String>,
    filters: Set<String>
) {
    val normalizedTreeUri = authorizedRomTreeUri?.trim()?.takeIf(String::isNotBlank)
    if (normalizedTreeUri == null) {
        prefs.edit()
            .remove(RECOMMENDATION_FILTER_CACHE_TREE_URI_PREF_KEY)
            .remove(RECOMMENDATION_FILTER_CACHE_VALUES_PREF_KEY)
            .remove(RECOMMENDATION_FILTER_CACHE_SUPPORTED_PREF_KEY)
            .apply()
        return
    }
    prefs.edit()
        .putString(RECOMMENDATION_FILTER_CACHE_TREE_URI_PREF_KEY, normalizedTreeUri)
        .putStringSet(RECOMMENDATION_FILTER_CACHE_VALUES_PREF_KEY, filters.toSet())
        .putStringSet(RECOMMENDATION_FILTER_CACHE_SUPPORTED_PREF_KEY, supportedFilters.toSet())
        .apply()
}

@Composable
private fun LauncherRoot(viewModel: LauncherViewModel = viewModel()) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcutOptions.collectAsStateWithLifecycle()
    val activities by viewModel.activityOptions.collectAsStateWithLifecycle()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
    val genreFilter by viewModel.genreFilter.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val authorizedRomTreeUri by viewModel.authorizedRomTreeUri.collectAsStateWithLifecycle()
    val artworkSuggestions by viewModel.artworkSuggestions.collectAsStateWithLifecycle()
    val artworkLoading by viewModel.artworkLoading.collectAsStateWithLifecycle()
    val artworkPending by viewModel.artworkPending.collectAsStateWithLifecycle()
    val activeArtworkBackfillPlatforms by viewModel.activeArtworkBackfillPlatforms.collectAsStateWithLifecycle()
    val launcherResumeNonce by viewModel.launcherResumeNonce.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("retro_launcher", Context.MODE_PRIVATE) }
    val launcherBackgroundOptions = remember(context) { availableLauncherBackgroundAssets(context) }
    val hasRomFolderConfigured = !authorizedRomTreeUri.isNullOrBlank()
    val configuredRomFolderPath = remember(authorizedRomTreeUri) {
        authorizedRomTreeUri.selectedRomFolderDisplayPath()
    }
    val supportedRecommendationFilters = remember { GameRecommendations.supportedFilters() }
    val cachedRecommendationPlatformFilters = remember(prefs, authorizedRomTreeUri, supportedRecommendationFilters) {
        loadCachedRecommendationPlatformFilters(
            prefs = prefs,
            authorizedRomTreeUri = authorizedRomTreeUri,
            supportedFilters = supportedRecommendationFilters
        )
    }
    val collectionOptions = remember(games) {
        games.mapNotNull { it.genre?.trim()?.takeIf(String::isNotBlank) }
            .filter { GamePlatforms.normalizeOrNull(it) == null }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val recommendationPlatformFiltersState by produceState(
        initialValue = if (hasRomFolderConfigured && cachedRecommendationPlatformFilters.isPresent) {
            RecommendationFilterScanState(
                filters = cachedRecommendationPlatformFilters.filters,
                completed = supportedRecommendationFilters.size,
                total = supportedRecommendationFilters.size
            )
        } else if (hasRomFolderConfigured) {
            RecommendationFilterScanState(total = supportedRecommendationFilters.size)
        } else {
            RecommendationFilterScanState()
        },
        authorizedRomTreeUri,
        supportedRecommendationFilters,
        cachedRecommendationPlatformFilters
    ) {
        if (authorizedRomTreeUri.isNullOrBlank() || supportedRecommendationFilters.isEmpty()) {
            value = RecommendationFilterScanState()
            return@produceState
        }
        val totalFilters = supportedRecommendationFilters.size
        if (cachedRecommendationPlatformFilters.isPresent) {
            value = RecommendationFilterScanState(
                filters = cachedRecommendationPlatformFilters.filters,
                completed = totalFilters,
                total = totalFilters
            )
            return@produceState
        }
        val availableFilters = linkedSetOf<String>()
        value = RecommendationFilterScanState(total = totalFilters)
        supportedRecommendationFilters.forEachIndexed { index, filter ->
            val hasAvailableRoms = withContext(Dispatchers.IO) {
                GameRecommendations.filterHasAvailableRoms(
                    context = context,
                    authorizedRomTreeUri = authorizedRomTreeUri,
                    filter = filter
                )
            }
            if (hasAvailableRoms) {
                availableFilters += filter
            }
            value = RecommendationFilterScanState(
                filters = availableFilters.toSet(),
                completed = index + 1,
                total = totalFilters
            )
        }
        saveCachedRecommendationPlatformFilters(
            prefs = prefs,
            authorizedRomTreeUri = authorizedRomTreeUri,
            supportedFilters = supportedRecommendationFilters,
            filters = availableFilters.toSet()
        )
    }
    val recommendationPlatformFilters = recommendationPlatformFiltersState.filters
    val recommendationFiltersLoading = recommendationPlatformFiltersState.isLoading
    val recommendationFiltersProgressPercent = recommendationPlatformFiltersState.progressPercent
    val availablePlatformFilters = remember(games, recommendationPlatformFilters, genreFilter) {
        val platformsWithGames = games.mapNotNull { game -> game.filterPlatformLabel() }
        val availableFilters = (platformsWithGames + recommendationPlatformFilters)
            .distinctBy { it.lowercase() }
        GamePlatforms.filterValues().filter { candidate ->
            availableFilters.any { filter -> filter.equals(candidate, ignoreCase = true) }
        }
    }
    val filterOptions = remember(collectionOptions, availablePlatformFilters) {
        orderedFilterOptions(
            collections = collectionOptions,
            platformFilters = availablePlatformFilters
        )
    }

    var addDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addDialogSessionId by rememberSaveable { mutableIntStateOf(0) }
    var addDialogInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var addDialogPlatformFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var addDialogInitialCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var editGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectPreferredGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageTarget by remember { mutableStateOf<String?>(null) }
    var launchChooserGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var rpcsxLaunchMode by rememberSaveable {
        mutableStateOf(prefs.getString("rpcsx_launch_mode", "external") ?: "external")
    }
    var galleryFilterTransitionDirection by rememberSaveable { mutableIntStateOf(0) }
    var launcherLayoutMode by rememberSaveable {
        mutableStateOf(normalizeLauncherLayoutMode(prefs.getString("launcher_layout_mode", "grid")))
    }
    var launcherBackgroundAnimationEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(LAUNCHER_BACKGROUND_ANIMATION_PREF_KEY, false))
    }
    var launcherBackgroundBlurEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(LAUNCHER_BACKGROUND_BLUR_PREF_KEY, false))
    }
    var showTop10Badges by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SHOW_TOP_10_BADGES_PREF_KEY, true))
    }
    var launcherTileBackgroundMode by rememberSaveable {
        mutableStateOf(
            normalizeLauncherTileBackgroundMode(
                prefs.getString(LAUNCHER_TILE_BACKGROUND_PREF_KEY, LAUNCHER_TILE_BACKGROUND_MODE_NORMAL)
            )
        )
    }
    var launcherCustomBackgroundUri by rememberSaveable {
        mutableStateOf(prefs.getString(LAUNCHER_BACKGROUND_CUSTOM_URI_PREF_KEY, null)?.takeIf(String::isNotBlank))
    }
    var launcherBackground by rememberSaveable(launcherBackgroundOptions) {
        mutableStateOf(
            normalizeLauncherBackground(
                prefs.getString(LAUNCHER_BACKGROUND_PREF_KEY, LAUNCHER_BACKGROUND_NONE),
                launcherBackgroundOptions,
                hasCustomBackground = !prefs.getString(LAUNCHER_BACKGROUND_CUSTOM_URI_PREF_KEY, null).isNullOrBlank()
            )
        )
    }
    val hasCustomLauncherBackground = !launcherCustomBackgroundUri.isNullOrBlank()
    val launcherBackgroundIdentity = when {
        launcherBackground.equals(LAUNCHER_BACKGROUND_CUSTOM, ignoreCase = true) -> launcherCustomBackgroundUri
        launcherBackground.equals(LAUNCHER_BACKGROUND_NONE, ignoreCase = true) -> null
        else -> launcherBackground
    }
    val animateLauncherBackground = launcherBackgroundAnimationEnabled && !launcherBackgroundIdentity.isNullOrBlank()
    val launcherBackgroundBitmap = rememberLauncherBackgroundBitmap(
        assetName = launcherBackground.takeUnless {
            it.equals(LAUNCHER_BACKGROUND_NONE, ignoreCase = true) ||
                it.equals(LAUNCHER_BACKGROUND_CUSTOM, ignoreCase = true)
        },
        customUriString = launcherCustomBackgroundUri.takeIf {
            launcherBackground.equals(LAUNCHER_BACKGROUND_CUSTOM, ignoreCase = true)
        },
        animated = animateLauncherBackground
    )
    val launcherBackgroundMotion = rememberLauncherBackgroundMotion(
        enabled = animateLauncherBackground,
        backgroundKey = launcherBackgroundIdentity,
        launchCycle = launcherResumeNonce
    )
    val effectiveGenreFilter = remember(genreFilter) {
        val resolvedFilter = GamePlatforms.filterLabel(genreFilter) ?: GameFilters.normalizeFilter(genreFilter)
        if (GamePlatforms.isVisibleFilterLabel(resolvedFilter)) {
            resolvedFilter
        } else {
            GameFilters.ALL
        }
    }
    LaunchedEffect(genreFilter) {
        if (!GamePlatforms.isVisibleFilterLabel(genreFilter)) {
            viewModel.setGenreFilter(GameFilters.ALL)
        }
    }
    val visibleGames = remember(games, effectiveGenreFilter) {
        orderedGamesForFilter(games, effectiveGenreFilter)
    }
    var skippedRecommendationIds by rememberSaveable(effectiveGenreFilter) {
        mutableStateOf(emptySet<String>())
    }
    var recommendationCursorId by rememberSaveable(effectiveGenreFilter) {
        mutableStateOf<String?>(null)
    }
    var requestedRecommendationAddedGameId by rememberSaveable(effectiveGenreFilter) {
        mutableStateOf<String?>(null)
    }
    var pendingRecommendationDuringAdvance by remember(effectiveGenreFilter) {
        mutableStateOf<GameRecommendation?>(null)
    }
    val openAddDialog: (Int, String?, String?) -> Unit = { initialTab, platformFilter, collectionFilter ->
        addDialogInitialTab = initialTab
        addDialogPlatformFilter = platformFilter
        addDialogInitialCollection = collectionFilter
        addDialogSessionId += 1
        addDialogOpen = true
    }
    val shouldResolveRecommendation = remember(
        effectiveGenreFilter,
        authorizedRomTreeUri,
        recommendationPlatformFilters
    ) {
        false
    }
    LaunchedEffect(effectiveGenreFilter, authorizedRomTreeUri, shouldResolveRecommendation) {
        Log.w(
            "RetroLauncher",
            "Recommendation gate filter=$effectiveGenreFilter supported=${GameRecommendations.supportsFilter(effectiveGenreFilter)} hasTree=${!authorizedRomTreeUri.isNullOrBlank()} resolve=$shouldResolveRecommendation"
        )
    }
    val recommendationState by produceState(
        initialValue = RecommendationUiState(isLoading = shouldResolveRecommendation),
        effectiveGenreFilter,
        authorizedRomTreeUri,
        games,
        skippedRecommendationIds,
        recommendationCursorId,
        shouldResolveRecommendation
    ) {
        if (!shouldResolveRecommendation) {
            value = RecommendationUiState()
            return@produceState
        }
        value = RecommendationUiState(isLoading = true)
        val suggestions = withContext(Dispatchers.IO) {
            GameRecommendations.suggestionsForFilter(
                context = context,
                filter = effectiveGenreFilter,
                authorizedRomTreeUri = authorizedRomTreeUri,
                games = games,
                excludedRecommendationIds = skippedRecommendationIds,
                afterRecommendationId = recommendationCursorId,
                limit = 1
            )
        }
        value = RecommendationUiState(suggestions = suggestions)
    }
    val currentRecommendation = remember(visibleGames, recommendationState, skippedRecommendationIds) {
        recommendationState.suggestions.filterNot { recommendation ->
            recommendation.displayEntry.id in skippedRecommendationIds ||
                visibleGames.any { game -> game.id == recommendation.displayEntry.id }
        }.firstOrNull()
    }
    LaunchedEffect(effectiveGenreFilter, recommendationState.isLoading, currentRecommendation?.displayEntry?.id) {
        if (!recommendationState.isLoading || currentRecommendation != null) {
            pendingRecommendationDuringAdvance = null
        }
    }
    val activeRecommendationTile: RecommendationTileUi? = null
    val activeRecommendationAddTile = remember(
        effectiveGenreFilter,
        collectionOptions
    ) {
        recommendationAddTileForFilter(effectiveGenreFilter, collectionOptions)
    }
    val visibleGamesWithRecommendation = remember(visibleGames, activeRecommendationAddTile) {
        buildList {
            val addTileId = activeRecommendationAddTile?.entry?.id
            addAll(
                visibleGames.filterNot { game ->
                    addTileId != null && game.id == addTileId
                }
            )
            activeRecommendationAddTile?.entry?.let(::add)
        }
    }
    val recommendationId = activeRecommendationTile?.entry?.id
    val recommendationAddId = activeRecommendationAddTile?.entry?.id
    val recommendationPreview = rememberRecommendationPreviewBitmap(activeRecommendationTile?.recommendation?.previewAssetPath)
    val recommendationActions: RecommendationTileActions? = null
    val activePlatformFilter = remember(effectiveGenreFilter) { GamePlatforms.filterLabel(effectiveGenreFilter) }
    val activeCollectionFilter = remember(effectiveGenreFilter, collectionOptions, activePlatformFilter) {
        if (
            effectiveGenreFilter != GameFilters.ALL &&
            activePlatformFilter == null &&
            collectionOptions.any { it.equals(effectiveGenreFilter, ignoreCase = true) }
        ) {
            effectiveGenreFilter
        } else {
            null
        }
    }
    val swipeCycleFilters = remember(filterOptions, collectionOptions, availablePlatformFilters) {
        val orderedOptions = filterOptions
        orderedOptions.filter { option ->
            if (option.equals(GameFilters.ALL, ignoreCase = true)) {
                collectionOptions.isNotEmpty() || availablePlatformFilters.isNotEmpty()
            } else {
                val platformOption = GamePlatforms.filterLabel(option)
                if (platformOption != null) {
                    availablePlatformFilters.any { filter -> filter.equals(platformOption, ignoreCase = true) }
                } else {
                    collectionOptions.any { collection -> collection.equals(option, ignoreCase = true) }
                }
            }
        }
    }
    val selectFilter: (String, Int) -> Unit = { selectedFilter, direction ->
        if (!selectedFilter.equals(genreFilter, ignoreCase = true)) {
            galleryFilterTransitionDirection = direction
            viewModel.setGenreFilter(selectedFilter)
        }
    }
    LaunchedEffect(effectiveGenreFilter, games.size, visibleGamesWithRecommendation.size, swipeCycleFilters) {
        if (games.isEmpty()) return@LaunchedEffect
        if (visibleGamesWithRecommendation.isNotEmpty()) return@LaunchedEffect
        if (effectiveGenreFilter.equals(GameFilters.ALL, ignoreCase = true)) return@LaunchedEffect
        val nextFilter = nextFilterForSwipe(effectiveGenreFilter, swipeCycleFilters)
            ?.takeUnless { it.equals(effectiveGenreFilter, ignoreCase = true) }
            ?: return@LaunchedEffect
        selectFilter(nextFilter, 1)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = pendingImageTarget ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.updateGameImage(id, uri)
        }
        pendingImageTarget = null
    }

    val launcherBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val uriString = uri.toString()
        launcherCustomBackgroundUri = uriString
        launcherBackground = LAUNCHER_BACKGROUND_CUSTOM
        prefs.edit()
            .putString(LAUNCHER_BACKGROUND_CUSTOM_URI_PREF_KEY, uriString)
            .putString(LAUNCHER_BACKGROUND_PREF_KEY, LAUNCHER_BACKGROUND_CUSTOM)
            .apply()
    }

    val romFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.setAuthorizedRomTreeUri(uri.toString())
        }
    }

    LaunchedEffect(status) {
        val message = status
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(launcherBackgroundOptions, launcherCustomBackgroundUri) {
        val normalizedBackground = normalizeLauncherBackground(
            launcherBackground,
            launcherBackgroundOptions,
            hasCustomBackground = hasCustomLauncherBackground
        )
        if (!normalizedBackground.equals(launcherBackground, ignoreCase = true)) {
            launcherBackground = normalizedBackground
            prefs.edit().putString(LAUNCHER_BACKGROUND_PREF_KEY, normalizedBackground).apply()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundColor)
    ) {
        launcherBackgroundBitmap?.let { bitmap ->
            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = "Launcher background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (animateLauncherBackground) {
                            scaleX = launcherBackgroundMotion.scale
                            scaleY = launcherBackgroundMotion.scale
                            translationX = size.width * launcherBackgroundMotion.panXFraction
                            translationY = size.height * launcherBackgroundMotion.panYFraction
                        }
                    }
                    .then(
                        if (launcherBackgroundBlurEnabled) {
                            Modifier.blur(LauncherBackgroundBlurRadius)
                        } else {
                            Modifier
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LauncherBackgroundScrimColor)
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
            HeaderRow(
                gridColumns = gridColumns,
                genreFilter = effectiveGenreFilter,
                activePlatformFilter = activePlatformFilter,
                collections = collectionOptions,
                platformFilters = availablePlatformFilters,
                recommendationFiltersLoading = recommendationFiltersLoading,
                recommendationFiltersProgressPercent = recommendationFiltersProgressPercent,
                hasRomFolder = hasRomFolderConfigured,
                configuredRomFolderPath = configuredRomFolderPath,
                isDefaultLauncher = isDefaultLauncher,
                editMode = editMode,
                rpcsxLaunchMode = rpcsxLaunchMode,
                launcherLayoutMode = launcherLayoutMode,
                launcherBackground = launcherBackground,
                launcherBackgroundOptions = launcherBackgroundOptions,
                launcherBackgroundAnimationEnabled = launcherBackgroundAnimationEnabled,
                launcherBackgroundBlurEnabled = launcherBackgroundBlurEnabled,
                showTop10Badges = showTop10Badges,
                launcherTileBackgroundMode = launcherTileBackgroundMode,
                hasCustomLauncherBackground = hasCustomLauncherBackground,
                onGridSelected = viewModel::setGridColumns,
                onGenreSelected = { filter -> selectFilter(filter, 0) },
                onAdd = {
                    viewModel.refreshInstalledTargets()
                    openAddDialog(0, null, null)
                },
                onConfigureRomFolder = { romFolderPicker.launch(null) },
                onToggleEditMode = { editMode = !editMode },
                onRpcsxLaunchModeSelected = { mode ->
                    rpcsxLaunchMode = mode
                    prefs.edit().putString("rpcsx_launch_mode", mode).apply()
                },
                onLauncherLayoutModeSelected = { mode ->
                    val normalizedMode = normalizeLauncherLayoutMode(mode)
                    launcherLayoutMode = normalizedMode
                    prefs.edit().putString("launcher_layout_mode", normalizedMode).apply()
                },
                onLauncherBackgroundSelected = { background ->
                    val normalizedBackground = normalizeLauncherBackground(
                        background,
                        launcherBackgroundOptions,
                        hasCustomBackground = hasCustomLauncherBackground
                    )
                    launcherBackground = normalizedBackground
                    prefs.edit().putString(LAUNCHER_BACKGROUND_PREF_KEY, normalizedBackground).apply()
                },
                onLauncherBackgroundAnimationEnabledChanged = { enabled ->
                    launcherBackgroundAnimationEnabled = enabled
                    prefs.edit().putBoolean(LAUNCHER_BACKGROUND_ANIMATION_PREF_KEY, enabled).apply()
                },
                onLauncherBackgroundBlurEnabledChanged = { enabled ->
                    launcherBackgroundBlurEnabled = enabled
                    prefs.edit().putBoolean(LAUNCHER_BACKGROUND_BLUR_PREF_KEY, enabled).apply()
                },
                onShowTop10BadgesChanged = { enabled ->
                    showTop10Badges = enabled
                    prefs.edit().putBoolean(SHOW_TOP_10_BADGES_PREF_KEY, enabled).apply()
                },
                onLauncherTileBackgroundModeSelected = { mode ->
                    val normalizedMode = normalizeLauncherTileBackgroundMode(mode)
                    launcherTileBackgroundMode = normalizedMode
                    prefs.edit().putString(LAUNCHER_TILE_BACKGROUND_PREF_KEY, normalizedMode).apply()
                },
                onPickCustomLauncherBackground = {
                    launcherBackgroundPicker.launch(arrayOf("image/*"))
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (games.isEmpty() && visibleGamesWithRecommendation.isEmpty()) {
                EmptyState(isDefaultLauncher = isDefaultLauncher, onAdd = {
                    openAddDialog(0, null, null)
                })
            } else if (visibleGamesWithRecommendation.isEmpty()) {
                EmptyGenreState(activeGenre = effectiveGenreFilter)
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isGalleryLayoutMode(launcherLayoutMode)) {
                        AnimatedContent(
                            targetState = effectiveGenreFilter,
                            transitionSpec = {
                                val duration = 320
                                val direction = galleryFilterTransitionDirection
                                if (direction > 0) {
                                    (slideInVertically(animationSpec = tween(duration)) { it / 3 } + fadeIn(animationSpec = tween(duration)))
                                        .togetherWith(slideOutVertically(animationSpec = tween(duration)) { -it / 4 } + fadeOut(animationSpec = tween(duration)))
                                } else if (direction < 0) {
                                    (slideInVertically(animationSpec = tween(duration)) { -it / 3 } + fadeIn(animationSpec = tween(duration)))
                                        .togetherWith(slideOutVertically(animationSpec = tween(duration)) { it / 4 } + fadeOut(animationSpec = tween(duration)))
                                } else {
                                    fadeIn(animationSpec = tween(220)).togetherWith(fadeOut(animationSpec = tween(220)))
                                }
                            },
                            label = "galleryFilterTransition"
                        ) { activeFilter ->
                            val filteredGames = orderedGamesForFilter(games, activeFilter)
                            val filterPlatform = GamePlatforms.filterLabel(activeFilter)
                            val filterCollection = if (
                                activeFilter != GameFilters.ALL &&
                                filterPlatform == null &&
                                collectionOptions.any { it.equals(activeFilter, ignoreCase = true) }
                            ) {
                                activeFilter
                            } else {
                                null
                            }
                            val galleryRecommendationTile: RecommendationTileUi? = null
                            val galleryRecommendationAddTile = recommendationAddTileForFilter(activeFilter, collectionOptions)
                            val galleryRecommendationId = galleryRecommendationTile?.entry?.id
                            val galleryRecommendationAddId = galleryRecommendationAddTile?.entry?.id
                            val filteredGamesWithRecommendation =
                                buildList {
                                    addAll(
                                        filteredGames.filterNot { game ->
                                            (galleryRecommendationId != null && game.id == galleryRecommendationId) ||
                                                (galleryRecommendationAddId != null && game.id == galleryRecommendationAddId)
                                        }
                                    )
                                    galleryRecommendationTile?.entry?.let(::add)
                                    galleryRecommendationAddTile?.entry?.let(::add)
                                }
                            GameGallery(
                                games = filteredGamesWithRecommendation,
                                tileSizeColumns = gridColumns,
                                editMode = editMode,
                                artworkLoading = artworkLoading,
                                artworkPending = artworkPending,
                                activeArtworkBackfillPlatforms = activeArtworkBackfillPlatforms,
                                showTop10Badges = showTop10Badges,
                                activePlatformFilter = filterPlatform,
                                activeCollectionFilter = filterCollection,
                                launcherLayoutMode = launcherLayoutMode,
                                launcherTileBackgroundMode = launcherTileBackgroundMode,
                                launcherResumeNonce = launcherResumeNonce,
                                currentFilter = activeFilter,
                                swipeFilterOptions = swipeCycleFilters,
                                recommendationTile = galleryRecommendationTile,
                                recommendationAddTile = galleryRecommendationAddTile,
                                recommendationActions = null,
                                requestedFocusGameId = requestedRecommendationAddedGameId.takeIf {
                                    activeFilter.equals(effectiveGenreFilter, ignoreCase = true)
                                },
                                onFocusRequestHandled = { handledGameId ->
                                    if (requestedRecommendationAddedGameId == handledGameId) {
                                        requestedRecommendationAddedGameId = null
                                    }
                                },
                                onGenreSelected = { nextFilter, direction -> selectFilter(nextFilter, direction) },
                                onLaunch = { game ->
                                    if (galleryRecommendationId != null && game.id == galleryRecommendationId) {
                                        galleryRecommendationTile.recommendation?.let(viewModel::playRecommendation)
                                    } else if (game.id == galleryRecommendationAddId) {
                                        viewModel.refreshInstalledTargets()
                                        val addPlatformFilter = GamePlatforms.filterLabel(activeFilter)
                                        val addCollectionFilter = activeFilter.takeIf {
                                            addPlatformFilter == null &&
                                                collectionOptions.any { option -> option.equals(activeFilter, ignoreCase = true) }
                                        }
                                        val openAndroidTab = addPlatformFilter.equals(GamePlatforms.ANDROID, ignoreCase = true)
                                        openAddDialog(
                                            if (openAndroidTab) 1 else 2,
                                            addPlatformFilter.takeUnless {
                                                it.equals(GamePlatforms.ANDROID, ignoreCase = true)
                                            },
                                            addCollectionFilter
                                        )
                                    } else if (!editMode) {
                                        if (game.allTargets().size > 1 && game.preferredTarget() == null) {
                                            launchChooserGameId = game.id
                                        } else {
                                            viewModel.launch(game)
                                        }
                                    }
                                },
                                onEdit = { game ->
                                    if (game.id != galleryRecommendationId) {
                                        editGameId = game.id
                                    }
                                }
                            )
                        }
                    } else {
                        val gridNostalgicModeEnabled = showsGridNostalgicTiles(launcherLayoutMode)
                        val gridFilterOverlayPlatform = remember(effectiveGenreFilter) {
                            val filter = GamePlatforms.filterLabel(effectiveGenreFilter) ?: return@remember null
                            val matchingOverlayDrawableIds = GamePlatforms.values
                                .filter { platform -> GamePlatforms.matchesFilter(platform, filter) }
                                .mapNotNull { platform -> NostalgicOverlays.specForPlatform(platform)?.drawableResourceId }
                                .distinct()
                            if (matchingOverlayDrawableIds.size == 1) {
                                filter.takeIf { NostalgicOverlays.hasOverlay(it) }
                            } else {
                                null
                            }
                        }
                        val gridNostalgicLayoutMetrics = rememberSameNostalgicLayoutMetrics(
                            games = visibleGames,
                            filterOverlayPlatform = gridFilterOverlayPlatform,
                            enabled = gridNostalgicModeEnabled
                        )
                        val gridState = rememberLazyGridState()
                        val gridScope = rememberCoroutineScope()
                        val gridFocusRequester = remember { FocusRequester() }
                        LaunchedEffect(launcherLayoutMode, effectiveGenreFilter, visibleGamesWithRecommendation.size) {
                            gridFocusRequester.requestFocus()
                        }
                        val scrollGridByRows: (Int) -> Boolean = { rowDelta ->
                            val itemCount = visibleGamesWithRecommendation.size
                            if (itemCount == 0) {
                                false
                            } else {
                                val firstIndex = gridState.firstVisibleItemIndex
                                val step = gridColumns.coerceAtLeast(1) * rowDelta
                                val targetIndex = (firstIndex + step).coerceIn(0, itemCount - 1)
                                val canResetOffset = rowDelta < 0 && gridState.firstVisibleItemScrollOffset > 0
                                if (targetIndex != firstIndex || canResetOffset) {
                                    gridScope.launch {
                                        gridState.animateScrollToItem(targetIndex)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(gridFocusRequester)
                                .focusable()
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionLeft -> {
                                            previousFilterForSwipe(effectiveGenreFilter, swipeCycleFilters)?.let { previous ->
                                                if (!previous.equals(effectiveGenreFilter, ignoreCase = true)) {
                                                    selectFilter(previous, -1)
                                                    return@onPreviewKeyEvent true
                                                }
                                            }
                                            false
                                        }
                                        Key.DirectionRight -> {
                                            nextFilterForSwipe(effectiveGenreFilter, swipeCycleFilters)?.let { next ->
                                                if (!next.equals(effectiveGenreFilter, ignoreCase = true)) {
                                                    selectFilter(next, 1)
                                                    return@onPreviewKeyEvent true
                                                }
                                            }
                                            false
                                        }
                                        Key.DirectionUp -> scrollGridByRows(-1)
                                        Key.DirectionDown -> scrollGridByRows(1)
                                        else -> false
                                    }
                                }
                        ) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(gridColumns),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    visibleGamesWithRecommendation,
                                    key = { game ->
                                        recommendationListItemKey(
                                            game = game,
                                            recommendationId = recommendationId,
                                            recommendationAddId = recommendationAddId,
                                            filter = effectiveGenreFilter
                                        )
                                    }
                                ) { game ->
                                    val isDragging = draggingId == game.id
                                    val isRecommendation = game.id == recommendationId
                                    val isRecommendationAdd = game.id == recommendationAddId
                                    val isRecommendationLoading = isRecommendation && activeRecommendationTile?.isLoading == true
                                    if (isRecommendationAdd) {
                                            RecommendationAddCard(
                                                game = game,
                                                columns = gridColumns,
                                                tileBackgroundMode = launcherTileBackgroundMode,
                                                onAdd = {
                                                    viewModel.refreshInstalledTargets()
                                                    val addPlatformFilter = GamePlatforms.filterLabel(effectiveGenreFilter)
                                                val addCollectionFilter = effectiveGenreFilter.takeIf {
                                                    addPlatformFilter == null &&
                                                        collectionOptions.any { option -> option.equals(effectiveGenreFilter, ignoreCase = true) }
                                                }
                                                val openAndroidTab = addPlatformFilter.equals(GamePlatforms.ANDROID, ignoreCase = true)
                                                openAddDialog(
                                                    if (openAndroidTab) 1 else 2,
                                                    addPlatformFilter.takeUnless {
                                                        it.equals(GamePlatforms.ANDROID, ignoreCase = true)
                                                    },
                                                    addCollectionFilter
                                                )
                                            },
                                            modifier = Modifier
                                                .onGloballyPositioned {
                                                    itemBounds.remove(game.id)
                                                }
                                        )
                                    } else {
                                        val gameCardModifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                if (isRecommendation) {
                                                    itemBounds.remove(game.id)
                                                } else {
                                                    itemBounds[game.id] = coordinates.boundsInRoot()
                                                }
                                            }
                                            .graphicsLayer {
                                                if (isDragging) {
                                                    translationX = dragOffset.x
                                                    translationY = dragOffset.y
                                                    alpha = 0.9f
                                                }
                                            }
                                            .zIndex(if (isDragging) 1f else 0f)
                                            .then(
                                                if (editMode && !isRecommendation) {
                                                    Modifier.pointerInput(game.id, games, itemBounds) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = {
                                                                draggingId = game.id
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDragCancel = {
                                                                draggingId = null
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDragEnd = {
                                                                draggingId = null
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDrag = { change, amount ->
                                                                if (draggingId != game.id) return@detectDragGesturesAfterLongPress
                                                                change.consume()
                                                                dragOffset += amount
                                                                val source = itemBounds[game.id] ?: return@detectDragGesturesAfterLongPress
                                                                val draggedCenter = Offset(
                                                                    x = source.center.x + dragOffset.x,
                                                                    y = source.center.y + dragOffset.y
                                                                )
                                                                val targetId = itemBounds.entries.firstOrNull { (id, bounds) ->
                                                                    id != game.id && bounds.contains(draggedCenter)
                                                                }?.key
                                                                if (targetId != null) {
                                                                    viewModel.moveGame(game.id, targetId)
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                        if (gridNostalgicModeEnabled) {
                                            GridNostalgicCard(
                                                game = game,
                                                columns = gridColumns,
                                                editMode = editMode,
                                                tileBackgroundMode = launcherTileBackgroundMode,
                                                normalizeCanvasSize = true,
                                                normalizedLayoutMetrics = gridNostalgicLayoutMetrics,
                                                isArtworkLoading = game.id in artworkLoading,
                                                isArtworkPending = game.hasPendingArtworkBadge(
                                                    artworkLoading = artworkLoading,
                                                    artworkPending = artworkPending,
                                                    activeArtworkBackfillPlatforms = activeArtworkBackfillPlatforms
                                                ),
                                                topTenRank = if (showTop10Badges) PlatformTopTen.rankFor(game) else null,
                                                isRecommendation = isRecommendation,
                                                isRecommendationEnabled = activeRecommendationTile?.isEnabled != false,
                                                isRecommendationLoading = isRecommendationLoading,
                                                recommendationBadgeLabel = activeRecommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                                recommendationReason = (
                                                    activeRecommendationTile?.recommendation?.reason
                                                        ?: activeRecommendationTile?.entry?.subtitle
                                                    )?.takeIf { isRecommendation },
                                                previewImage = recommendationPreview.takeIf { isRecommendation },
                                                recommendationActions = recommendationActions.takeIf { isRecommendation },
                                                onLaunch = {
                                                    if (isRecommendation) {
                                                        activeRecommendationTile?.recommendation?.let(viewModel::playRecommendation)
                                                    } else if (draggingId == null && !editMode) {
                                                        if (game.allTargets().size > 1 && game.preferredTarget() == null) {
                                                            launchChooserGameId = game.id
                                                        } else {
                                                            viewModel.launch(game)
                                                        }
                                                    }
                                                },
                                                onEdit = {
                                                    if (!isRecommendation) {
                                                        editGameId = game.id
                                                    }
                                                },
                                                modifier = gameCardModifier
                                            )
                                        } else {
                                            GameCard(
                                                game = game,
                                                columns = gridColumns,
                                                editMode = editMode,
                                                tileBackgroundMode = launcherTileBackgroundMode,
                                                isArtworkLoading = game.id in artworkLoading,
                                                isArtworkPending = game.hasPendingArtworkBadge(
                                                    artworkLoading = artworkLoading,
                                                    artworkPending = artworkPending,
                                                    activeArtworkBackfillPlatforms = activeArtworkBackfillPlatforms
                                                ),
                                                topTenRank = if (showTop10Badges) PlatformTopTen.rankFor(game) else null,
                                                isRecommendation = isRecommendation,
                                                isRecommendationEnabled = activeRecommendationTile?.isEnabled != false,
                                                isRecommendationLoading = isRecommendationLoading,
                                                recommendationBadgeLabel = activeRecommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                                recommendationReason = (
                                                    activeRecommendationTile?.recommendation?.reason
                                                        ?: activeRecommendationTile?.entry?.subtitle
                                                    )?.takeIf { isRecommendation },
                                                previewImage = recommendationPreview.takeIf { isRecommendation },
                                                recommendationActions = recommendationActions.takeIf { isRecommendation },
                                                onLaunch = {
                                                    if (isRecommendation) {
                                                        activeRecommendationTile?.recommendation?.let(viewModel::playRecommendation)
                                                    } else if (draggingId == null && !editMode) {
                                                        if (game.allTargets().size > 1 && game.preferredTarget() == null) {
                                                            launchChooserGameId = game.id
                                                        } else {
                                                            viewModel.launch(game)
                                                        }
                                                    }
                                                },
                                                onEdit = {
                                                    if (!isRecommendation) {
                                                        editGameId = game.id
                                                    }
                                                },
                                                modifier = gameCardModifier
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (recommendationFiltersLoading && hasRomFolderConfigured) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF111927),
                    border = BorderStroke(1.dp, Color(0xFF475467))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF98A2B3)
                        )
                        Text(
                            text = "Loading filters $recommendationFiltersProgressPercent%",
                            color = Color(0xFF98A2B3),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
    }

    if (addDialogOpen) {
        key(addDialogSessionId) {
            AddGameDialog(
                games = games,
                shortcuts = shortcuts,
                activities = activities,
                templates = viewModel.templates(),
                isDefaultLauncher = isDefaultLauncher,
                authorizedRomTreeUri = authorizedRomTreeUri,
                onDismiss = { addDialogOpen = false },
                initialTab = addDialogInitialTab,
                initialPlatformFilter = addDialogPlatformFilter,
                initialCollection = addDialogInitialCollection,
                onAddShortcut = {
                    viewModel.addShortcut(it)
                    addDialogOpen = false
                },
                onAddActivity = {
                    viewModel.addActivity(it)
                    addDialogOpen = false
                    addDialogInitialTab = 0
                    addDialogPlatformFilter = null
                    addDialogInitialCollection = null
                    selectFilter(GamePlatforms.ANDROID, 0)
                },
                onAddCustom = {
                    val targetFilter = addDialogInitialCollection ?: GamePlatforms.filterLabel(it.platform) ?: addDialogPlatformFilter
                    viewModel.addCustom(it)
                    addDialogOpen = false
                    addDialogInitialTab = 0
                    addDialogPlatformFilter = null
                    addDialogInitialCollection = null
                    targetFilter?.let { filter ->
                        selectFilter(filter, 0)
                    }
                },
                onAddCustomBatch = { entries ->
                    val targetFilter = addDialogInitialCollection
                        ?: GamePlatforms.filterLabel(entries.firstOrNull()?.platform)
                        ?: addDialogPlatformFilter
                    viewModel.addCustomEntries(entries)
                    addDialogOpen = false
                    addDialogInitialTab = 0
                    addDialogPlatformFilter = null
                    addDialogInitialCollection = null
                    targetFilter?.let { filter ->
                        selectFilter(filter, 0)
                    }
                },
                onFillMissingArtworkForPlatform = { platform ->
                    viewModel.fillMissingArtworkForPlatform(platform)
                },
                onVerifyScummVm = { gameFolderUri, gameId ->
                    viewModel.verifyScummVmConfig(gameFolderUri, gameId)
                },
                collectionSuggestions = collectionOptions
            )
        }
    }

    val editGame = games.firstOrNull { it.id == editGameId }
    editGame?.let { selected ->
        EditGameDialog(
            game = selected,
            onDismiss = { editGameId = null },
            onSave = {
                viewModel.updateGame(it)
                editGameId = null
            },
            onDelete = {
                viewModel.removeGame(selected.id)
                editGameId = null
            },
            onPickImage = {
                pendingImageTarget = selected.id
                imagePicker.launch(arrayOf("image/*"))
            },
            onRemoveImage = {
                viewModel.updateGameImage(selected.id, null)
            },
            onImportImageUrl = { url ->
                viewModel.importImageFromUrl(selected.id, url)
            },
            onSearchWebImage = { title ->
                val queryText = webArtworkSearchQuery(selected, title)
                val query = Uri.encode(queryText)
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?tbm=isch&q=$query")
                )
                runCatching { context.startActivity(intent) }
            },
            suggestions = artworkSuggestions[selected.id].orEmpty(),
            suggestionsLoading = selected.id in artworkLoading,
            onLoadSuggestions = { title ->
                viewModel.loadArtworkSuggestions(selected.id, title)
            },
            onUseTopSuggestion = { title ->
                viewModel.importTopArtworkSuggestion(selected.id, title)
            },
            onSelectPreferredTarget = { selectPreferredGameId = selected.id },
            onRemoveTarget = { target ->
                viewModel.removeTarget(selected.id, target)
            },
            collectionSuggestions = collectionOptions
        )
    }

    val launchChooserGame = games.firstOrNull { it.id == launchChooserGameId }
    launchChooserGame?.let { selected ->
        LaunchTargetDialog(
            game = selected,
            onDismiss = { launchChooserGameId = null },
            onLaunch = { target ->
                viewModel.setPreferredTarget(selected.id, target)
                viewModel.launchTarget(selected, target)
                launchChooserGameId = null
            }
        )
    }

    val selectPreferredGame = games.firstOrNull { it.id == selectPreferredGameId }
    selectPreferredGame?.let { selected ->
        LaunchTargetDialog(
            game = selected,
            onDismiss = { selectPreferredGameId = null },
            onLaunch = { target ->
                viewModel.setPreferredTarget(selected.id, target)
                selectPreferredGameId = null
            }
        )
    }
}

@Composable
private fun HeaderRow(
    gridColumns: Int,
    genreFilter: String,
    activePlatformFilter: String?,
    collections: List<String>,
    platformFilters: List<String>,
    recommendationFiltersLoading: Boolean,
    recommendationFiltersProgressPercent: Int,
    hasRomFolder: Boolean,
    configuredRomFolderPath: String?,
    isDefaultLauncher: Boolean,
    editMode: Boolean,
    rpcsxLaunchMode: String,
    launcherLayoutMode: String,
    launcherBackground: String,
    launcherBackgroundOptions: List<String>,
    launcherBackgroundAnimationEnabled: Boolean,
    launcherBackgroundBlurEnabled: Boolean,
    showTop10Badges: Boolean,
    launcherTileBackgroundMode: String,
    hasCustomLauncherBackground: Boolean,
    onGridSelected: (Int) -> Unit,
    onGenreSelected: (String) -> Unit,
    onAdd: () -> Unit,
    onConfigureRomFolder: () -> Unit,
    onToggleEditMode: () -> Unit,
    onRpcsxLaunchModeSelected: (String) -> Unit,
    onLauncherLayoutModeSelected: (String) -> Unit,
    onLauncherBackgroundSelected: (String) -> Unit,
    onLauncherBackgroundAnimationEnabledChanged: (Boolean) -> Unit,
    onLauncherBackgroundBlurEnabledChanged: (Boolean) -> Unit,
    onShowTop10BadgesChanged: (Boolean) -> Unit,
    onLauncherTileBackgroundModeSelected: (String) -> Unit,
    onPickCustomLauncherBackground: () -> Unit
) {
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    val headerButtonModifier = Modifier.focusProperties { canFocus = false }
    val activePlatformIcon = rememberPlatformBadgeBitmap(
        showBadge = !activePlatformFilter.isNullOrBlank(),
        platform = activePlatformFilter
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Retro Launcher",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFF5F7FA),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            activePlatformIcon?.let { icon ->
                Image(
                    painter = BitmapPainter(icon),
                    contentDescription = activePlatformFilter,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasRomFolder) {
                OutlinedButton(
                    onClick = onConfigureRomFolder,
                    modifier = headerButtonModifier
                ) {
                    Text("ROM folder")
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF111927),
                border = BorderStroke(1.dp, Color(0xFF475467))
            ) {
                IconButton(
                    onClick = onAdd,
                    modifier = headerButtonModifier
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add game",
                        tint = Color(0xFF98A2B3)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF111927),
                border = BorderStroke(1.dp, Color(0xFF475467))
            ) {
                IconButton(
                    onClick = { filtersOpen = true },
                    modifier = headerButtonModifier
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Filters",
                        tint = Color(0xFF98A2B3)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF111927),
                border = BorderStroke(1.dp, Color(0xFF475467))
            ) {
                IconButton(
                    onClick = { optionsOpen = true },
                    modifier = headerButtonModifier
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Options",
                        tint = Color(0xFF98A2B3)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF111927),
                border = BorderStroke(1.dp, Color(0xFF475467))
            ) {
                IconButton(
                    onClick = onToggleEditMode,
                    modifier = headerButtonModifier
                ) {
                    Icon(
                        imageVector = if (editMode) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = if (editMode) "Unlocked" else "Locked",
                        tint = if (editMode) Color(0xFFF2AA4C) else Color(0xFF98A2B3)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF111927),
                border = BorderStroke(1.dp, Color(0xFF475467))
            ) {
                IconButton(
                    onClick = { infoOpen = true },
                    modifier = headerButtonModifier
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF98A2B3)
                    )
                }
            }
        }
    }

    if (filtersOpen) {
        FiltersDialog(
            genreFilter = genreFilter,
            collections = collections,
            platformFilters = platformFilters,
            recommendationFiltersLoading = recommendationFiltersLoading,
            recommendationFiltersProgressPercent = recommendationFiltersProgressPercent,
            onDismiss = { filtersOpen = false },
            onGenreSelected = onGenreSelected
        )
    }

    if (optionsOpen) {
        OptionsDialog(
            gridColumns = gridColumns,
            hasRomFolder = hasRomFolder,
            configuredRomFolderPath = configuredRomFolderPath,
            isDefaultLauncher = isDefaultLauncher,
            editMode = editMode,
            rpcsxLaunchMode = rpcsxLaunchMode,
            launcherLayoutMode = launcherLayoutMode,
            launcherBackground = launcherBackground,
            launcherBackgroundOptions = launcherBackgroundOptions,
            launcherBackgroundAnimationEnabled = launcherBackgroundAnimationEnabled,
            launcherBackgroundBlurEnabled = launcherBackgroundBlurEnabled,
            showTop10Badges = showTop10Badges,
            launcherTileBackgroundMode = launcherTileBackgroundMode,
            hasCustomLauncherBackground = hasCustomLauncherBackground,
            onDismiss = { optionsOpen = false },
            onGridSelected = onGridSelected,
            onToggleEditMode = onToggleEditMode,
            onConfigureRomFolder = onConfigureRomFolder,
            onRpcsxLaunchModeSelected = onRpcsxLaunchModeSelected,
            onLauncherLayoutModeSelected = onLauncherLayoutModeSelected,
            onLauncherBackgroundSelected = onLauncherBackgroundSelected,
            onLauncherBackgroundAnimationEnabledChanged = onLauncherBackgroundAnimationEnabledChanged,
            onLauncherBackgroundBlurEnabledChanged = onLauncherBackgroundBlurEnabledChanged,
            onShowTop10BadgesChanged = onShowTop10BadgesChanged,
            onLauncherTileBackgroundModeSelected = onLauncherTileBackgroundModeSelected,
            onPickCustomLauncherBackground = onPickCustomLauncherBackground
        )
    }
    if (infoOpen) {
        InfoDialog(onDismiss = { infoOpen = false })
    }
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Retro Launcher\nFree version",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1B2433)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Retro Launcher icon",
                        modifier = Modifier.size(52.dp)
                    )
                }
                Text(
                    "Created by Yakir Ari (yakirari@gmail.com)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8C2D1)
                )
                Text(
                    "Clean, Simple, Fun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF98A2B3)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsDialog(
    gridColumns: Int,
    hasRomFolder: Boolean,
    configuredRomFolderPath: String?,
    isDefaultLauncher: Boolean,
    editMode: Boolean,
    rpcsxLaunchMode: String,
    launcherLayoutMode: String,
    launcherBackground: String,
    launcherBackgroundOptions: List<String>,
    launcherBackgroundAnimationEnabled: Boolean,
    launcherBackgroundBlurEnabled: Boolean,
    showTop10Badges: Boolean,
    launcherTileBackgroundMode: String,
    hasCustomLauncherBackground: Boolean,
    onDismiss: () -> Unit,
    onGridSelected: (Int) -> Unit,
    onToggleEditMode: () -> Unit,
    onConfigureRomFolder: () -> Unit,
    onRpcsxLaunchModeSelected: (String) -> Unit,
    onLauncherLayoutModeSelected: (String) -> Unit,
    onLauncherBackgroundSelected: (String) -> Unit,
    onLauncherBackgroundAnimationEnabledChanged: (Boolean) -> Unit,
    onLauncherBackgroundBlurEnabledChanged: (Boolean) -> Unit,
    onShowTop10BadgesChanged: (Boolean) -> Unit,
    onLauncherTileBackgroundModeSelected: (String) -> Unit,
    onPickCustomLauncherBackground: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Options", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    if (hasRomFolder) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = onConfigureRomFolder) {
                                Text("ROM folder")
                            }
                            Text(
                                text = "Current: ${configuredRomFolderPath ?: "Unknown"}",
                                color = Color(0xFF98A2B3),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text("Layout", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_GRID,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_GRID) },
                            label = { Text("Grid") }
                        )
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_GRIDSTALGIC,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_GRIDSTALGIC) },
                            label = { Text("Gridstalgic") }
                        )
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_GALLERY,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_GALLERY) },
                            label = { Text("Gallery") }
                        )
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_NOSTALGIC,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_NOSTALGIC) },
                            label = { Text("Nostalgic") }
                        )
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_SHELFSTALGIC,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_SHELFSTALGIC) },
                            label = { Text("Shelfstalgic") }
                        )
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_SAMESTALGIC,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_SAMESTALGIC) },
                            label = { Text("Samestalgic") }
                        )
                        FilterChip(
                            selected = launcherLayoutMode == LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC,
                            onClick = { onLauncherLayoutModeSelected(LAUNCHER_LAYOUT_MODE_GALLERYSTALGIC) },
                            label = { Text("Gallerystalgic") }
                        )
                    }
                    Text("Basement", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = launcherBackground == LAUNCHER_BACKGROUND_NONE,
                            onClick = { onLauncherBackgroundSelected(LAUNCHER_BACKGROUND_NONE) },
                            label = { Text("None") }
                        )
                        launcherBackgroundOptions.forEach { backgroundAsset ->
                            FilterChip(
                                selected = launcherBackground.equals(backgroundAsset, ignoreCase = true),
                                onClick = { onLauncherBackgroundSelected(backgroundAsset) },
                                label = { Text(launcherBackgroundLabel(backgroundAsset)) }
                            )
                        }
                        FilterChip(
                            selected = launcherBackground == LAUNCHER_BACKGROUND_CUSTOM,
                            onClick = {
                                if (hasCustomLauncherBackground) {
                                    onLauncherBackgroundSelected(LAUNCHER_BACKGROUND_CUSTOM)
                                } else {
                                    onPickCustomLauncherBackground()
                                }
                            },
                            label = { Text("Custom") }
                        )
                    }
                    OutlinedButton(onClick = onPickCustomLauncherBackground) {
                        Text(if (hasCustomLauncherBackground) "Change custom background" else "Choose custom background")
                    }
                    if (hasCustomLauncherBackground) {
                        Text(
                            text = if (launcherBackground == LAUNCHER_BACKGROUND_CUSTOM) {
                                "Using your custom background"
                            } else {
                                "Custom background ready"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF98A2B3)
                        )
                    }
                    Text("Basement animation", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !launcherBackgroundAnimationEnabled,
                            onClick = { onLauncherBackgroundAnimationEnabledChanged(false) },
                            label = { Text("False") }
                        )
                        FilterChip(
                            selected = launcherBackgroundAnimationEnabled,
                            onClick = { onLauncherBackgroundAnimationEnabledChanged(true) },
                            label = { Text("True") }
                        )
                    }
                    Text("Basement blur", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !launcherBackgroundBlurEnabled,
                            onClick = { onLauncherBackgroundBlurEnabledChanged(false) },
                            label = { Text("False") }
                        )
                        FilterChip(
                            selected = launcherBackgroundBlurEnabled,
                            onClick = { onLauncherBackgroundBlurEnabledChanged(true) },
                            label = { Text("True") }
                        )
                    }
                    Text("Show top 10", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showTop10Badges,
                            onClick = { onShowTop10BadgesChanged(false) },
                            label = { Text("False") }
                        )
                        FilterChip(
                            selected = showTop10Badges,
                            onClick = { onShowTop10BadgesChanged(true) },
                            label = { Text("True") }
                        )
                    }
                    Text("Tile background", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = launcherTileBackgroundMode == LAUNCHER_TILE_BACKGROUND_MODE_NORMAL,
                            onClick = { onLauncherTileBackgroundModeSelected(LAUNCHER_TILE_BACKGROUND_MODE_NORMAL) },
                            label = { Text("Normal") }
                        )
                        FilterChip(
                            selected = launcherTileBackgroundMode == LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT,
                            onClick = { onLauncherTileBackgroundModeSelected(LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT) },
                            label = { Text("Transparent") }
                        )
                        FilterChip(
                            selected = launcherTileBackgroundMode == LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY,
                            onClick = { onLauncherTileBackgroundModeSelected(LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY) },
                            label = { Text("Glossy") }
                        )
                    }
                    Text(
                        text = if (isDefaultLauncher) "Launcher status: Active" else "Launcher status: Not default",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF98A2B3)
                    )

                    Text("Tile size", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 8).forEach { columns ->
                            FilterChip(
                                selected = gridColumns == columns,
                                onClick = { onGridSelected(columns) },
                                label = { Text("$columns") }
                            )
                        }
                    }

                    Text("Tile lock", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    FilterChip(
                        selected = editMode,
                        onClick = onToggleEditMode,
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (editMode) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                    contentDescription = null
                                )
                                Text(if (editMode) "Unlocked" else "Locked")
                            }
                        }
                    )

                    Text("RPCSX mode", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = rpcsxLaunchMode == "external",
                            onClick = { onRpcsxLaunchModeSelected("external") },
                            label = { Text("External UI") }
                        )
                        FilterChip(
                            selected = rpcsxLaunchMode == "embedded",
                            onClick = { onRpcsxLaunchModeSelected("embedded") },
                            label = { Text("Embedded") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersDialog(
    genreFilter: String,
    collections: List<String>,
    platformFilters: List<String>,
    recommendationFiltersLoading: Boolean,
    recommendationFiltersProgressPercent: Int,
    onDismiss: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                if (recommendationFiltersLoading) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF162033),
                        border = BorderStroke(1.dp, Color(0xFF344054))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF98A2B3)
                            )
                            Text(
                                text = "Scanning platform folders for recommendations $recommendationFiltersProgressPercent%",
                                color = Color(0xFF98A2B3),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        orderedFilterOptions(
                            collections = collections,
                            platformFilters = platformFilters
                        ).forEach { option ->
                            val isPlatform = GamePlatforms.filterLabel(option) != null
                            FilterChip(
                                selected = genreFilter == option,
                                onClick = { onGenreSelected(option) },
                                label = { Text(option) },
                                colors = if (isPlatform) {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2A3A52),
                                        selectedLabelColor = Color.White
                                    )
                                } else {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2C4A3E),
                                        selectedLabelColor = Color.White
                                    )
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isDefaultLauncher: Boolean, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "No game tiles yet",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isDefaultLauncher) {
                "Import a launcher shortcut, add an Android activity, or define a custom emulator intent."
            } else {
                "You can add games now, but shortcut import works only after this app becomes the default Home launcher."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF98A2B3),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Text("Create first tile")
        }
    }
}

@Composable
private fun EmptyGenreState(activeGenre: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "No games in $activeGenre",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose another collection/platform filter to see more tiles.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF98A2B3),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameGallery(
    games: List<GameEntry>,
    tileSizeColumns: Int,
    editMode: Boolean,
    artworkLoading: Set<String>,
    artworkPending: Set<String>,
    activeArtworkBackfillPlatforms: Set<String>,
    showTop10Badges: Boolean,
    activePlatformFilter: String?,
    activeCollectionFilter: String?,
    launcherLayoutMode: String,
    launcherTileBackgroundMode: String,
    launcherResumeNonce: Int,
    currentFilter: String,
    swipeFilterOptions: List<String>,
    recommendationTile: RecommendationTileUi?,
    recommendationAddTile: RecommendationAddTileUi?,
    recommendationActions: RecommendationTileActions?,
    requestedFocusGameId: String?,
    onFocusRequestHandled: (String) -> Unit,
    onGenreSelected: (String, Int) -> Unit,
    onLaunch: (GameEntry) -> Unit,
    onEdit: (GameEntry) -> Unit
) {
    val context = LocalContext.current
    val nostalgicModeEnabled = isNostalgicLayoutMode(launcherLayoutMode)
    val shelfNostalgicModeEnabled = isShelfNostalgicLayoutMode(launcherLayoutMode)
    val sameNostalgicCanvasEnabled = usesSameNostalgicCanvas(launcherLayoutMode)
    val nostalgicGalleryInfoEnabled = showsNostalgicGalleryInfo(launcherLayoutMode)
    val nostalgicGlassEffectEnabled = sameNostalgicCanvasEnabled
    val galleryScope = rememberCoroutineScope()
    val galleryFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val focusedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item -> abs((item.offset + item.size / 2) - center) }
                ?.index
                ?: listState.firstVisibleItemIndex
        }
    }
    val recommendationId = recommendationTile?.entry?.id
    val recommendationAddId = recommendationAddTile?.entry?.id
    val recommendationPreview = rememberRecommendationPreviewBitmap(recommendationTile?.recommendation?.previewAssetPath)
    val selectedRecommendationActionKey: RecommendationActionKey? = null
    LaunchedEffect(currentFilter, games.size, launcherResumeNonce) {
        galleryFocusRequester.requestFocus()
    }
    LaunchedEffect(currentFilter, requestedFocusGameId, games) {
        val targetGameId = requestedFocusGameId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val targetIndex = games.indexOfFirst { game -> game.id == targetGameId }
        if (targetIndex == -1) return@LaunchedEffect
        if (shelfNostalgicModeEnabled) {
            listState.scrollToItem(targetIndex)
        } else {
            listState.animateScrollToItem(targetIndex)
        }
        galleryFocusRequester.requestFocus()
        onFocusRequestHandled(targetGameId)
    }
    val moveToPreviousGame = {
        val targetIndex = (focusedIndex - 1).coerceAtLeast(0)
        if (targetIndex != focusedIndex) {
            galleryScope.launch {
                if (shelfNostalgicModeEnabled) {
                    listState.scrollToItem(targetIndex)
                } else {
                    listState.animateScrollToItem(targetIndex)
                }
            }
            true
        } else {
            false
        }
    }
    val moveToNextGame = {
        val targetIndex = (focusedIndex + 1).coerceAtMost(games.lastIndex)
        if (targetIndex != focusedIndex) {
            galleryScope.launch {
                if (shelfNostalgicModeEnabled) {
                    listState.scrollToItem(targetIndex)
                } else {
                    listState.animateScrollToItem(targetIndex)
                }
            }
            true
        } else {
            false
        }
    }
    val switchToPreviousFilter = {
        previousFilterForSwipe(currentFilter, swipeFilterOptions)?.let { previous ->
            if (!previous.equals(currentFilter, ignoreCase = true)) {
                onGenreSelected(previous, -1)
                true
            } else {
                false
            }
        } ?: false
    }
    val switchToNextFilter = {
        nextFilterForSwipe(currentFilter, swipeFilterOptions)?.let { next ->
            if (!next.equals(currentFilter, ignoreCase = true)) {
                onGenreSelected(next, 1)
                true
            } else {
                false
            }
        } ?: false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(galleryFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (isRecommendationActivateKey(event.key)) {
                    val focusedGame = games.getOrNull(focusedIndex) ?: return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyUp) {
                        onLaunch(focusedGame)
                    }
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> moveToPreviousGame()
                    Key.DirectionRight -> moveToNextGame()
                    Key.DirectionDown -> switchToNextFilter()
                    Key.DirectionUp -> switchToPreviousFilter()
                    else -> false
                }
            }
            .pointerInput(currentFilter, swipeFilterOptions, editMode) {
                if (editMode || swipeFilterOptions.size < 2) return@pointerInput
                var totalDragY = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                    },
                    onDragEnd = {
                        if (totalDragY > 80f) {
                            nextFilterForSwipe(currentFilter, swipeFilterOptions)?.let { next ->
                                if (!next.equals(currentFilter, ignoreCase = true)) {
                                    onGenreSelected(next, 1)
                                }
                            }
                        } else if (totalDragY < -80f) {
                            previousFilterForSwipe(currentFilter, swipeFilterOptions)?.let { previous ->
                                if (!previous.equals(currentFilter, ignoreCase = true)) {
                                    onGenreSelected(previous, -1)
                                }
                            }
                        }
                        totalDragY = 0f
                    },
                    onDragCancel = { totalDragY = 0f }
                )
            }
    ) {
        val activePlatformIcon = rememberPlatformBadgeBitmap(
            showBadge = !activePlatformFilter.isNullOrBlank(),
            platform = activePlatformFilter
        )
        val filterOverlayPlatform = remember(activePlatformFilter) {
            val filter = activePlatformFilter ?: return@remember null
            val matchingOverlayDrawableIds = GamePlatforms.values
                .filter { platform -> GamePlatforms.matchesFilter(platform, filter) }
                .mapNotNull { platform -> NostalgicOverlays.specForPlatform(platform)?.drawableResourceId }
                .distinct()
            if (matchingOverlayDrawableIds.size == 1) {
                filter.takeIf { NostalgicOverlays.hasOverlay(it) }
            } else {
                null
            }
        }
        val mixedNostalgicMode = nostalgicModeEnabled &&
            filterOverlayPlatform == null &&
            remember(games) { games.any { NostalgicOverlays.hasOverlay(it.platform) } }
        val nostalgicMode = nostalgicModeEnabled && (filterOverlayPlatform != null || mixedNostalgicMode)
        val prefetchArtworkPlan = remember(games, focusedIndex, nostalgicMode, filterOverlayPlatform) {
            if (games.isEmpty()) {
                emptyList()
            } else {
                val start = (focusedIndex - GALLERY_ARTWORK_PREFETCH_BEHIND).coerceAtLeast(0)
                val end = (focusedIndex + GALLERY_ARTWORK_PREFETCH_AHEAD + 1).coerceAtMost(games.size)
                games.subList(start, end).mapNotNull { game ->
                    val artworkUri = game.imageUri?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    val overlayPlatform = filterOverlayPlatform ?: game.platform
                    val overlaySpec = if (nostalgicMode) {
                        NostalgicOverlays.specForPlatform(overlayPlatform)
                    } else {
                        null
                    }
                    PrefetchArtworkRequest(
                        uriString = artworkUri,
                        applyVintageArtwork = overlaySpec?.applyVintageArtwork == true,
                        preTrimLeftFraction = overlaySpec?.preTrimLeftFraction ?: 0f,
                        preTrimTopFraction = overlaySpec?.preTrimTopFraction ?: 0f,
                        preTrimWidthFraction = overlaySpec?.preTrimWidthFraction ?: 1f,
                        preTrimHeightFraction = overlaySpec?.preTrimHeightFraction ?: 1f
                    )
                }.distinct()
            }
        }
        LaunchedEffect(prefetchArtworkPlan) {
            if (prefetchArtworkPlan.isEmpty()) return@LaunchedEffect
            withContext(Dispatchers.IO) {
                prefetchArtworkPlan.forEach { request ->
                    ensureArtworkBitmapCached(
                        contentResolver = context.contentResolver,
                        uriString = request.uriString,
                        applyVintageArtwork = request.applyVintageArtwork,
                        preTrimLeftFraction = request.preTrimLeftFraction,
                        preTrimTopFraction = request.preTrimTopFraction,
                        preTrimWidthFraction = request.preTrimWidthFraction,
                        preTrimHeightFraction = request.preTrimHeightFraction
                    )
                }
            }
        }
        val activeCollectionIcon = rememberCollectionBadgeBitmap(
            showBadge = !activeCollectionFilter.isNullOrBlank()
        )
        val sameNostalgicLayoutMetrics = rememberSameNostalgicLayoutMetrics(
            games = games,
            filterOverlayPlatform = filterOverlayPlatform,
            enabled = sameNostalgicCanvasEnabled && nostalgicMode
        )
        val cardWidth = maxWidth * galleryCardWidthFraction(tileSizeColumns)
        val nostalgicItemWidthFraction = if (sameNostalgicCanvasEnabled) {
            sameNostalgicGalleryWidthFraction(
                tileSizeColumns = tileSizeColumns,
                targetAspectRatio = sameNostalgicLayoutMetrics.targetAspectRatio,
                occupiedWidthFraction = sameNostalgicLayoutMetrics.occupiedWidthFraction
            )
        } else {
            nostalgicGalleryWidthFraction(tileSizeColumns)
        }
        val nostalgicItemWidth = maxWidth * nostalgicItemWidthFraction
        val mixedNostalgicItemWidth = nostalgicItemWidth
            .coerceAtLeast(320.dp)
            .coerceAtMost(maxWidth - 16.dp)
        val mixedNostalgicFallbackWidth = (cardWidth * 0.84f)
            .coerceAtLeast(260.dp)
            .coerceAtMost(mixedNostalgicItemWidth * 0.9f)
        val sameNostalgicTargetAspectRatio = sameNostalgicLayoutMetrics.targetAspectRatio
        val sameNostalgicReferenceWidth = if (mixedNostalgicMode) {
            mixedNostalgicItemWidth
        } else {
            nostalgicItemWidth
        }
        val shelfstalgicMaxItemWidth = (maxWidth - 24.dp).coerceAtLeast(180.dp)
        val shelfstalgicFocusedItemWidth = (
            maxWidth * shelfstalgicFocusWidthFraction(tileSizeColumns)
            ).coerceIn(
                minOf(220.dp, shelfstalgicMaxItemWidth),
                shelfstalgicMaxItemWidth
            )
        val shelfstalgicCompactItemWidth = (
            maxWidth * shelfstalgicCompactWidthFraction(tileSizeColumns)
            ).coerceIn(
                minOf(160.dp, shelfstalgicMaxItemWidth),
                shelfstalgicFocusedItemWidth * 0.82f
            )
        val shelfstalgicSlotWidth = (shelfstalgicCompactItemWidth * 0.74f)
            .coerceAtLeast(120.dp)
            .coerceAtMost(shelfstalgicCompactItemWidth)
        val sameNostalgicOverlayHeight = if (sameNostalgicCanvasEnabled) {
            sameNostalgicReferenceWidth / sameNostalgicTargetAspectRatio
        } else {
            null
        }
        val sameNostalgicSpacing = sameNostalgicItemSpacing(
            itemWidth = sameNostalgicReferenceWidth,
            targetAspectRatio = sameNostalgicLayoutMetrics.targetAspectRatio,
            occupiedWidthFraction = sameNostalgicLayoutMetrics.occupiedWidthFraction,
            tileSizeColumns = tileSizeColumns
        )
        val nostalgicTopPadding = when {
            shelfNostalgicModeEnabled -> 4.dp
            mixedNostalgicMode -> 18.dp
            nostalgicMode -> 8.dp
            else -> 12.dp
        }
        val sidePeek = when {
            shelfNostalgicModeEnabled -> ((maxWidth - shelfstalgicSlotWidth) / 2f).coerceAtLeast(12.dp)
            mixedNostalgicMode -> ((maxWidth - mixedNostalgicItemWidth) / 2f).coerceAtLeast(12.dp)
            nostalgicMode -> ((maxWidth - nostalgicItemWidth) / 2f).coerceAtLeast(0.dp)
            else -> (maxWidth - cardWidth) / 2f
        }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(
                when {
                    sameNostalgicCanvasEnabled -> sameNostalgicSpacing
                    shelfNostalgicModeEnabled -> shelfstalgicItemSpacing(tileSizeColumns)
                    mixedNostalgicMode -> 4.dp
                    nostalgicMode -> 0.dp
                    else -> 10.dp
                }
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = sidePeek),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = nostalgicTopPadding)
        ) {
            itemsIndexed(
                games,
                key = { _, game ->
                    recommendationListItemKey(
                        game = game,
                        recommendationId = recommendationId,
                        recommendationAddId = recommendationAddId,
                        filter = currentFilter
                    )
                }
            ) { index, game ->
                val isRecommendation = game.id == recommendationId
                val isRecommendationAdd = game.id == recommendationAddId
                val overlayPlatform = filterOverlayPlatform ?: game.platform
                val overlaySpec = if (nostalgicMode) NostalgicOverlays.specForPlatform(overlayPlatform) else null
                val mameCabinetRenderData = if (nostalgicMode) {
                    rememberMameNostalgicCabinetRenderData(
                        game = game,
                        normalizeCanvasSize = sameNostalgicCanvasEnabled,
                        glassEffectEnabled = nostalgicGlassEffectEnabled
                    )
                } else {
                    null
                }
                val isArtworkLoading = game.id in artworkLoading
                val isArtworkPending = game.hasPendingArtworkBadge(
                    artworkLoading = artworkLoading,
                    artworkPending = artworkPending,
                    activeArtworkBackfillPlatforms = activeArtworkBackfillPlatforms
                )
                val topTenRank = if (showTop10Badges) PlatformTopTen.rankFor(game) else null
                val isRecommendationLoading = isRecommendation && recommendationTile?.isLoading == true
                val distance = abs(index - focusedIndex)
                val relativeOffset = index - focusedIndex
                val shelfstalgicLayout = rememberShelfstalgicItemLayout(
                    enabled = shelfNostalgicModeEnabled,
                    relativeOffset = relativeOffset,
                    slotWidth = shelfstalgicSlotWidth,
                    focusedWidth = shelfstalgicFocusedItemWidth,
                    compactWidth = shelfstalgicCompactItemWidth
                )
                if (isRecommendationAdd) {
                    if (shelfstalgicLayout != null) {
                        ShelfstalgicSlot(layout = shelfstalgicLayout) {
                            RecommendationAddCard(
                                game = game,
                                columns = tileSizeColumns,
                                tileBackgroundMode = launcherTileBackgroundMode,
                                onAdd = { onLaunch(game) },
                                modifier = Modifier.width(shelfstalgicLayout.width)
                            )
                        }
                    } else {
                        val targetScale = when (distance) {
                            0 -> 1f
                            else -> 0.75f
                        }
                        val targetAlpha = when (distance) {
                            0 -> 1f
                            else -> 0.65f
                        }
                        val targetYOffset = when (distance) {
                            0 -> 0f
                            else -> 18f
                        }
                        val scale by animateFloatAsState(targetValue = targetScale, label = "galleryAddScale")
                        val alpha by animateFloatAsState(targetValue = targetAlpha, label = "galleryAddAlpha")
                        val translationY by animateFloatAsState(targetValue = targetYOffset, label = "galleryAddYOffset")

                        RecommendationAddCard(
                            game = game,
                            columns = tileSizeColumns,
                            tileBackgroundMode = launcherTileBackgroundMode,
                            onAdd = { onLaunch(game) },
                            modifier = Modifier
                                .width(if (mixedNostalgicMode) mixedNostalgicFallbackWidth else cardWidth)
                                .padding(vertical = 6.dp)
                                .zIndex((10 - distance).toFloat())
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                    this.translationY = translationY
                                }
                        )
                    }
                } else if (nostalgicMode && mameCabinetRenderData != null) {
                    val overlayItemWidth = when {
                        shelfstalgicLayout != null -> shelfstalgicLayout.width
                        mixedNostalgicMode -> mixedNostalgicItemWidth
                        else -> nostalgicItemWidth
                    }
                    if (shelfstalgicLayout != null) {
                        ShelfstalgicSlot(layout = shelfstalgicLayout) {
                            MameCabinetNostalgicItem(
                                game = game,
                                editMode = editMode,
                                tileBackgroundMode = launcherTileBackgroundMode,
                                cabinetRenderData = mameCabinetRenderData,
                                compactMode = shelfstalgicLayout.compactMode,
                                showShelfTitle = true,
                                normalizedOverlayHeight = sameNostalgicOverlayHeight,
                                showGalleryInfo = nostalgicGalleryInfoEnabled,
                                isArtworkLoading = isArtworkLoading,
                                isArtworkPending = isArtworkPending,
                                topTenRank = topTenRank,
                                isRecommendation = isRecommendation,
                                isRecommendationEnabled = recommendationTile?.isEnabled != false,
                                isRecommendationLoading = isRecommendationLoading,
                                recommendationBadgeLabel = recommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                recommendationReason = (
                                    recommendationTile?.recommendation?.reason
                                        ?: recommendationTile?.entry?.subtitle
                                    )?.takeIf { isRecommendation },
                                recommendationActions = recommendationActions.takeIf {
                                    isRecommendation && !isRecommendationLoading
                                },
                                selectedRecommendationActionKey = selectedRecommendationActionKey.takeIf {
                                    isRecommendation && !isRecommendationLoading && index == focusedIndex
                                },
                                onLaunch = { onLaunch(game) },
                                onEdit = { onEdit(game) },
                                modifier = Modifier.width(overlayItemWidth)
                            )
                        }
                    } else {
                        Box(
                            modifier = if (mixedNostalgicMode) {
                                Modifier
                                    .width(overlayItemWidth)
                                    .padding(top = 20.dp, bottom = 12.dp)
                            } else {
                                Modifier
                                    .fillParentMaxWidth(nostalgicItemWidthFraction)
                                    .padding(top = 8.dp, bottom = 8.dp)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            MameCabinetNostalgicItem(
                                game = game,
                                editMode = editMode,
                                tileBackgroundMode = launcherTileBackgroundMode,
                                cabinetRenderData = mameCabinetRenderData,
                                compactMode = mixedNostalgicMode,
                                showShelfTitle = false,
                                normalizedOverlayHeight = sameNostalgicOverlayHeight,
                                showGalleryInfo = nostalgicGalleryInfoEnabled,
                                isArtworkLoading = isArtworkLoading,
                                isArtworkPending = isArtworkPending,
                                topTenRank = topTenRank,
                                isRecommendation = isRecommendation,
                                isRecommendationEnabled = recommendationTile?.isEnabled != false,
                                isRecommendationLoading = isRecommendationLoading,
                                recommendationBadgeLabel = recommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                recommendationReason = (
                                    recommendationTile?.recommendation?.reason
                                        ?: recommendationTile?.entry?.subtitle
                                    )?.takeIf { isRecommendation },
                                recommendationActions = recommendationActions.takeIf {
                                    isRecommendation && !isRecommendationLoading
                                },
                                selectedRecommendationActionKey = selectedRecommendationActionKey.takeIf {
                                    isRecommendation && !isRecommendationLoading && index == focusedIndex
                                },
                                onLaunch = { onLaunch(game) },
                                onEdit = { onEdit(game) },
                                modifier = if (mixedNostalgicMode) {
                                    Modifier.width(overlayItemWidth)
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            )
                        }
                    }
                } else if (nostalgicMode && overlaySpec != null) {
                    val overlayItemWidth = when {
                        shelfstalgicLayout != null -> shelfstalgicLayout.width
                        mixedNostalgicMode -> mixedNostalgicItemWidth
                        else -> nostalgicItemWidth
                    }
                    if (shelfstalgicLayout != null) {
                        ShelfstalgicSlot(layout = shelfstalgicLayout) {
                            NostalgicGalleryItem(
                                game = game,
                                editMode = editMode,
                                overlayPlatform = overlayPlatform,
                                overlaySpec = overlaySpec,
                                tileBackgroundMode = launcherTileBackgroundMode,
                                compactMode = shelfstalgicLayout.compactMode,
                                showShelfTitle = true,
                                normalizeCanvasSize = sameNostalgicCanvasEnabled,
                                normalizedOverlayHeight = sameNostalgicOverlayHeight,
                                glassEffectEnabled = nostalgicGlassEffectEnabled,
                                showGalleryInfo = nostalgicGalleryInfoEnabled,
                                isArtworkLoading = isArtworkLoading,
                                isArtworkPending = isArtworkPending,
                                topTenRank = topTenRank,
                                isRecommendation = isRecommendation,
                                isRecommendationEnabled = recommendationTile?.isEnabled != false,
                                isRecommendationLoading = isRecommendationLoading,
                                recommendationBadgeLabel = recommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                recommendationReason = (
                                    recommendationTile?.recommendation?.reason
                                        ?: recommendationTile?.entry?.subtitle
                                    )?.takeIf { isRecommendation },
                                previewImage = recommendationPreview.takeIf { isRecommendation },
                                recommendationActions = recommendationActions.takeIf {
                                    isRecommendation && !isRecommendationLoading
                                },
                                selectedRecommendationActionKey = selectedRecommendationActionKey.takeIf {
                                    isRecommendation && !isRecommendationLoading && index == focusedIndex
                                },
                                onLaunch = { onLaunch(game) },
                                onEdit = { onEdit(game) },
                                modifier = Modifier.width(overlayItemWidth)
                            )
                        }
                    } else {
                        Box(
                            modifier = if (mixedNostalgicMode) {
                                Modifier
                                    .width(overlayItemWidth)
                                    .padding(top = 20.dp, bottom = 12.dp)
                            } else {
                                Modifier
                                    .fillParentMaxWidth(nostalgicItemWidthFraction)
                                    .padding(top = 8.dp, bottom = 8.dp)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            NostalgicGalleryItem(
                                game = game,
                                editMode = editMode,
                                overlayPlatform = overlayPlatform,
                                overlaySpec = overlaySpec,
                                tileBackgroundMode = launcherTileBackgroundMode,
                                compactMode = mixedNostalgicMode,
                                showShelfTitle = false,
                                normalizeCanvasSize = sameNostalgicCanvasEnabled,
                                normalizedOverlayHeight = sameNostalgicOverlayHeight,
                                glassEffectEnabled = nostalgicGlassEffectEnabled,
                                showGalleryInfo = nostalgicGalleryInfoEnabled,
                                isArtworkLoading = isArtworkLoading,
                                isArtworkPending = isArtworkPending,
                                topTenRank = topTenRank,
                                isRecommendation = isRecommendation,
                                isRecommendationEnabled = recommendationTile?.isEnabled != false,
                                isRecommendationLoading = isRecommendationLoading,
                                recommendationBadgeLabel = recommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                recommendationReason = (
                                    recommendationTile?.recommendation?.reason
                                        ?: recommendationTile?.entry?.subtitle
                                    )?.takeIf { isRecommendation },
                                previewImage = recommendationPreview.takeIf { isRecommendation },
                                recommendationActions = recommendationActions.takeIf {
                                    isRecommendation && !isRecommendationLoading
                                },
                                selectedRecommendationActionKey = selectedRecommendationActionKey.takeIf {
                                    isRecommendation && !isRecommendationLoading && index == focusedIndex
                                },
                                onLaunch = { onLaunch(game) },
                                onEdit = { onEdit(game) },
                                modifier = if (mixedNostalgicMode) {
                                    Modifier.width(overlayItemWidth)
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            )
                        }
                    }
                } else {
                    if (shelfstalgicLayout != null) {
                        ShelfstalgicSlot(layout = shelfstalgicLayout) {
                            GameCard(
                                game = game,
                                columns = tileSizeColumns,
                                editMode = editMode,
                                tileBackgroundMode = launcherTileBackgroundMode,
                                shelfMode = true,
                                shelfCompactMode = shelfstalgicLayout.compactMode,
                                isArtworkLoading = isArtworkLoading,
                                isArtworkPending = isArtworkPending,
                                topTenRank = topTenRank,
                                isRecommendation = isRecommendation,
                                isRecommendationEnabled = recommendationTile?.isEnabled != false,
                                isRecommendationLoading = isRecommendationLoading,
                                recommendationBadgeLabel = recommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                                recommendationReason = (
                                    recommendationTile?.recommendation?.reason
                                        ?: recommendationTile?.entry?.subtitle
                                    )?.takeIf { isRecommendation },
                                previewImage = recommendationPreview.takeIf { isRecommendation },
                                recommendationActions = recommendationActions.takeIf { isRecommendation },
                                selectedRecommendationActionKey = selectedRecommendationActionKey.takeIf {
                                    isRecommendation && index == focusedIndex
                                },
                                onLaunch = { onLaunch(game) },
                                onEdit = { onEdit(game) },
                                modifier = Modifier.width(shelfstalgicLayout.width)
                            )
                        }
                    } else {
                        val targetScale = when (distance) {
                            0 -> 1f
                            else -> 0.75f
                        }
                        val targetAlpha = when (distance) {
                            0 -> 1f
                            else -> 0.65f
                        }
                        val targetYOffset = when (distance) {
                            0 -> 0f
                            else -> 18f
                        }
                        val scale by animateFloatAsState(targetValue = targetScale, label = "galleryScale")
                        val alpha by animateFloatAsState(targetValue = targetAlpha, label = "galleryAlpha")
                        val translationY by animateFloatAsState(targetValue = targetYOffset, label = "galleryYOffset")

                        GameCard(
                            game = game,
                            columns = tileSizeColumns,
                            editMode = editMode,
                            tileBackgroundMode = launcherTileBackgroundMode,
                            isArtworkLoading = isArtworkLoading,
                            isArtworkPending = isArtworkPending,
                            topTenRank = topTenRank,
                            isRecommendation = isRecommendation,
                            isRecommendationEnabled = recommendationTile?.isEnabled != false,
                            isRecommendationLoading = isRecommendationLoading,
                            recommendationBadgeLabel = recommendationTile?.badgeLabel?.takeIf { isRecommendation } ?: "Recommended",
                            recommendationReason = (
                                recommendationTile?.recommendation?.reason
                                    ?: recommendationTile?.entry?.subtitle
                                )?.takeIf { isRecommendation },
                            previewImage = recommendationPreview.takeIf { isRecommendation },
                            recommendationActions = recommendationActions.takeIf { isRecommendation },
                            selectedRecommendationActionKey = selectedRecommendationActionKey.takeIf {
                                isRecommendation && index == focusedIndex
                            },
                            onLaunch = { onLaunch(game) },
                            onEdit = { onEdit(game) },
                            modifier = Modifier
                                .width(if (mixedNostalgicMode) mixedNostalgicFallbackWidth else cardWidth)
                                .padding(vertical = 6.dp)
                                .zIndex((10 - distance).toFloat())
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                    this.translationY = translationY
                                }
                        )
                    }
                }
            }
        }
        ActiveFilterBadge(
            activePlatformFilter = activePlatformFilter,
            activePlatformIcon = activePlatformIcon,
            activeCollectionFilter = activeCollectionFilter,
            activeCollectionIcon = activeCollectionIcon,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 10.dp)
                .zIndex(1f)
        )
    }
}

@Composable
private fun ActiveFilterBadge(
    activePlatformFilter: String?,
    activePlatformIcon: ImageBitmap?,
    activeCollectionFilter: String?,
    activeCollectionIcon: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    when {
        !activePlatformFilter.isNullOrBlank() -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xB21B2433)),
                modifier = modifier
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    activePlatformIcon?.let { icon ->
                        Image(
                            painter = BitmapPainter(icon),
                            contentDescription = activePlatformFilter,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = activePlatformFilter,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        !activeCollectionFilter.isNullOrBlank() -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xB22C4A3E)),
                modifier = modifier
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    activeCollectionIcon?.let { icon ->
                        Image(
                            painter = BitmapPainter(icon),
                            contentDescription = activeCollectionFilter,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = activeCollectionFilter,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkStatusBadge(
    compact: Boolean,
    label: String,
    compactLabel: String? = null,
    showSpinner: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeLabel = if (compact) compactLabel else label
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xC6111927),
        border = BorderStroke(1.dp, Color(0x665A6169)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = Color(0xFFE5E7EB),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(if (compact) 12.dp else 14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(if (compact) 8.dp else 10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF6C453))
                )
            }
            badgeLabel?.let {
                Text(
                    text = it,
                    color = Color(0xFFE5E7EB),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ArtworkPendingBadge(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    ArtworkStatusBadge(
        compact = compact,
        label = "Finding art",
        compactLabel = null,
        showSpinner = true,
        modifier = modifier
    )
}

@Composable
private fun ArtworkQueuedBadge(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    ArtworkStatusBadge(
        compact = compact,
        label = "Pending download",
        compactLabel = "Pending",
        showSpinner = false,
        modifier = modifier
    )
}

@Composable
private fun MameCabinetNostalgicItem(
    game: GameEntry,
    editMode: Boolean,
    tileBackgroundMode: String,
    cabinetRenderData: MameCabinetRenderData,
    compactMode: Boolean,
    showShelfTitle: Boolean,
    normalizedOverlayHeight: Dp?,
    showGalleryInfo: Boolean,
    isArtworkLoading: Boolean = false,
    isArtworkPending: Boolean = false,
    topTenRank: Int? = null,
    isRecommendation: Boolean = false,
    isRecommendationEnabled: Boolean = true,
    isRecommendationLoading: Boolean = false,
    recommendationBadgeLabel: String = "Recommended",
    recommendationReason: String? = null,
    recommendationActions: RecommendationTileActions? = null,
    selectedRecommendationActionKey: RecommendationActionKey? = null,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grayscaleFilter = rememberRecommendationGrayscaleFilter()
    val cabinetAspectRatio = cabinetRenderData.aspectRatio
    val gallerystalgicContentHeightFraction = if (showGalleryInfo) GALLERYSTALGIC_CONTENT_HEIGHT_FRACTION else 1f
    val gallerystalgicContentWidthFraction = if (showGalleryInfo) GALLERYSTALGIC_CONTENT_MAX_WIDTH_FRACTION else 1f
    val onClick = when {
        isRecommendationLoading -> null
        isRecommendation && !isRecommendationEnabled -> null
        isRecommendation && recommendationActions != null -> null
        editMode -> onEdit
        else -> onLaunch
    }
    Column(
        modifier = (
            if (compactMode) modifier else modifier.fillMaxWidth(0.985f)
            ).then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = if (normalizedOverlayHeight != null) {
                Modifier
                    .fillMaxWidth()
                    .height(normalizedOverlayHeight)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(cabinetAspectRatio)
            }
        ) {
            if (showGalleryInfo) {
                GallerystalgicTileBackdrop(
                    tileBackgroundMode = tileBackgroundMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .then(
                        if (normalizedOverlayHeight != null) {
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight(gallerystalgicContentHeightFraction)
                                .fillMaxWidth(gallerystalgicContentWidthFraction)
                                .aspectRatio(cabinetAspectRatio, matchHeightConstraintsFirst = true)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
            ) {
                Image(
                    painter = BitmapPainter(cabinetRenderData.bitmap),
                    contentDescription = game.platform,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isRecommendation) 0.78f else 1f
                        }
                )
                if (topTenRank != null && !isRecommendation) {
                    TopTenBadge(
                        compact = compactMode,
                        rank = topTenRank,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
                if (isRecommendation) {
                    RecommendationTileBadge(
                        text = if (isRecommendationLoading) "Scanning" else recommendationBadgeLabel,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    RecommendationTileSummary(
                        title = game.title,
                        reason = recommendationReason,
                        actions = recommendationActions.takeUnless { isRecommendationLoading },
                        selectedActionKey = selectedRecommendationActionKey.takeUnless { isRecommendationLoading },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.92f)
                            .padding(bottom = 12.dp)
                    )
                }
                if (isArtworkLoading && !isRecommendation) {
                    ArtworkPendingBadge(
                        compact = compactMode,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                } else if (isArtworkPending && !isRecommendation) {
                    ArtworkQueuedBadge(
                        compact = compactMode,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
                if (isRecommendationLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66161B22))
                    )
                    CircularProgressIndicator(
                        color = Color(0xFFE5E7EB),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                    )
                }
            }
            if (showGalleryInfo && !isRecommendation) {
                NostalgicGalleryInfoPanel(
                    game = game,
                    tileBackgroundMode = tileBackgroundMode,
                    compactMode = compactMode,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(vertical = 12.dp)
                )
            }
        }
        if (showShelfTitle && !showGalleryInfo && !isRecommendation) {
            ShelfstalgicTitleCaption(
                game = game,
                compact = compactMode,
                modifier = Modifier.padding(top = if (compactMode) 3.dp else 5.dp)
            )
        }
    }
}

@Composable
private fun NostalgicGalleryItem(
    game: GameEntry,
    editMode: Boolean,
    overlayPlatform: String?,
    overlaySpec: NostalgicOverlaySpec,
    tileBackgroundMode: String,
    compactMode: Boolean,
    showShelfTitle: Boolean,
    normalizeCanvasSize: Boolean,
    normalizedOverlayHeight: Dp?,
    glassEffectEnabled: Boolean,
    showGalleryInfo: Boolean,
    isArtworkLoading: Boolean = false,
    isArtworkPending: Boolean = false,
    topTenRank: Int? = null,
    isRecommendation: Boolean = false,
    isRecommendationEnabled: Boolean = true,
    isRecommendationLoading: Boolean = false,
    recommendationBadgeLabel: String = "Recommended",
    recommendationReason: String? = null,
    previewImage: ImageBitmap? = null,
    recommendationActions: RecommendationTileActions? = null,
    selectedRecommendationActionKey: RecommendationActionKey? = null,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val image = rememberNostalgicArtworkBitmap(game.imageUri, overlaySpec) ?: previewImage
    val overlayRenderData = rememberNostalgicOverlayRenderData(
        overlaySpec = overlaySpec,
        normalizeCanvasSize = normalizeCanvasSize
    )
    val artworkPlacement = overlayRenderData?.artworkPlacement ?: overlaySpec.defaultArtworkPlacement()
    val artworkMaskBitmap = overlayRenderData?.artworkMaskBitmap
    val platformBadge = rememberPlatformBadgeBitmap(showBadge = image == null, platform = game.platform)
    val grayscaleFilter = rememberRecommendationGrayscaleFilter()
    val gallerystalgicContentHeightFraction = if (showGalleryInfo) GALLERYSTALGIC_CONTENT_HEIGHT_FRACTION else 1f
    val gallerystalgicContentWidthFraction = if (showGalleryInfo) GALLERYSTALGIC_CONTENT_MAX_WIDTH_FRACTION else 1f
    val onClick = when {
        isRecommendationLoading -> null
        isRecommendation && !isRecommendationEnabled -> null
        isRecommendation && recommendationActions != null -> null
        editMode -> onEdit
        else -> onLaunch
    }
    Column(
        modifier = (
            if (compactMode) modifier else modifier.fillMaxWidth(0.985f)
            ).then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        val overlayAspectRatio = overlayRenderData?.aspectRatio ?: overlaySpec.aspectRatio
        BoxWithConstraints(
            modifier = if (normalizedOverlayHeight != null) {
                Modifier
                    .fillMaxWidth()
                    .height(normalizedOverlayHeight)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(overlayAspectRatio)
            }
        ) {
            if (showGalleryInfo) {
                GallerystalgicTileBackdrop(
                    tileBackgroundMode = tileBackgroundMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                )
            }
            BoxWithConstraints(
                modifier = Modifier.then(
                    if (normalizedOverlayHeight != null) {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxHeight(gallerystalgicContentHeightFraction)
                            .fillMaxWidth(gallerystalgicContentWidthFraction)
                            .aspectRatio(overlayAspectRatio, matchHeightConstraintsFirst = true)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
            ) {
                val artworkWidth = maxWidth * artworkPlacement.widthFraction
                val artworkHeight = maxHeight * artworkPlacement.heightFraction
                val artworkStart = maxWidth * artworkPlacement.leftFraction
                val artworkTop = maxHeight * artworkPlacement.topFraction
                val artworkCornerRadius = maxWidth * artworkPlacement.cornerRadiusFraction
                val artworkMaskModifier = if (artworkMaskBitmap != null) {
                    Modifier
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            val maskSize = IntSize(
                                width = size.width.roundToInt().coerceAtLeast(1),
                                height = size.height.roundToInt().coerceAtLeast(1)
                            )
                            onDrawWithContent {
                                drawContent()
                                drawImage(
                                    image = artworkMaskBitmap,
                                    dstSize = maskSize,
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                } else {
                    Modifier.clip(RoundedCornerShape(artworkCornerRadius))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = artworkStart, top = artworkTop)
                        .size(artworkWidth, artworkHeight)
                        .then(artworkMaskModifier)
                        .background(Color(0xFF050505))
                ) {
                    if (image != null) {
                        Image(
                            painter = BitmapPainter(image),
                            contentDescription = game.title,
                            contentScale = if (overlaySpec.fitArtworkToWindow) ContentScale.FillWidth else ContentScale.Crop,
                            colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isRecommendation) 0.78f else 1f
                                }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF141414)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (platformBadge != null) {
                                Image(
                                    painter = BitmapPainter(platformBadge),
                                    contentDescription = game.platform,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                                    modifier = Modifier
                                        .fillMaxWidth(0.52f)
                                        .graphicsLayer {
                                            alpha = if (isRecommendation) 0.82f else 1f
                                        }
                                )
                            } else if (!isRecommendationLoading) {
                                Text(
                                    text = game.title.take(1).uppercase(),
                                    color = if (isRecommendation) Color(0xFFD0D5DD) else Color.White,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                    if (glassEffectEnabled) {
                        NostalgicGlassPane(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                overlayRenderData?.overlayBitmap?.let { overlay ->
                    Image(
                        painter = BitmapPainter(overlay),
                        contentDescription = game.platform,
                        contentScale = ContentScale.Fit,
                        colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (isRecommendation) 0.82f else 1f
                            }
                    )
                }
                if (topTenRank != null && !isRecommendation) {
                    TopTenBadge(
                        compact = compactMode,
                        rank = topTenRank,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
                if (isRecommendation) {
                    RecommendationTileBadge(
                        text = if (isRecommendationLoading) "Scanning" else recommendationBadgeLabel,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    RecommendationTileSummary(
                        title = game.title,
                        reason = recommendationReason,
                        actions = recommendationActions.takeUnless { isRecommendationLoading },
                        selectedActionKey = selectedRecommendationActionKey.takeUnless { isRecommendationLoading },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.92f)
                            .padding(bottom = 12.dp)
                    )
                }
                if (isArtworkLoading && !isRecommendation) {
                    ArtworkPendingBadge(
                        compact = compactMode,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                } else if (isArtworkPending && !isRecommendation) {
                    ArtworkQueuedBadge(
                        compact = compactMode,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
                if (isRecommendationLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66161B22))
                    )
                    CircularProgressIndicator(
                        color = Color(0xFFE5E7EB),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                    )
                }
            }
            if (showGalleryInfo && !isRecommendation) {
                NostalgicGalleryInfoPanel(
                    game = game,
                    tileBackgroundMode = tileBackgroundMode,
                    compactMode = compactMode,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(vertical = 12.dp)
                )
            }
        }
        if (showShelfTitle && !showGalleryInfo && !isRecommendation) {
            ShelfstalgicTitleCaption(
                game = game,
                compact = compactMode,
                modifier = Modifier.padding(top = if (compactMode) 3.dp else 5.dp)
            )
        }
    }
}

@Composable
private fun NostalgicGlassPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithCache {
            val rimThickness = (min(size.width, size.height) * 0.014f).coerceAtLeast(1.5f)
            val topGlow = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height
            )
            val diagonalHighlight = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                start = Offset(x = size.width * 0.04f, y = 0f),
                end = Offset(x = size.width * 0.92f, y = size.height)
            )
            val bottomShade = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x12000000),
                    Color(0x26000000)
                ),
                startY = size.height * 0.58f,
                endY = size.height
            )
            onDrawBehind {
                drawRect(brush = bottomShade)
                drawRect(brush = topGlow)
                drawRect(brush = diagonalHighlight)
                drawRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset.Zero,
                    size = Size(width = size.width, height = rimThickness)
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.06f),
                    topLeft = Offset(x = 0f, y = (size.height - rimThickness).coerceAtLeast(0f)),
                    size = Size(width = size.width, height = rimThickness)
                )
            }
        }
    )
}

@Composable
private fun GallerystalgicTileBackdrop(
    tileBackgroundMode: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                tileBackdropColor(
                    mode = tileBackgroundMode,
                    baseColor = Color(0xFF161B22),
                    normalAlpha = 0.30f,
                    translucentAlpha = 0.18f
                )
            )
            .glossyTileOverlay(isGlossyTileBackgroundMode(tileBackgroundMode))
    )
}

@Composable
private fun NostalgicGalleryInfoPanel(
    game: GameEntry,
    tileBackgroundMode: String,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = tileBackdropColor(
            mode = tileBackgroundMode,
            baseColor = Color(0xFF161B22),
            normalAlpha = 0.75f,
            translucentAlpha = 0.46f
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .glossyTileOverlay(isGlossyTileBackgroundMode(tileBackgroundMode))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = game.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            game.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFD0D5DD),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShelfstalgicSlot(
    layout: ShelfstalgicItemLayout,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .width(layout.slotWidth)
            .padding(top = layout.topPadding, bottom = layout.bottomPadding)
            .zIndex(layout.zIndex)
            .graphicsLayer {
                alpha = layout.alpha
                translationY = layout.translationY
            },
        contentAlignment = Alignment.TopStart
    ) {
        content()
    }
}

@Composable
private fun ShelfstalgicTitleCaption(
    game: GameEntry,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xCC161B22),
        shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 4.dp else 7.dp
            )
        ) {
            Text(
                text = game.title,
                color = Color.White,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                game.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFFD0D5DD),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationTileBadge(
    text: String = "Recommended",
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = Color(0xFFE5E7EB),
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x995A6169))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun TopTenBadge(
    compact: Boolean,
    rank: Int,
    modifier: Modifier = Modifier
) {
    val badgeSize = if (compact) 22.dp else 26.dp
    val iconSize = if (compact) 14.dp else 17.dp
    val outerShape = RoundedCornerShape(if (compact) 8.dp else 10.dp)
    val containerColor = Color(0x660D1117).copy(alpha = if (rank <= 3) 0.40f else 0.30f)
    val iconTint = Color(0xFFF5F7FA).copy(alpha = if (rank <= 3) 0.92f else 0.82f)
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(outerShape)
            .background(containerColor)
    ) {
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Top 10",
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun RecommendationTileSummary(
    title: String,
    reason: String?,
    actions: RecommendationTileActions? = null,
    selectedActionKey: RecommendationActionKey? = null,
    modifier: Modifier = Modifier
) {
    val summaryText = reason?.trim().takeIf { !it.isNullOrBlank() }
    if (summaryText == null && actions == null) return
    Surface(
        color = Color(0xC0161B22),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summaryText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summaryText,
                    color = Color(0xFFD0D5DD),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actions != null) {
                Spacer(modifier = Modifier.height(if (summaryText != null) 10.dp else 8.dp))
                RecommendationActionRow(
                    actions = actions,
                    selectedActionKey = selectedActionKey,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationActionRow(
    actions: RecommendationTileActions,
    selectedActionKey: RecommendationActionKey? = null,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecommendationActionChip(
            label = "Play",
            containerColor = Color(0xFF1E6B55),
            contentColor = Color.White,
            selected = selectedActionKey == RecommendationActionKey.Play,
            onClick = actions.onPlay
        )
        RecommendationActionChip(
            label = "Add",
            containerColor = Color(0x332A3A52),
            contentColor = Color(0xFFE5E7EB),
            selected = selectedActionKey == RecommendationActionKey.Add,
            borderColor = Color(0x665D6B82),
            onClick = actions.onAdd
        )
        actions.onSkip?.let { onSkip ->
            RecommendationActionChip(
                label = "Skip",
                containerColor = Color(0x1FFFFFFF),
                contentColor = Color(0xFFD0D5DD),
                selected = selectedActionKey == RecommendationActionKey.Skip,
                borderColor = Color(0x444B5563),
                onClick = onSkip
            )
        }
    }
}

@Composable
private fun RecommendationActionChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        color = if (selected) containerColor.copy(alpha = 0.95f) else containerColor,
        shape = shape,
        border = if (selected) {
            BorderStroke(2.dp, Color.White.copy(alpha = 0.92f))
        } else {
            borderColor?.let { BorderStroke(1.dp, it) }
        },
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 68.dp)
                .heightIn(min = 40.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun rememberRecommendationGrayscaleFilter(): ColorMatrix {
    return remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }
}

private fun nextFilterForSwipe(currentFilter: String, options: List<String>): String? {
    if (options.isEmpty()) return null
    val currentIndex = options.indexOfFirst { it.equals(currentFilter, ignoreCase = true) }
    val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % options.size
    return options.getOrNull(nextIndex)
}

private fun previousFilterForSwipe(currentFilter: String, options: List<String>): String? {
    if (options.isEmpty()) return null
    val currentIndex = options.indexOfFirst { it.equals(currentFilter, ignoreCase = true) }
    val previousIndex = if (currentIndex == -1) options.lastIndex else (currentIndex - 1 + options.size) % options.size
    return options.getOrNull(previousIndex)
}

private fun orderedFilterOptions(
    collections: List<String>,
    platformFilters: List<String>
): List<String> =
    (listOf(GameFilters.ALL) + collections.sortedBy { it.lowercase() } + platformFilters)
        .distinctBy { it.lowercase() }

private fun GameEntry.filterPlatformLabel(): String? =
    if (sourceType == GameSourceType.ACTIVITY) {
        GamePlatforms.ANDROID
    } else {
        GamePlatforms.filterLabel(platform)
    }

private fun GameEntry.matchesPlatformFilter(filter: String): Boolean =
    if (sourceType == GameSourceType.ACTIVITY) {
        filter.equals(GamePlatforms.ANDROID, ignoreCase = true)
    } else {
        GamePlatforms.matchesFilter(platform, filter)
    }

private fun GameEntry.hasPendingArtworkBadge(
    artworkLoading: Set<String>,
    artworkPending: Set<String>,
    activeArtworkBackfillPlatforms: Set<String>
): Boolean {
    if (id in artworkLoading) return false
    if (id in artworkPending) return true
    if (!imageUri.isNullOrBlank()) return false
    val normalizedPlatform = GamePlatforms.normalizeOrNull(platform) ?: return false
    return normalizedPlatform in activeArtworkBackfillPlatforms
}

private fun orderedGamesForFilter(
    games: List<GameEntry>,
    filter: String
): List<GameEntry> {
    val filteredGames = if (filter == GameFilters.ALL) {
        games
    } else {
        val platformFilter = GamePlatforms.filterLabel(filter)
        if (platformFilter != null) {
            games.filter { game -> game.matchesPlatformFilter(platformFilter) }
        } else {
            games.filter { it.genre.equals(filter, ignoreCase = true) }
        }
    }
    if (filter == GameFilters.ALL) return filteredGames

    val originalIndexById = games.withIndex().associate { (index, game) -> game.id to index }
    return filteredGames.sortedWith(
        compareByDescending<GameEntry> { it.lastPlayedAtEpochMillis ?: Long.MIN_VALUE }
            .thenBy { game -> originalIndexById[game.id] ?: Int.MAX_VALUE }
    )
}

private fun galleryCardWidthFraction(tileSizeColumns: Int): Float = when (tileSizeColumns) {
    1 -> 0.64f
    2 -> 0.58f
    3 -> 0.55f
    4 -> 0.52f
    8 -> 0.46f
    else -> 0.64f
}

private fun nostalgicGalleryWidthFraction(tileSizeColumns: Int): Float = when (tileSizeColumns) {
    1 -> 0.98f
    2 -> 0.84f
    3 -> 0.78f
    4 -> 0.72f
    8 -> 0.60f
    else -> 0.98f
}

private fun sameNostalgicGalleryWidthFraction(
    tileSizeColumns: Int,
    targetAspectRatio: Float,
    occupiedWidthFraction: Float
): Float = when (tileSizeColumns) {
    1 -> nostalgicGalleryWidthFraction(tileSizeColumns)
    else -> {
        val baseWidthFraction = nostalgicGalleryWidthFraction(tileSizeColumns)
        val wideShellPressure = sameNostalgicOverlapPressure(
            targetAspectRatio = targetAspectRatio,
            occupiedWidthFraction = occupiedWidthFraction
        )
        val reduction = when (tileSizeColumns) {
            2 -> 0.12f
            3 -> 0.12f
            4 -> 0.13f
            8 -> 0.16f
            else -> 0.10f
        }
        val minWidthFraction = when (tileSizeColumns) {
            2 -> 0.72f
            3 -> 0.66f
            4 -> 0.57f
            8 -> 0.42f
            else -> baseWidthFraction - 0.08f
        }
        (baseWidthFraction - (wideShellPressure * reduction)).coerceIn(minWidthFraction, baseWidthFraction)
    }
}

private fun sameNostalgicItemSpacing(
    itemWidth: Dp,
    targetAspectRatio: Float,
    occupiedWidthFraction: Float,
    tileSizeColumns: Int
): Dp {
    val overlapPressure = sameNostalgicOverlapPressure(
        targetAspectRatio = targetAspectRatio,
        occupiedWidthFraction = occupiedWidthFraction
    )
    val baseSpacingFraction = when (tileSizeColumns) {
        1 -> 0.03f
        2 -> 0.055f
        3 -> 0.05f
        4 -> 0.045f
        8 -> 0.065f
        else -> 0.04f
    }
    val pressureSpacingFraction = when (tileSizeColumns) {
        1 -> 0.10f
        2 -> 0.14f
        3 -> 0.14f
        4 -> 0.15f
        8 -> 0.17f
        else -> 0.12f
    }
    val spacingFraction = baseSpacingFraction + (overlapPressure * pressureSpacingFraction)
    val minSpacing = when (tileSizeColumns) {
        2 -> 24.dp
        3 -> 22.dp
        4 -> 20.dp
        8 -> 28.dp
        else -> 18.dp
    }
    val maxSpacing = when (tileSizeColumns) {
        8 -> 116.dp
        else -> 104.dp
    }
    return (itemWidth * spacingFraction).coerceIn(minSpacing, maxSpacing)
}

private fun sameNostalgicOverlapPressure(
    targetAspectRatio: Float,
    occupiedWidthFraction: Float
): Float {
    val aspectPressure = (
        (targetAspectRatio - NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO) / 0.34f
        ).coerceIn(0f, 1f)
    val occupiedPressure = (
        (occupiedWidthFraction.coerceIn(0.35f, 1f) - 0.62f) / 0.20f
        ).coerceIn(0f, 1f)
    return max(aspectPressure, occupiedPressure)
}

private fun shelfstalgicFocusWidthFraction(tileSizeColumns: Int): Float = when (tileSizeColumns) {
    1 -> 0.84f
    2 -> 0.76f
    3 -> 0.71f
    4 -> 0.66f
    8 -> 0.55f
    else -> 0.84f
}

private fun shelfstalgicCompactWidthFraction(tileSizeColumns: Int): Float = when (tileSizeColumns) {
    1 -> 0.56f
    2 -> 0.50f
    3 -> 0.46f
    4 -> 0.42f
    8 -> 0.36f
    else -> 0.56f
}

private fun shelfstalgicItemSpacing(tileSizeColumns: Int): Dp = when (tileSizeColumns) {
    1 -> 2.dp
    2 -> 4.dp
    3 -> 4.dp
    4 -> 6.dp
    8 -> 8.dp
    else -> 4.dp
}

@Composable
private fun rememberShelfstalgicItemLayout(
    enabled: Boolean,
    relativeOffset: Int,
    slotWidth: Dp,
    focusedWidth: Dp,
    compactWidth: Dp
): ShelfstalgicItemLayout? {
    if (!enabled) return null

    val targetWidth = when {
        relativeOffset == 0 -> focusedWidth
        relativeOffset > 0 -> when (relativeOffset) {
            1 -> compactWidth * 0.88f
            2 -> compactWidth * 0.80f
            else -> compactWidth * 0.72f
        }
        else -> compactWidth * 0.58f
    }
    val targetAlpha = when {
        relativeOffset == 0 -> 1f
        relativeOffset > 0 -> when (relativeOffset) {
            1 -> 0.92f
            2 -> 0.78f
            else -> 0.62f
        }
        else -> 0.28f
    }
    val targetYOffset = when {
        relativeOffset == 0 -> 0f
        relativeOffset > 0 -> when (relativeOffset) {
            1 -> 8f
            2 -> 12f
            else -> 15f
        }
        else -> 10f
    }
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shelfstalgicWidth"
    )
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shelfstalgicAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = targetYOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shelfstalgicYOffset"
    )

    return ShelfstalgicItemLayout(
        slotWidth = slotWidth,
        width = width,
        compactMode = relativeOffset != 0,
        topPadding = when {
            relativeOffset == 0 -> 8.dp
            relativeOffset > 0 -> 10.dp
            else -> 12.dp
        },
        bottomPadding = if (relativeOffset == 0) 8.dp else 4.dp,
        alpha = alpha,
        translationY = translationY,
        zIndex = when {
            relativeOffset == 0 -> 20f
            relativeOffset > 0 -> (20 - relativeOffset).toFloat()
            else -> 1f
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(
    game: GameEntry,
    columns: Int,
    editMode: Boolean,
    tileBackgroundMode: String,
    shelfMode: Boolean = false,
    shelfCompactMode: Boolean = false,
    isArtworkLoading: Boolean = false,
    isArtworkPending: Boolean = false,
    topTenRank: Int? = null,
    isRecommendation: Boolean = false,
    isRecommendationEnabled: Boolean = true,
    isRecommendationLoading: Boolean = false,
    recommendationBadgeLabel: String = "Recommended",
    recommendationReason: String? = null,
    previewImage: ImageBitmap? = null,
    recommendationActions: RecommendationTileActions? = null,
    selectedRecommendationActionKey: RecommendationActionKey? = null,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val image = previewImage ?: rememberDecodedBitmap(game.imageUri)
    val showText = columns != 8 || isRecommendation
    val showSubtitle = (columns < 4 || isRecommendation) && !shelfCompactMode
    val grayscaleFilter = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }
    val shapeModifier = if (columns == 1) {
        // Real-card feel for featured/gallery tiles (portrait ratio ~2.5:3.5).
        Modifier.aspectRatio(FEATURED_GALLERY_CARD_ASPECT_RATIO)
    } else {
        Modifier.aspectRatio(1f)
    }
    val glossyTileBackground = isGlossyTileBackgroundMode(tileBackgroundMode)
    val containerColor = tileSurfaceColor(
        mode = tileBackgroundMode,
        normalColor = if (isRecommendation) Color(0xFF2A2F35) else Color(0xFF161B22),
        translucentAlpha = if (isRecommendation) 0.82f else 0.68f
    )
    val artworkBackground = tileSurfaceColor(
        mode = tileBackgroundMode,
        normalColor = if (isRecommendation) Color(0xFF20262D) else Color(0xFF202938),
        translucentAlpha = if (isRecommendation) 0.68f else 0.54f
    )
    val titleColor = if (isRecommendation) Color(0xFFD0D5DD) else Color.White
    val subtitleColor = if (isRecommendation) Color(0xFFB1BAC4) else Color(0xFF98A2B3)
    val textPadding = when {
        shelfCompactMode -> 6.dp
        shelfMode -> 9.dp
        else -> 12.dp
    }
    val titleStyle = when {
        shelfCompactMode -> MaterialTheme.typography.labelSmall
        shelfMode -> MaterialTheme.typography.titleSmall
        else -> MaterialTheme.typography.titleMedium
    }
    val subtitleStyle = if (shelfCompactMode) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.bodySmall
    }
    val cardClick: (() -> Unit)? = when {
        isRecommendationLoading -> null
        isRecommendation && !isRecommendationEnabled -> null
        isRecommendation && recommendationActions != null -> null
        isRecommendation -> onLaunch
        editMode -> onEdit
        else -> onLaunch
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .then(shapeModifier)
            .then(
                if (cardClick != null) {
                    Modifier.clickable(onClick = cardClick)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glossyTileOverlay(glossyTileBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(artworkBackground)
                ) {
                    when {
                        image != null -> Image(
                            painter = BitmapPainter(image),
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isRecommendation) 0.76f else 1f
                                }
                        )

                        else -> Image(
                            painter = painterResource(id = R.drawable.ic_tile_placeholder),
                            contentDescription = game.title,
                            contentScale = ContentScale.Fit,
                            colorFilter = if (isRecommendation) {
                                ColorFilter.tint(Color(0xFFBBC3CC))
                            } else {
                                null
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (columns == 1) 120.dp else 72.dp)
                                .graphicsLayer {
                                    alpha = if (isRecommendation) 0.72f else 1f
                                }
                        )
                    }

                    if (isRecommendationLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66161B22))
                        )
                        CircularProgressIndicator(
                            color = Color(0xFFE5E7EB),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (columns == 1) 44.dp else 34.dp)
                        )
                    }
                    if (isArtworkLoading && !isRecommendation) {
                        ArtworkPendingBadge(
                            compact = !showText,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    } else if (isArtworkPending && !isRecommendation) {
                        ArtworkQueuedBadge(
                            compact = !showText,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }

                    if (topTenRank != null && !isRecommendation) {
                        TopTenBadge(
                            compact = !showText,
                            rank = topTenRank,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )
                    }

                    if (isRecommendation && showText) {
                        Text(
                            text = if (isRecommendationLoading) "Scanning" else recommendationBadgeLabel,
                            color = Color(0xFFE5E7EB),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x995A6169))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    } else if (editMode && showText) {
                        Text(
                            text = "Edit",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = if (topTenRank != null) 34.dp else 8.dp, end = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xB21B2433))
                                .clickable(onClick = onEdit)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                if (showText) {
                    Column(modifier = Modifier.padding(textPadding)) {
                        Text(
                            text = game.title,
                            color = titleColor,
                            style = titleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (isRecommendation) {
                            when {
                                isRecommendationLoading -> game.subtitle.takeIf { it.isNotBlank() }
                                else -> recommendationReason?.takeIf { it.isNotBlank() }
                            }
                        } else {
                            game.subtitle.takeIf { it.isNotBlank() }
                        }
                        if (showSubtitle && !subtitleText.isNullOrBlank()) {
                            Text(
                                text = subtitleText,
                                color = subtitleColor,
                                style = subtitleStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isRecommendation && !isRecommendationLoading && recommendationActions != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            RecommendationActionRow(
                                actions = recommendationActions,
                                selectedActionKey = selectedRecommendationActionKey,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridstalgicOverlayFrame(
    itemAspectRatio: Float,
    itemOccupiedWidthFraction: Float,
    normalizedLayoutMetrics: SameNostalgicLayoutMetrics?,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val safeItemAspectRatio = itemAspectRatio.coerceAtLeast(0.1f)
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (normalizedLayoutMetrics != null) {
            val targetAspectRatio = normalizedLayoutMetrics.targetAspectRatio.coerceAtLeast(0.1f)
            val referenceModifier = if ((maxWidth / targetAspectRatio) <= maxHeight) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(targetAspectRatio)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(targetAspectRatio, matchHeightConstraintsFirst = true)
            }
            BoxWithConstraints(
                modifier = referenceModifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                val safeCurrentOccupiedWidth = itemOccupiedWidthFraction.coerceIn(0.35f, 1f)
                val targetOccupiedWidth = normalizedLayoutMetrics.occupiedWidthFraction.coerceIn(0.35f, 1f)
                val widthFromOccupiedWidth = maxWidth * (targetOccupiedWidth / safeCurrentOccupiedWidth)
                val widthFromAspectRatio = maxHeight * safeItemAspectRatio
                val normalizedWidth = widthFromOccupiedWidth
                    .coerceAtMost(maxWidth)
                    .coerceAtMost(widthFromAspectRatio)
                    .coerceAtLeast(1.dp)
                BoxWithConstraints(
                    modifier = Modifier
                        .width(normalizedWidth)
                        .aspectRatio(safeItemAspectRatio)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        } else {
            val fittedModifier = if ((maxWidth / safeItemAspectRatio) <= maxHeight) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(safeItemAspectRatio)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(safeItemAspectRatio, matchHeightConstraintsFirst = true)
            }
            BoxWithConstraints(
                modifier = fittedModifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridNostalgicCard(
    game: GameEntry,
    columns: Int,
    editMode: Boolean,
    tileBackgroundMode: String,
    normalizeCanvasSize: Boolean,
    normalizedLayoutMetrics: SameNostalgicLayoutMetrics? = null,
    isArtworkLoading: Boolean = false,
    isArtworkPending: Boolean = false,
    topTenRank: Int? = null,
    isRecommendation: Boolean = false,
    isRecommendationEnabled: Boolean = true,
    isRecommendationLoading: Boolean = false,
    recommendationBadgeLabel: String = "Recommended",
    recommendationReason: String? = null,
    previewImage: ImageBitmap? = null,
    recommendationActions: RecommendationTileActions? = null,
    selectedRecommendationActionKey: RecommendationActionKey? = null,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overlaySpec = NostalgicOverlays.specForPlatform(game.platform)
    val nostalgicImage = if (overlaySpec != null) {
        rememberNostalgicArtworkBitmap(game.imageUri, overlaySpec) ?: previewImage
    } else {
        null
    }
    val defaultImage = if (overlaySpec == null) {
        previewImage ?: rememberDecodedBitmap(game.imageUri)
    } else {
        null
    }
    val overlayRenderData = overlaySpec?.let {
        rememberNostalgicOverlayRenderData(
            overlaySpec = it,
            normalizeCanvasSize = normalizeCanvasSize
        )
    }
    val mameCabinetRenderData = rememberMameNostalgicCabinetRenderData(
        game = game,
        normalizeCanvasSize = normalizeCanvasSize
    )
    val platformBadge = rememberPlatformBadgeBitmap(
        showBadge = nostalgicImage == null && defaultImage == null,
        platform = game.platform
    )
    val showText = columns != 8 || isRecommendation
    val showSubtitle = columns < 4 || isRecommendation
    val grayscaleFilter = rememberRecommendationGrayscaleFilter()
    val shapeModifier = if (columns == 1) {
        Modifier.aspectRatio(FEATURED_GALLERY_CARD_ASPECT_RATIO)
    } else {
        Modifier.aspectRatio(1f)
    }
    val glossyTileBackground = isGlossyTileBackgroundMode(tileBackgroundMode)
    val containerColor = tileSurfaceColor(
        mode = tileBackgroundMode,
        normalColor = if (isRecommendation) Color(0xFF2A2F35) else Color(0xFF161B22),
        translucentAlpha = if (isRecommendation) 0.82f else 0.68f
    )
    val artworkBackground = tileSurfaceColor(
        mode = tileBackgroundMode,
        normalColor = if (isRecommendation) Color(0xFF20262D) else Color(0xFF202938),
        translucentAlpha = if (isRecommendation) 0.68f else 0.54f
    )
    val titleColor = if (isRecommendation) Color(0xFFD0D5DD) else Color.White
    val subtitleColor = if (isRecommendation) Color(0xFFB1BAC4) else Color(0xFF98A2B3)
    val tilePadding = if (columns == 1) 10.dp else 6.dp
    val cardClick: (() -> Unit)? = when {
        isRecommendationLoading -> null
        isRecommendation && !isRecommendationEnabled -> null
        isRecommendation && recommendationActions != null -> null
        isRecommendation -> onLaunch
        editMode -> onEdit
        else -> onLaunch
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .then(shapeModifier)
            .then(
                if (cardClick != null) {
                    Modifier.clickable(onClick = cardClick)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glossyTileOverlay(glossyTileBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(artworkBackground)
                ) {
                    when {
                        mameCabinetRenderData != null -> {
                            GridstalgicOverlayFrame(
                                itemAspectRatio = mameCabinetRenderData.aspectRatio,
                                itemOccupiedWidthFraction = mameCabinetRenderData.occupiedWidthFraction,
                                normalizedLayoutMetrics = normalizedLayoutMetrics,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(tilePadding)
                            ) {
                                Image(
                                    painter = BitmapPainter(mameCabinetRenderData.bitmap),
                                    contentDescription = game.platform,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = if (isRecommendation) 0.78f else 1f
                                        }
                                )
                            }
                        }

                        overlaySpec != null && overlayRenderData != null -> {
                            val artworkPlacement = overlayRenderData.artworkPlacement ?: overlaySpec.defaultArtworkPlacement()
                            val artworkMaskBitmap = overlayRenderData.artworkMaskBitmap
                            GridstalgicOverlayFrame(
                                itemAspectRatio = overlayRenderData.aspectRatio,
                                itemOccupiedWidthFraction = overlayRenderData.occupiedWidthFraction,
                                normalizedLayoutMetrics = normalizedLayoutMetrics,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(tilePadding)
                            ) {
                                val artworkWidth = maxWidth * artworkPlacement.widthFraction
                                val artworkHeight = maxHeight * artworkPlacement.heightFraction
                                val artworkStart = maxWidth * artworkPlacement.leftFraction
                                val artworkTop = maxHeight * artworkPlacement.topFraction
                                val artworkCornerRadius = maxWidth * artworkPlacement.cornerRadiusFraction
                                val artworkMaskModifier = if (artworkMaskBitmap != null) {
                                    Modifier
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                        .drawWithCache {
                                            val maskSize = IntSize(
                                                width = size.width.roundToInt().coerceAtLeast(1),
                                                height = size.height.roundToInt().coerceAtLeast(1)
                                            )
                                            onDrawWithContent {
                                                drawContent()
                                                drawImage(
                                                    image = artworkMaskBitmap,
                                                    dstSize = maskSize,
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                        }
                                } else {
                                    Modifier.clip(RoundedCornerShape(artworkCornerRadius))
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = artworkStart, top = artworkTop)
                                        .size(artworkWidth, artworkHeight)
                                        .then(artworkMaskModifier)
                                        .background(Color(0xFF050505))
                                ) {
                                    if (nostalgicImage != null) {
                                        Image(
                                            painter = BitmapPainter(nostalgicImage),
                                            contentDescription = game.title,
                                            contentScale = if (overlaySpec.fitArtworkToWindow) {
                                                ContentScale.FillWidth
                                            } else {
                                                ContentScale.Crop
                                            },
                                            colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    alpha = if (isRecommendation) 0.78f else 1f
                                                }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF141414)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (platformBadge != null) {
                                                Image(
                                                    painter = BitmapPainter(platformBadge),
                                                    contentDescription = game.platform,
                                                    contentScale = ContentScale.Fit,
                                                    colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.52f)
                                                        .graphicsLayer {
                                                            alpha = if (isRecommendation) 0.82f else 1f
                                                        }
                                                )
                                            } else if (!isRecommendationLoading) {
                                                Text(
                                                    text = game.title.take(1).uppercase(),
                                                    color = if (isRecommendation) Color(0xFFD0D5DD) else Color.White,
                                                    style = MaterialTheme.typography.headlineMedium
                                                )
                                            }
                                        }
                                    }
                                }

                                Image(
                                    painter = BitmapPainter(overlayRenderData.overlayBitmap),
                                    contentDescription = game.platform,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = if (isRecommendation) 0.82f else 1f
                                        }
                                )
                            }
                        }

                        defaultImage != null -> Image(
                            painter = BitmapPainter(defaultImage),
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (isRecommendation) 0.76f else 1f
                                }
                        )

                        else -> Image(
                            painter = painterResource(id = R.drawable.ic_tile_placeholder),
                            contentDescription = game.title,
                            contentScale = ContentScale.Fit,
                            colorFilter = if (isRecommendation) {
                                ColorFilter.tint(Color(0xFFBBC3CC))
                            } else {
                                null
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (columns == 1) 120.dp else 72.dp)
                                .graphicsLayer {
                                    alpha = if (isRecommendation) 0.72f else 1f
                                }
                        )
                    }

                    if (isRecommendationLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66161B22))
                        )
                        CircularProgressIndicator(
                            color = Color(0xFFE5E7EB),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(if (columns == 1) 44.dp else 34.dp)
                        )
                    }
                    if (isArtworkLoading && !isRecommendation) {
                        ArtworkPendingBadge(
                            compact = !showText,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    } else if (isArtworkPending && !isRecommendation) {
                        ArtworkQueuedBadge(
                            compact = !showText,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }

                    if (topTenRank != null && !isRecommendation) {
                        TopTenBadge(
                            compact = !showText,
                            rank = topTenRank,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )
                    }

                    if (isRecommendation && showText) {
                        Text(
                            text = if (isRecommendationLoading) "Scanning" else recommendationBadgeLabel,
                            color = Color(0xFFE5E7EB),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x995A6169))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    } else if (editMode && showText) {
                        Text(
                            text = "Edit",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = if (topTenRank != null) 34.dp else 8.dp, end = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xB21B2433))
                                .clickable(onClick = onEdit)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                if (showText) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = game.title,
                            color = titleColor,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (isRecommendation) {
                            when {
                                isRecommendationLoading -> game.subtitle.takeIf { it.isNotBlank() }
                                else -> recommendationReason?.takeIf { it.isNotBlank() }
                            }
                        } else {
                            game.subtitle.takeIf { it.isNotBlank() }
                        }
                        if (showSubtitle && !subtitleText.isNullOrBlank()) {
                            Text(
                                text = subtitleText,
                                color = subtitleColor,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isRecommendation && !isRecommendationLoading && recommendationActions != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            RecommendationActionRow(
                                actions = recommendationActions,
                                selectedActionKey = selectedRecommendationActionKey,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationAddCard(
    game: GameEntry,
    columns: Int,
    tileBackgroundMode: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showText = columns != 8
    val showSubtitle = columns < 4
    val glossyTileBackground = isGlossyTileBackgroundMode(tileBackgroundMode)
    val shapeModifier = if (columns == 1) {
        Modifier.aspectRatio(FEATURED_GALLERY_CARD_ASPECT_RATIO)
    } else {
        Modifier.aspectRatio(1f)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = tileSurfaceColor(
                mode = tileBackgroundMode,
                normalColor = Color(0xFF1C2A22),
                translucentAlpha = 0.76f
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(shapeModifier)
            .clickable(onClick = onAdd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glossyTileOverlay(glossyTileBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            tileSurfaceColor(
                                mode = tileBackgroundMode,
                                normalColor = Color(0xFF223229),
                                translucentAlpha = 0.62f
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add game",
                        tint = Color(0xFFE7F5EC),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(if (columns == 1) 88.dp else 56.dp)
                    )
                    if (showText) {
                        Text(
                            text = "Add game",
                            color = Color(0xFFE7F5EC),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x663D5A46))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                if (showText) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = game.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showSubtitle && game.subtitle.isNotBlank()) {
                            Text(
                                text = game.subtitle,
                                color = Color(0xFFB7C8BC),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun recommendationLoadingEntry(filter: String): GameEntry {
    val normalizedFilter = GamePlatforms.filterLabel(filter) ?: GameFilters.normalizeFilter(filter)
    val filterId = normalizedFilter
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    val subtitle = if (normalizedFilter.equals("Arcade", ignoreCase = true)) {
        "Scanning arcade ROMs"
    } else {
        "Scanning ${normalizedFilter.lowercase()} titles"
    }
    return GameEntry(
        id = "recommendation-loading:$filterId",
        title = "Finding a recommendation",
        subtitle = subtitle,
        sourceType = GameSourceType.CUSTOM_INTENT
    )
}

private fun recommendationUnavailableEntry(filter: String): GameEntry {
    val normalizedFilter = GamePlatforms.filterLabel(filter) ?: GameFilters.normalizeFilter(filter)
    val filterId = normalizedFilter
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return GameEntry(
        id = "recommendation-unavailable:$filterId",
        title = "Recommendation slot",
        subtitle = "No recommendation available",
        platform = normalizedFilter.takeIf { it != GameFilters.ALL },
        sourceType = GameSourceType.CUSTOM_INTENT
    )
}

private fun recommendationListItemKey(
    game: GameEntry,
    recommendationId: String?,
    recommendationAddId: String?,
    filter: String
): String {
    val normalizedFilter = normalizedRecommendationFilterKey(filter)
    return when (game.id) {
        recommendationId -> "recommendation-slot:$normalizedFilter"
        recommendationAddId -> "recommendation-add-slot:$normalizedFilter"
        else -> game.id
    }
}

private fun normalizedRecommendationFilterKey(filter: String): String =
    (GamePlatforms.filterLabel(filter) ?: GameFilters.normalizeFilter(filter)).systemId()

private fun genericRecommendationAddTile(
    filterKey: String,
    collectionFilter: String? = null
): RecommendationAddTileUi =
    RecommendationAddTileUi(
        entry = GameEntry(
            id = "recommendation-add:${filterKey.systemId()}",
            title = "Add your own game",
            subtitle = "Choose platform, emulator, and ROM",
            sourceType = GameSourceType.CUSTOM_INTENT
        ),
        platformFilter = null,
        collectionFilter = collectionFilter
    )

private fun recommendationAddTileForFilter(
    filter: String,
    collectionFilters: List<String> = emptyList()
): RecommendationAddTileUi? {
    if (filter.equals(GameFilters.ALL, ignoreCase = true)) {
        return genericRecommendationAddTile("all")
    }
    val normalizedFilter = GamePlatforms.filterLabel(filter)
    if (normalizedFilter != null) {
        if (normalizedFilter.equals(GamePlatforms.ANDROID, ignoreCase = true)) {
            return RecommendationAddTileUi(
                entry = GameEntry(
                    id = "recommendation-add:${normalizedFilter.systemId()}",
                    title = "Add Android tile",
                    subtitle = "Choose Android app or activity",
                    platform = GamePlatforms.ANDROID,
                    sourceType = GameSourceType.ACTIVITY
                ),
                platformFilter = GamePlatforms.ANDROID
            )
        }
        val relatedPlatforms = GamePlatforms.values
            .filter { platform -> GamePlatforms.matchesFilter(platform, normalizedFilter) }
            .distinctBy { it.lowercase() }
        if (relatedPlatforms.isEmpty()) return null
        val subtitle = if (relatedPlatforms.size == 1) {
            "${relatedPlatforms.first()} • choose emulator and ROM"
        } else {
            "Choose platform, emulator, and ROM"
        }
        return RecommendationAddTileUi(
            entry = GameEntry(
                id = "recommendation-add:${normalizedFilter.systemId()}",
                title = "Add your own game",
                subtitle = subtitle,
                platform = if (relatedPlatforms.size == 1) relatedPlatforms.first() else normalizedFilter,
                sourceType = GameSourceType.CUSTOM_INTENT
            ),
            platformFilter = normalizedFilter
        )
    }
    if (collectionFilters.any { it.equals(filter, ignoreCase = true) }) {
        return genericRecommendationAddTile(
            filterKey = "collection-${GameFilters.normalizeFilter(filter)}",
            collectionFilter = filter
        )
    }
    return null
}

@Composable
private fun rememberDecodedBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    val normalizedUri = uriString?.takeIf(String::isNotBlank)
    val cacheKey = remember(normalizedUri) {
        normalizedUri?.let { artworkBitmapCacheKey(it, applyVintageArtwork = false) }
    }
    return produceState<ImageBitmap?>(
        initialValue = cacheKey?.let(::getCachedTileBitmap)?.asImageBitmap(),
        cacheKey
    ) {
        val resolvedCacheKey = cacheKey
        val resolvedUri = normalizedUri
        if (resolvedCacheKey == null || resolvedUri == null) {
            value = null
            return@produceState
        }
        getCachedTileBitmap(resolvedCacheKey)?.let { cached ->
            value = cached.asImageBitmap()
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            ensureArtworkBitmapCached(
                contentResolver = context.contentResolver,
                uriString = resolvedUri,
                applyVintageArtwork = false
            )?.asImageBitmap()
        }
    }.value
}

@Composable
private fun rememberRecommendationPreviewBitmap(assetPath: String?): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, assetPath) {
        value = withContext(Dispatchers.IO) {
            val path = assetPath?.takeIf { it.isNotBlank() } ?: return@withContext null
            runCatching {
                context.assets.open(path).use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

@Composable
private fun rememberLauncherBackgroundBitmap(
    assetName: String?,
    customUriString: String?,
    animated: Boolean
): ImageBitmap? {
    val context = LocalContext.current
    val maxDimension = if (animated) {
        LAUNCHER_BACKGROUND_ANIMATED_MAX_DIMENSION
    } else {
        LAUNCHER_BACKGROUND_STATIC_MAX_DIMENSION
    }
    return produceState<ImageBitmap?>(initialValue = null, assetName, customUriString, animated) {
        val normalizedAssetName = assetName?.takeIf { it.isNotBlank() }
        value = withContext(Dispatchers.IO) {
            when {
                !customUriString.isNullOrBlank() -> runCatching {
                    decodeSampledBitmap(
                        contentResolver = context.contentResolver,
                        uri = Uri.parse(customUriString),
                        maxDimension = maxDimension
                    )?.asImageBitmap()
                }.getOrNull()

                normalizedAssetName != null -> runCatching {
                    decodeSampledAssetBitmap(
                        context = context,
                        assetPath = "backgrounds/$normalizedAssetName",
                        maxDimension = maxDimension
                    )?.asImageBitmap()
                }.getOrNull()

                else -> null
            }
        }
    }.value
}

@Composable
private fun rememberLauncherBackgroundMotion(
    enabled: Boolean,
    backgroundKey: String?,
    launchCycle: Int
): LauncherBackgroundMotion {
    if (!enabled || backgroundKey.isNullOrBlank()) {
        return LauncherBackgroundMotion(scale = 1f, panXFraction = 0f, panYFraction = 0f)
    }
    val path = remember(backgroundKey) { launcherBackgroundMotionPath(backgroundKey) }
    val cycleLength = path.size.toFloat().coerceAtLeast(1f)
    val initialPhase = remember(backgroundKey, launchCycle, path.size) {
        launcherBackgroundMotionStartOffset(
            backgroundKey = backgroundKey,
            launchCycle = launchCycle,
            pathSize = path.size
        )
    }
    val phase = remember(backgroundKey) { Animatable(initialPhase) }
    var lastLaunchCycle by remember(backgroundKey) { mutableIntStateOf(launchCycle) }

    LaunchedEffect(backgroundKey, launchCycle, path.size) {
        val targetPhase = launcherBackgroundMotionStartOffset(
            backgroundKey = backgroundKey,
            launchCycle = launchCycle,
            pathSize = path.size
        )
        if (lastLaunchCycle != launchCycle) {
            phase.snapTo(targetPhase)
        }
        lastLaunchCycle = launchCycle

        while (isActive) {
            phase.animateTo(
                targetValue = phase.value + 1f,
                animationSpec = tween(
                    durationMillis = LAUNCHER_BACKGROUND_ANIMATION_SEGMENT_MILLIS,
                    easing = LinearEasing
                )
            )
        }
    }
    return launcherBackgroundMotionAtPhase(path, phase.value)
}

private fun launcherBackgroundMotionStartOffset(
    backgroundKey: String,
    launchCycle: Int,
    pathSize: Int
): Float {
    if (pathSize <= 0) return 0f
    val totalSteps = pathSize * LAUNCHER_BACKGROUND_START_STEPS_PER_SEGMENT
    val baseStep = positiveModulo(backgroundKey.lowercase().hashCode(), totalSteps)
    val cycleStep = positiveModulo(launchCycle * LAUNCHER_BACKGROUND_START_STEP_DELTA, totalSteps)
    return (baseStep + cycleStep) % totalSteps / LAUNCHER_BACKGROUND_START_STEPS_PER_SEGMENT.toFloat()
}

private fun positiveModulo(value: Int, modulus: Int): Int {
    if (modulus <= 0) return 0
    val remainder = value % modulus
    return if (remainder < 0) remainder + modulus else remainder
}

private fun positiveModulo(value: Float, modulus: Float): Float {
    if (modulus <= 0f) return 0f
    val remainder = value % modulus
    return if (remainder < 0f) remainder + modulus else remainder
}

private fun launcherBackgroundMotionAtPhase(
    path: List<LauncherBackgroundMotion>,
    phase: Float
): LauncherBackgroundMotion {
    if (path.isEmpty()) {
        return LauncherBackgroundMotion(scale = 1f, panXFraction = 0f, panYFraction = 0f)
    }
    val normalizedPhase = positiveModulo(phase, path.size.toFloat().coerceAtLeast(1f))
    val startIndex = floor(normalizedPhase).toInt().coerceIn(0, path.lastIndex)
    val endIndex = (startIndex + 1) % path.size
    val progress = normalizedPhase - floor(normalizedPhase)
    val start = path[startIndex]
    val end = path[endIndex]
    return LauncherBackgroundMotion(
        scale = lerpFloat(start.scale, end.scale, progress),
        panXFraction = lerpFloat(start.panXFraction, end.panXFraction, progress),
        panYFraction = lerpFloat(start.panYFraction, end.panYFraction, progress)
    )
}

private fun launcherBackgroundMotionPath(backgroundKey: String): List<LauncherBackgroundMotion> {
    val baseFrames = listOf(
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = -0.42f, panYFraction = -0.30f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = -0.14f, panYFraction = -0.36f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = 0.18f, panYFraction = -0.33f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = 0.39f, panYFraction = -0.18f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = 0.43f, panYFraction = 0.06f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = 0.28f, panYFraction = 0.28f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = -0.02f, panYFraction = 0.37f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = -0.31f, panYFraction = 0.31f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = -0.43f, panYFraction = 0.10f),
        LauncherBackgroundMotion(scale = LAUNCHER_BACKGROUND_ANIMATION_SCALE, panXFraction = -0.24f, panYFraction = -0.04f)
    )
    val hash = backgroundKey.lowercase().hashCode()
    val rotation = ((hash % baseFrames.size) + baseFrames.size) % baseFrames.size
    val mirrorX = (hash and 1) != 0
    val mirrorY = (hash and 2) != 0
    return List(baseFrames.size) { index ->
        val frame = baseFrames[(index + rotation) % baseFrames.size]
        frame.copy(
            panXFraction = if (mirrorX) -frame.panXFraction else frame.panXFraction,
            panYFraction = if (mirrorY) -frame.panYFraction else frame.panYFraction
        )
    }
}

private fun lerpFloat(start: Float, end: Float, progress: Float): Float =
    start + ((end - start) * progress.coerceIn(0f, 1f))

private fun decodeSampledAssetBitmap(
    context: Context,
    assetPath: String,
    maxDimension: Int
): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(assetPath).use { stream ->
        if (BitmapFactory.decodeStream(stream, null, boundsOptions) == null && boundsOptions.outWidth <= 0) {
            return null
        }
    }
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = calculateInSampleSize(
            width = boundsOptions.outWidth,
            height = boundsOptions.outHeight,
            maxDimension = maxDimension
        )
    }
    return context.assets.open(assetPath).use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

@Composable
private fun rememberNostalgicArtworkBitmap(
    uriString: String?,
    overlaySpec: NostalgicOverlaySpec
): ImageBitmap? {
    val context = LocalContext.current
    val normalizedUri = uriString?.takeIf(String::isNotBlank)
    val cacheKey = remember(
        normalizedUri,
        overlaySpec.applyVintageArtwork,
        overlaySpec.preTrimLeftFraction,
        overlaySpec.preTrimTopFraction,
        overlaySpec.preTrimWidthFraction,
        overlaySpec.preTrimHeightFraction
    ) {
        normalizedUri?.let {
            artworkBitmapCacheKey(
                uriString = it,
                applyVintageArtwork = overlaySpec.applyVintageArtwork,
                preTrimLeftFraction = overlaySpec.preTrimLeftFraction,
                preTrimTopFraction = overlaySpec.preTrimTopFraction,
                preTrimWidthFraction = overlaySpec.preTrimWidthFraction,
                preTrimHeightFraction = overlaySpec.preTrimHeightFraction
            )
        }
    }
    return produceState<ImageBitmap?>(
        initialValue = cacheKey?.let(::getCachedTileBitmap)?.asImageBitmap(),
        cacheKey
    ) {
        val artworkUri = normalizedUri
        if (artworkUri.isNullOrBlank()) {
            value = null
            return@produceState
        }
        getCachedTileBitmap(cacheKey.orEmpty())?.let { cached ->
            value = cached.asImageBitmap()
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            ensureArtworkBitmapCached(
                contentResolver = context.contentResolver,
                uriString = artworkUri,
                applyVintageArtwork = overlaySpec.applyVintageArtwork,
                preTrimLeftFraction = overlaySpec.preTrimLeftFraction,
                preTrimTopFraction = overlaySpec.preTrimTopFraction,
                preTrimWidthFraction = overlaySpec.preTrimWidthFraction,
                preTrimHeightFraction = overlaySpec.preTrimHeightFraction
            )?.asImageBitmap()
        }
    }.value
}

@Composable
private fun rememberPlatformBadgeBitmap(
    showBadge: Boolean,
    platform: String?
): ImageBitmap? {
    val resources = LocalContext.current.resources
    val svg = remember(resources, platform) { PlatformSvgIcons.svgForPlatform(resources, platform) }
    val normalizedSvg = remember(platform, svg) { svg?.let { normalizeBadgeSvgColors(platform, it) } }
    val cacheKey = remember(platform, normalizedSvg) {
        platform?.takeIf(String::isNotBlank)?.lowercase()?.let { normalizedPlatform ->
            "$normalizedPlatform:${normalizedSvg?.hashCode() ?: 0}"
        }
    }
    return produceState<ImageBitmap?>(initialValue = cacheKey?.let(platformBadgeBitmapCache::get), showBadge, cacheKey, normalizedSvg) {
        if (!showBadge || normalizedSvg.isNullOrBlank()) {
            value = null
            return@produceState
        }
        cacheKey?.let(platformBadgeBitmapCache::get)?.let { cached ->
            value = cached
            return@produceState
        }
        if (platform.equals("Nintendo Wii", ignoreCase = true)) {
            android.util.Log.d(
                "RetroLauncher",
                "Wii badge render start svgHash=${normalizedSvg.hashCode()} len=${normalizedSvg.length}"
            )
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val iconSize = 132
                val picture = SVG.getFromString(normalizedSvg).renderToPicture()
                if (platform.equals("Nintendo Wii", ignoreCase = true)) {
                    android.util.Log.d(
                        "RetroLauncher",
                        "Wii badge render parsed width=${picture.width} height=${picture.height}"
                    )
                }
                val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.TRANSPARENT)
                val sourceWidth = picture.width.takeIf { it > 0 }?.toFloat() ?: iconSize.toFloat()
                val sourceHeight = picture.height.takeIf { it > 0 }?.toFloat() ?: iconSize.toFloat()
                val scale = min(iconSize / sourceWidth, iconSize / sourceHeight) * platformBadgeScale(platform)
                val offsetX = (iconSize - (sourceWidth * scale)) / 2f
                val offsetY = (iconSize - (sourceHeight * scale)) / 2f
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)
                canvas.drawPicture(picture)
                canvas.restore()
                bitmap.asImageBitmap().also { rendered ->
                    cacheKey?.let { key -> platformBadgeBitmapCache[key] = rendered }
                }
            }.onFailure {
                if (platform.equals("Nintendo Wii", ignoreCase = true)) {
                    android.util.Log.e("RetroLauncher", "Wii badge render failed", it)
                }
            }.getOrNull()
        }
    }.value
}

private fun platformBadgeScale(platform: String?): Float = when {
    platform.equals("Nintendo 3DS", ignoreCase = true) -> 0.8f
    platform.equals("Nintendo GameCube", ignoreCase = true) -> 0.8f
    platform.equals("Sony PlayStation Portable", ignoreCase = true) -> 0.9f
    else -> 1f
}

@Composable
private fun rememberSameNostalgicLayoutMetrics(
    games: List<GameEntry>,
    filterOverlayPlatform: String?,
    enabled: Boolean
): SameNostalgicLayoutMetrics {
    val context = LocalContext.current
    return produceState(
        initialValue = SameNostalgicLayoutMetrics(
            targetAspectRatio = NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO,
            occupiedWidthFraction = 0.62f
        ),
        games,
        filterOverlayPlatform,
        enabled
    ) {
        if (!enabled || games.isEmpty()) {
            value = SameNostalgicLayoutMetrics(
                targetAspectRatio = NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO,
                occupiedWidthFraction = 0.62f
            )
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            resolveSameNostalgicLayoutMetrics(
                context = context,
                games = games,
                filterOverlayPlatform = filterOverlayPlatform
            )
        }
    }.value
}

private fun resolveSameNostalgicLayoutMetrics(
    context: Context,
    games: List<GameEntry>,
    filterOverlayPlatform: String?
): SameNostalgicLayoutMetrics {
    val availableAssets = runCatching {
        context.assets.list("overlay_mame")
            ?.filterNot { it.contains("Zone.Identifier", ignoreCase = true) }
            ?.associateBy { it.lowercase() }
            .orEmpty()
    }.getOrDefault(emptyMap())
    val resolvedMetrics = games.asSequence()
        .mapNotNull { game ->
            when {
                supportsMameCabinetNostalgia(game.platform) -> {
                    val aspectRatio = resolveSameNostalgicMameAspectRatio(
                        context = context,
                        game = game,
                        availableAssets = availableAssets
                    ) ?: return@mapNotNull null
                    SameNostalgicLayoutMetrics(
                        targetAspectRatio = aspectRatio,
                        occupiedWidthFraction = resolveSameNostalgicMameOccupiedWidthFraction(
                            context = context,
                            game = game,
                            availableAssets = availableAssets
                        ) ?: 1f
                    )
                }

                else ->
                    NostalgicOverlays.specForPlatform(filterOverlayPlatform ?: game.platform)
                        ?.let { overlaySpec ->
                            SameNostalgicLayoutMetrics(
                                targetAspectRatio = resolveSameNostalgicOverlayAspectRatio(
                                    resources = context.resources,
                                    overlaySpec = overlaySpec
                                ),
                                occupiedWidthFraction = resolveSameNostalgicOverlayOccupiedWidthFraction(
                                    resources = context.resources,
                                    overlaySpec = overlaySpec
                                )
                            )
                        }
            }
        }
        .toList()
    return SameNostalgicLayoutMetrics(
        targetAspectRatio = max(
            NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO,
            resolvedMetrics.maxOfOrNull { it.targetAspectRatio } ?: NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO
        ),
        occupiedWidthFraction = resolvedMetrics.maxOfOrNull { it.occupiedWidthFraction }?.coerceIn(0.35f, 1f) ?: 0.62f
    )
}

private fun resolveSameNostalgicOverlayAspectRatio(
    resources: Resources,
    overlaySpec: NostalgicOverlaySpec
): Float {
    sameNostalgicOverlayAspectRatioCache[overlaySpec.drawableResourceId]?.let { return it }
    val resolved = runCatching {
        BitmapFactory.decodeResource(
            resources,
            overlaySpec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false }
        )?.let { source ->
            val recoloredOverlay = recolorNostalgicOverlayBitmap(source, APP_BACKGROUND_COLOR_ARGB)
            val renderedOverlay = cropBitmapToVisibleOverlayBounds(recoloredOverlay).bitmap
            renderedOverlay.width.toFloat() / renderedOverlay.height.toFloat()
        }
    }.getOrNull() ?: overlaySpec.aspectRatio
    sameNostalgicOverlayAspectRatioCache[overlaySpec.drawableResourceId] = resolved
    return resolved
}

private fun resolveSameNostalgicOverlayOccupiedWidthFraction(
    resources: Resources,
    overlaySpec: NostalgicOverlaySpec
): Float {
    sameNostalgicOverlayOccupiedWidthCache[overlaySpec.drawableResourceId]?.let { return it }
    val resolved = runCatching {
        BitmapFactory.decodeResource(
            resources,
            overlaySpec.drawableResourceId,
            BitmapFactory.Options().apply { inScaled = false }
        )?.let { source ->
            val recoloredOverlay = recolorNostalgicOverlayBitmap(source, APP_BACKGROUND_COLOR_ARGB)
            val renderedOverlay = cropBitmapToVisibleOverlayBounds(recoloredOverlay).bitmap
            resolveRenderedOverlayOccupiedWidthFraction(renderedOverlay)
        }
    }.getOrNull() ?: 0.62f
    sameNostalgicOverlayOccupiedWidthCache[overlaySpec.drawableResourceId] = resolved
    return resolved
}

private fun resolveSameNostalgicMameAspectRatio(
    context: Context,
    game: GameEntry,
    availableAssets: Map<String, String>
): Float? {
    val assetName = resolveMameCabinetAssetName(context, game, availableAssets)
    val fallbackAssetName = resolveMameFallbackCabinetAssetName(game.platform, availableAssets)
    val selectedAssetName = assetName?.takeIf { it.isNotBlank() }
        ?: fallbackAssetName?.takeIf { it.isNotBlank() }
        ?: return null
    sameNostalgicMameAspectRatioCache[selectedAssetName]?.let { return it }
    val renderedBitmap = when {
        !assetName.isNullOrBlank() -> {
            buildMameFallbackCabinetBitmap(
                context = context,
                assetName = assetName,
                artworkUriString = null,
                backgroundColor = APP_BACKGROUND_COLOR_ARGB,
                drawScreenBackdrop = false
            )
        }

        !fallbackAssetName.isNullOrBlank() -> {
            buildMameFallbackCabinetBitmap(
                context = context,
                assetName = fallbackAssetName,
                artworkUriString = null,
                backgroundColor = APP_BACKGROUND_COLOR_ARGB
            )
        }

        else -> null
    } ?: return null
    val aspectRatio = cropBitmapToVisibleOverlayBounds(renderedBitmap).bitmap.run {
        width.toFloat() / height.toFloat()
    }
    sameNostalgicMameAspectRatioCache[selectedAssetName] = aspectRatio
    return aspectRatio
}

private fun resolveSameNostalgicMameOccupiedWidthFraction(
    context: Context,
    game: GameEntry,
    availableAssets: Map<String, String>
): Float? {
    val assetName = resolveMameCabinetAssetName(context, game, availableAssets)
    val fallbackAssetName = resolveMameFallbackCabinetAssetName(game.platform, availableAssets)
    val selectedAssetName = assetName?.takeIf { it.isNotBlank() }
        ?: fallbackAssetName?.takeIf { it.isNotBlank() }
        ?: return null
    sameNostalgicMameOccupiedWidthCache[selectedAssetName]?.let { return it }
    val renderedBitmap = when {
        !assetName.isNullOrBlank() -> {
            buildMameFallbackCabinetBitmap(
                context = context,
                assetName = assetName,
                artworkUriString = null,
                backgroundColor = APP_BACKGROUND_COLOR_ARGB,
                drawScreenBackdrop = false
            )
        }

        !fallbackAssetName.isNullOrBlank() -> {
            buildMameFallbackCabinetBitmap(
                context = context,
                assetName = fallbackAssetName,
                artworkUriString = null,
                backgroundColor = APP_BACKGROUND_COLOR_ARGB
            )
        }

        else -> null
    } ?: return null
    val occupiedWidthFraction = resolveRenderedOverlayOccupiedWidthFraction(
        cropBitmapToVisibleOverlayBounds(renderedBitmap).bitmap
    )
    sameNostalgicMameOccupiedWidthCache[selectedAssetName] = occupiedWidthFraction
    return occupiedWidthFraction
}

private fun resolveRenderedOverlayOccupiedWidthFraction(renderedBitmap: Bitmap): Float =
    findNostalgicOverlayOccupiedBounds(renderedBitmap)
        ?.width
        ?.toFloat()
        ?.div(renderedBitmap.width.toFloat().coerceAtLeast(1f))
        ?.coerceIn(0.35f, 1f)
        ?: 1f

@Composable
private fun rememberMameNostalgicCabinetRenderData(
    game: GameEntry,
    normalizeCanvasSize: Boolean = false,
    glassEffectEnabled: Boolean = false
): MameCabinetRenderData? {
    val context = LocalContext.current
    val availableAssets = remember(context) {
        runCatching {
            context.assets.list("overlay_mame")
                ?.filterNot { it.contains("Zone.Identifier", ignoreCase = true) }
                ?.associateBy { it.lowercase() }
                .orEmpty()
        }.getOrDefault(emptyMap())
    }
    val assetName = remember(game.platform, game.intentDataUri, availableAssets) {
        resolveMameCabinetAssetName(context, game, availableAssets)
    }
    val fallbackAssetName = remember(game.platform, availableAssets) {
        resolveMameFallbackCabinetAssetName(game.platform, availableAssets)
    }
    return produceState<MameCabinetRenderData?>(
        initialValue = null,
        assetName,
        fallbackAssetName,
        game.imageUri,
        normalizeCanvasSize,
        glassEffectEnabled
    ) {
        if (assetName.isNullOrBlank() && fallbackAssetName.isNullOrBlank()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    !assetName.isNullOrBlank() -> {
                        buildMameFallbackCabinetBitmap(
                            context = context,
                            assetName = assetName,
                            artworkUriString = null,
                            backgroundColor = APP_BACKGROUND_COLOR_ARGB,
                            drawScreenBackdrop = false,
                            glassEffectEnabled = glassEffectEnabled
                        )
                    }

                    !fallbackAssetName.isNullOrBlank() -> {
                        buildMameFallbackCabinetBitmap(
                            context = context,
                            assetName = fallbackAssetName,
                            artworkUriString = game.imageUri,
                            backgroundColor = APP_BACKGROUND_COLOR_ARGB,
                            glassEffectEnabled = glassEffectEnabled
                        )
                    }

                    else -> null
                }
                    ?.let { renderedBitmap ->
                        val displayBitmap = if (normalizeCanvasSize) {
                            cropBitmapToVisibleOverlayBounds(renderedBitmap).bitmap
                        } else {
                            renderedBitmap
                        }
                        MameCabinetRenderData(
                            bitmap = displayBitmap.asImageBitmap(),
                            aspectRatio = displayBitmap.width.toFloat() / displayBitmap.height.toFloat(),
                            occupiedWidthFraction = resolveRenderedOverlayOccupiedWidthFraction(displayBitmap)
                        )
                    }
            }.getOrNull()
        }
    }.value
}

@Composable
private fun rememberNostalgicOverlayRenderData(
    overlaySpec: NostalgicOverlaySpec,
    normalizeCanvasSize: Boolean = false
): NostalgicOverlayRenderData? {
    val resources = LocalContext.current.resources
    val cacheKey = "${overlaySpec.drawableResourceId}:$normalizeCanvasSize"
    return produceState<NostalgicOverlayRenderData?>(initialValue = nostalgicOverlayRenderCache[cacheKey], cacheKey, overlaySpec) {
        nostalgicOverlayRenderCache[cacheKey]?.let { cached ->
            value = cached
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeResource(
                    resources,
                    overlaySpec.drawableResourceId,
                    BitmapFactory.Options().apply { inScaled = false }
                )?.let { source ->
                    val transparentBounds = findNostalgicArtworkWindowBounds(source)
                    val baseArtworkPlacement = detectNostalgicArtworkPlacement(source, overlaySpec, transparentBounds)
                        ?: overlaySpec.defaultArtworkPlacement()
                    val recoloredOverlay = recolorNostalgicOverlayBitmap(source, APP_BACKGROUND_COLOR_ARGB)
                    val renderedOverlay = if (normalizeCanvasSize) {
                        cropBitmapToVisibleOverlayBounds(
                            source = recoloredOverlay
                        )
                    } else {
                        RenderedBitmapData(
                            bitmap = recoloredOverlay,
                            offsetX = 0,
                            offsetY = 0
                        )
                    }
                    NostalgicOverlayRenderData(
                        overlayBitmap = renderedOverlay.bitmap.asImageBitmap(),
                        aspectRatio = renderedOverlay.bitmap.width.toFloat() / renderedOverlay.bitmap.height.toFloat(),
                        occupiedWidthFraction = resolveRenderedOverlayOccupiedWidthFraction(renderedOverlay.bitmap),
                        artworkPlacement = baseArtworkPlacement.adjustForRenderedCanvas(
                            sourceWidth = source.width,
                            sourceHeight = source.height,
                            renderedWidth = renderedOverlay.bitmap.width,
                            renderedHeight = renderedOverlay.bitmap.height,
                            offsetX = renderedOverlay.offsetX,
                            offsetY = renderedOverlay.offsetY
                        ),
                        artworkMaskBitmap = if (overlaySpec.clipArtworkToTransparencyMask) {
                            transparentBounds?.let { bounds ->
                                buildNostalgicArtworkMaskBitmap(source, bounds).asImageBitmap()
                            }
                        } else {
                            null
                        }
                    ).also { decoded -> nostalgicOverlayRenderCache[cacheKey] = decoded }
                }
            }.getOrNull()
        }
    }.value
}

private fun NostalgicOverlaySpec.defaultArtworkPlacement(): NostalgicArtworkPlacement =
    NostalgicArtworkPlacement(
        leftFraction = artworkLeftFraction,
        topFraction = artworkTopFraction,
        widthFraction = artworkWidthFraction,
        heightFraction = artworkHeightFraction,
        cornerRadiusFraction = artworkCornerRadiusFraction
    )

private fun NostalgicArtworkPlacement.adjustForRenderedCanvas(
    sourceWidth: Int,
    sourceHeight: Int,
    renderedWidth: Int,
    renderedHeight: Int,
    offsetX: Int,
    offsetY: Int
): NostalgicArtworkPlacement {
    val safeSourceWidth = sourceWidth.toFloat().coerceAtLeast(1f)
    val safeSourceHeight = sourceHeight.toFloat().coerceAtLeast(1f)
    val safeRenderedWidth = renderedWidth.toFloat().coerceAtLeast(1f)
    val safeRenderedHeight = renderedHeight.toFloat().coerceAtLeast(1f)
    val left = offsetX + (leftFraction * safeSourceWidth)
    val top = offsetY + (topFraction * safeSourceHeight)
    val width = widthFraction * safeSourceWidth
    val height = heightFraction * safeSourceHeight
    val cornerRadius = cornerRadiusFraction * safeSourceWidth
    return NostalgicArtworkPlacement(
        leftFraction = left / safeRenderedWidth,
        topFraction = top / safeRenderedHeight,
        widthFraction = width / safeRenderedWidth,
        heightFraction = height / safeRenderedHeight,
        cornerRadiusFraction = cornerRadius / safeRenderedWidth
    )
}

private fun cropBitmapToVisibleOverlayBounds(source: Bitmap): RenderedBitmapData {
    val visibleBounds = findVisibleOverlayBounds(source)
        ?: return RenderedBitmapData(
            bitmap = source,
            offsetX = 0,
            offsetY = 0
        )
    val croppedBitmap = Bitmap.createBitmap(
        source,
        visibleBounds.left,
        visibleBounds.top,
        visibleBounds.width.coerceAtLeast(1),
        visibleBounds.height.coerceAtLeast(1)
    )
    return RenderedBitmapData(
        bitmap = croppedBitmap,
        offsetX = -visibleBounds.left,
        offsetY = -visibleBounds.top
    )
}

private fun detectNostalgicArtworkPlacement(
    overlayBitmap: Bitmap,
    overlaySpec: NostalgicOverlaySpec,
    transparentBounds: TransparentRegionBounds? = findNostalgicArtworkWindowBounds(overlayBitmap)
): NostalgicArtworkPlacement? {
    if (!overlaySpec.detectArtworkPlacementFromTransparency) {
        return null
    }
    transparentBounds ?: return null
    val width = overlayBitmap.width.toFloat().coerceAtLeast(1f)
    val height = overlayBitmap.height.toFloat().coerceAtLeast(1f)
    return NostalgicArtworkPlacement(
        leftFraction = transparentBounds.left / width,
        topFraction = transparentBounds.top / height,
        widthFraction = transparentBounds.width / width,
        heightFraction = transparentBounds.height / height,
        cornerRadiusFraction = overlaySpec.artworkCornerRadiusFraction
    )
}

private fun findNostalgicArtworkWindowBounds(overlayBitmap: Bitmap): TransparentRegionBounds? =
    findLargestTransparentRegion(overlayBitmap)
        ?.takeIf { it.pixelCount >= MIN_NOSTALGIC_ARTWORK_WINDOW_PIXELS }

private fun findVisibleOverlayBounds(bitmap: Bitmap): TransparentRegionBounds? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var maxY = Int.MIN_VALUE
    var pixelCount = 0

    pixels.forEachIndexed { index, pixel ->
        if (android.graphics.Color.alpha(pixel) <= NOSTALGIC_OVERLAY_TRANSPARENT_ALPHA_THRESHOLD) {
            return@forEachIndexed
        }
        val x = index % width
        val y = index / width
        pixelCount += 1
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }

    if (pixelCount == 0) return null
    return TransparentRegionBounds(
        left = minX,
        top = minY,
        right = maxX + 1,
        bottom = maxY + 1,
        pixelCount = pixelCount,
        touchesEdge = minX <= 0 || minY <= 0 || maxX >= width - 1 || maxY >= height - 1
    )
}

private fun findNostalgicOverlayOccupiedBounds(bitmap: Bitmap): TransparentRegionBounds? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var maxY = Int.MIN_VALUE
    var pixelCount = 0

    pixels.forEachIndexed { index, pixel ->
        if (android.graphics.Color.alpha(pixel) <= NOSTALGIC_OVERLAY_TRANSPARENT_ALPHA_THRESHOLD) {
            return@forEachIndexed
        }
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        if (
            red <= NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD &&
            green <= NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD &&
            blue <= NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD
        ) {
            return@forEachIndexed
        }
        val x = index % width
        val y = index / width
        pixelCount += 1
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }

    if (pixelCount == 0) return null
    return TransparentRegionBounds(
        left = minX,
        top = minY,
        right = maxX + 1,
        bottom = maxY + 1,
        pixelCount = pixelCount,
        touchesEdge = minX <= 0 || minY <= 0 || maxX >= width - 1 || maxY >= height - 1
    )
}

private fun buildNostalgicArtworkMaskBitmap(
    overlayBitmap: Bitmap,
    bounds: TransparentRegionBounds
): Bitmap {
    val width = bounds.width.coerceAtLeast(1)
    val height = bounds.height.coerceAtLeast(1)
    val maskPixels = IntArray(width * height)
    val overlayPixels = IntArray(overlayBitmap.width * overlayBitmap.height)
    overlayBitmap.getPixels(overlayPixels, 0, overlayBitmap.width, 0, 0, overlayBitmap.width, overlayBitmap.height)
    for (y in 0 until height) {
        val sourceRowStart = (bounds.top + y) * overlayBitmap.width
        val maskRowStart = y * width
        for (x in 0 until width) {
            val sourcePixel = overlayPixels[sourceRowStart + bounds.left + x]
            maskPixels[maskRowStart + x] = if (isTransparentArtworkPixel(sourcePixel)) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.TRANSPARENT
            }
        }
    }
    return Bitmap.createBitmap(maskPixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun findLargestTransparentRegion(bitmap: Bitmap): TransparentRegionBounds? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null

    val totalPixels = width * height
    val pixels = IntArray(totalPixels)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val visited = BooleanArray(totalPixels)
    val queue = IntArray(totalPixels)
    var bestRegion: TransparentRegionBounds? = null

    for (index in pixels.indices) {
        if (visited[index] || !isTransparentArtworkPixel(pixels[index])) {
            continue
        }

        var queueStart = 0
        var queueEnd = 0
        queue[queueEnd++] = index
        visited[index] = true

        var pixelCount = 0
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var touchesEdge = false

        while (queueStart < queueEnd) {
            val current = queue[queueStart++]
            val x = current % width
            val y = current / width

            pixelCount += 1
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                touchesEdge = true
            }

            if (x > 0) {
                val left = current - 1
                if (!visited[left] && isTransparentArtworkPixel(pixels[left])) {
                    visited[left] = true
                    queue[queueEnd++] = left
                }
            }
            if (x < width - 1) {
                val right = current + 1
                if (!visited[right] && isTransparentArtworkPixel(pixels[right])) {
                    visited[right] = true
                    queue[queueEnd++] = right
                }
            }
            if (y > 0) {
                val up = current - width
                if (!visited[up] && isTransparentArtworkPixel(pixels[up])) {
                    visited[up] = true
                    queue[queueEnd++] = up
                }
            }
            if (y < height - 1) {
                val down = current + width
                if (!visited[down] && isTransparentArtworkPixel(pixels[down])) {
                    visited[down] = true
                    queue[queueEnd++] = down
                }
            }
        }

        val region = TransparentRegionBounds(
            left = minX,
            top = minY,
            right = maxX + 1,
            bottom = maxY + 1,
            pixelCount = pixelCount,
            touchesEdge = touchesEdge
        )
        if (bestRegion == null || region.pixelCount > bestRegion.pixelCount) {
            bestRegion = region
        }
    }

    return bestRegion
}

private fun isTransparentArtworkPixel(pixel: Int): Boolean =
    android.graphics.Color.alpha(pixel) <= NOSTALGIC_OVERLAY_TRANSPARENT_ALPHA_THRESHOLD

private fun shouldTreatDarkBaseColorAsTransparent(baseColor: Int): Boolean {
    val red = android.graphics.Color.red(baseColor)
    val green = android.graphics.Color.green(baseColor)
    val blue = android.graphics.Color.blue(baseColor)
    return red <= NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD &&
        green <= NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD &&
        blue <= NOSTALGIC_OVERLAY_DARK_BASE_COLOR_THRESHOLD
}

private fun recolorNostalgicOverlayBitmap(source: Bitmap, baseColor: Int): Bitmap {
    val width = source.width
    val height = source.height
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)

    val recolored = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val recoloredPixels = IntArray(sourcePixels.size)
    val baseRed = android.graphics.Color.red(baseColor)
    val baseGreen = android.graphics.Color.green(baseColor)
    val baseBlue = android.graphics.Color.blue(baseColor)
    val treatBlackPixelsAsTransparent = shouldTreatDarkBaseColorAsTransparent(baseColor)

    sourcePixels.forEachIndexed { index, pixel ->
        val alpha = android.graphics.Color.alpha(pixel)
        if (alpha == 0) {
            recoloredPixels[index] = pixel
            return@forEachIndexed
        }

        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        recoloredPixels[index] = if (red == 0 && green == 0 && blue == 0) {
            if (treatBlackPixelsAsTransparent) {
                android.graphics.Color.TRANSPARENT
            } else {
                android.graphics.Color.argb(alpha, baseRed, baseGreen, baseBlue)
            }
        } else {
            pixel
        }
    }

    recolored.setPixels(recoloredPixels, 0, width, 0, 0, width, height)
    return recolored
}

private fun applyVintageArtworkBitmap(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)

    val treated = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val treatedPixels = IntArray(sourcePixels.size)
    val centerX = (width - 1) / 2f
    val centerY = (height - 1) / 2f
    val maxDistance = sqrt((centerX * centerX) + (centerY * centerY)).coerceAtLeast(1f)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            val pixel = sourcePixels[index]
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha == 0) {
                treatedPixels[index] = pixel
                continue
            }

            val red = android.graphics.Color.red(pixel).toFloat()
            val green = android.graphics.Color.green(pixel).toFloat()
            val blue = android.graphics.Color.blue(pixel).toFloat()
            val luma = (0.299f * red) + (0.587f * green) + (0.114f * blue)

            var vintageRed = luma + ((red - luma) * 0.35f)
            var vintageGreen = luma + ((green - luma) * 0.35f)
            var vintageBlue = luma + ((blue - luma) * 0.35f)

            vintageRed = (vintageRed * 0.8f) + (228f * 0.2f)
            vintageGreen = (vintageGreen * 0.8f) + (204f * 0.2f)
            vintageBlue = (vintageBlue * 0.8f) + (170f * 0.2f)

            vintageRed = ((vintageRed - 128f) * 0.92f) + 136f
            vintageGreen = ((vintageGreen - 128f) * 0.92f) + 134f
            vintageBlue = ((vintageBlue - 128f) * 0.92f) + 128f

            val dx = (x - centerX) / maxDistance
            val dy = (y - centerY) / maxDistance
            val vignette = 1f - (0.08f * ((dx * dx) + (dy * dy)))
            val grain = (((x * 37) + (y * 17) + (width * 11)) and 15) - 7

            treatedPixels[index] = android.graphics.Color.argb(
                alpha,
                vintageChannel(vintageRed, vignette, grain.toFloat()),
                vintageChannel(vintageGreen, vignette, grain * 0.8f),
                vintageChannel(vintageBlue, vignette, grain * 0.6f)
            )
        }
    }

    treated.setPixels(treatedPixels, 0, width, 0, 0, width, height)
    return treated
}

private fun resolveMameCabinetAssetName(
    context: Context,
    game: GameEntry,
    availableAssets: Map<String, String>
): String? {
    if (!supportsMameCabinetNostalgia(game.platform) || availableAssets.isEmpty()) {
        return null
    }
    val assetBaseNames = linkedSetOf<String>().apply {
        addAll(game.arcadeOverlayAssetBaseNames())
        extractArcadeRomShortName(context, game.intentDataUri)?.let(::add)
    }
    if (assetBaseNames.isEmpty()) return null
    return findMatchingArcadeOverlayAssetName(availableAssets, assetBaseNames)
        ?.takeUnless(::isGenericArcadeFallbackAssetName)
}

private fun resolveMameFallbackCabinetAssetName(
    platform: String?,
    availableAssets: Map<String, String>
): String? {
    if (!supportsMameCabinetNostalgia(platform) || availableAssets.isEmpty()) {
        return null
    }
    return listOf("mame.png", "mame.jpg", "mame.jpeg")
        .firstNotNullOfOrNull { candidate -> availableAssets[candidate] }
}

private fun supportsMameCabinetNostalgia(platform: String?): Boolean =
    platform.equals("MAME Arcade", ignoreCase = true) ||
        platform.equals("Final Burn Neo", ignoreCase = true)

private fun extractArcadeRomShortName(context: Context, uriString: String?): String? {
    val raw = uriString?.takeIf { it.isNotBlank() } ?: return null
    val parsed = runCatching { Uri.parse(raw) }.getOrNull()
    val displayName = parsed?.let { uri ->
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
        }.getOrNull()
    }
    val decoded = runCatching { URLDecoder.decode(raw, Charsets.UTF_8.name()) }.getOrDefault(raw)
    val fallback = parsed?.lastPathSegment?.substringAfterLast('/') ?: decoded.substringAfterLast('/')
    val fileName = (displayName ?: fallback)
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?.substringAfterLast('/')
        ?: return null
    return fileName
        .substringBeforeLast('.', missingDelimiterValue = fileName)
        .lowercase()
        .takeIf { it.isNotBlank() }
}

private fun buildMameFallbackCabinetBitmap(
    context: Context,
    assetName: String,
    artworkUriString: String?,
    backgroundColor: Int,
    drawScreenBackdrop: Boolean = true,
    glassEffectEnabled: Boolean = false
): Bitmap? {
    val overlay = context.assets.open("overlay_mame/$assetName").use { stream ->
        BitmapFactory.decodeStream(stream)
    } ?: return null
    val screenRegion = resolveMameFallbackScreenRegion(assetName, overlay)
        ?: return normalizeMameCabinetBitmap(overlay, backgroundColor)
    val screenBounds = screenRegion.bounds
    val artwork = artworkUriString
        ?.takeIf { it.isNotBlank() }
        ?.let { uriString ->
            ensureArtworkBitmapCached(
                contentResolver = context.contentResolver,
                uriString = uriString,
                applyVintageArtwork = true
            )
        }

    val overlayToDraw = if (screenRegion.clearOverlayWindow) {
        clearBitmapRect(overlay, screenBounds)
    } else {
        overlay
    }
    val foreground = extractMameCabinetForeground(overlayToDraw)
        ?: return normalizeMameCabinetBitmap(overlayToDraw, backgroundColor)
    val layout = calculateMameCabinetLayout(foreground.bitmap.width, foreground.bitmap.height)
    val normalizedScreenBounds = android.graphics.RectF(
        (screenBounds.left - foreground.sourceBounds.left).toFloat().coerceIn(0f, foreground.bitmap.width.toFloat()),
        (screenBounds.top - foreground.sourceBounds.top).toFloat().coerceIn(0f, foreground.bitmap.height.toFloat()),
        (screenBounds.right - foreground.sourceBounds.left).toFloat().coerceIn(0f, foreground.bitmap.width.toFloat()),
        (screenBounds.bottom - foreground.sourceBounds.top).toFloat().coerceIn(0f, foreground.bitmap.height.toFloat())
    )
    val targetScreenBounds = android.graphics.RectF(
        layout.drawLeft + (normalizedScreenBounds.left * layout.scale),
        layout.drawTop + (normalizedScreenBounds.top * layout.scale),
        layout.drawLeft + (normalizedScreenBounds.right * layout.scale),
        layout.drawTop + (normalizedScreenBounds.bottom * layout.scale)
    )

    val composed = Bitmap.createBitmap(MAME_CABINET_CANVAS_WIDTH, MAME_CABINET_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(composed)
    val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG or
            android.graphics.Paint.FILTER_BITMAP_FLAG or
            android.graphics.Paint.DITHER_FLAG
    )

    if (!shouldTreatDarkBaseColorAsTransparent(backgroundColor)) {
        canvas.drawColor(backgroundColor)
    }
    if (drawScreenBackdrop) {
        canvas.drawRect(
            targetScreenBounds.left,
            targetScreenBounds.top,
            targetScreenBounds.right,
            targetScreenBounds.bottom,
            android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
        )
    }
    if (artwork != null) {
        canvas.drawBitmap(
            artwork,
            coverSourceRect(
                sourceWidth = artwork.width,
                sourceHeight = artwork.height,
                targetWidth = targetScreenBounds.width().roundToInt(),
                targetHeight = targetScreenBounds.height().roundToInt()
            ),
            targetScreenBounds,
            paint
        )
    }
    if (glassEffectEnabled) {
        drawGlassPane(
            canvas = canvas,
            bounds = targetScreenBounds,
            preserveTransparentBase = artwork == null && !drawScreenBackdrop
        )
    }
    canvas.drawBitmap(
        foreground.bitmap,
        null,
        android.graphics.RectF(
            layout.drawLeft,
            layout.drawTop,
            layout.drawLeft + layout.drawWidth,
            layout.drawTop + layout.drawHeight
        ),
        paint
    )
    return composed
}

private fun drawGlassPane(
    canvas: Canvas,
    bounds: android.graphics.RectF,
    preserveTransparentBase: Boolean = false
) {
    if (bounds.width() <= 0f || bounds.height() <= 0f) return

    val rimThickness = (min(bounds.width(), bounds.height()) * 0.014f).coerceAtLeast(1.5f)
    val topGlowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(
            bounds.left,
            bounds.top,
            bounds.left,
            bounds.bottom,
            intArrayOf(
                android.graphics.Color.argb(52, 255, 255, 255),
                android.graphics.Color.argb(20, 255, 255, 255),
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.38f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    val diagonalHighlightPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(
            bounds.left + (bounds.width() * 0.04f),
            bounds.top,
            bounds.left + (bounds.width() * 0.92f),
            bounds.bottom,
            intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(56, 255, 255, 255),
                android.graphics.Color.argb(16, 255, 255, 255),
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.36f, 0.58f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    val bottomShadePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(
            bounds.left,
            bounds.top + (bounds.height() * 0.58f),
            bounds.left,
            bounds.bottom,
            intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(18, 0, 0, 0),
                android.graphics.Color.argb(38, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    val topRimPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(46, 255, 255, 255)
    }
    val bottomRimPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(15, 255, 255, 255)
    }

    if (!preserveTransparentBase) {
        canvas.drawRect(bounds, bottomShadePaint)
    }
    canvas.drawRect(bounds, topGlowPaint)
    canvas.drawRect(bounds, diagonalHighlightPaint)
    canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + rimThickness, topRimPaint)
    canvas.drawRect(bounds.left, bounds.bottom - rimThickness, bounds.right, bounds.bottom, bottomRimPaint)
}

private fun resolveMameFallbackScreenRegion(
    assetName: String,
    source: Bitmap
): MameFallbackScreenRegion? =
    findTransparentMameScreenBounds(source)?.let { bounds ->
        MameFallbackScreenRegion(bounds = bounds, clearOverlayWindow = false)
    } ?: defaultMameFallbackScreenRegion(assetName, source.width, source.height)

private fun findTransparentMameScreenBounds(source: Bitmap): android.graphics.Rect? {
    val transparentBounds = findLargestTransparentRegion(source)
        ?.takeIf {
            it.pixelCount >= MIN_NOSTALGIC_ARTWORK_WINDOW_PIXELS &&
                !it.touchesEdge
        }
        ?: return null
    return android.graphics.Rect(
        transparentBounds.left,
        transparentBounds.top,
        transparentBounds.right,
        transparentBounds.bottom
    )
}

private fun defaultMameFallbackScreenRegion(
    assetName: String,
    width: Int,
    height: Int
): MameFallbackScreenRegion? {
    val assetStem = assetName.substringBeforeLast('.', assetName).lowercase()
    if (assetStem != "mame") return null

    val left = (width * (190f / 1073f)).roundToInt().coerceIn(0, width)
    val top = (height * (532f / 1536f)).roundToInt().coerceIn(0, height)
    val right = (width * (795f / 1073f)).roundToInt().coerceIn(left + 1, width)
    val bottom = (height * (991f / 1536f)).roundToInt().coerceIn(top + 1, height)

    return MameFallbackScreenRegion(
        bounds = android.graphics.Rect(left, top, right, bottom),
        clearOverlayWindow = true
    )
}

private fun clearBitmapRect(source: Bitmap, bounds: android.graphics.Rect): Bitmap {
    val cleared = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
    val width = cleared.width
    val height = cleared.height
    val safeLeft = bounds.left.coerceIn(0, width)
    val safeTop = bounds.top.coerceIn(0, height)
    val safeRight = bounds.right.coerceIn(safeLeft, width)
    val safeBottom = bounds.bottom.coerceIn(safeTop, height)
    if (safeLeft >= safeRight || safeTop >= safeBottom) {
        return cleared
    }

    val rowPixels = IntArray(safeRight - safeLeft) { android.graphics.Color.TRANSPARENT }
    for (y in safeTop until safeBottom) {
        cleared.setPixels(rowPixels, 0, rowPixels.size, safeLeft, y, rowPixels.size, 1)
    }
    return cleared
}

private fun extractMameCabinetForeground(source: Bitmap): MameCabinetForeground? {
    val width = source.width
    val height = source.height
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)

    var left = width
    var top = height
    var right = -1
    var bottom = -1
    sourcePixels.forEachIndexed { index, pixel ->
        if (isMameCabinetBackgroundPixel(pixel)) return@forEachIndexed
        val x = index % width
        val y = index / width
        if (x < left) left = x
        if (x > right) right = x
        if (y < top) top = y
        if (y > bottom) bottom = y
    }

    if (right < left || bottom < top) {
        return null
    }

    val objectWidth = right - left + 1
    val objectHeight = bottom - top + 1
    val cleaned = Bitmap.createBitmap(objectWidth, objectHeight, Bitmap.Config.ARGB_8888)
    val cleanedPixels = IntArray(objectWidth * objectHeight)

    for (y in 0 until objectHeight) {
        for (x in 0 until objectWidth) {
            val pixel = sourcePixels[((top + y) * width) + left + x]
            cleanedPixels[(y * objectWidth) + x] = if (isMameCabinetBackgroundPixel(pixel)) {
                android.graphics.Color.TRANSPARENT
            } else {
                pixel
            }
        }
    }
    cleaned.setPixels(cleanedPixels, 0, objectWidth, 0, 0, objectWidth, objectHeight)
    return MameCabinetForeground(
        bitmap = cleaned,
        sourceBounds = android.graphics.Rect(left, top, right + 1, bottom + 1)
    )
}

private fun calculateMameCabinetLayout(objectWidth: Int, objectHeight: Int): MameCabinetLayout {
    val scale = min(
        MAME_CABINET_TARGET_MAX_WIDTH / objectWidth.toFloat(),
        MAME_CABINET_TARGET_MAX_HEIGHT / objectHeight.toFloat()
    )
    val drawWidth = objectWidth * scale
    val drawHeight = objectHeight * scale
    val drawLeft = (MAME_CABINET_CANVAS_WIDTH - drawWidth) / 2f
    val drawTop = (MAME_CABINET_CANVAS_HEIGHT - drawHeight - MAME_CABINET_BOTTOM_PADDING)
        .coerceAtLeast(0f)
    return MameCabinetLayout(
        drawLeft = drawLeft,
        drawTop = drawTop,
        drawWidth = drawWidth,
        drawHeight = drawHeight,
        scale = scale
    )
}

private fun coverSourceRect(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int
): android.graphics.Rect {
    if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
        return android.graphics.Rect(0, 0, sourceWidth.coerceAtLeast(1), sourceHeight.coerceAtLeast(1))
    }
    val sourceAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
    val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
    return if (sourceAspect > targetAspect) {
        val croppedWidth = (sourceHeight * targetAspect).roundToInt().coerceIn(1, sourceWidth)
        val left = ((sourceWidth - croppedWidth) / 2f).roundToInt().coerceAtLeast(0)
        android.graphics.Rect(left, 0, (left + croppedWidth).coerceAtMost(sourceWidth), sourceHeight)
    } else {
        val croppedHeight = (sourceWidth / targetAspect).roundToInt().coerceIn(1, sourceHeight)
        val top = ((sourceHeight - croppedHeight) / 2f).roundToInt().coerceAtLeast(0)
        android.graphics.Rect(0, top, sourceWidth, (top + croppedHeight).coerceAtMost(sourceHeight))
    }
}

private fun normalizeMameCabinetBitmap(source: Bitmap, backgroundColor: Int): Bitmap {
    val output = Bitmap.createBitmap(
        MAME_CABINET_CANVAS_WIDTH,
        MAME_CABINET_CANVAS_HEIGHT,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    if (!shouldTreatDarkBaseColorAsTransparent(backgroundColor)) {
        canvas.drawColor(backgroundColor)
    }
    val foreground = extractMameCabinetForeground(source) ?: return output
    val layout = calculateMameCabinetLayout(foreground.bitmap.width, foreground.bitmap.height)
    val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG or
            android.graphics.Paint.FILTER_BITMAP_FLAG or
            android.graphics.Paint.DITHER_FLAG
    )
    canvas.drawBitmap(
        foreground.bitmap,
        null,
        android.graphics.RectF(
            layout.drawLeft,
            layout.drawTop,
            layout.drawLeft + layout.drawWidth,
            layout.drawTop + layout.drawHeight
        ),
        paint
    )
    return output
}

private fun isMameCabinetBackgroundPixel(pixel: Int): Boolean {
    val alpha = android.graphics.Color.alpha(pixel)
    if (alpha == 0) return true
    return android.graphics.Color.red(pixel) == 255 &&
        android.graphics.Color.green(pixel) == 255 &&
        android.graphics.Color.blue(pixel) == 255
}

private fun vintageChannel(channel: Float, vignette: Float, grain: Float): Int =
    (((channel * vignette) + grain).toInt()).coerceIn(0, 255)
@Composable
private fun rememberCollectionBadgeBitmap(showBadge: Boolean): ImageBitmap? {
    return produceState<ImageBitmap?>(initialValue = null, showBadge) {
        if (!showBadge) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val iconSize = 132
                val picture = SVG.getFromString(COLLECTION_BADGE_SVG).renderToPicture()
                val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.TRANSPARENT)
                val sourceWidth = picture.width.takeIf { it > 0 }?.toFloat() ?: iconSize.toFloat()
                val sourceHeight = picture.height.takeIf { it > 0 }?.toFloat() ?: iconSize.toFloat()
                val scale = min(iconSize / sourceWidth, iconSize / sourceHeight)
                val offsetX = (iconSize - (sourceWidth * scale)) / 2f
                val offsetY = (iconSize - (sourceHeight * scale)) / 2f
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)
                canvas.drawPicture(picture)
                canvas.restore()
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

private const val COLLECTION_BADGE_SVG = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1900 2100">
  <rect x="165" y="303" width="1587" height="1738" rx="25" ry="20" fill="#8C8C8C"/>
  <rect x="67" y="71" width="1780" height="1595" rx="25" ry="20" fill="#D8D8D8"/>
  <rect x="337" y="56" width="390" height="251" rx="25" ry="20" fill="#BEBEBE"/>
  <rect x="341" y="307" width="386" height="1728" rx="25" ry="20" fill="#A8A8A8"/>
  <rect x="782" y="66" width="838" height="1332" rx="25" ry="20" fill="#C9C9C9"/>
  <path d="m 1033.8646,1527.8325 -77.19195,133.7004 -77.27727,-133.8482 z" fill="#7D7D7D"/>
  <rect x="347" y="1841" width="373" height="189" rx="25" ry="20" fill="#C4C4C4"/>
  <rect x="347" y="1768" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1691" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1615" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1538" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1465" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1388" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1313" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1236" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1168" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1090" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="1015" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="938" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="865" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="788" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="713" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="635" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="567" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="490" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="415" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="337" width="376" height="41" fill="#CDCDCD"/>
  <rect x="347" y="270" width="376" height="37" fill="#E3E3E3"/>
  <rect x="347" y="201" width="376" height="37" fill="#E3E3E3"/>
  <rect x="347" y="133" width="376" height="37" fill="#E3E3E3"/>
  <rect x="347" y="66" width="376" height="37" fill="#E3E3E3"/>
  <rect x="65" y="53" width="67" height="18" rx="25" ry="20" fill="#F0F0F0"/>
  <rect x="231" y="53" width="110" height="18" rx="25" ry="20" fill="#F0F0F0"/>
  <rect x="727" y="53" width="934" height="18" rx="25" ry="20" fill="#F0F0F0"/>
  <rect x="727" y="42" width="41" height="30" rx="25" ry="20" fill="#F0F0F0"/>
  <rect x="1611" y="34" width="69" height="37" rx="25" ry="20" fill="#F0F0F0"/>
  <rect x="1777" y="34" width="69" height="37" rx="25" ry="20" fill="#F0F0F0"/>
</svg>
"""

private fun normalizeBadgeSvgColors(platform: String?, svg: String): String {
    val preserveOriginalColors = setOf(
        "Nintendo 3DS",
        "Nintendo GameCube",
        "Game Boy Color",
        "Nintendo Entertainment System",
        "Nintendo 64",
        "Super Nintendo"
    )
    if (preserveOriginalColors.any { it.equals(platform, ignoreCase = true) }) {
        return svg
    }
    val fillRegex = Regex("""fill\s*=\s*(['"])(?!none|transparent)[^'"]*\1""", RegexOption.IGNORE_CASE)
    val strokeRegex = Regex("""stroke\s*=\s*(['"])(?!none|transparent)[^'"]*\1""", RegexOption.IGNORE_CASE)
    val fillStyleRegex = Regex("""fill\s*:\s*(?!none|transparent)[^;'" ]+""", RegexOption.IGNORE_CASE)
    val strokeStyleRegex = Regex("""stroke\s*:\s*(?!none|transparent)[^;'" ]+""", RegexOption.IGNORE_CASE)
    val normalized = svg
        .replace(fillRegex, """fill="#FFFFFF"""")
        .replace(strokeRegex, """stroke="#FFFFFF"""")
        .replace(fillStyleRegex, """fill:#FFFFFF""")
        .replace(strokeStyleRegex, """stroke:#FFFFFF""")
    if (platform.equals(GamePlatforms.ANDROID, ignoreCase = true)) {
        val shapeTagRegex = Regex(
            """<(path|circle|ellipse|rect|polygon|polyline)(?![^>]*\bfill\s*=)""",
            RegexOption.IGNORE_CASE
        )
        return normalized.replace(shapeTagRegex) { match ->
            """${match.value} fill="#FFFFFF""""
        }
    }
    val hasExplicitFill = Regex("""\bfill\s*=|\bfill\s*:""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
    return if (hasExplicitFill) {
        normalized
    } else {
        normalized.replaceFirst("<svg", """<svg fill="#FFFFFF"""")
    }
}

private fun InputStream?.decodeBitmap(): Bitmap? {
    if (this == null) return null
    return use {
        BitmapFactory.decodeStream(it)
    }
}

private data class PrefetchArtworkRequest(
    val uriString: String,
    val applyVintageArtwork: Boolean,
    val preTrimLeftFraction: Float = 0f,
    val preTrimTopFraction: Float = 0f,
    val preTrimWidthFraction: Float = 1f,
    val preTrimHeightFraction: Float = 1f
)

private fun artworkBitmapCacheKey(
    uriString: String,
    applyVintageArtwork: Boolean,
    preTrimLeftFraction: Float = 0f,
    preTrimTopFraction: Float = 0f,
    preTrimWidthFraction: Float = 1f,
    preTrimHeightFraction: Float = 1f
): String =
    buildString {
        append(if (applyVintageArtwork) "vintage" else "plain")
        append(':')
        append(preTrimLeftFraction)
        append(':')
        append(preTrimTopFraction)
        append(':')
        append(preTrimWidthFraction)
        append(':')
        append(preTrimHeightFraction)
        append(':')
        append(uriString)
    }

private fun getCachedTileBitmap(cacheKey: String): Bitmap? =
    synchronized(recentTileBitmapCache) { recentTileBitmapCache.get(cacheKey) }

private fun putCachedTileBitmap(cacheKey: String, bitmap: Bitmap) {
    synchronized(recentTileBitmapCache) {
        recentTileBitmapCache.put(cacheKey, bitmap)
    }
}

private fun ensureArtworkBitmapCached(
    contentResolver: ContentResolver,
    uriString: String,
    applyVintageArtwork: Boolean,
    preTrimLeftFraction: Float = 0f,
    preTrimTopFraction: Float = 0f,
    preTrimWidthFraction: Float = 1f,
    preTrimHeightFraction: Float = 1f
): Bitmap? {
    val cacheKey = artworkBitmapCacheKey(
        uriString = uriString,
        applyVintageArtwork = applyVintageArtwork,
        preTrimLeftFraction = preTrimLeftFraction,
        preTrimTopFraction = preTrimTopFraction,
        preTrimWidthFraction = preTrimWidthFraction,
        preTrimHeightFraction = preTrimHeightFraction
    )
    getCachedTileBitmap(cacheKey)?.let { return it }

    val decoded = decodeSampledBitmap(
        contentResolver = contentResolver,
        uri = resolveArtworkSourceUri(uriString),
        maxDimension = TILE_ARTWORK_MAX_DIMENSION
    ) ?: return null
    val croppedBitmap = cropArtworkBitmap(
        source = decoded,
        leftFraction = preTrimLeftFraction,
        topFraction = preTrimTopFraction,
        widthFraction = preTrimWidthFraction,
        heightFraction = preTrimHeightFraction
    )
    val finalBitmap = if (applyVintageArtwork) {
        applyVintageArtworkBitmap(croppedBitmap)
    } else {
        croppedBitmap
    }
    putCachedTileBitmap(cacheKey, finalBitmap)
    return finalBitmap
}

private fun cropArtworkBitmap(
    source: Bitmap,
    leftFraction: Float,
    topFraction: Float,
    widthFraction: Float,
    heightFraction: Float
): Bitmap {
    val safeLeftFraction = leftFraction.coerceIn(0f, 1f)
    val safeTopFraction = topFraction.coerceIn(0f, 1f)
    val safeRightFraction = (safeLeftFraction + widthFraction.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    val safeBottomFraction = (safeTopFraction + heightFraction.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    if (
        safeLeftFraction == 0f &&
        safeTopFraction == 0f &&
        safeRightFraction == 1f &&
        safeBottomFraction == 1f
    ) {
        return source
    }

    val sourceWidth = source.width.coerceAtLeast(1)
    val sourceHeight = source.height.coerceAtLeast(1)
    val left = (sourceWidth * safeLeftFraction).roundToInt().coerceIn(0, sourceWidth - 1)
    val top = (sourceHeight * safeTopFraction).roundToInt().coerceIn(0, sourceHeight - 1)
    val right = (sourceWidth * safeRightFraction).roundToInt().coerceIn(left + 1, sourceWidth)
    val bottom = (sourceHeight * safeBottomFraction).roundToInt().coerceIn(top + 1, sourceHeight)
    if (left == 0 && top == 0 && right == sourceWidth && bottom == sourceHeight) {
        return source
    }
    return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
}

private fun decodeSampledBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
    maxDimension: Int
): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri).use { stream ->
        if (BitmapFactory.decodeStream(stream, null, boundsOptions) == null && boundsOptions.outWidth <= 0) {
            return null
        }
    }
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = calculateInSampleSize(
            width = boundsOptions.outWidth,
            height = boundsOptions.outHeight,
            maxDimension = maxDimension
        )
    }
    return contentResolver.openInputStream(uri).decodeBitmap(options)
}

private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    val largestDimension = max(width, height)
    if (largestDimension <= 0 || largestDimension <= maxDimension) {
        return 1
    }
    var sampleSize = 1
    while ((largestDimension / sampleSize) > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private fun InputStream?.decodeBitmap(options: BitmapFactory.Options): Bitmap? {
    if (this == null) return null
    return use {
        BitmapFactory.decodeStream(it, null, options)
    }
}

private fun InstalledShortcutOption.resolvedIntentDataUri(): String? =
    intentDataUri ?: shortcutId.takeIf(::looksLikeLaunchPayload)

private fun String?.extractDocumentRoot(): String? {
    val raw = this?.takeIf { it.isNotBlank() } ?: return null
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
    val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        ?: runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return null
    return documentId.substringBefore(':', missingDelimiterValue = documentId)
}

private fun String?.selectedRomFolderDisplayPath(): String? {
    val raw = this?.takeIf { it.isNotBlank() } ?: return null
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        ?: return null
    val root = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
    val decodedPath = Uri.decode(relativePath).trim('/').takeIf { it.isNotBlank() }
    return decodedPath ?: root.takeIf { it.isNotBlank() }
}

private fun looksLikeLaunchPayload(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isBlank()) return false
    return normalized.startsWith("content://") ||
        normalized.startsWith("file://") ||
        normalized.startsWith("/") ||
        normalized.contains("%3A") ||
        normalized.contains("%2F") ||
        normalized.contains(".nsp", ignoreCase = true) ||
        normalized.contains(".xci", ignoreCase = true) ||
        normalized.contains(".gcm", ignoreCase = true) ||
        normalized.contains(".gcz", ignoreCase = true) ||
        normalized.contains(".iso", ignoreCase = true) ||
        normalized.contains(".wbfs", ignoreCase = true) ||
        normalized.contains(".rvz", ignoreCase = true) ||
        normalized.contains(".wia", ignoreCase = true) ||
        normalized.contains(".ciso", ignoreCase = true) ||
        normalized.contains(".dol", ignoreCase = true) ||
        normalized.contains(".elf", ignoreCase = true)
}

@Composable
private fun AddGameDialog(
    games: List<GameEntry>,
    shortcuts: List<InstalledShortcutOption>,
    activities: List<InstalledActivityOption>,
    templates: List<CustomIntentTemplate>,
    isDefaultLauncher: Boolean,
    authorizedRomTreeUri: String?,
    onDismiss: () -> Unit,
    initialTab: Int = 0,
    initialPlatformFilter: String? = null,
    initialCollection: String? = null,
    onAddShortcut: (InstalledShortcutOption) -> Unit,
    onAddActivity: (InstalledActivityOption) -> Unit,
    onAddCustom: (GameEntry) -> Unit,
    onAddCustomBatch: (List<GameEntry>) -> Unit,
    onFillMissingArtworkForPlatform: (String?) -> Unit,
    onVerifyScummVm: (gameFolderUri: String, gameId: String) -> Boolean,
    collectionSuggestions: List<String>
) {
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add game tile", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Shortcuts", "Android", "Games").forEachIndexed { index, label ->
                        FilterChip(
                            selected = tab == index,
                            onClick = { tab = index },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                when (tab) {
                    0 -> ShortcutPicker(
                        games = games,
                        shortcuts = shortcuts,
                        isDefaultLauncher = isDefaultLauncher,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        onAddShortcut = onAddShortcut
                    )
                    1 -> ActivityPicker(activities = activities, onAddActivity = onAddActivity)
                    else -> CustomIntentEditor(
                        games = games,
                        templates = templates,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        initialPlatformFilter = initialPlatformFilter,
                        initialCollection = initialCollection,
                        onAddCustom = onAddCustom,
                        onAddCustomBatch = onAddCustomBatch,
                        onFillMissingArtworkForPlatform = onFillMissingArtworkForPlatform,
                        onVerifyScummVm = onVerifyScummVm,
                        collectionSuggestions = collectionSuggestions
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutPicker(
    games: List<GameEntry>,
    shortcuts: List<InstalledShortcutOption>,
    isDefaultLauncher: Boolean,
    authorizedRomTreeUri: String?,
    onAddShortcut: (InstalledShortcutOption) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val authorizedRoot = remember(authorizedRomTreeUri) { authorizedRomTreeUri.extractDocumentRoot() }
    val existingShortcutKeys = remember(games) {
        games
            .flatMap { game -> game.allTargets() }
            .filter { it.sourceType == GameSourceType.SHORTCUT }
            .mapNotNull { target ->
                val packageName = target.packageName ?: return@mapNotNull null
                val shortcutId = target.shortcutId ?: return@mapNotNull null
                val userSerial = target.userSerial ?: return@mapNotNull null
                "$packageName|$shortcutId|$userSerial"
            }
            .toSet()
    }
    val filtered = remember(shortcuts, query, authorizedRoot) {
        shortcuts.filter {
            val matchesQuery = it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
            val shortcutRoot = it.resolvedIntentDataUri().extractDocumentRoot()
            val matchesAuthorizedRoot = authorizedRoot == null || shortcutRoot == null || shortcutRoot == authorizedRoot
            matchesQuery && matchesAuthorizedRoot
        }
    }
    val visibleShortcuts = remember(filtered, existingShortcutKeys) {
        filtered.filterNot { option ->
            "${option.packageName}|${option.shortcutId}|${option.userSerial}" in existingShortcutKeys
        }
    }

    Column {
        if (!isDefaultLauncher) {
            Text(
                text = "Shortcut enumeration is only available when this app is the active Home launcher.",
                color = Color(0xFFFDB022),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        RetroTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search shortcuts") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectionList(items = visibleShortcuts.take(60).map {
            SelectionRow(
                title = it.title,
                subtitle = it.subtitle,
                onClick = { onAddShortcut(it) }
            )
        })
    }
}

@Composable
private fun LaunchTargetDialog(
    game: GameEntry,
    onDismiss: () -> Unit,
    onLaunch: (LaunchTarget) -> Unit
) {
    val targets = remember(game) { game.allTargets() }
    val preferredTarget = remember(game) { game.preferredTarget() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "Choose emulator",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF98A2B3)
                )
                targets.forEach { target ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2433)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunch(target) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = target.label.ifBlank { target.packageName ?: target.sourceType.name },
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (preferredTarget?.stableKey() == target.stableKey()) {
                                    Text(
                                        text = "Default",
                                        color = Color(0xFFF2AA4C),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Text(
                                text = target.packageName ?: target.activityName ?: target.sourceType.name,
                                color = Color(0xFF98A2B3),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityPicker(
    activities: List<InstalledActivityOption>,
    showActivityName: Boolean = true,
    onAddActivity: (InstalledActivityOption) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(activities, query) {
        activities.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true) ||
                it.activityName.contains(query, ignoreCase = true)
        }
    }

    Column {
        RetroTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Android activities") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectionList(items = filtered.take(100).map {
            SelectionRow(
                title = it.title,
                subtitle = if (showActivityName) {
                    "${it.subtitle} • ${it.activityName}"
                } else {
                    it.subtitle
                },
                onClick = { onAddActivity(it) }
            )
        })
    }
}

@Composable
private fun SelectionList(items: List<SelectionRow>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(item.subtitle, color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private data class SelectionRow(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

private data class RetroArchSystemPreset(
    val id: String,
    val system: String,
    val coreLabel: String,
    val coreSoFile: String,
    val romFolder: String
)

private val retroArchSystemPresets = listOf(
    RetroArchSystemPreset("mame", "MAME Arcade", "MAME 2003-Plus", "mame2003_plus_libretro_android.so", "mame"),
    RetroArchSystemPreset("dos", "DOS Games", "DOSBox Pure", "dosbox_pure_libretro_android.so", "dos"),
    RetroArchSystemPreset("nes", "Nintendo Entertainment System", "Mesen / Nestopia", "mesen_libretro_android.so", "nes"),
    RetroArchSystemPreset("snes", "Super Nintendo", "Snes9x", "snes9x_libretro_android.so", "snes"),
    RetroArchSystemPreset("saturn", "Sega Saturn", "YabaSanshiro", "yabasanshiro_libretro_android.so", "saturn"),
    RetroArchSystemPreset("n64", "Nintendo 64", "Mupen64Plus-Next (GLES3)", "mupen64plus_next_gles3_libretro_android.so", "n64"),
    RetroArchSystemPreset("nds", "Nintendo DS", "melonDS", "melonds_libretro_android.so", "nds"),
    RetroArchSystemPreset("psx", "Sony PlayStation", "Beetle PSX HW / PCSX ReARMed", "beetle_psx_hw_libretro_android.so", "ps"),
    RetroArchSystemPreset("psp", "Sony PlayStation Portable", "PPSSPP", "ppsspp_libretro_android.so", "psp"),
    RetroArchSystemPreset("genesis", "Sega Genesis / Mega Drive", "Genesis Plus GX", "genesis_plus_gx_libretro_android.so", "md"),
    RetroArchSystemPreset("fbneo", "Final Burn Neo", "FinalBurn Neo", "fbneo_libretro_android.so", "fbneo")
)

private enum class GamePickerKind { FILE, FOLDER }

private data class GameSystemChoice(
    val id: String,
    val label: String,
    val platform: String?,
    val romFolderHint: String?,
    val pickerKind: GamePickerKind
)

private data class GameSystemEmulatorOption(
    val key: String,
    val templateIndex: Int,
    val systemId: String,
    val systemLabel: String,
    val platform: String?,
    val romFolderHint: String?,
    val pickerKind: GamePickerKind,
    val retroArchSystemId: String? = null
)

private data class ResolvedEmulatorSelectionDraft(
    val template: CustomIntentTemplate,
    val platform: String?,
    val retroArchSystem: RetroArchSystemPreset?,
    val subtitle: String,
    val packageName: String,
    val activityName: String,
    val action: String,
    val dataUri: String,
    val mimeType: String,
    val categories: String,
    val extras: String,
    val scummVmGameId: String,
    val edenTitleId: String,
    val edenResolutionSetup: Int?
)

private fun resolveEmulatorSelectionDraft(
    templates: List<CustomIntentTemplate>,
    option: GameSystemEmulatorOption?
): ResolvedEmulatorSelectionDraft? {
    val flowOption = option ?: return null
    val template = templates.getOrNull(flowOption.templateIndex) ?: return null
    val retroArchSystem = flowOption.retroArchSystemId
        ?.let { id -> retroArchSystemPresets.firstOrNull { it.id == id } }
    val extras = if (template.packageName.isRetroArchPackageName()) {
        retroArchSystem?.let { preset ->
            retroArchExtrasJson(template.packageName, preset.coreSoFile)
        } ?: template.extrasJson
    } else {
        template.extrasJson
    }
    return ResolvedEmulatorSelectionDraft(
        template = template,
        platform = flowOption.platform,
        retroArchSystem = retroArchSystem,
        subtitle = template.defaultSubtitle(
            platform = flowOption.platform,
            retroArchSystem = retroArchSystem?.system
        ),
        packageName = template.packageName,
        activityName = template.activityName,
        action = template.action,
        dataUri = template.dataUri,
        mimeType = template.mimeType,
        categories = template.categoriesCsv,
        extras = extras,
        scummVmGameId = readScummVmGameIdFromExtras(template.extrasJson),
        edenTitleId = readEdenTitleIdFromExtras(template.extrasJson),
        edenResolutionSetup = readEdenResolutionSetupFromExtras(template.extrasJson)
    )
}

private fun canonicalRomFolderHint(platform: String?, explicitHint: String?): String? =
    preferredRomFolderHintForPlatform(platform)
        ?: explicitHint?.takeIf { it.isNotBlank() }

private fun effectiveRomFolderHint(
    option: GameSystemEmulatorOption,
    fallbackHint: String?
): String? = canonicalRomFolderHint(option.platform, option.romFolderHint)
    ?: fallbackHint?.takeIf { it.isNotBlank() }
    ?: preferredRomFolderHintForPlatform(option.platform)

private fun romPickerCacheKey(
    option: GameSystemEmulatorOption,
    fallbackHint: String?
): String {
    val normalizedPlatform = GamePlatforms.normalizeOrNull(option.platform)
        ?.lowercase()
        .orEmpty()
    return listOf(
        option.pickerKind.name,
        effectiveRomFolderHint(option, fallbackHint).orEmpty().lowercase(),
        normalizedPlatform
    ).joinToString(":")
}

private fun canonicalSystemId(label: String, platform: String?): String =
    platform?.systemId() ?: label.systemId()

private fun systemChoiceSortOrder(platform: String?, label: String): Int {
    val normalized = platform?.let(GamePlatforms::normalizeOrNull) ?: GamePlatforms.normalizeOrNull(label)
    return normalized
        ?.let(GamePlatforms.values::indexOf)
        ?.takeIf { it >= 0 }
        ?: Int.MAX_VALUE
}

private fun buildGameSystemEmulatorOptions(
    templates: List<CustomIntentTemplate>
): List<GameSystemEmulatorOption> {
    return templates.flatMapIndexed { index, template ->
        when {
            template.packageName.isRetroArchPackageName() -> {
                retroArchSystemPresets
                    .filterNot { preset ->
                        (template.packageName == "com.retroarch" && preset.id == "dos") ||
                            preset.id == "psp" ||
                            preset.id == "psx"
                    }
                    .map { preset ->
                    val platform = GamePlatforms.normalizeOrNull(preset.system)
                        ?: GamePlatforms.inferFromUriString(preset.romFolder)
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:${preset.id}",
                        templateIndex = index,
                        systemId = canonicalSystemId(preset.system, platform),
                        systemLabel = preset.system,
                        platform = platform,
                        romFolderHint = canonicalRomFolderHint(platform, preset.romFolder),
                        pickerKind = GamePickerKind.FILE,
                        retroArchSystemId = preset.id
                    )
                    }
            }

            template.packageName.isDolphinPackageName() -> {
                listOf(
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:gamecube",
                        templateIndex = index,
                        systemId = canonicalSystemId("Nintendo GameCube", "Nintendo GameCube"),
                        systemLabel = "Nintendo GameCube",
                        platform = "Nintendo GameCube",
                        romFolderHint = canonicalRomFolderHint("Nintendo GameCube", "gamecube"),
                        pickerKind = GamePickerKind.FILE
                    ),
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:wii",
                        templateIndex = index,
                        systemId = canonicalSystemId("Nintendo Wii", "Nintendo Wii"),
                        systemLabel = "Nintendo Wii",
                        platform = "Nintendo Wii",
                        romFolderHint = canonicalRomFolderHint("Nintendo Wii", "wii"),
                        pickerKind = GamePickerKind.FILE
                    )
                )
            }

            template.packageName.isScummVmPackageName() -> {
                listOf(
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:scummvm",
                        templateIndex = index,
                        systemId = canonicalSystemId("ScummVM", "ScummVM"),
                        systemLabel = "ScummVM",
                        platform = "ScummVM",
                        romFolderHint = canonicalRomFolderHint(
                            "ScummVM",
                            template.romSubfolderHint?.ifBlank { "scummvm" } ?: "scummvm"
                        ),
                        pickerKind = GamePickerKind.FOLDER
                    )
                )
            }

            template.packageName == "com.explusalpha.MdEmu" -> {
                listOf(
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:mastersystem",
                        templateIndex = index,
                        systemId = canonicalSystemId("Sega Master System", "Sega Master System"),
                        systemLabel = "Sega Master System",
                        platform = "Sega Master System",
                        romFolderHint = canonicalRomFolderHint("Sega Master System", "sms"),
                        pickerKind = GamePickerKind.FILE
                    ),
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:genesis",
                        templateIndex = index,
                        systemId = canonicalSystemId("Sega Genesis / Mega Drive", "Sega Genesis / Mega Drive"),
                        systemLabel = "Sega Genesis / Mega Drive",
                        platform = "Sega Genesis / Mega Drive",
                        romFolderHint = canonicalRomFolderHint("Sega Genesis / Mega Drive", "md"),
                        pickerKind = GamePickerKind.FILE
                    ),
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:segacd",
                        templateIndex = index,
                        systemId = canonicalSystemId("Sega CD", "Sega CD"),
                        systemLabel = "Sega CD",
                        platform = "Sega CD",
                        romFolderHint = canonicalRomFolderHint("Sega CD", "segacd"),
                        pickerKind = GamePickerKind.FILE
                    ),
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:32x",
                        templateIndex = index,
                        systemId = canonicalSystemId("Sega 32X", "Sega 32X"),
                        systemLabel = "Sega 32X",
                        platform = "Sega 32X",
                        romFolderHint = canonicalRomFolderHint("Sega 32X", "32x"),
                        pickerKind = GamePickerKind.FILE
                    )
                )
            }

            template.packageName == "com.explusalpha.GbcEmu" -> {
                listOf(
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:gb",
                        templateIndex = index,
                        systemId = canonicalSystemId("Game Boy", "Game Boy"),
                        systemLabel = "Game Boy",
                        platform = "Game Boy",
                        romFolderHint = canonicalRomFolderHint("Game Boy", "gb"),
                        pickerKind = GamePickerKind.FILE
                    ),
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:gbc",
                        templateIndex = index,
                        systemId = canonicalSystemId("Game Boy Color", "Game Boy Color"),
                        systemLabel = "Game Boy Color",
                        platform = "Game Boy Color",
                        romFolderHint = canonicalRomFolderHint("Game Boy Color", "gbc"),
                        pickerKind = GamePickerKind.FILE
                    )
                )
            }

            template.packageName == "com.explusalpha.NgpEmu" -> {
                listOf(
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:ngp",
                        templateIndex = index,
                        systemId = canonicalSystemId("Neo Geo Pocket", "Neo Geo Pocket"),
                        systemLabel = "Neo Geo Pocket",
                        platform = "Neo Geo Pocket",
                        romFolderHint = canonicalRomFolderHint("Neo Geo Pocket", "ngp"),
                        pickerKind = GamePickerKind.FILE
                    ),
                    GameSystemEmulatorOption(
                        key = "${template.packageName}:ngpc",
                        templateIndex = index,
                        systemId = canonicalSystemId("Neo Geo Pocket Color", "Neo Geo Pocket Color"),
                        systemLabel = "Neo Geo Pocket Color",
                        platform = "Neo Geo Pocket Color",
                        romFolderHint = canonicalRomFolderHint("Neo Geo Pocket Color", "ngpc"),
                        pickerKind = GamePickerKind.FILE
                    )
                )
            }

            else -> {
                val platform = templateSystemPlatform(template)
                val systemLabel = templateSystemLabel(template)
                listOf(
                    GameSystemEmulatorOption(
                        key = "${template.packageName.ifBlank { "template-$index" }}:${systemLabel.systemId()}",
                        templateIndex = index,
                        systemId = canonicalSystemId(systemLabel, platform),
                        systemLabel = systemLabel,
                        platform = platform,
                        romFolderHint = canonicalRomFolderHint(
                            platform = platform,
                            explicitHint = template.romSubfolderHint?.takeIf { it.isNotBlank() }
                        ),
                        pickerKind = GamePickerKind.FILE
                    )
                )
            }
        }
    }
}

private fun templateSystemLabel(template: CustomIntentTemplate): String {
    return templateSystemPlatform(template)
        ?: when {
            template.packageName.isScummVmPackageName() -> "ScummVM"
            template.emulatedPlatform.equals("Sony PSP", ignoreCase = true) -> "Sony PlayStation Portable"
            template.emulatedPlatform.equals("PlayStation 1", ignoreCase = true) -> "Sony PlayStation"
            template.emulatedPlatform.equals("Classic adventure", ignoreCase = true) -> "ScummVM"
            !template.emulatedPlatform.isNullOrBlank() -> template.emulatedPlatform
            !template.romSubfolderHint.isNullOrBlank() ->
                GamePlatforms.inferFromUriString(template.romSubfolderHint) ?: template.romSubfolderHint
            else -> template.label
        }
}

private fun templateSystemPlatform(template: CustomIntentTemplate): String? {
    return GamePlatforms.normalizeOrNull(template.emulatedPlatform)
        ?: GamePlatforms.inferFromPackageName(template.packageName.ifBlank { null })
        ?: GamePlatforms.inferFromUriString(template.romSubfolderHint)
        ?: when {
            template.packageName.isScummVmPackageName() -> "ScummVM"
            template.emulatedPlatform.equals("Sony PSP", ignoreCase = true) -> "Sony PlayStation Portable"
            template.emulatedPlatform.equals("PlayStation 1", ignoreCase = true) -> "Sony PlayStation"
            template.emulatedPlatform.equals("Classic adventure", ignoreCase = true) -> "ScummVM"
            else -> null
        }
}

private fun String.systemId(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomIntentEditor(
    games: List<GameEntry>,
    templates: List<CustomIntentTemplate>,
    authorizedRomTreeUri: String?,
    initialPlatformFilter: String? = null,
    initialCollection: String? = null,
    onAddCustom: (GameEntry) -> Unit,
    onAddCustomBatch: (List<GameEntry>) -> Unit,
    onFillMissingArtworkForPlatform: (String?) -> Unit,
    onVerifyScummVm: (gameFolderUri: String, gameId: String) -> Boolean,
    collectionSuggestions: List<String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemEmulatorOptions = remember(templates) { buildGameSystemEmulatorOptions(templates) }
    val scopedPlatformFilter = remember(initialPlatformFilter) { GamePlatforms.filterLabel(initialPlatformFilter) }
    val filteredSystemEmulatorOptions = remember(systemEmulatorOptions, scopedPlatformFilter) {
        val platformFilter = scopedPlatformFilter ?: return@remember systemEmulatorOptions
        systemEmulatorOptions
            .filter { option ->
                option.platform?.let { platform -> GamePlatforms.matchesFilter(platform, platformFilter) } == true
            }
            .ifEmpty { systemEmulatorOptions }
    }
    val systemChoices = remember(filteredSystemEmulatorOptions) {
        filteredSystemEmulatorOptions
            .distinctBy { it.systemId }
            .map {
                GameSystemChoice(
                    id = it.systemId,
                    label = it.systemLabel,
                    platform = it.platform,
                    romFolderHint = it.romFolderHint,
                    pickerKind = it.pickerKind
                )
            }
            .sortedWith(
                compareBy<GameSystemChoice> { choice ->
                    systemChoiceSortOrder(choice.platform, choice.label)
                }.thenBy { choice ->
                    choice.label.lowercase()
                }
            )
    }
    val emulatorsPerSystem = remember(filteredSystemEmulatorOptions) {
        filteredSystemEmulatorOptions.groupingBy { it.systemId }.eachCount()
    }
    val systemChoiceKey = remember(systemChoices) { systemChoices.joinToString(separator = "|") { it.id } }
    var step by rememberSaveable(scopedPlatformFilter, systemChoiceKey) {
        mutableIntStateOf(0)
    }
    var systemSelectionPendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSystemId by rememberSaveable(scopedPlatformFilter, systemChoiceKey) {
        mutableStateOf(systemChoices.firstOrNull()?.id.orEmpty())
    }
    var selectedEmulatorKey by rememberSaveable(scopedPlatformFilter, systemChoiceKey) { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var autoSubtitle by rememberSaveable { mutableStateOf("") }
    var collection by rememberSaveable(initialCollection) { mutableStateOf(initialCollection.orEmpty()) }
    var packageName by rememberSaveable { mutableStateOf("") }
    var activityName by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf("") }
    var dataUri by rememberSaveable { mutableStateOf("") }
    var scummVmGameId by rememberSaveable { mutableStateOf("") }
    var edenTitleId by rememberSaveable { mutableStateOf("") }
    var edenResolutionSetup by rememberSaveable { mutableStateOf(defaultEdenResolutionSetup()) }
    var gameNativeAppId by rememberSaveable { mutableStateOf("") }
    var gameNativeGameSource by rememberSaveable { mutableStateOf(GAMENATIVE_GAME_SOURCES.first()) }
    var mimeType by rememberSaveable { mutableStateOf("") }
    var categories by rememberSaveable { mutableStateOf("") }
    var extras by rememberSaveable { mutableStateOf("") }
    var artworkUri by rememberSaveable { mutableStateOf<String?>(null) }
    var gamePickerOpening by rememberSaveable { mutableStateOf(false) }
    var bulkScanRunning by rememberSaveable { mutableStateOf(false) }
    var pendingScanPlatform by rememberSaveable { mutableStateOf<String?>(null) }
    var scanFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var lastTemplatePackageName by rememberSaveable { mutableStateOf("") }
    var lastTemplateActivityName by rememberSaveable { mutableStateOf("") }
    var lastTemplateAction by rememberSaveable { mutableStateOf("") }
    var lastTemplateDataUri by rememberSaveable { mutableStateOf("") }
    var lastTemplateMimeType by rememberSaveable { mutableStateOf("") }
    var lastTemplateCategories by rememberSaveable { mutableStateOf("") }
    var lastTemplateExtras by rememberSaveable { mutableStateOf("") }
    var lastTemplateScummVmGameId by rememberSaveable { mutableStateOf("") }
    var lastTemplateEdenTitleId by rememberSaveable { mutableStateOf("") }
    var lastTemplateEdenResolutionSetup by rememberSaveable { mutableStateOf(defaultEdenResolutionSetup()) }
    val resolvedPickerInitialUris = remember(authorizedRomTreeUri) {
        mutableStateMapOf<String, String?>()
    }
    val pickGameUri: (Uri?) -> Unit = { uri ->
        gamePickerOpening = false
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            dataUri = uri.toString()
            val inferredTitle = inferGameTitleFromUri(context, uri)
            if (!inferredTitle.isNullOrBlank() && (title.isBlank() || templates.any { it.label.equals(title, ignoreCase = true) })) {
                title = inferredTitle
            }
            if (packageName.isScummVmPackageName() && scummVmGameId.isBlank()) {
                inferScummVmGameIdFromUri(context, uri)?.let { inferredId ->
                    scummVmGameId = inferredId
                }
            }
            if (packageName.isEdenPackageName() && edenTitleId.isBlank()) {
                inferEdenTitleIdFromUri(context, uri)?.let { inferredId ->
                    edenTitleId = inferredId
                }
            }
        }
    }
    val gamePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pickGameUri(result.data?.data)
    }
    val gameFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        pickGameUri(uri)
    }
    val artworkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            artworkUri = uri.toString()
        }
    }
    val installedPackageNames = remember(templates) {
        templates
            .mapNotNull { it.packageName.takeIf { pkg -> pkg.isNotBlank() } }
            .distinct()
    }
    val installedPackages by produceState(
        initialValue = emptyMap<String, Boolean>(),
        context,
        installedPackageNames
    ) {
        value = withContext(Dispatchers.IO) {
            installedPackageNames.associateWith { pkg -> isPackageInstalled(context, pkg) }
        }
    }
    LaunchedEffect(systemChoices) {
        if (selectedSystemId.isBlank() || systemChoices.none { it.id == selectedSystemId }) {
            selectedSystemId = systemChoices.firstOrNull()?.id.orEmpty()
        }
    }
    LaunchedEffect(step) {
        if (step != 0) {
            systemSelectionPendingId = null
        }
    }
    val emulatorChoices = remember(filteredSystemEmulatorOptions, selectedSystemId, installedPackages, templates) {
        filteredSystemEmulatorOptions
            .filter { it.systemId == selectedSystemId }
            .sortedWith(
                compareByDescending<GameSystemEmulatorOption> { option ->
                    val pkg = templates.getOrNull(option.templateIndex)?.packageName
                    pkg.isNullOrBlank() || installedPackages[pkg] != false
                }.thenBy { option ->
                    templates.getOrNull(option.templateIndex)?.packageName.isRetroArchPackageName()
                }.thenBy { option ->
                    templates.getOrNull(option.templateIndex)?.label.orEmpty().lowercase()
                }
            )
    }
    val selectedSystemChoice = systemChoices.firstOrNull { it.id == selectedSystemId } ?: systemChoices.firstOrNull()
    LaunchedEffect(selectedSystemId, emulatorChoices) {
        if (selectedEmulatorKey.isBlank() || emulatorChoices.none { it.key == selectedEmulatorKey }) {
            selectedEmulatorKey = emulatorChoices.firstOrNull()?.key.orEmpty()
        }
    }
    LaunchedEffect(authorizedRomTreeUri, emulatorChoices, selectedSystemChoice?.romFolderHint) {
        val fallbackHint = selectedSystemChoice?.romFolderHint
        emulatorChoices
            .distinctBy { option -> romPickerCacheKey(option, fallbackHint) }
            .forEach { option ->
                val resolvedHint = effectiveRomFolderHint(option, fallbackHint)
                val cacheKey = romPickerCacheKey(option, fallbackHint)
                if (resolvedPickerInitialUris.containsKey(cacheKey)) return@forEach
                launch {
                    val resolvedUri = withContext(Dispatchers.IO) {
                        initialRomPickerUri(
                            context = context,
                            authorizedRomTreeUri = authorizedRomTreeUri,
                            subfolderHint = resolvedHint,
                            platform = option.platform,
                            pickerKind = option.pickerKind
                        )?.toString()
                    }
                    resolvedPickerInitialUris[cacheKey] = resolvedUri
                }
            }
    }
    val selectedFlowOption = emulatorChoices.firstOrNull { it.key == selectedEmulatorKey }
        ?: emulatorChoices.firstOrNull()
        ?: filteredSystemEmulatorOptions.firstOrNull()
    val selectedFlowDraft = remember(templates, selectedFlowOption) {
        resolveEmulatorSelectionDraft(templates, selectedFlowOption)
    }
    val selectedTemplate = selectedFlowDraft?.template ?: templates.first()
    val templatePackage = selectedTemplate.packageName.takeIf { it.isNotBlank() }
    val isRetroArchTemplate = templatePackage.isRetroArchPackageName()
    val isScummVmTemplate = templatePackage.isScummVmPackageName()
    val isEdenTemplate = templatePackage.isEdenPackageName()
    val isGameNativeTemplate = templatePackage.isGameNativePackageName()
    val isRedreamTemplate = templatePackage == "io.recompiled.redream"
    val selectedRetroArchSystem = selectedFlowDraft?.retroArchSystem
    val stepLabels = listOf("System", "Emulator", "Game", "Artwork", "Advanced")
    val gameStepIndex = 2
    val artworkStepIndex = 3
    val advancedStepIndex = 4
    val maxStep = stepLabels.lastIndex
    LaunchedEffect(maxStep) {
        if (step > maxStep) step = maxStep
    }
    LaunchedEffect(selectedFlowOption?.key) {
        val flowDraft = selectedFlowDraft ?: return@LaunchedEffect
        val nextPackageName = flowDraft.packageName
        val nextActivityName = flowDraft.activityName
        val nextAction = flowDraft.action
        val nextDataUri = flowDraft.dataUri
        val nextMimeType = flowDraft.mimeType
        val nextCategories = flowDraft.categories
        val nextExtras = flowDraft.extras
        val nextScummVmGameId = flowDraft.scummVmGameId
        val nextEdenTitleId = flowDraft.edenTitleId
        val nextEdenResolutionSetup = flowDraft.edenResolutionSetup
        if (packageName.isBlank() || packageName == lastTemplatePackageName) {
            packageName = nextPackageName
        }
        if (activityName.isBlank() || activityName == lastTemplateActivityName) {
            activityName = nextActivityName
        }
        if (action.isBlank() || action == lastTemplateAction) {
            action = nextAction
        }
        if (dataUri.isBlank() || dataUri == lastTemplateDataUri) {
            dataUri = nextDataUri
        }
        if (mimeType.isBlank() || mimeType == lastTemplateMimeType) {
            mimeType = nextMimeType
        }
        if (categories.isBlank() || categories == lastTemplateCategories) {
            categories = nextCategories
        }
        if (extras.isBlank() || extras == lastTemplateExtras) {
            extras = nextExtras
        }
        if (scummVmGameId.isBlank() || scummVmGameId == lastTemplateScummVmGameId) {
            scummVmGameId = nextScummVmGameId
        }
        if (edenTitleId.isBlank() || edenTitleId == lastTemplateEdenTitleId) {
            edenTitleId = nextEdenTitleId
        }
        if (edenResolutionSetup == null || edenResolutionSetup == lastTemplateEdenResolutionSetup) {
            edenResolutionSetup = nextEdenResolutionSetup
        }
        if (nextPackageName.isGameNativePackageName()) {
            if (gameNativeAppId.isBlank()) {
                gameNativeAppId = readGameNativeAppIdFromExtras(nextExtras)
            }
            gameNativeGameSource = readGameNativeSourceFromExtras(nextExtras)
        }
        lastTemplatePackageName = nextPackageName
        lastTemplateActivityName = nextActivityName
        lastTemplateAction = nextAction
        lastTemplateDataUri = nextDataUri
        lastTemplateMimeType = nextMimeType
        lastTemplateCategories = nextCategories
        lastTemplateExtras = nextExtras
        lastTemplateScummVmGameId = nextScummVmGameId
        lastTemplateEdenTitleId = nextEdenTitleId
        lastTemplateEdenResolutionSetup = nextEdenResolutionSetup
        val generatedSubtitle = flowDraft.subtitle
        if (!generatedSubtitle.isNullOrBlank() && (subtitle.isBlank() || subtitle == autoSubtitle)) {
            subtitle = generatedSubtitle
            autoSubtitle = generatedSubtitle
        }
    }
    val emulatorInstalled = templatePackage?.let { installedPackages[it] } != false
    val canSave = title.isNotBlank() &&
        listOf(packageName, activityName, action, dataUri).any { it.isNotBlank() } &&
        (!isScummVmTemplate || (dataUri.isNotBlank() && scummVmGameId.isNotBlank())) &&
        (!isGameNativeTemplate || gameNativeAppId.isNotBlank())
    val selectedRomPathHint = selectedFlowOption?.let { option ->
        effectiveRomFolderHint(option, selectedSystemChoice?.romFolderHint)
    } ?: selectedSystemChoice?.romFolderHint
        ?: preferredRomFolderHintForPlatform(selectedSystemChoice?.platform)
    val launchGamePicker: (GameSystemEmulatorOption, Uri?) -> Unit = { flowOption, initialUri ->
        val launchResult = runCatching {
            if (flowOption.pickerKind == GamePickerKind.FOLDER) {
                gameFolderPicker.launch(initialUri)
            } else {
                gamePicker.launch(createRomFilePickerIntent(initialUri))
            }
        }
        if (launchResult.isFailure) {
            gamePickerOpening = false
        }
    }
    val openGamePickerForOption: (GameSystemEmulatorOption) -> Unit = openPicker@{ flowOption ->
        if (gamePickerOpening) return@openPicker
        gamePickerOpening = true
        val resolvedHint = effectiveRomFolderHint(flowOption, selectedSystemChoice?.romFolderHint)
        val cacheKey = romPickerCacheKey(flowOption, selectedSystemChoice?.romFolderHint)
        if (resolvedPickerInitialUris.containsKey(cacheKey)) {
            launchGamePicker(flowOption, resolvedPickerInitialUris[cacheKey]?.let(Uri::parse))
        } else {
            scope.launch {
                val resolvedUri = withContext(Dispatchers.IO) {
                    initialRomPickerUri(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        subfolderHint = resolvedHint,
                        platform = flowOption.platform,
                        pickerKind = flowOption.pickerKind
                    )?.toString()
                }
                resolvedPickerInitialUris[cacheKey] = resolvedUri
                launchGamePicker(flowOption, resolvedUri?.let(Uri::parse))
            }
        }
    }
    val openSelectedGamePicker = openPicker@{
        val flowOption = selectedFlowOption ?: return@openPicker
        openGamePickerForOption(flowOption)
    }
    val activeScanPlatform = pendingScanPlatform?.takeIf { pendingPlatform ->
        selectedSystemChoice?.platform.equals(pendingPlatform, ignoreCase = true)
    }
    val scanningMameAfterEmulatorPick = activeScanPlatform.equals("MAME Arcade", ignoreCase = true)
    val scanningScummVmAfterEmulatorPick = activeScanPlatform.equals("ScummVM", ignoreCase = true)
    val scanningConfiguredPlatformAfterEmulatorPick = activeScanPlatform?.takeIf { platform ->
        !platform.equals("MAME Arcade", ignoreCase = true) &&
            !platform.equals("ScummVM", ignoreCase = true) &&
            GameRecommendations.supportsPlatformScan(platform)
    }
    val activeScanSystemLabel = selectedSystemChoice?.label
        ?: GamePlatforms.normalizeOrNull(activeScanPlatform)
        ?: activeScanPlatform
        ?: "system"
    val startMameScanForOption: (GameSystemEmulatorOption) -> Unit = startScan@{ flowOption ->
        if (bulkScanRunning) return@startScan
        val flowDraft = resolveEmulatorSelectionDraft(templates, flowOption)
        if (flowDraft == null) {
            scanFeedback = "Couldn't resolve the selected emulator template for scanning."
            pendingScanPlatform = null
            return@startScan
        }
        if (authorizedRomTreeUri.isNullOrBlank()) {
            scanFeedback = "Authorize the ROM folder first to scan MAME titles."
            pendingScanPlatform = null
            return@startScan
        }
        onFillMissingArtworkForPlatform(selectedSystemChoice?.platform ?: "MAME Arcade")
        bulkScanRunning = true
        scanFeedback = null
        scope.launch {
            val entries = runCatching {
                withContext(Dispatchers.IO) {
                    GameRecommendations.scanImportableMameRoms(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        games = games
                    ).map { candidate ->
                        GameEntry(
                            title = candidate.title,
                            subtitle = flowDraft.subtitle,
                            genre = collection.ifBlank { null },
                            platform = flowDraft.platform ?: selectedSystemChoice?.platform ?: "MAME Arcade",
                            sourceType = GameSourceType.CUSTOM_INTENT,
                            packageName = flowDraft.packageName.ifBlank { null },
                            activityName = flowDraft.activityName.ifBlank { null },
                            intentAction = flowDraft.action.ifBlank { null },
                            intentDataUri = candidate.romUri,
                            mimeType = flowDraft.mimeType.ifBlank { null },
                            categoriesCsv = flowDraft.categories.ifBlank { null },
                            extrasJson = flowDraft.extras.ifBlank { null }
                        )
                    }
                }
            }.getOrElse { error ->
                scanFeedback = "MAME scan failed: ${error.message ?: error.javaClass.simpleName}"
                emptyList()
            }
            bulkScanRunning = false
            pendingScanPlatform = null
            if (entries.isEmpty()) {
                scanFeedback = scanFeedback
                    ?: "No new MAME titles with matching overlay art were found in the ROM folder."
            } else {
                onAddCustomBatch(entries)
            }
        }
    }
    val startConfiguredPlatformScanForOption: (GameSystemEmulatorOption) -> Unit = startScan@{ flowOption ->
        if (bulkScanRunning) return@startScan
        val flowDraft = resolveEmulatorSelectionDraft(templates, flowOption)
        val scanPlatform = scanningConfiguredPlatformAfterEmulatorPick
            ?: flowDraft?.platform
            ?: selectedSystemChoice?.platform
        if (flowDraft == null) {
            scanFeedback = "Couldn't resolve the selected emulator template for scanning."
            pendingScanPlatform = null
            return@startScan
        }
        if (scanPlatform.isNullOrBlank()) {
            scanFeedback = "Couldn't resolve the selected platform scanner."
            pendingScanPlatform = null
            return@startScan
        }
        if (authorizedRomTreeUri.isNullOrBlank()) {
            scanFeedback = "Authorize the ROM folder first to scan $activeScanSystemLabel titles."
            pendingScanPlatform = null
            return@startScan
        }
        onFillMissingArtworkForPlatform(scanPlatform)
        bulkScanRunning = true
        scanFeedback = null
        scope.launch {
            val entries = runCatching {
                withContext(Dispatchers.IO) {
                    GameRecommendations.scanImportablePlatformRoms(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        games = games,
                        platform = scanPlatform,
                        packageName = flowDraft.packageName.ifBlank { null },
                        retroArchSystemId = flowOption.retroArchSystemId
                    ).map { candidate ->
                        val extrasJson = if (flowDraft.packageName.isEdenPackageName()) {
                            mergeEdenLaunchConfigIntoExtras(
                                extrasJson = flowDraft.extras,
                                titleId = inferEdenTitleIdFromUriString(context, candidate.romUri).orEmpty(),
                                resolutionSetup = flowDraft.edenResolutionSetup
                            ).ifBlank { null }
                        } else {
                            flowDraft.extras.ifBlank { null }
                        }
                        GameEntry(
                            title = candidate.title,
                            subtitle = flowDraft.subtitle,
                            genre = collection.ifBlank { null },
                            platform = flowDraft.platform ?: selectedSystemChoice?.platform ?: scanPlatform,
                            sourceType = GameSourceType.CUSTOM_INTENT,
                            packageName = flowDraft.packageName.ifBlank { null },
                            activityName = flowDraft.activityName.ifBlank { null },
                            intentAction = flowDraft.action.ifBlank { null },
                            intentDataUri = candidate.romUri,
                            mimeType = flowDraft.mimeType.ifBlank { null },
                            categoriesCsv = flowDraft.categories.ifBlank { null },
                            extrasJson = extrasJson
                        )
                    }
                }
            }.getOrElse { error ->
                scanFeedback = "$activeScanSystemLabel scan failed: ${error.message ?: error.javaClass.simpleName}"
                emptyList()
            }
            bulkScanRunning = false
            pendingScanPlatform = null
            if (entries.isEmpty()) {
                scanFeedback = scanFeedback
                    ?: "No new $activeScanSystemLabel titles were found in the ROM folder."
            } else {
                onAddCustomBatch(entries)
            }
        }
    }
    val startScummVmScanForOption: (GameSystemEmulatorOption) -> Unit = startScan@{ flowOption ->
        if (bulkScanRunning) return@startScan
        val flowDraft = resolveEmulatorSelectionDraft(templates, flowOption)
        if (flowDraft == null) {
            scanFeedback = "Couldn't resolve the selected emulator template for scanning."
            pendingScanPlatform = null
            return@startScan
        }
        if (authorizedRomTreeUri.isNullOrBlank()) {
            scanFeedback = "Authorize the ROM folder first to scan ScummVM folders."
            pendingScanPlatform = null
            return@startScan
        }
        onFillMissingArtworkForPlatform(selectedSystemChoice?.platform ?: "ScummVM")
        bulkScanRunning = true
        scanFeedback = null
        scope.launch {
            val (entries, skippedFolders) = runCatching {
                withContext(Dispatchers.IO) {
                    var skipped = 0
                    val importedEntries = GameRecommendations.scanImportableScummVmFolders(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        games = games
                    ).mapNotNull { candidate ->
                        val folderUri = Uri.parse(candidate.folderUri)
                        val detectedGameId = inferScummVmGameIdFromUri(context, folderUri)
                        if (detectedGameId.isNullOrBlank()) {
                            skipped += 1
                            null
                        } else {
                            GameEntry(
                                title = inferGameTitleFromUri(context, folderUri) ?: candidate.title,
                                subtitle = flowDraft.subtitle,
                                genre = collection.ifBlank { null },
                                platform = flowDraft.platform ?: selectedSystemChoice?.platform ?: "ScummVM",
                                sourceType = GameSourceType.CUSTOM_INTENT,
                                packageName = flowDraft.packageName.ifBlank { null },
                                activityName = flowDraft.activityName.ifBlank { null },
                                intentAction = flowDraft.action.ifBlank { null },
                                intentDataUri = candidate.folderUri,
                                mimeType = flowDraft.mimeType.ifBlank { null },
                                categoriesCsv = flowDraft.categories.ifBlank { null },
                                extrasJson = mergeScummVmGameIdIntoExtras(flowDraft.extras, detectedGameId)
                                    .ifBlank { null }
                            )
                        }
                    }
                    importedEntries to skipped
                }
            }.getOrElse { error ->
                scanFeedback = "ScummVM scan failed: ${error.message ?: error.javaClass.simpleName}"
                emptyList<GameEntry>() to 0
            }
            bulkScanRunning = false
            pendingScanPlatform = null
            if (entries.isEmpty()) {
                scanFeedback = scanFeedback ?: if (skippedFolders > 0) {
                    "No ScummVM folders with a detectable game id were found. Add .scummvm markers or import those folders manually."
                } else {
                    "No new ScummVM folders were found in the ROM folder."
                }
            } else {
                onAddCustomBatch(entries)
            }
        }
    }

    Column(
        modifier = Modifier
            .height(460.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stepLabels.forEachIndexed { index, label ->
                FilterChip(selected = step == index, onClick = { step = index }, label = { Text(label) })
            }
        }

        when {
            step == 0 -> {
                HelpCard(
                    title = "Choose system",
                    body = "Start with the system you want to play. Folder hints and compatible emulators are derived from this selection."
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    systemChoices.forEach { systemChoice ->
                        val supportsScan = GameRecommendations.supportsPlatformScan(systemChoice.platform)
                        SystemChoiceCard(
                            choice = systemChoice,
                            selected = selectedSystemId == systemChoice.id,
                            emulatorCount = emulatorsPerSystem[systemChoice.id] ?: 0,
                            enabled = systemSelectionPendingId == null && !bulkScanRunning,
                            pending = systemSelectionPendingId == systemChoice.id,
                            secondaryActionLabel = if (supportsScan) "Scan" else null,
                            secondaryActionEnabled = authorizedRomTreeUri != null &&
                                systemSelectionPendingId == null &&
                                !bulkScanRunning,
                            onSecondaryAction = if (supportsScan) {
                                {
                                    if (systemSelectionPendingId == null && !bulkScanRunning) {
                                        scanFeedback = null
                                        pendingScanPlatform = systemChoice.platform
                                        systemSelectionPendingId = systemChoice.id
                                        selectedSystemId = systemChoice.id
                                        step = 1
                                    }
                                }
                            } else {
                                null
                            },
                            onClick = {
                                if (systemSelectionPendingId != null || bulkScanRunning) return@SystemChoiceCard
                                scanFeedback = null
                                pendingScanPlatform = null
                                systemSelectionPendingId = systemChoice.id
                                selectedSystemId = systemChoice.id
                                step = 1
                            }
                        )
                    }
                }
                selectedSystemChoice?.let { systemChoice ->
                    HelpCard(
                        title = "Selected system",
                        body = systemChoice.label
                    )
                }
                if (authorizedRomTreeUri.isNullOrBlank()) {
                    when {
                        selectedSystemChoice?.platform.equals("MAME Arcade", ignoreCase = true) -> {
                            HelpCard(
                                title = "Scan unavailable",
                                body = "Authorize the ROM folder first, then tap Scan to bulk-add MAME titles with matching overlay art."
                            )
                        }

                        selectedSystemChoice?.platform.equals("ScummVM", ignoreCase = true) -> {
                            HelpCard(
                                title = "Scan unavailable",
                                body = "Authorize the ROM folder first, then tap Scan to bulk-add ScummVM folders."
                            )
                        }

                        GameRecommendations.supportsPlatformScan(selectedSystemChoice?.platform) -> {
                            HelpCard(
                                title = "Scan unavailable",
                                body = "Authorize the ROM folder first, then tap Scan to bulk-add supported ${selectedSystemChoice?.label ?: "system"} titles."
                            )
                        }
                    }
                }
                if (selectedRomPathHint != null) {
                    HelpCard(
                        title = "Suggested ROM path",
                        body = folderPathHintText(selectedRomPathHint)
                    )
                }
            }

            step == 1 -> {
                Text(
                    text = "Choose emulator",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                HelpCard(
                    title = selectedSystemChoice?.label ?: "System",
                    body = when {
                        scanningMameAfterEmulatorPick ->
                            "Select the emulator to use for the bulk scan. Retro Launcher will scan the MAME folder and add every title that has matching overlay art and is not already added."

                        scanningScummVmAfterEmulatorPick ->
                            "Select the emulator to use for the bulk scan. Retro Launcher will scan the ScummVM folder and add subfolders that expose a detectable ScummVM game id and are not already added."

                        scanningConfiguredPlatformAfterEmulatorPick != null ->
                            "Select the emulator to use for the bulk scan. Retro Launcher will use the validated ${activeScanSystemLabel} scanner, look in the suggested ROM folder, and add supported titles that are not already added."

                        else ->
                            "Select one of the compatible emulators for this system. The launch contract stays the same as today; only the order is different."
                    }
                )
                scanFeedback?.let { message ->
                    HelpCard(title = "Scan", body = message)
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cardSpacing = 8.dp
                    val cardWidth = (maxWidth - cardSpacing) / 2
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(cardSpacing, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emulatorChoices.forEach { option ->
                            val template = templates.getOrElse(option.templateIndex) { selectedTemplate }
                            val optionPackage = template.packageName.takeIf { it.isNotBlank() }
                            val optionInstalled = optionPackage?.let { installedPackages[it] } != false
                            EmulatorTemplateCard(
                                template = template,
                                selected = selectedEmulatorKey == option.key,
                                width = cardWidth,
                                secondaryText = option.systemLabel,
                                tertiaryText = option.romFolderHint?.let { "ROM folder: $it" },
                                installed = optionInstalled,
                                enabled = !gamePickerOpening && !bulkScanRunning,
                                pending = (gamePickerOpening || bulkScanRunning) && selectedEmulatorKey == option.key,
                                onClick = {
                                    if (gamePickerOpening || bulkScanRunning) return@EmulatorTemplateCard
                                    selectedEmulatorKey = option.key
                                    scanFeedback = null
                                    when {
                                        scanningMameAfterEmulatorPick -> startMameScanForOption(option)
                                        scanningScummVmAfterEmulatorPick -> startScummVmScanForOption(option)
                                        scanningConfiguredPlatformAfterEmulatorPick != null -> startConfiguredPlatformScanForOption(option)
                                        else -> {
                                            pendingScanPlatform = null
                                            step = gameStepIndex
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                if (!emulatorInstalled && !templatePackage.isNullOrBlank()) {
                    HelpCard(
                        title = "Emulator not installed",
                        body = "Package $templatePackage is not installed on this device."
                    )
                }
            }

            step == gameStepIndex -> {
                Text(
                    if (isScummVmTemplate) "Choose game folder + ScummVM ID" else "Choose game ROM",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                HelpCard(
                    title = "Recommended folder",
                    body = folderPathHintText(selectedRomPathHint)
                )
                if (isRedreamTemplate) {
                    HelpCard(
                        title = "Redream file types",
                        body = "Choose a .gdi, .cue, .chd, or .cdi file. If you tap a Dreamcast track file like .bin, Retro Launcher will try to resolve the matching .gdi or .cue automatically."
                    )
                }
                RetroTextField(
                    value = dataUri,
                    onValueChange = { dataUri = it },
                    label = { Text(if (isScummVmTemplate) "Game folder URI" else "Game URI") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isScummVmTemplate) {
                    LabeledField(
                        value = scummVmGameId,
                        onValueChange = { scummVmGameId = it.trim().lowercase() },
                        label = "ScummVM game id",
                        help = "Use the exact game id from ScummVM (example: monkey, tentacle, ft)."
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !gamePickerOpening,
                        onClick = openSelectedGamePicker
                    ) {
                        if (gamePickerOpening) {
                            Text("Opening...")
                        } else {
                            Text(if (isScummVmTemplate) "Choose game folder" else "Choose game")
                        }
                    }
                    OutlinedButton(
                        enabled = !gamePickerOpening,
                        onClick = openSelectedGamePicker
                    ) {
                        if (gamePickerOpening) {
                            Text("Opening...")
                        } else {
                            Text("Browse")
                        }
                    }
                    if (isScummVmTemplate) {
                        Button(
                            enabled = dataUri.isNotBlank() && scummVmGameId.isNotBlank(),
                            onClick = { onVerifyScummVm(dataUri, scummVmGameId) }
                        ) {
                            Text("Verify")
                        }
                    }
                }
                LabeledField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Tile title",
                    help = "Auto-filled from file name when possible."
                )
            }

            step == artworkStepIndex -> {
                Text("Artwork", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { artworkPicker.launch(arrayOf("image/*")) }) { Text("Choose image") }
                    OutlinedButton(onClick = { artworkUri = null }) { Text("Remove image") }
                }
                val selectedArtwork = rememberDecodedBitmap(artworkUri)
                if (selectedArtwork != null) {
                    Image(
                        painter = BitmapPainter(selectedArtwork),
                        contentDescription = "Selected artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    HelpCard(title = "No artwork selected", body = "Optional step. You can add artwork later.")
                }
            }

            step == advancedStepIndex -> {
                LabeledField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = "Subtitle",
                    help = "Optional helper text, like emulator name or platform."
                )
                CollectionPicker(
                    selectedCollection = collection,
                    onCollectionSelected = { collection = it },
                    suggestions = collectionSuggestions
                )
                LabeledField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = "Package name",
                    help = "Usually required. Example: org.dolphinemu.dolphinemu or com.magneticchen.daijishou."
                )
                LabeledField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = "Activity class",
                    help = "Optional. Only needed when the emulator requires a specific screen/class."
                )
                LabeledField(
                    value = action,
                    onValueChange = { action = it },
                    label = "Intent action",
                    help = "Common values: android.intent.action.MAIN or android.intent.action.VIEW."
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RetroTextField(
                        value = dataUri,
                        onValueChange = { dataUri = it },
                        label = { Text("Data URI") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        enabled = !gamePickerOpening,
                        onClick = openSelectedGamePicker
                    ) {
                        if (gamePickerOpening) {
                            Text("Opening...")
                        } else {
                            Text(if (isScummVmTemplate) "Pick folder" else "Pick file")
                        }
                    }
                    Text(
                        if (isGameNativeTemplate) {
                            "Not used for GameNative \u2014 leave empty and fill in the app ID below instead."
                        } else {
                            "Usually the ROM/game path or custom deep link. Leave empty if you only want to open the app."
                        },
                        color = Color(0xFF98A2B3),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LabeledField(
                    value = mimeType,
                    onValueChange = { mimeType = it },
                    label = "MIME type",
                    help = "Optional. Only fill this if the emulator expects one."
                )
                LabeledField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = "Categories CSV",
                    help = "Advanced. Comma-separated Android categories."
                )
                LabeledField(
                    value = extras,
                    onValueChange = { extras = it },
                    label = "Extras JSON",
                    help = "Advanced. Example: {\"path\":\"/storage/emulated/0/ROMs/game.iso\"}. You can use {dataUri} or {filePath} placeholders."
                )
                if (isEdenTemplate) {
                    HelpCard(
                        title = "Eden launch options",
                        body = "Resolution override uses Eden's custom-config intent. Retro Launcher normalizes update and addon title IDs to the base game automatically."
                    )
                    LabeledField(
                        value = edenTitleId,
                        onValueChange = { edenTitleId = it.trim().uppercase() },
                        label = "Eden base title ID",
                        help = "Example: 0100XXXXXXXXX000. Used for per-launch custom settings like resolution."
                    )
                    Text(
                        text = "Eden resolution",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        edenResolutionOptions().forEach { option ->
                            FilterChip(
                                selected = edenResolutionSetup == option.value,
                                onClick = { edenResolutionSetup = option.value },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
                if (isGameNativeTemplate) {
                    HelpCard(
                        title = "GameNative launch options",
                        body = "GameNative games aren't ROM files \u2014 there's no game folder to pick. Enter the numeric app ID and store this game is tracked under inside GameNative's own library."
                    )
                    LabeledField(
                        value = gameNativeAppId,
                        onValueChange = { gameNativeAppId = it.filter(Char::isDigit) },
                        label = "GameNative app ID",
                        help = "The numeric ID GameNative assigned this game internally (not a Steam/GOG store ID)."
                    )
                    Text(
                        text = "Game source",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GAMENATIVE_GAME_SOURCES.forEach { source ->
                            FilterChip(
                                selected = gameNativeGameSource == source,
                                onClick = { gameNativeGameSource = source },
                                label = { Text(source) }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                enabled = step > 0,
                onClick = { step-- }
            ) { Text("Back") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step < maxStep) {
                    Button(onClick = { step++ }) { Text("Next") }
                }
                Button(
                    enabled = canSave,
                    onClick = {
                        val mergedExtras = if (isScummVmTemplate) {
                            mergeScummVmGameIdIntoExtras(extras, scummVmGameId)
                        } else {
                            extras
                        }
                        val resolvedEdenTitleId = edenTitleId.ifBlank {
                            inferEdenTitleIdFromUriString(context, dataUri).orEmpty()
                        }
                        val finalExtras = if (isEdenTemplate) {
                            mergeEdenLaunchConfigIntoExtras(
                                extrasJson = mergedExtras,
                                titleId = resolvedEdenTitleId,
                                resolutionSetup = edenResolutionSetup
                            )
                        } else if (isGameNativeTemplate) {
                            mergeGameNativeLaunchConfigIntoExtras(
                                extrasJson = mergedExtras,
                                appId = gameNativeAppId,
                                gameSource = gameNativeGameSource
                            )
                        } else {
                            mergedExtras
                        }
                        val retroArchPlatform = if (isRetroArchTemplate) {
                            selectedRetroArchSystem?.let { retroArchSystem ->
                                GamePlatforms.inferFromUriString(retroArchSystem.romFolder)
                                    ?: GamePlatforms.inferFromUriString(retroArchSystem.id)
                                    ?: GamePlatforms.normalizeOrNull(retroArchSystem.system)
                            }
                        } else {
                            null
                        }
                        onAddCustom(
                            GameEntry(
                                title = title.ifBlank { "Custom game" },
                                subtitle = subtitle,
                                genre = collection.ifBlank { null },
                                platform = retroArchPlatform
                                    ?: selectedFlowOption?.platform
                                    ?: GamePlatforms.inferFromPackageName(packageName.ifBlank { null })
                                    ?: GamePlatforms.normalizeOrNull(selectedTemplate.emulatedPlatform)
                                    ?: GamePlatforms.inferFromUriString(selectedFlowOption?.romFolderHint)
                                    ?: GamePlatforms.inferFromUriString(selectedTemplate.romSubfolderHint)
                                    ?: GamePlatforms.inferFromUriString(selectedTemplate.emulatedPlatform),
                                imageUri = artworkUri,
                                sourceType = GameSourceType.CUSTOM_INTENT,
                                packageName = packageName.ifBlank { null },
                                activityName = activityName.ifBlank { null },
                                intentAction = action.ifBlank { null },
                                intentDataUri = dataUri.ifBlank { null },
                                mimeType = mimeType.ifBlank { null },
                                categoriesCsv = categories.ifBlank { null },
                                extrasJson = finalExtras.ifBlank { null }
                            )
                        )
                    }
                ) {
                    Text("Save custom tile")
                }
            }
        }
    }
}

@Composable
private fun SystemChoiceCard(
    choice: GameSystemChoice,
    selected: Boolean,
    emulatorCount: Int,
    enabled: Boolean = true,
    pending: Boolean = false,
    secondaryActionLabel: String? = null,
    secondaryActionEnabled: Boolean = true,
    onSecondaryAction: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF2A3A52) else Color(0xFF1F2937)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.65f
            }
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = choice.label,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            choice.romFolderHint?.let { hint ->
                Text(
                    text = folderLabelText(hint),
                    color = Color(0xFF98A2B3),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (emulatorCount == 1) "1 emulator" else "$emulatorCount emulators",
                color = Color(0xFFF2AA4C),
                style = MaterialTheme.typography.labelSmall
            )
            if (pending) {
                Text(
                    text = "Opening...",
                    color = Color(0xFF98A2B3),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    enabled = secondaryActionEnabled
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun EmulatorTemplateCard(
    template: CustomIntentTemplate,
    selected: Boolean,
    width: Dp = 160.dp,
    secondaryText: String? = template.emulatedPlatform,
    tertiaryText: String? = null,
    installed: Boolean = true,
    enabled: Boolean = true,
    pending: Boolean = false,
    onClick: () -> Unit
) {
    val icon = rememberPackageIconBitmap(template.packageName.takeIf { it.isNotBlank() })
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF2A3A52) else Color(0xFF1F2937)
        ),
        modifier = Modifier
            .width(width)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.65f
            }
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF111927)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        painter = BitmapPainter(icon),
                        contentDescription = template.label,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = template.label.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Text(
                text = template.label,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            secondaryText?.takeIf { it.isNotBlank() }?.let { platform ->
                Text(
                    text = platform,
                    color = Color(0xFF98A2B3),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            tertiaryText?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    text = detail,
                    color = Color(0xFFB8C2D1),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!installed) {
                Text(
                    text = "Not installed",
                    color = Color(0xFFFDB022),
                    style = MaterialTheme.typography.labelSmall
                )
            } else if (pending) {
                Text(
                    text = "Opening...",
                    color = Color(0xFF98A2B3),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun rememberPackageIconBitmap(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, context, packageName) {
        value = withContext(Dispatchers.IO) {
            if (packageName.isNullOrBlank()) return@withContext null
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

private fun initialRomPickerUri(
    context: Context,
    authorizedRomTreeUri: String?,
    subfolderHint: String?,
    platform: String?,
    pickerKind: GamePickerKind
): Uri? =
    resolvePreferredRomFolderUri(
        context = context,
        authorizedRomTreeUri = authorizedRomTreeUri,
        subfolderHint = subfolderHint,
        platform = platform,
        preferTreeUri = pickerKind == GamePickerKind.FOLDER
    )

private fun createRomFilePickerIntent(initialUri: Uri?): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        if (initialUri != null) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
    }

private fun String?.isRetroArchPackageName(): Boolean =
    this == "com.retroarch" || this == "com.retroarch.aarch64"

private fun String?.isDolphinPackageName(): Boolean =
    this == "org.dolphinemu.dolphinemu" || this == "org.dolphinemu.mmjr"

private fun String?.isScummVmPackageName(): Boolean =
    this == "org.scummvm.scummvm"

private fun String?.isEdenPackageName(): Boolean =
    this == EDEN_PACKAGE_NAME

private fun String?.isGameNativePackageName(): Boolean =
    this == GAMENATIVE_PACKAGE_NAME

private fun readScummVmGameIdFromExtras(extrasJson: String?): String {
    val raw = extrasJson?.takeIf { it.isNotBlank() } ?: return ""
    return runCatching {
        JSONObject(raw).optString(SCUMMVM_EXTRA_GAME_ID)
    }.getOrNull()
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        .orEmpty()
}

private fun mergeScummVmGameIdIntoExtras(extrasJson: String, gameId: String): String {
    val trimmed = gameId.trim().lowercase()
    val json = runCatching {
        extrasJson.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
    }.getOrDefault(JSONObject())
    if (trimmed.isNotBlank()) {
        json.put(SCUMMVM_EXTRA_GAME_ID, trimmed)
    } else {
        json.remove(SCUMMVM_EXTRA_GAME_ID)
    }
    return json.toString()
}

private data class EdenResolutionOption(
    val label: String,
    val value: Int?
)

private fun edenResolutionOptions(): List<EdenResolutionOption> = listOf(
    EdenResolutionOption(label = "Default", value = null),
    EdenResolutionOption(label = "0.5x", value = 1),
    EdenResolutionOption(label = "0.75x", value = 2),
    EdenResolutionOption(label = "1x", value = 3)
)

private fun defaultEdenResolutionSetup(): Int? = null

private fun readEdenTitleIdFromExtras(extrasJson: String?): String {
    val raw = extrasJson?.takeIf { it.isNotBlank() } ?: return ""
    return runCatching {
        JSONObject(raw).optString(EDEN_EXTRA_TITLE_ID)
    }.getOrNull()
        ?.let(::normalizeSwitchBaseTitleId)
        .orEmpty()
}

private fun readEdenResolutionSetupFromExtras(extrasJson: String?): Int? {
    val raw = extrasJson?.takeIf { it.isNotBlank() } ?: return defaultEdenResolutionSetup()
    return runCatching {
        val json = JSONObject(raw)
        if (!json.has(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP)) {
            defaultEdenResolutionSetup()
        } else {
            json.optInt(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }
        }
    }.getOrDefault(defaultEdenResolutionSetup())
}

private fun mergeEdenLaunchConfigIntoExtras(
    extrasJson: String,
    titleId: String,
    resolutionSetup: Int?
): String {
    val json = runCatching {
        extrasJson.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
    }.getOrDefault(JSONObject())
    val normalizedTitleId = normalizeSwitchBaseTitleId(titleId).orEmpty()
    if (normalizedTitleId.isNotBlank()) {
        json.put(EDEN_EXTRA_TITLE_ID, normalizedTitleId)
    } else {
        json.remove(EDEN_EXTRA_TITLE_ID)
    }
    if (resolutionSetup != null) {
        json.put(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP, resolutionSetup)
    } else {
        json.remove(RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP)
    }
    return json.toString()
}

private fun inferScummVmGameIdFromUri(context: Context, uri: Uri): String? {
    val root = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()
        ?: runCatching { DocumentFile.fromSingleUri(context, uri) }.getOrNull()
        ?: return null
    val topLevel = runCatching { root.listFiles().asSequence() }.getOrDefault(emptySequence())
    val fromTop = topLevel
        .filter { it.isFile }
        .mapNotNull { markerNameToScummVmGameId(it.name ?: it.uri.lastPathSegment.orEmpty()) }
        .firstOrNull()
    if (!fromTop.isNullOrBlank()) return fromTop
    return topLevel
        .filter { it.isDirectory }
        .flatMap { dir -> runCatching { dir.listFiles().asSequence() }.getOrDefault(emptySequence()) }
        .filter { it.isFile }
        .mapNotNull { markerNameToScummVmGameId(it.name ?: it.uri.lastPathSegment.orEmpty()) }
        .firstOrNull()
}

private fun markerNameToScummVmGameId(rawName: String): String? {
    val name = rawName.trim()
    val lower = name.lowercase()
    return when {
        lower.endsWith(".scummvm") -> name.substring(0, name.length - ".scummvm".length)
        lower.endsWith(".svm") -> name.substring(0, name.length - ".svm".length)
        else -> null
    }?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
}

private fun inferEdenTitleIdFromUri(context: Context, uri: Uri): String? {
    val displayName = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
    }.getOrNull()
    val fallback = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.let { runCatching { URLDecoder.decode(it, Charsets.UTF_8.name()) }.getOrDefault(it) }
    return extractEdenTitleId(displayName ?: fallback)
        ?: normalizeSwitchBaseTitleId(inferSwitchTitleIdFromPackageUri(context, uri))
}

private fun inferEdenTitleIdFromUriString(context: Context, uriString: String?): String? {
    val parsed = uriString?.takeIf { it.isNotBlank() }?.let {
        runCatching { Uri.parse(it) }.getOrNull()
    } ?: return null
    return inferEdenTitleIdFromUri(context, parsed)
}

private fun extractEdenTitleId(rawValue: String?): String? {
    val candidate = rawValue?.trim().orEmpty()
    if (candidate.isBlank()) return null
    return EDEN_TITLE_ID_REGEX.find(candidate)?.value?.let(::normalizeSwitchBaseTitleId)
}

private const val SCUMMVM_EXTRA_GAME_ID = "scummvmGameId"
private const val EDEN_PACKAGE_NAME = "dev.eden.eden_emulator"
private const val EDEN_EXTRA_TITLE_ID = "title_id"
private const val RETRO_LAUNCHER_EDEN_RESOLUTION_SETUP = "_retroLauncherEdenResolutionSetup"
private val EDEN_TITLE_ID_REGEX = Regex("""0100[0-9A-Fa-f]{12}""")
private const val GAMENATIVE_PACKAGE_NAME = "app.gamenative"
private const val GAMENATIVE_EXTRA_APP_ID = "app_id"
private const val GAMENATIVE_EXTRA_GAME_SOURCE = "game_source"
private val GAMENATIVE_GAME_SOURCES = listOf("STEAM", "GOG", "EPIC", "AMAZON", "CUSTOM_GAME")

private fun readGameNativeAppIdFromExtras(extrasJson: String?): String {
    val raw = extrasJson?.takeIf { it.isNotBlank() } ?: return ""
    return runCatching {
        JSONObject(raw).optInt(GAMENATIVE_EXTRA_APP_ID, 0)
    }.getOrDefault(0).takeIf { it > 0 }?.toString().orEmpty()
}

private fun readGameNativeSourceFromExtras(extrasJson: String?): String {
    val raw = extrasJson?.takeIf { it.isNotBlank() } ?: return GAMENATIVE_GAME_SOURCES.first()
    return runCatching {
        JSONObject(raw).optString(GAMENATIVE_EXTRA_GAME_SOURCE)
    }.getOrNull()
        ?.uppercase()
        ?.takeIf { it in GAMENATIVE_GAME_SOURCES }
        ?: GAMENATIVE_GAME_SOURCES.first()
}

private fun mergeGameNativeLaunchConfigIntoExtras(
    extrasJson: String,
    appId: String,
    gameSource: String
): String {
    val json = runCatching {
        extrasJson.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
    }.getOrDefault(JSONObject())
    val normalizedAppId = appId.trim().toIntOrNull()
    if (normalizedAppId != null && normalizedAppId > 0) {
        json.put(GAMENATIVE_EXTRA_APP_ID, normalizedAppId)
    }
    json.put(GAMENATIVE_EXTRA_GAME_SOURCE, gameSource.trim().uppercase().ifBlank { GAMENATIVE_GAME_SOURCES.first() })
    return json.toString()
}

private fun webArtworkSearchQuery(game: GameEntry, fallbackTitle: String): String {
    return game.artworkSearchQuery(fallbackTitle)
}

private fun retroArchExtrasJson(packageName: String, coreSoFile: String): String {
    val corePath = "/data/data/$packageName/cores/$coreSoFile"
    return "{\"ROM\":\"{filePath}\",\"LIBRETRO\":\"$corePath\"}"
}

@Composable
private fun HelpCard(title: String, body: String) {
    HelpCard(title = title, body = AnnotatedString(body))
}

@Composable
private fun HelpCard(title: String, body: AnnotatedString) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2433)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
            Text(body, color = Color(0xFFB8C2D1), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun folderPathHintText(folderName: String?): AnnotatedString = buildAnnotatedString {
    append("<selected ROM folder>")
    romFolderHintDisplayLabel(folderName)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { hint ->
            append("/")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(hint)
            }
        }
}

private fun folderLabelText(folderName: String): AnnotatedString = buildAnnotatedString {
    append("ROM folder: ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(romFolderHintDisplayLabel(folderName) ?: folderName)
    }
}

@Composable
private fun LabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    help: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RetroTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Text(help, color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CollectionPicker(
    selectedCollection: String,
    onCollectionSelected: (String) -> Unit,
    suggestions: List<String>
) {
    val normalizedSuggestions = remember(suggestions) {
        suggestions
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val query = selectedCollection.trim()
    val visibleSuggestions = remember(query, normalizedSuggestions) {
        if (query.isBlank()) {
            normalizedSuggestions.take(8)
        } else {
            normalizedSuggestions
                .filter { it.contains(query, ignoreCase = true) }
                .take(8)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabeledField(
            value = selectedCollection,
            onValueChange = onCollectionSelected,
            label = "Collection",
            help = "Type any collection name. Existing collections are suggested below."
        )
        if (visibleSuggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleSuggestions.forEach { suggestion ->
                    FilterChip(
                        selected = selectedCollection.equals(suggestion, ignoreCase = true),
                        onClick = { onCollectionSelected(suggestion) },
                        label = { Text(suggestion) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2C4A3E),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RetroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color(0xFF98A2B3),
            focusedContainerColor = Color(0xFF111927),
            unfocusedContainerColor = Color(0xFF111927),
            disabledContainerColor = Color(0xFF111927),
            cursorColor = Color(0xFFF2AA4C),
            focusedBorderColor = Color(0xFFF2AA4C),
            unfocusedBorderColor = Color(0xFF475467),
            focusedLabelColor = Color(0xFFF2AA4C),
            unfocusedLabelColor = Color(0xFF98A2B3)
        )
    )
}

@Composable
private fun EditGameDialog(
    game: GameEntry,
    onDismiss: () -> Unit,
    onSave: (GameEntry) -> Unit,
    onDelete: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onImportImageUrl: (String) -> Unit,
    onSearchWebImage: (String) -> Unit,
    suggestions: List<ArtworkCandidate>,
    suggestionsLoading: Boolean,
    onLoadSuggestions: (String) -> Unit,
    onUseTopSuggestion: (String) -> Unit,
    onSelectPreferredTarget: () -> Unit,
    onRemoveTarget: (LaunchTarget) -> Unit,
    collectionSuggestions: List<String>
) {
    val context = LocalContext.current
    var step by rememberSaveable(game.id) { mutableIntStateOf(0) }
    val primaryTargetKey = game.primaryTarget().stableKey()
    var title by rememberSaveable(game.id, game.title) { mutableStateOf(game.title) }
    var subtitle by rememberSaveable(game.id, game.subtitle, primaryTargetKey) { mutableStateOf(game.subtitle) }
    var collection by rememberSaveable(game.id, game.genre) { mutableStateOf(game.genre.orEmpty()) }
    var packageName by rememberSaveable(game.id, primaryTargetKey, game.packageName) { mutableStateOf(game.packageName.orEmpty()) }
    var activityName by rememberSaveable(game.id, primaryTargetKey, game.activityName) { mutableStateOf(game.activityName.orEmpty()) }
    var action by rememberSaveable(game.id, primaryTargetKey, game.intentAction) { mutableStateOf(game.intentAction.orEmpty()) }
    var dataUri by rememberSaveable(game.id, primaryTargetKey, game.intentDataUri) { mutableStateOf(game.intentDataUri.orEmpty()) }
    var mimeType by rememberSaveable(game.id, primaryTargetKey, game.mimeType) { mutableStateOf(game.mimeType.orEmpty()) }
    var categories by rememberSaveable(game.id, primaryTargetKey, game.categoriesCsv) { mutableStateOf(game.categoriesCsv.orEmpty()) }
    var extras by rememberSaveable(game.id, primaryTargetKey, game.extrasJson) { mutableStateOf(game.extrasJson.orEmpty()) }
    var edenTitleId by rememberSaveable(game.id, primaryTargetKey, game.extrasJson) { mutableStateOf(readEdenTitleIdFromExtras(game.extrasJson)) }
    var edenResolutionSetup by rememberSaveable(game.id, primaryTargetKey, game.extrasJson) { mutableStateOf(readEdenResolutionSetupFromExtras(game.extrasJson)) }
    var gameNativeAppId by rememberSaveable(game.id, primaryTargetKey, game.extrasJson) { mutableStateOf(readGameNativeAppIdFromExtras(game.extrasJson)) }
    var gameNativeGameSource by rememberSaveable(game.id, primaryTargetKey, game.extrasJson) { mutableStateOf(readGameNativeSourceFromExtras(game.extrasJson)) }
    var imageUrl by rememberSaveable(game.id) { mutableStateOf("") }
    val dataUriPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            dataUri = uri.toString()
            if (packageName.isEdenPackageName() && edenTitleId.isBlank()) {
                inferEdenTitleIdFromUri(context, uri)?.let { inferredId ->
                    edenTitleId = inferredId
                }
            }
        }
    }
    LaunchedEffect(packageName, dataUri) {
        if (packageName.isEdenPackageName() && edenTitleId.isBlank()) {
            inferEdenTitleIdFromUriString(context, dataUri)?.let { inferredId ->
                edenTitleId = inferredId
            }
        }
    }
    val targets = remember(game) { game.allTargets() }
    val preferred = remember(game) { game.preferredTarget() ?: game.primaryTarget() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Edit tile", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Basics", "Launch", "Artwork").forEachIndexed { index, label ->
                        FilterChip(selected = step == index, onClick = { step = index }, label = { Text(label) })
                    }
                }

                when (step) {
                    0 -> {
                        RetroTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                        RetroTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                        CollectionPicker(
                            selectedCollection = collection,
                            onCollectionSelected = { collection = it },
                            suggestions = collectionSuggestions
                        )

                        if (targets.size > 1) {
                            HelpCard(
                                title = "Launch targets",
                                body = "Choose which saved target is default, or remove ones you no longer use."
                            )
                            targets.forEach { target ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2433)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = target.label.ifBlank { target.packageName ?: target.sourceType.name },
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = target.packageName ?: target.activityName ?: target.sourceType.name,
                                                    color = Color(0xFF98A2B3),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (preferred.stableKey() == target.stableKey()) {
                                                Text(
                                                    text = "Default",
                                                    color = Color(0xFFF2AA4C),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        TextButton(
                                            onClick = { onRemoveTarget(target) },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                            OutlinedButton(onClick = onSelectPreferredTarget) {
                                Text("Choose default")
                            }
                        }
                    }

                    1 -> {
                        HelpCard(
                            title = "Advanced launch details",
                            body = "Adjust package, action, URI, and extras for the primary target."
                        )
                        RetroTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package name") }, modifier = Modifier.fillMaxWidth())
                        RetroTextField(value = activityName, onValueChange = { activityName = it }, label = { Text("Activity class") }, modifier = Modifier.fillMaxWidth())
                        RetroTextField(value = action, onValueChange = { action = it }, label = { Text("Intent action") }, modifier = Modifier.fillMaxWidth())
                        RetroTextField(value = dataUri, onValueChange = { dataUri = it }, label = { Text("Data URI") }, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = { dataUriPicker.launch(arrayOf("*/*")) }) {
                            Text("Pick file")
                        }
                        RetroTextField(value = mimeType, onValueChange = { mimeType = it }, label = { Text("MIME type") }, modifier = Modifier.fillMaxWidth())
                        RetroTextField(value = categories, onValueChange = { categories = it }, label = { Text("Categories CSV") }, modifier = Modifier.fillMaxWidth())
                        RetroTextField(value = extras, onValueChange = { extras = it }, label = { Text("Extras JSON") }, modifier = Modifier.fillMaxWidth())
                        if (packageName.isEdenPackageName()) {
                            HelpCard(
                                title = "Eden launch options",
                                body = "Resolution override uses Eden's custom-config intent. Retro Launcher normalizes update and addon title IDs to the base game automatically."
                            )
                            LabeledField(
                                value = edenTitleId,
                                onValueChange = { edenTitleId = it.trim().uppercase() },
                                label = "Eden base title ID",
                                help = "Example: 0100XXXXXXXXX000. Used for per-launch custom settings like resolution."
                            )
                            Text(
                                text = "Eden resolution",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                edenResolutionOptions().forEach { option ->
                                    FilterChip(
                                        selected = edenResolutionSetup == option.value,
                                        onClick = { edenResolutionSetup = option.value },
                                        label = { Text(option.label) }
                                    )
                                }
                            }
                        }
                        if (packageName.isGameNativePackageName()) {
                            HelpCard(
                                title = "GameNative launch options",
                                body = "GameNative games aren't ROM files \u2014 there's no game folder to pick. Enter the numeric app ID and store this game is tracked under inside GameNative's own library."
                            )
                            LabeledField(
                                value = gameNativeAppId,
                                onValueChange = { gameNativeAppId = it.filter(Char::isDigit) },
                                label = "GameNative app ID",
                                help = "The numeric ID GameNative assigned this game internally (not a Steam/GOG store ID)."
                            )
                            Text(
                                text = "Game source",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GAMENATIVE_GAME_SOURCES.forEach { source ->
                                    FilterChip(
                                        selected = gameNativeGameSource == source,
                                        onClick = { gameNativeGameSource = source },
                                        label = { Text(source) }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onPickImage) {
                                Text("Choose image")
                            }
                            OutlinedButton(onClick = onRemoveImage) {
                                Text("Remove image")
                            }
                        }
                        HelpCard(
                            title = "Better artwork",
                            body = "Fetch suggestions in-app, or paste a direct image URL."
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onUseTopSuggestion(title.ifBlank { game.title }) }) {
                                Text("Auto top image")
                            }
                            OutlinedButton(onClick = { onLoadSuggestions(title.ifBlank { game.title }) }) {
                                Text("Find images")
                            }
                            OutlinedButton(onClick = { onSearchWebImage(title.ifBlank { game.title }) }) {
                                Text("Search web")
                            }
                        }
                        if (suggestionsLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFF2AA4C),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        if (suggestions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                suggestions.forEach { candidate ->
                                    ArtworkCandidateCard(candidate = candidate, onUse = {
                                        onImportImageUrl(candidate.imageUrl)
                                    })
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RetroTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it },
                                label = { Text("Image URL") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (URLUtil.isValidUrl(imageUrl)) {
                                            onImportImageUrl(imageUrl)
                                        }
                                    }
                                )
                            )
                            Button(
                                enabled = URLUtil.isValidUrl(imageUrl),
                                onClick = { onImportImageUrl(imageUrl) },
                                modifier = Modifier.widthIn(min = 88.dp)
                            ) {
                                Text("Use")
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDelete) {
                            Text("Delete")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = step > 0,
                                onClick = { step-- }
                            ) { Text("Back") }
                            if (step < 2) {
                                Button(onClick = { step++ }) { Text("Next") }
                            }
                        }
                        Button(
                            onClick = {
                                val resolvedEdenTitleId = edenTitleId.ifBlank {
                                    inferEdenTitleIdFromUriString(context, dataUri).orEmpty()
                                }
                                val mergedExtras = if (packageName.isEdenPackageName()) {
                                    mergeEdenLaunchConfigIntoExtras(
                                        extrasJson = extras,
                                        titleId = resolvedEdenTitleId,
                                        resolutionSetup = edenResolutionSetup
                                    )
                                } else if (packageName.isGameNativePackageName()) {
                                    mergeGameNativeLaunchConfigIntoExtras(
                                        extrasJson = extras,
                                        appId = gameNativeAppId,
                                        gameSource = gameNativeGameSource
                                    )
                                } else {
                                    extras
                                }
                                onSave(
                                    game.copy(
                                        title = title,
                                        subtitle = subtitle,
                                        genre = collection.ifBlank { null },
                                        packageName = packageName.ifBlank { null },
                                        activityName = activityName.ifBlank { null },
                                        intentAction = action.ifBlank { null },
                                        intentDataUri = dataUri.ifBlank { null },
                                        mimeType = mimeType.ifBlank { null },
                                        categoriesCsv = categories.ifBlank { null },
                                        extrasJson = mergedExtras.ifBlank { null }
                                    )
                                )
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkCandidateCard(
    candidate: ArtworkCandidate,
    onUse: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2433)),
        modifier = Modifier.width(140.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val image = rememberRemoteBitmap(candidate.imageUrl)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF202938))
            ) {
                if (image != null) {
                    Image(
                        painter = BitmapPainter(image),
                        contentDescription = candidate.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = candidate.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = candidate.source,
                    color = Color(0xFF98A2B3),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Button(
                onClick = onUse,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Use")
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun rememberRemoteBitmap(url: String): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                java.net.URL(url).openStream().use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

private fun inferGameTitleFromUri(context: Context, uri: Uri): String? {
    val displayName = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
    }.getOrNull()
    val fallback = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.let { runCatching { URLDecoder.decode(it, Charsets.UTF_8.name()) }.getOrDefault(it) }
    return sanitizeGameTitleFromFileName(displayName ?: fallback)
}

private fun hasPersistedReadPermission(context: Context, uriString: String?): Boolean {
    val parsed = uriString?.takeIf { it.isNotBlank() }?.let {
        runCatching { Uri.parse(it) }.getOrNull()
    } ?: return false
    val hasRuntimeReadAccess = context.checkUriPermission(
        parsed,
        Process.myPid(),
        Process.myUid(),
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED
    if (hasRuntimeReadAccess) return true
    val parsedTreeId = runCatching { DocumentsContract.getTreeDocumentId(parsed) }
        .getOrElse { runCatching { DocumentsContract.getDocumentId(parsed) }.getOrNull() }
    return context.contentResolver.persistedUriPermissions.any { permission ->
        if (!permission.isReadPermission) return@any false
        if (permission.uri == parsed || permission.uri.toString() == parsed.toString()) return@any true
        if (permission.uri.authority != parsed.authority) return@any false
        val permissionTreeId = runCatching { DocumentsContract.getTreeDocumentId(permission.uri) }
            .getOrElse { runCatching { DocumentsContract.getDocumentId(permission.uri) }.getOrNull() }
        permissionTreeId != null && permissionTreeId == parsedTreeId
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean = runCatching {
    val pm = context.packageManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0)
    }
}.isSuccess

private fun sanitizeGameTitleFromFileName(rawName: String?): String? {
    val base = rawName?.trim().orEmpty().ifBlank { return null }
    val withoutExtension = base.substringBeforeLast('.', missingDelimiterValue = base)
    val cleaned = withoutExtension
        .replace('_', ' ')
        .replace('.', ' ')
        .replace(Regex("""\[[^\]]*]"""), " ")
        .replace(Regex("""\((?:[^)]*(?:USA|EUR|JPN|Rev|v\d+)[^)]*)\)""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    return cleaned.ifBlank { null }
}
