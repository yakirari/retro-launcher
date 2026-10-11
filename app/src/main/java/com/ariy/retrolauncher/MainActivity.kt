package com.ariy.retrolauncher

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusDirection
import com.ariy.retrolauncher.ui.AdaptiveLauncherDialog
import com.ariy.retrolauncher.ui.curatedDisplayTitle
import com.ariy.retrolauncher.ui.tileTitle
import com.ariy.retrolauncher.ui.libraryDisplayTitle
import com.ariy.retrolauncher.ui.cabinetBackgroundMask
import com.ariy.retrolauncher.ui.responsiveGridColumns
import com.ariy.retrolauncher.ui.responsiveGalleryWidth
import com.ariy.retrolauncher.ui.artworkContentBounds
import com.ariy.retrolauncher.ui.CoverArtwork
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
import com.ariy.retrolauncher.launch.arcadeRomShortNameForArtworkSearch
import com.ariy.retrolauncher.model.GlobalGallerySettings
import com.ariy.retrolauncher.model.validated
import com.ariy.retrolauncher.ui.libraryGameCaption
import com.ariy.retrolauncher.ui.playStationCoverBounds
import com.ariy.retrolauncher.ui.GameSortOrder
import com.ariy.retrolauncher.ui.sortLibraryGames
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
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
private const val TRANSPARENT_TILE_BACKGROUND_ALPHA_SCALE = 0.55f
private const val LAUNCHER_BACKGROUND_STATIC_MAX_DIMENSION = 1_920
private const val LAUNCHER_BACKGROUND_ANIMATED_MAX_DIMENSION = 3_072
private const val LAUNCHER_BACKGROUND_ANIMATION_SCALE = 2.03f
private val LauncherBackgroundScrimColor = Color(0x8C0D1117)
private val LauncherBackgroundBlurRadius = 8.dp
private const val LAUNCHER_BACKGROUND_ANIMATION_SEGMENT_MILLIS = 11_000
private const val LAUNCHER_BACKGROUND_START_STEPS_PER_SEGMENT = 32
private const val LAUNCHER_BACKGROUND_START_STEP_DELTA = 11

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
        importRequestedShortcut(intent)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importRequestedShortcut(intent)
    }

    private fun importRequestedShortcut(intent: Intent) {
        val packageName = intent.getStringExtra("pinnedShortcutPackage") ?: return
        val shortcutId = intent.getStringExtra("pinnedShortcutId") ?: return
        val user = intent.getLongExtra("pinnedShortcutUser", -1L)
        intent.removeExtra("pinnedShortcutPackage")
        intent.removeExtra("pinnedShortcutId")
        intent.removeExtra("pinnedShortcutUser")
        viewModel.importPublishedShortcut(packageName, shortcutId, user)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onLauncherResumed()
    }

    override fun onPause() {
        viewModel.onLauncherPaused()
        super.onPause()
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

private fun SharedPreferences.Editor.putGallerySettings(settings: GlobalGallerySettings): SharedPreferences.Editor {
    settings.toPreferenceValues().forEach { (key, value) ->
        when (value) {
            is Int -> putInt(key, value)
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
        }
    }
    return this
}

private fun loadGlobalGallerySettings(prefs: SharedPreferences): GlobalGallerySettings = synchronized(prefs) {
    val values = prefs.all
    val settings = GlobalGallerySettings.fromPreferenceValues(values)
    // Finish the one-time migration before loading Compose state; no asynchronous legacy count load.
    if (GlobalGallerySettings.needsMigration(values)) {
        prefs.edit().putGallerySettings(settings).commit()
    }
    settings
}

@Composable
private fun LauncherRoot(viewModel: LauncherViewModel = viewModel()) {
    val resetting by viewModel.resetting.collectAsStateWithLifecycle()
    val resetNonce by viewModel.resetNonce.collectAsStateWithLifecycle()
    val resetFailures by viewModel.resetFailures.collectAsStateWithLifecycle()
    LaunchedEffect(resetNonce) {
        if (resetNonce != 0L) recentTileBitmapCache.evictAll()
    }
    if (resetting) {
        BackHandler { }
        Surface(color = AppBackgroundColor, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Resetting launcher…", color = Color.White)
                Text("Stopping scans and downloads before clearing private data. ROMs and apps are kept.", color = Color(0xFF98A2B3))
            }
        }
    } else if (resetFailures.isNotEmpty()) {
        BackHandler { viewModel.dismissResetFailures() }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissResetFailures,
            title = { Text("Reset incomplete") },
            text = { Text("Some saved data or permissions may remain:\n\n${resetFailures.joinToString("\n")}\n\nROMs and installed apps were not deleted. Retry Reset launcher from Options.") },
            confirmButton = { TextButton(onClick = viewModel::dismissResetFailures) { Text("OK") } }
        )
    } else {
        // Dispose every remembered UI state and Compose worker; reload preferences without restart.
        androidx.compose.runtime.key(resetNonce) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLauncherSessionActive provides { viewModel.acceptsSession(resetNonce) }
            ) { LauncherScreen(viewModel, resetNonce) }
        }
    }
}

@Composable
private fun LauncherScreen(viewModel: LauncherViewModel, session: Long) {
    // This is the navigation root. Finishing HOME on Back/B only restarts the same gallery.
    // Dialogs handle Back in their own window before it reaches this root handler.
    BackHandler { }
    val games by viewModel.games.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcutOptions.collectAsStateWithLifecycle()
    val activities by viewModel.activityOptions.collectAsStateWithLifecycle()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
    val setupCompleted by viewModel.setupCompleted.collectAsStateWithLifecycle()
    val libraryScan by viewModel.libraryScan.collectAsStateWithLifecycle()
    var setupOpen by rememberSaveable { mutableStateOf(false) }
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
    var gallerySettings by remember(prefs) { mutableStateOf(loadGlobalGallerySettings(prefs)) }
    val updateGallerySettings: (GlobalGallerySettings) -> Unit = { settings ->
        if (viewModel.acceptsSession(session)) {
            gallerySettings = settings.validated()
            prefs.edit().putGallerySettings(gallerySettings).apply()
        }
    }
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
        if (viewModel.acceptsSession(session)) {
            saveCachedRecommendationPlatformFilters(
                prefs = prefs,
                authorizedRomTreeUri = authorizedRomTreeUri,
                supportedFilters = supportedRecommendationFilters,
                filters = availableFilters.toSet()
            )
        }
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
    var galleryFilterTransitionDirection by rememberSaveable { mutableIntStateOf(0) }
    var launcherBackgroundAnimationEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(LAUNCHER_BACKGROUND_ANIMATION_PREF_KEY, false))
    }
    var launcherBackgroundBlurEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(LAUNCHER_BACKGROUND_BLUR_PREF_KEY, false))
    }
    var showTop10Badges by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SHOW_TOP_10_BADGES_PREF_KEY, true))
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
    var gameSortOrder by rememberSaveable { mutableStateOf(GameSortOrder.fromStored(prefs.getString("game_sort_order", null))) }
    val preparedGames = rememberPreparedGames(games, effectiveGenreFilter, gameSortOrder)
    val visibleGames = preparedGames.games
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
    val launcherLayoutMode = gallerySettings.layoutMode
    val gridColumns = gallerySettings.tileCount
    val launcherTileBackgroundMode = gallerySettings.tileBackgroundMode
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
        if (viewModel.acceptsSession(session) && !selectedFilter.equals(genreFilter, ignoreCase = true)) {
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
        if (!viewModel.acceptsSession(session)) return@rememberLauncherForActivityResult
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
        if (!viewModel.acceptsSession(session)) return@rememberLauncherForActivityResult
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
        if (!viewModel.acceptsSession(session)) return@rememberLauncherForActivityResult
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onSuccess {
                setupOpen = true
                viewModel.authorizeAndScanLibrary(uri.toString())
            }.onFailure {
                viewModel.setStatus("Could not retain access to this ROM folder. Choose a folder that permits persistent read access and try again.")
            }
        }
    }

    if (!setupCompleted || setupOpen) {
        com.ariy.retrolauncher.ui.LibrarySetupDialog(
            firstRun = !setupCompleted, progress = libraryScan, hasRoot = viewModel.hasSelectedRomRoot(),
            isDefaultLauncher = isDefaultLauncher,
            installedCount = activities.filter { com.ariy.retrolauncher.launch.isEmulatorLauncherPackage(it.packageName) }
                .distinctBy { it.packageName to it.userSerial }.size,
            sharedGameCount = games.count { game -> game.allTargets().any {
                it.sourceType == GameSourceType.SHORTCUT && it.packageName?.let(LibrarySetupPolicy::isSharedHubPackage) == true
            } },
            onChooseRoot = { setupOpen = true; romFolderPicker.launch(null) },
            onScan = viewModel::scanLibrary, onCancel = viewModel::cancelLibraryScan,
            onSetHome = {
                runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
                    .onFailure { viewModel.setStatus("Open Android Settings → Default apps → Home app and select Retro Launcher.") }
            },
            onDismiss = { viewModel.completeSetup(); setupOpen = false }
        )
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

    androidx.compose.runtime.CompositionLocalProvider(LocalGameCaptionsEnabled provides gallerySettings.showGameCaptions) {
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
                gameSortOrder = gameSortOrder,
                onGameSortOrderSelected = { order ->
                    gameSortOrder = order
                    prefs.edit().putString("game_sort_order", order.name).apply()
                },
                showGameCaptions = gallerySettings.showGameCaptions,
                onShowGameCaptionsChanged = { enabled ->
                    updateGallerySettings(gallerySettings.copy(showGameCaptions = enabled))
                },
                artworkPlatforms = games.mapNotNull { GamePlatforms.normalizeOrNull(it.platform) }.distinct().sorted(),
                artworkRefreshPlatforms = activeArtworkBackfillPlatforms,
                onRefreshPlatformArtwork = { viewModel.fillMissingArtworkForPlatform(it, refreshAll = true) },
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
                launcherLayoutMode = launcherLayoutMode,
                launcherBackground = launcherBackground,
                launcherBackgroundOptions = launcherBackgroundOptions,
                launcherBackgroundAnimationEnabled = launcherBackgroundAnimationEnabled,
                launcherBackgroundBlurEnabled = launcherBackgroundBlurEnabled,
                showTop10Badges = showTop10Badges,
                launcherTileBackgroundMode = launcherTileBackgroundMode,
                hasCustomLauncherBackground = hasCustomLauncherBackground,
                onGridSelected = { updateGallerySettings(gallerySettings.withTileCount(it)) },
                onGenreSelected = { filter -> selectFilter(filter, 0) },
                onAdd = {
                    viewModel.refreshInstalledTargets()
                    openAddDialog(0, null, null)
                },
                onConfigureRomFolder = { romFolderPicker.launch(null) },
                onScanLibrary = { setupOpen = true },
                onToggleEditMode = { editMode = !editMode },
                onLauncherLayoutModeSelected = { mode ->
                    updateGallerySettings(gallerySettings.selectPreset(mode))
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
                    updateGallerySettings(gallerySettings.copy(tileBackgroundMode = mode))
                },
                onResetLauncher = {
                    if (viewModel.acceptsSession(session)) viewModel.resetLauncher()
                },
                onPickCustomLauncherBackground = {
                    launcherBackgroundPicker.launch(arrayOf("image/*"))
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (!preparedGames.ready && visibleGames.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (games.isEmpty() && visibleGamesWithRecommendation.isEmpty()) {
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
                            val filteredGames = if (activeFilter == effectiveGenreFilter) visibleGames
                                else rememberPreparedGames(games, activeFilter, gameSortOrder).games
                            val galleryGames = remember(filteredGames, activeFilter) {
                                com.ariy.retrolauncher.ui.mainGalleryGames(filteredGames, activeFilter)
                            }
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
                            val filteredGamesWithRecommendation = remember(galleryGames, galleryRecommendationTile, galleryRecommendationAddTile) {
                                buildList {
                                    addAll(
                                        galleryGames.filterNot { game ->
                                            (galleryRecommendationId != null && game.id == galleryRecommendationId) ||
                                                (galleryRecommendationAddId != null && game.id == galleryRecommendationAddId)
                                        }
                                    )
                                    galleryRecommendationTile?.entry?.let(::add)
                                    galleryRecommendationAddTile?.entry?.let(::add)
                                }
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
                                val visibleColumns = gridState.layoutInfo.visibleItemsInfo
                                    .maxOfOrNull { it.column + 1 }?.coerceAtLeast(1) ?: 1
                                val step = visibleColumns * rowDelta
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
                        BoxWithConstraints(
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
                            val visibleColumns = responsiveGridColumns(maxWidth.value, gridColumns)
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(visibleColumns),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                                columns = visibleColumns,
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
                                                columns = visibleColumns,
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
                                                columns = visibleColumns,
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
                    if (viewModel.acceptsSession(session)) viewModel.addShortcut(it)
                    addDialogOpen = false
                },
                onAddActivity = {
                    if (viewModel.acceptsSession(session)) viewModel.addActivity(it)
                    addDialogOpen = false
                    addDialogInitialTab = 0
                    addDialogPlatformFilter = null
                    addDialogInitialCollection = null
                    selectFilter(GamePlatforms.filterLabel(
                        com.ariy.retrolauncher.launch.standaloneActivityPlatform(it.packageName)
                    ) ?: GamePlatforms.ANDROID, 0)
                },
                onAddCustom = {
                    val targetFilter = addDialogInitialCollection ?: GamePlatforms.filterLabel(it.platform) ?: addDialogPlatformFilter
                    if (viewModel.acceptsSession(session)) viewModel.addCustom(it)
                    addDialogOpen = false
                    addDialogInitialTab = 0
                    addDialogPlatformFilter = null
                    addDialogInitialCollection = null
                    targetFilter?.let { filter ->
                        selectFilter(filter, 0)
                    }
                },
                onAddCustomBatch = { entries, refreshPlatform ->
                    val targetFilter = addDialogInitialCollection
                        ?: GamePlatforms.filterLabel(entries.firstOrNull()?.platform)
                        ?: GamePlatforms.filterLabel(refreshPlatform)
                        ?: addDialogPlatformFilter
                    if (viewModel.acceptsSession(session)) viewModel.addCustomEntries(entries, refreshPlatform)
                    addDialogOpen = false
                    addDialogInitialTab = 0
                    addDialogPlatformFilter = null
                    addDialogInitialCollection = null
                    targetFilter?.let { filter ->
                        selectFilter(filter, 0)
                    }
                },
                onFillMissingArtworkForPlatform = { platform ->
                    if (viewModel.acceptsSession(session)) viewModel.fillMissingArtworkForPlatform(platform, refreshAll = false)
                },
                onRefreshAllArtworkForPlatform = { platform ->
                    if (viewModel.acceptsSession(session)) viewModel.fillMissingArtworkForPlatform(platform, refreshAll = true)
                },
                onVerifyScummVm = { gameFolderUri, gameId ->
                    viewModel.acceptsSession(session) && viewModel.verifyScummVmConfig(gameFolderUri, gameId)
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
                viewModel.launchGroupedTarget(selected, target)
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
}

private val LocalGameCaptionsEnabled = androidx.compose.runtime.staticCompositionLocalOf { true }
private val LocalLauncherSessionActive = androidx.compose.runtime.staticCompositionLocalOf<() -> Boolean> { { true } }

@Composable
private fun gameTileSubtitle(game: GameEntry): String? = listOfNotNull(
    tileTitle(game.libraryDisplayTitle()).secondary,
    game.libraryGameCaption().takeIf { LocalGameCaptionsEnabled.current }
).joinToString("\n").takeIf(String::isNotBlank)

@Composable
private fun HeaderRow(
    gridColumns: Int,
    gameSortOrder: GameSortOrder,
    onGameSortOrderSelected: (GameSortOrder) -> Unit,
    artworkPlatforms: List<String>,
    artworkRefreshPlatforms: Set<String>,
    onRefreshPlatformArtwork: (String) -> Unit,
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
    launcherLayoutMode: String,
    launcherBackground: String,
    launcherBackgroundOptions: List<String>,
    launcherBackgroundAnimationEnabled: Boolean,
    launcherBackgroundBlurEnabled: Boolean,
    showTop10Badges: Boolean,
    launcherTileBackgroundMode: String,
    hasCustomLauncherBackground: Boolean,
    showGameCaptions: Boolean,
    onShowGameCaptionsChanged: (Boolean) -> Unit,
    onGridSelected: (Int) -> Unit,
    onGenreSelected: (String) -> Unit,
    onAdd: () -> Unit,
    onConfigureRomFolder: () -> Unit,
    onScanLibrary: () -> Unit,
    onToggleEditMode: () -> Unit,
    onLauncherLayoutModeSelected: (String) -> Unit,
    onLauncherBackgroundSelected: (String) -> Unit,
    onLauncherBackgroundAnimationEnabledChanged: (Boolean) -> Unit,
    onLauncherBackgroundBlurEnabledChanged: (Boolean) -> Unit,
    onShowTop10BadgesChanged: (Boolean) -> Unit,
    onLauncherTileBackgroundModeSelected: (String) -> Unit,
    onResetLauncher: () -> Unit,
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
            gameSortOrder = gameSortOrder,
            onGameSortOrderSelected = onGameSortOrderSelected,
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
            artworkPlatforms = artworkPlatforms,
            artworkRefreshPlatforms = artworkRefreshPlatforms,
            initialArtworkPlatform = GamePlatforms.normalizeOrNull(activePlatformFilter),
            onRefreshPlatformArtwork = onRefreshPlatformArtwork,
            hasRomFolder = hasRomFolder,
            configuredRomFolderPath = configuredRomFolderPath,
            isDefaultLauncher = isDefaultLauncher,
            editMode = editMode,
            launcherLayoutMode = launcherLayoutMode,
            launcherBackground = launcherBackground,
            launcherBackgroundOptions = launcherBackgroundOptions,
            launcherBackgroundAnimationEnabled = launcherBackgroundAnimationEnabled,
            launcherBackgroundBlurEnabled = launcherBackgroundBlurEnabled,
            showTop10Badges = showTop10Badges,
            launcherTileBackgroundMode = launcherTileBackgroundMode,
            hasCustomLauncherBackground = hasCustomLauncherBackground,
            showGameCaptions = showGameCaptions,
            onShowGameCaptionsChanged = onShowGameCaptionsChanged,
            onDismiss = { optionsOpen = false },
            onGridSelected = onGridSelected,
            onToggleEditMode = onToggleEditMode,
            onConfigureRomFolder = onConfigureRomFolder,
            onScanLibrary = { optionsOpen = false; onScanLibrary() },
            onLauncherLayoutModeSelected = onLauncherLayoutModeSelected,
            onLauncherBackgroundSelected = onLauncherBackgroundSelected,
            onLauncherBackgroundAnimationEnabledChanged = onLauncherBackgroundAnimationEnabledChanged,
            onLauncherBackgroundBlurEnabledChanged = onLauncherBackgroundBlurEnabledChanged,
            onShowTop10BadgesChanged = onShowTop10BadgesChanged,
            onLauncherTileBackgroundModeSelected = onLauncherTileBackgroundModeSelected,
            onResetLauncher = onResetLauncher,
            onPickCustomLauncherBackground = onPickCustomLauncherBackground
        )
    }
    if (infoOpen) {
        InfoDialog(onDismiss = { infoOpen = false })
    }
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    AdaptiveLauncherDialog(onDismissRequest = onDismiss, expanded = false) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
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
    artworkPlatforms: List<String>,
    artworkRefreshPlatforms: Set<String>,
    initialArtworkPlatform: String?,
    onRefreshPlatformArtwork: (String) -> Unit,
    hasRomFolder: Boolean,
    configuredRomFolderPath: String?,
    isDefaultLauncher: Boolean,
    editMode: Boolean,
    launcherLayoutMode: String,
    launcherBackground: String,
    launcherBackgroundOptions: List<String>,
    launcherBackgroundAnimationEnabled: Boolean,
    launcherBackgroundBlurEnabled: Boolean,
    showTop10Badges: Boolean,
    launcherTileBackgroundMode: String,
    hasCustomLauncherBackground: Boolean,
    showGameCaptions: Boolean,
    onShowGameCaptionsChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onGridSelected: (Int) -> Unit,
    onToggleEditMode: () -> Unit,
    onConfigureRomFolder: () -> Unit,
    onScanLibrary: () -> Unit,
    onLauncherLayoutModeSelected: (String) -> Unit,
    onLauncherBackgroundSelected: (String) -> Unit,
    onLauncherBackgroundAnimationEnabledChanged: (Boolean) -> Unit,
    onLauncherBackgroundBlurEnabledChanged: (Boolean) -> Unit,
    onShowTop10BadgesChanged: (Boolean) -> Unit,
    onLauncherTileBackgroundModeSelected: (String) -> Unit,
    onResetLauncher: () -> Unit,
    onPickCustomLauncherBackground: () -> Unit
) {
    val selectablePlatforms = artworkPlatforms
        .map { GamePlatforms.normalizeOrNull(it) ?: it }
        .filter { it.isNotBlank() }
        .distinct()
    var artworkPlatform by rememberSaveable {
        mutableStateOf(initialArtworkPlatform?.takeIf { it in selectablePlatforms } ?: selectablePlatforms.firstOrNull())
    }
    var generalTab by rememberSaveable { mutableStateOf(true) }
    var confirmArtworkRefresh by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val selectedPlatform = artworkPlatform?.takeIf { it in selectablePlatforms } ?: selectablePlatforms.firstOrNull()
    val displayControls: @Composable (String, Int, String, Boolean, (String) -> Unit, (Int) -> Unit, (String) -> Unit, (Boolean) -> Unit) -> Unit =
        { layout, columns, tileBackground, captions, onLayout, onColumns, onTileBackground, onCaptions ->
            Text("Layout", color = Color.White, style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    GlobalGallerySettings.GRID to "Grid",
                    GlobalGallerySettings.SHELF to "Shelf"
                ).forEach { (mode, label) ->
                    FilterChip(selected = layout == mode, onClick = { onLayout(mode) }, label = { Text(label) })
                }
            }
            Text("Tile count", color = Color.White, style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlobalGallerySettings.ALLOWED_TILE_COUNTS.forEach { count ->
                    FilterChip(selected = columns == count, onClick = { onColumns(count) }, label = { Text("$count") })
                }
            }
            Text("Tile transparency", color = Color.White, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    LAUNCHER_TILE_BACKGROUND_MODE_NORMAL to "Normal",
                    LAUNCHER_TILE_BACKGROUND_MODE_TRANSPARENT to "Transparent",
                    LAUNCHER_TILE_BACKGROUND_MODE_GLOSSY to "Glossy"
                ).forEach { (mode, label) ->
                    FilterChip(selected = tileBackground == mode, onClick = { onTileBackground(mode) }, label = { Text(label) })
                }
            }
            Text("Year captions", color = Color.White, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !captions, onClick = { onCaptions(false) }, label = { Text("Off") })
                FilterChip(selected = captions, onClick = { onCaptions(true) }, label = { Text("On") })
            }
        }
    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Options", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                androidx.compose.material3.TabRow(
                    selectedTabIndex = if (generalTab) 0 else 1,
                    containerColor = Color(0xFF111927),
                    contentColor = Color.White
                ) {
                    androidx.compose.material3.Tab(
                        selected = generalTab,
                        onClick = { generalTab = true },
                        text = { Text("General") }
                    )
                    androidx.compose.material3.Tab(
                        selected = !generalTab,
                        onClick = { generalTab = false },
                        text = { Text("Artwork") }
                    )
                }
                HorizontalDivider(color = Color(0xFF293548))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!generalTab) {
                        Text("Platform", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Choose a platform to refresh its artwork. This does not change the current filter or gallery settings.",
                            color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectablePlatforms.forEach { platform ->
                                FilterChip(
                                    selected = selectedPlatform == platform,
                                    onClick = { artworkPlatform = platform; confirmArtworkRefresh = false },
                                    label = { Text(platform) }
                                )
                            }
                        }
                        if (selectedPlatform == null) {
                            Text("No platforms available yet.", color = Color(0xFF98A2B3))
                        }
                        HorizontalDivider(color = Color(0xFF293548))
                        Text("Platform artwork", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        val refreshing = selectedPlatform in artworkRefreshPlatforms
                        Text(
                            "Refresh covers only—no ROM scan or game changes. Existing images, including custom covers, are replaced only when a new image is found.",
                            color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            enabled = selectedPlatform != null && !refreshing,
                            onClick = { confirmArtworkRefresh = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (refreshing) "Refreshing images…" else "Refresh platform images") }
                        if (confirmArtworkRefresh) {
                            Text("Replace artwork for $selectedPlatform?", color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { confirmArtworkRefresh = false }) { Text("Cancel") }
                                Button(
                                    enabled = !refreshing && selectedPlatform != null,
                                    onClick = {
                                        selectedPlatform?.let(onRefreshPlatformArtwork)
                                        confirmArtworkRefresh = false
                                    }
                                ) { Text("Refresh images") }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF293548))
                    }
                    if (generalTab) {
                        OutlinedButton(onClick = onScanLibrary, modifier = Modifier.fillMaxWidth()) {
                            Text("Scan library / Setup")
                        }
                        Text("Global gallery", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Grid and Shelf each remember their tile count (default 2). Layout, tile transparency and captions apply to every platform and collection; tiles are transparent by default.",
                            color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall
                        )
                        displayControls(
                            launcherLayoutMode, gridColumns, launcherTileBackgroundMode, showGameCaptions,
                            onLauncherLayoutModeSelected, onGridSelected, onLauncherTileBackgroundModeSelected, onShowGameCaptionsChanged
                        )
                        HorizontalDivider(color = Color(0xFF293548))
                        Text("Global launcher options", color = Color.White, style = MaterialTheme.typography.titleMedium)
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
                        } else {
                            OutlinedButton(onClick = onConfigureRomFolder) { Text("Choose ROM folder") }
                        }
                        Text("Background", color = Color.White, style = MaterialTheme.typography.titleSmall)
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
                        Text("Background animation", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !launcherBackgroundAnimationEnabled,
                                onClick = { onLauncherBackgroundAnimationEnabledChanged(false) },
                                label = { Text("Off") }
                            )
                            FilterChip(
                                selected = launcherBackgroundAnimationEnabled,
                                onClick = { onLauncherBackgroundAnimationEnabledChanged(true) },
                                label = { Text("On") }
                            )
                        }
                        Text("Background blur", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !launcherBackgroundBlurEnabled,
                                onClick = { onLauncherBackgroundBlurEnabledChanged(false) },
                                label = { Text("Off") }
                            )
                            FilterChip(
                                selected = launcherBackgroundBlurEnabled,
                                onClick = { onLauncherBackgroundBlurEnabledChanged(true) },
                                label = { Text("On") }
                            )
                        }
                        Text("Top 10 badges", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !showTop10Badges,
                                onClick = { onShowTop10BadgesChanged(false) },
                                label = { Text("Off") }
                            )
                            FilterChip(
                                selected = showTop10Badges,
                                onClick = { onShowTop10BadgesChanged(true) },
                                label = { Text("On") }
                            )
                        }
                        Text(
                            text = if (isDefaultLauncher) "Launcher status: Active" else "Launcher status: Not default",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF98A2B3)
                        )

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

                    }
                }

                HorizontalDivider(color = Color(0xFFB42318))
                Text("Destructive action · All launcher data", color = Color(0xFFFDA29B), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = { confirmReset = true },
                    border = BorderStroke(1.dp, Color(0xFFB42318)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset launcher", color = Color(0xFFFDA29B)) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
    if (confirmReset) {
        com.ariy.retrolauncher.ui.ResetLauncherDialog(
            onCancel = { confirmReset = false },
            onConfirm = {
                confirmReset = false
                onDismiss()
                onResetLauncher()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersDialog(
    genreFilter: String,
    gameSortOrder: GameSortOrder,
    onGameSortOrderSelected: (GameSortOrder) -> Unit,
    collections: List<String>,
    platformFilters: List<String>,
    recommendationFiltersLoading: Boolean,
    recommendationFiltersProgressPercent: Int,
    onDismiss: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
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
                    Text("Default sorting", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Played games always come first. This order applies within played and unplayed groups, across all platforms. Unknown years go last in each group.",
                        color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameSortOrder.entries.forEach { order ->
                            FilterChip(selected = gameSortOrder == order, onClick = { onGameSortOrderSelected(order) }, label = { Text(order.label) })
                        }
                    }
                    Text("Popularity uses your successful launches, counted from this update—not an online ranking.", color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF293548))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Show games", color = Color.White, style = MaterialTheme.typography.titleSmall)
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
    var loadedCount by remember(currentFilter) {
        mutableIntStateOf(com.ariy.retrolauncher.ui.GalleryPaging.initialCount(games.size))
    }
    val displayedCount = loadedCount.coerceAtMost(games.size)
    LaunchedEffect(currentFilter, games.size, listState) {
        androidx.compose.runtime.snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: listState.firstVisibleItemIndex
        }.collect { lastVisible ->
            loadedCount = com.ariy.retrolauncher.ui.GalleryPaging.nextCount(games.size, loadedCount, lastVisible)
        }
    }
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
        loadedCount = com.ariy.retrolauncher.ui.GalleryPaging.countThrough(games.size, loadedCount, targetIndex)
        androidx.compose.runtime.withFrameNanos { }
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
        val targetIndex = (focusedIndex + 1).coerceAtMost(games.lastIndex).coerceAtLeast(0)
        if (targetIndex != focusedIndex) {
            loadedCount = com.ariy.retrolauncher.ui.GalleryPaging.countThrough(games.size, loadedCount, targetIndex)
            galleryScope.launch {
                androidx.compose.runtime.withFrameNanos { }
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
                        preTrimHeightFraction = overlaySpec?.preTrimHeightFraction ?: 1f,
                        trimPlayStationSpine = overlaySpec?.trimPlayStationSpine == true
                    )
                }.distinct()
            }
        }
        LaunchedEffect(prefetchArtworkPlan) {
            if (prefetchArtworkPlan.isEmpty()) return@LaunchedEffect
            withContext(Dispatchers.IO) {
                prefetchArtworkPlan.forEach { request ->
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    ensureArtworkBitmapCached(
                        contentResolver = context.contentResolver,
                        uriString = request.uriString,
                        applyVintageArtwork = request.applyVintageArtwork,
                        preTrimLeftFraction = request.preTrimLeftFraction,
                        preTrimTopFraction = request.preTrimTopFraction,
                        preTrimWidthFraction = request.preTrimWidthFraction,
                        preTrimHeightFraction = request.preTrimHeightFraction,
                        trimPlayStationSpine = request.trimPlayStationSpine
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
        val fittedOverlayAspectRatio = remember(games, filterOverlayPlatform) {
            games.asSequence().map { filterOverlayPlatform ?: it.platform }.distinct()
                .mapNotNull { NostalgicOverlays.specForPlatform(it)?.croppedAspectRatio }
                .minOrNull()?.coerceAtMost(NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO)
                ?: NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO
        }
        val cardWidth = responsiveGalleryWidth(
            maxWidth.value, maxHeight.value, galleryCardWidthFraction(tileSizeColumns), FEATURED_GALLERY_CARD_ASPECT_RATIO
        ).dp
        val desiredNostalgicWidthFraction = if (sameNostalgicCanvasEnabled) {
            sameNostalgicGalleryWidthFraction(
                tileSizeColumns = tileSizeColumns,
                targetAspectRatio = sameNostalgicLayoutMetrics.targetAspectRatio,
                occupiedWidthFraction = sameNostalgicLayoutMetrics.occupiedWidthFraction
            )
        } else {
            nostalgicGalleryWidthFraction(tileSizeColumns)
        }
        val nostalgicItemWidth = responsiveGalleryWidth(
            maxWidth.value, maxHeight.value, desiredNostalgicWidthFraction,
            if (sameNostalgicCanvasEnabled) sameNostalgicLayoutMetrics.targetAspectRatio else fittedOverlayAspectRatio
        ).dp
        val nostalgicItemWidthFraction = (nostalgicItemWidth / maxWidth).coerceIn(0.01f, 1f)
        val mixedNostalgicItemWidth = nostalgicItemWidth
            .coerceAtMost(maxWidth - 16.dp)
        val mixedNostalgicFallbackWidth = (cardWidth * 0.84f)
            .coerceAtMost(mixedNostalgicItemWidth * 0.9f)
        val sameNostalgicTargetAspectRatio = sameNostalgicLayoutMetrics.targetAspectRatio
        val sameNostalgicReferenceWidth = if (mixedNostalgicMode) {
            mixedNostalgicItemWidth
        } else {
            nostalgicItemWidth
        }
        val shelfstalgicMaxItemWidth = responsiveGalleryWidth(
            maxWidth.value, maxHeight.value, 0.94f, fittedOverlayAspectRatio
        ).dp
        val shelfstalgicFocusedItemWidth = (
            maxWidth * shelfstalgicFocusWidthFraction(tileSizeColumns)
            ).coerceIn(
                minOf(180.dp, shelfstalgicMaxItemWidth),
                shelfstalgicMaxItemWidth
            )
        val shelfstalgicCompactItemWidth = (
            maxWidth * shelfstalgicCompactWidthFraction(tileSizeColumns)
            ).coerceIn(
                minOf(120.dp, shelfstalgicFocusedItemWidth * 0.82f),
                shelfstalgicFocusedItemWidth * 0.82f
            )
        val shelfstalgicSlotWidth = (shelfstalgicCompactItemWidth * 0.74f)
            .coerceAtLeast(minOf(88.dp, shelfstalgicCompactItemWidth))
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
            items(
                count = displayedCount,
                key = { index ->
                    val game = games[index]
                    recommendationListItemKey(
                        game = game,
                        recommendationId = recommendationId,
                        recommendationAddId = recommendationAddId,
                        filter = currentFilter
                    )
                }
            ) { index ->
                val game = games[index]
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
                text = tileTitle(game.libraryDisplayTitle()).primary,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            gameTileSubtitle(game)?.let { subtitle ->
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
                text = tileTitle(game.libraryDisplayTitle(), split = !compact).primary,
                color = Color.White,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                gameTileSubtitle(game)?.let { subtitle ->
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
        GamePlatforms.filterLabel(com.ariy.retrolauncher.launch.standaloneActivityPlatform(packageName))
    } else {
        GamePlatforms.filterLabel(platform)
    }

private fun GameEntry.matchesPlatformFilter(filter: String): Boolean =
    if (sourceType == GameSourceType.ACTIVITY) {
        GamePlatforms.matchesFilter(com.ariy.retrolauncher.launch.standaloneActivityPlatform(packageName), filter)
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

private data class PreparedGames(val games: List<GameEntry>, val ready: Boolean)

@Composable
private fun rememberPreparedGames(games: List<GameEntry>, filter: String, sortOrder: GameSortOrder): PreparedGames {
    // Keep a same-filter snapshot on artwork updates, but never show a previous filter's games.
    var prepared by remember(filter, sortOrder) {
        mutableStateOf(if (games.size <= com.ariy.retrolauncher.ui.GalleryPaging.PAGE_SIZE) {
            PreparedGames(orderedGamesForFilter(games, filter, sortOrder), true)
        } else PreparedGames(emptyList(), false))
    }
    LaunchedEffect(games, filter, sortOrder) {
        val ordered = withContext(Dispatchers.Default) {
            orderedGamesForFilter(games, filter, sortOrder)
        }
        prepared = PreparedGames(ordered, true)
    }
    return prepared
}

private fun orderedGamesForFilter(
    games: List<GameEntry>,
    filter: String,
    sortOrder: GameSortOrder
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
    return sortLibraryGames(filteredGames, sortOrder)
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
    val showSubtitle = showText && !shelfCompactMode
    val grayscaleFilter = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }
    val shapeModifier = Modifier.aspectRatio(
        if (game.platform.equals(GamePlatforms.ANDROID, ignoreCase = true) || !showText) 1f
        else FEATURED_GALLERY_CARD_ASPECT_RATIO
    )
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
        else -> if (columns >= 3) 8.dp else 10.dp
    }
    val titleStyle = when {
        shelfCompactMode -> MaterialTheme.typography.labelSmall
        shelfMode -> MaterialTheme.typography.titleSmall
        else -> if (columns >= 3) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
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
                        image != null -> CoverArtwork(
                            image = image,
                            contentDescription = game.title,
                            colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                            alpha = if (isRecommendation) 0.76f else 1f,
                            modifier = Modifier.fillMaxSize()
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
                            text = tileTitle(game.libraryDisplayTitle(), split = showSubtitle && !isRecommendation).primary,
                            color = titleColor,
                            style = titleStyle,
                            maxLines = if (shelfCompactMode) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (isRecommendation) {
                            when {
                                isRecommendationLoading -> game.subtitle.takeIf { it.isNotBlank() }
                                else -> recommendationReason?.takeIf { it.isNotBlank() }
                            }
                        } else {
                            gameTileSubtitle(game)
                        }
                        if (showSubtitle && !subtitleText.isNullOrBlank()) {
                            Text(
                                text = subtitleText,
                                color = subtitleColor,
                                style = subtitleStyle,
                                maxLines = if (columns <= 2 && !shelfCompactMode) 3 else 1,
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
    val showSubtitle = showText
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
    val tilePadding = if (columns == 1) 4.dp else 2.dp
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
                                    .fillMaxSize(overlaySpec.gridArtworkScale)
                                    .align(Alignment.Center)
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

                        defaultImage != null -> CoverArtwork(
                            image = defaultImage,
                            contentDescription = game.title,
                            colorFilter = if (isRecommendation) ColorFilter.colorMatrix(grayscaleFilter) else null,
                            alpha = if (isRecommendation) 0.76f else 1f,
                            modifier = Modifier.fillMaxSize()
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
                    Column(modifier = Modifier.padding(if (columns >= 3) 8.dp else 10.dp)) {
                        Text(
                            text = tileTitle(game.libraryDisplayTitle(), split = showSubtitle && !isRecommendation).primary,
                            color = titleColor,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (isRecommendation) {
                            when {
                                isRecommendationLoading -> game.subtitle.takeIf { it.isNotBlank() }
                                else -> recommendationReason?.takeIf { it.isNotBlank() }
                            }
                        } else {
                            gameTileSubtitle(game)
                        }
                        if (showSubtitle && !subtitleText.isNullOrBlank()) {
                            Text(
                                text = subtitleText,
                                color = subtitleColor,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = if (columns <= 2) 3 else 1,
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
        if (com.ariy.retrolauncher.model.PcGameStores.usesShortcutAdding(normalizedFilter)) return null
        if (normalizedFilter == GamePlatforms.EMULATORS) return null
        if (GamePlatforms.normalizeOrNull(normalizedFilter) == GamePlatforms.ANDROID) {
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
        overlaySpec.preTrimHeightFraction,
        overlaySpec.trimPlayStationSpine
    ) {
        normalizedUri?.let {
            artworkBitmapCacheKey(
                uriString = it,
                applyVintageArtwork = overlaySpec.applyVintageArtwork,
                preTrimLeftFraction = overlaySpec.preTrimLeftFraction,
                preTrimTopFraction = overlaySpec.preTrimTopFraction,
                preTrimWidthFraction = overlaySpec.preTrimWidthFraction,
                preTrimHeightFraction = overlaySpec.preTrimHeightFraction,
                trimPlayStationSpine = overlaySpec.trimPlayStationSpine
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
                preTrimHeightFraction = overlaySpec.preTrimHeightFraction,
                trimPlayStationSpine = overlaySpec.trimPlayStationSpine
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
    // Shared canvas sizing must not decode every cabinet in a large library before showing a page.
    // Keep this representative sample stable as further lazy pages are appended.
    val metricGames = remember(games) { games.take(com.ariy.retrolauncher.ui.GalleryPaging.PAGE_SIZE) }
    return produceState(
        initialValue = SameNostalgicLayoutMetrics(
            targetAspectRatio = NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO,
            occupiedWidthFraction = 0.62f
        ),
        metricGames,
        filterOverlayPlatform,
        enabled
    ) {
        if (!enabled || metricGames.isEmpty()) {
            value = SameNostalgicLayoutMetrics(
                targetAspectRatio = NOSTALGIC_OVERLAY_REFERENCE_ASPECT_RATIO,
                occupiedWidthFraction = 0.62f
            )
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            resolveSameNostalgicLayoutMetrics(
                context = context,
                games = metricGames,
                filterOverlayPlatform = filterOverlayPlatform
            )
        }
    }.value
}

private suspend fun resolveSameNostalgicLayoutMetrics(
    context: Context,
    games: List<GameEntry>,
    filterOverlayPlatform: String?
): SameNostalgicLayoutMetrics {
    val coroutineContext = kotlinx.coroutines.currentCoroutineContext()
    val availableAssets = runCatching {
        context.assets.list("overlay_mame")
            ?.filterNot { it.contains("Zone.Identifier", ignoreCase = true) }
            ?.associateBy { it.lowercase() }
            .orEmpty()
    }.getOrDefault(emptyMap())
    val resolvedMetrics = games.asSequence()
        .mapNotNull { game ->
            coroutineContext.ensureActive()
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
    if (!supportsMameCabinetNostalgia(game.platform)) {
        return null
    }
    val assetBaseNames = linkedSetOf<String>().apply {
        game.arcadeRomShortNameForArtworkSearch()?.let(::add)
        addAll(game.arcadeOverlayAssetBaseNames())
        extractArcadeRomShortName(context, game.intentDataUri)?.let(::add)
    }
    for (rom in assetBaseNames + game.title) {
        com.ariy.retrolauncher.launch.ArcadeCabinetArtStore.find(context, rom)?.let {
            return "full_cabinets/${it.asset}"
        }
    }
    // Exact ROM/clone references and exact catalog parents precede legacy fuzzy matching.
    for (rom in assetBaseNames) {
        val name = "$rom.png"
        if (runCatching { context.assets.open("cabinets_mame/$name").use { true } }.getOrDefault(false)) return name
    }
    if (assetBaseNames.isEmpty()) return null
    // A sequel without its own cabinet must not inherit a predecessor by prefix.
    return assetBaseNames.firstNotNullOfOrNull { stem ->
        listOf("png", "jpg", "jpeg").firstNotNullOfOrNull { extension -> availableAssets["${stem.lowercase()}.$extension"] }
    }?.takeUnless(::isGenericArcadeFallbackAssetName)
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
    if (assetName.startsWith("full_cabinets/")) {
        // Complete silhouettes are already composed; never insert title cards or covers into their CRTs.
        return runCatching { context.assets.open(assetName).use(BitmapFactory::decodeStream) }.getOrNull()
    }
    if (artworkUriString.isNullOrBlank() && assetName.substringBeforeLast('.') != "mame") {
        val premade = runCatching {
            context.assets.open("cabinets_mame/$assetName").use(BitmapFactory::decodeStream)
        }.getOrNull()
        if (premade != null) return premade
    }
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
    val backgroundMask = cabinetBackgroundMask(sourcePixels, width, height)

    var left = width
    var top = height
    var right = -1
    var bottom = -1
    sourcePixels.forEachIndexed { index, pixel ->
        if (backgroundMask[index] || android.graphics.Color.alpha(pixel) <= 8) return@forEachIndexed
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
            cleanedPixels[(y * objectWidth) + x] = if (backgroundMask[((top + y) * width) + left + x]) {
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
    val preTrimHeightFraction: Float = 1f,
    val trimPlayStationSpine: Boolean = false
)

private fun artworkBitmapCacheKey(
    uriString: String,
    applyVintageArtwork: Boolean,
    preTrimLeftFraction: Float = 0f,
    preTrimTopFraction: Float = 0f,
    preTrimWidthFraction: Float = 1f,
    preTrimHeightFraction: Float = 1f,
    trimPlayStationSpine: Boolean = false
): String =
    buildString {
        append(if (applyVintageArtwork) "vintage" else "plain")
        append(if (trimPlayStationSpine) ":ps1-bands-v2" else "")
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
    preTrimHeightFraction: Float = 1f,
    trimPlayStationSpine: Boolean = false
): Bitmap? {
    val cacheKey = artworkBitmapCacheKey(
        uriString = uriString,
        applyVintageArtwork = applyVintageArtwork,
        preTrimLeftFraction = preTrimLeftFraction,
        preTrimTopFraction = preTrimTopFraction,
        preTrimWidthFraction = preTrimWidthFraction,
        preTrimHeightFraction = preTrimHeightFraction,
        trimPlayStationSpine = trimPlayStationSpine
    )
    getCachedTileBitmap(cacheKey)?.let { return it }

    val decoded = decodeSampledBitmap(
        contentResolver = contentResolver,
        uri = resolveArtworkSourceUri(uriString),
        maxDimension = TILE_ARTWORK_MAX_DIMENSION
    ) ?: return null
    val trimmed = if (preTrimLeftFraction == 0f && preTrimTopFraction == 0f &&
        preTrimWidthFraction == 1f && preTrimHeightFraction == 1f
    ) trimArtworkWhitespace(decoded) else decoded
    val platformTrimmed = if (trimPlayStationSpine) {
        val pixels = IntArray(trimmed.width * trimmed.height)
        trimmed.getPixels(pixels, 0, trimmed.width, 0, 0, trimmed.width, trimmed.height)
        val bounds = playStationCoverBounds(pixels, trimmed.width, trimmed.height)
        if (bounds.left > 0 || bounds.top > 0 || bounds.right < trimmed.width || bounds.bottom < trimmed.height) {
            Bitmap.createBitmap(trimmed, bounds.left, bounds.top, bounds.right - bounds.left, bounds.bottom - bounds.top)
        } else trimmed
    } else trimmed
    val croppedBitmap = cropArtworkBitmap(
        source = platformTrimmed,
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

private fun trimArtworkWhitespace(source: Bitmap): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    val bounds = artworkContentBounds(pixels, source.width, source.height)
    if (bounds.left == 0 && bounds.top == 0 && bounds.right == source.width && bounds.bottom == source.height) return source
    return Bitmap.createBitmap(source, bounds.left, bounds.top, bounds.right - bounds.left, bounds.bottom - bounds.top)
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
    onAddCustomBatch: (List<GameEntry>, String?) -> Unit,
    onFillMissingArtworkForPlatform: (String?) -> Unit,
    onRefreshAllArtworkForPlatform: (String?) -> Unit,
    onVerifyScummVm: (gameFolderUri: String, gameId: String) -> Boolean,
    collectionSuggestions: List<String>
) {
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Add games", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
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
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> ShortcutPicker(
                        games = games,
                        shortcuts = shortcuts,
                        activities = activities,
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
                        onRefreshAllArtworkForPlatform = onRefreshAllArtworkForPlatform,
                        onVerifyScummVm = onVerifyScummVm,
                        collectionSuggestions = collectionSuggestions
                    )
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
    activities: List<InstalledActivityOption>,
    isDefaultLauncher: Boolean,
    authorizedRomTreeUri: String?,
    onAddShortcut: (InstalledShortcutOption) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var setupFeedback by remember { mutableStateOf<String?>(null) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var refreshedShortcuts by remember { mutableStateOf(shortcuts) }
    var refreshing by remember { mutableStateOf(false) }
    val shortcutScope = rememberCoroutineScope()
    LaunchedEffect(shortcuts) { refreshedShortcuts = shortcuts }
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
    val filtered = remember(refreshedShortcuts, query, authorizedRoot) {
        refreshedShortcuts.filter {
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

    Column(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        RetroTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search shortcuts") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showSetup = !showSetup }) {
                Text(if (showSetup) "Hide shortcut setup" else "Shortcut setup")
            }
            TextButton(enabled = !refreshing, onClick = {
                refreshing = true
                shortcutScope.launch {
                    try {
                        refreshedShortcuts = withContext(Dispatchers.IO) {
                            com.ariy.retrolauncher.launch.InstalledTargets(context.applicationContext as android.app.Application)
                                .loadShortcuts()
                        }
                        setupFeedback = "${refreshedShortcuts.size} shared shortcuts found. Missing a hub game? Open Shortcut setup."
                    } catch (error: Exception) {
                        setupFeedback = "Shortcut refresh failed: ${error.message}"
                    } finally { refreshing = false }
                }
            }) { Text(if (refreshing) "Refreshing…" else "Refresh") }
        }
        SelectionList(modifier = Modifier.weight(1f), items = visibleShortcuts.map {
            SelectionRow(title = it.title, subtitle = it.subtitle, onClick = { onAddShortcut(it) })
        }, header = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        setupFeedback?.let { Text(it, color = Color(0xFFFDB022), style = MaterialTheme.typography.bodySmall) }
        if (!isDefaultLauncher) {
            Text("Set Retro Launcher as Home to access published shortcuts.", color = Color(0xFFFDB022), style = MaterialTheme.typography.bodySmall)
        }
        if (showSetup) {
        Text(
            text = "GameNative / GameHub / GameHub Lite: set Retro Launcher as Home, then use the game's Add to home screen action in its app and allow the request. Published shortcuts appear here; private libraries cannot be scanned. If no shortcut action exists, use Android to open the hub instead.",
            color = Color(0xFF98A2B3),
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }) {
            Text("Choose default Home launcher")
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.filter { com.ariy.retrolauncher.model.PcGameStores.fromAppLabel(it.sourceLabel) != null ||
                it.packageName in com.ariy.retrolauncher.model.PcGameStores.hubPackages }
                .distinctBy { it.packageName to it.userSerial }.forEach { app ->
                    OutlinedButton(onClick = {
                        val started = com.ariy.retrolauncher.launch.InstalledTargets(context.applicationContext as android.app.Application)
                            .startActivity(ComponentName(app.packageName, app.activityName), app.userSerial)
                        if (!started) setupFeedback = "Could not open ${app.sourceLabel}."
                    }) { Text("Open ${app.sourceLabel}") }
                }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.ariy.retrolauncher.model.PcGameStores.hubPackages.forEach { (packageName, label) ->
                if (activities.any { it.packageName == packageName }) OutlinedButton(
                    enabled = isDefaultLauncher && !refreshing,
                    onClick = {
                        refreshing = true
                        shortcutScope.launch {
                            try {
                                val published = withContext(Dispatchers.IO) {
                                    com.ariy.retrolauncher.launch.InstalledTargets(context.applicationContext as android.app.Application)
                                        .loadShortcuts(packageName)
                                }
                                refreshedShortcuts = refreshedShortcuts.filterNot { it.packageName == packageName } + published
                                val available = published.filterNot {
                                    "${it.packageName}|${it.shortcutId}|${it.userSerial}" in existingShortcutKeys
                                }
                                available.forEach(onAddShortcut)
                                setupFeedback = when {
                                    published.isEmpty() -> "No $label shortcuts shared with Retro Launcher. Recreate the game's home-screen shortcut inside $label while Retro Launcher is Home, then Allow. Existing pins in the stock launcher are not accessible here."
                                    available.isEmpty() -> "All ${published.size} shared $label shortcuts are already added."
                                    else -> "Imported ${available.size} $label shortcuts."
                                }
                            } catch (error: Exception) {
                                setupFeedback = "$label import failed: ${error.message}"
                            } finally { refreshing = false }
                        }
                    }
                ) { Text("Import $label shortcuts") }
            }
        }
        }
        }
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
    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = curatedDisplayTitle(game.title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Choose emulator",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF98A2B3)
                )
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    Column(modifier = Modifier.fillMaxSize()) {
        RetroTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Android activities") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectionList(modifier = Modifier.weight(1f), items = filtered.map {
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
private fun SelectionList(items: List<SelectionRow>, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (header != null) item { header() }
        if (items.isEmpty()) {
            item {
                Text(
                    "No matching items. Try another search or check that the app is installed.",
                    color = Color(0xFF98A2B3),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        items(items.size) { index ->
            val item = items[index]
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(curatedDisplayTitle(item.title), color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.subtitle, color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

internal data class RetroArchSystemPreset(
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

internal enum class GamePickerKind { FILE, FOLDER }

private data class GameSystemChoice(
    val id: String,
    val label: String,
    val platform: String?,
    val romFolderHint: String?,
    val pickerKind: GamePickerKind
)

internal data class GameSystemEmulatorOption(
    val key: String,
    val templateIndex: Int,
    val systemId: String,
    val systemLabel: String,
    val platform: String?,
    val romFolderHint: String?,
    val pickerKind: GamePickerKind,
    val retroArchSystemId: String? = null
)

internal data class ResolvedEmulatorSelectionDraft(
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

internal fun resolveEmulatorSelectionDraft(
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

internal fun buildGameSystemEmulatorOptions(
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
    onAddCustomBatch: (List<GameEntry>, String?) -> Unit,
    onFillMissingArtworkForPlatform: (String?) -> Unit,
    onRefreshAllArtworkForPlatform: (String?) -> Unit,
    onVerifyScummVm: (gameFolderUri: String, gameId: String) -> Boolean,
    collectionSuggestions: List<String>
) {
    val context = LocalContext.current
    val sessionActive = LocalLauncherSessionActive.current
    val scope = rememberCoroutineScope()
    val systemEmulatorOptions = remember(templates) { buildGameSystemEmulatorOptions(templates) }
    val scopedPlatformFilter = remember(initialPlatformFilter) { GamePlatforms.filterLabel(initialPlatformFilter) }
    val filteredSystemEmulatorOptions = remember(systemEmulatorOptions, scopedPlatformFilter) {
        val platformFilter = scopedPlatformFilter ?: return@remember systemEmulatorOptions
        systemEmulatorOptions
            .filter { option ->
                option.platform?.let { platform -> GamePlatforms.matchesFilter(platform, platformFilter) } == true ||
                    (platformFilter in com.ariy.retrolauncher.model.PcGameStores.platforms && option.platform in setOf("PC (Windows)", "GameNative"))
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
    var bulkScanRunning by remember { mutableStateOf(false) }
    var bulkScanJob by remember { mutableStateOf<Job?>(null) }
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
    val scummVmRegisteredPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (!sessionActive()) return@rememberLauncherForActivityResult
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val returned = result.data
            val launchIntent = returned?.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            val uri = launchIntent?.data
            val target = uri?.schemeSpecificPart
            if (uri?.scheme == "scummvm" && target != null && com.ariy.retrolauncher.launch.ScummVmGames.validTarget(target) &&
                (launchIntent.component?.packageName ?: launchIntent.`package`) == "org.scummvm.scummvm") {
                onAddCustom(GameEntry(
                    title = returned.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.takeIf(String::isNotBlank)
                        ?: com.ariy.retrolauncher.launch.ScummVmGames.title(target) ?: target,
                    platform = "ScummVM", genre = collection.ifBlank { null },
                    sourceType = GameSourceType.CUSTOM_INTENT,
                    packageName = "org.scummvm.scummvm", intentAction = Intent.ACTION_MAIN,
                    intentDataUri = uri.toString(),
                    extrasJson = com.ariy.retrolauncher.launch.ScummVmGames.extrasForTarget(target)
                ))
            } else scanFeedback = "ScummVM did not return a valid registered-game target."
        }
    }
    val scummVmConfigPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (!sessionActive()) return@rememberLauncherForActivityResult
        if (uri != null) scope.launch {
            runCatching {
                val entries = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readNBytes(2 * 1024 * 1024 + 1)
                        require(bytes.size <= 2 * 1024 * 1024) { "ScummVM config is too large" }
                        String(bytes, Charsets.UTF_8)
                    } ?: error("Cannot read config")
                    com.ariy.retrolauncher.launch.ScummVmGames.parseConfig(text).map { registered ->
                        GameEntry(title = registered.title, platform = "ScummVM", genre = collection.ifBlank { null },
                            sourceType = GameSourceType.CUSTOM_INTENT, packageName = "org.scummvm.scummvm",
                            intentAction = Intent.ACTION_MAIN, intentDataUri = Uri.fromParts("scummvm", registered.target, null).toString(),
                            extrasJson = com.ariy.retrolauncher.launch.ScummVmGames.extrasForTarget(registered.target))
                    }
                }
                if (entries.isEmpty()) scanFeedback = "No registered games found. Export ScummVM's existing scummvm.ini after adding games."
                else onAddCustomBatch(entries, "ScummVM")
            }.onFailure { error ->
                if (error is CancellationException) throw error
                scanFeedback = "ScummVM config import failed: ${error.message}"
            }
        }
    }
    val openScummVmSetup: () -> Unit = {
        runCatching {
            context.startActivity(context.packageManager.getLaunchIntentForPackage("org.scummvm.scummvm")
                ?: error("ScummVM is not installed"))
        }.onFailure { scanFeedback = it.message }
    }
    val chooseRegisteredScummVm: () -> Unit = {
        runCatching {
            scummVmRegisteredPicker.launch(Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(
                ComponentName("org.scummvm.scummvm", "org.scummvm.scummvm.ShortcutCreatorActivity")))
        }.onFailure { scanFeedback = "Cannot open ScummVM game picker: ${it.message}" }
    }
    val resolvedPickerInitialUris = remember(authorizedRomTreeUri) {
        mutableStateMapOf<String, String?>()
    }
    val pickGameUri: (Uri?) -> Unit = pick@{ uri ->
        if (!sessionActive()) return@pick
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
        if (!sessionActive()) return@rememberLauncherForActivityResult
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
    fun launchBulkScan(block: suspend () -> Unit): Job = scope.launch {
        try {
            block()
        } finally {
            bulkScanRunning = false
            pendingScanPlatform = null
            bulkScanJob = null
        }
    }
    val startGameNativeScan: () -> Unit = {
        if (!bulkScanRunning) {
            bulkScanRunning = true
            bulkScanJob = launchBulkScan {
                try {
                    val targets = com.ariy.retrolauncher.launch.InstalledTargets(context.applicationContext as android.app.Application)
                    if (!targets.isDefaultLauncher()) {
                        scanFeedback = "Set Retro Launcher as the default Home app to scan GameNative shortcuts."
                    } else {
                        val entries = withContext(Dispatchers.IO) {
                            val shortcuts = targets.loadShortcuts("app.gamenative")
                            com.ariy.retrolauncher.launch.GameNativeScan.select(shortcuts, games, collection)
                        }
                        if (entries.isEmpty()) {
                            scanFeedback = "No new GameNative shortcuts found. With Retro Launcher as default Home, create a home-screen shortcut inside GameNative and allow it, then scan again. Shortcuts pinned only to another launcher are not accessible. This is not a full-library scan."
                        } else {
                                onAddCustomBatch(entries, null)
                            scanFeedback = "Imported ${entries.size} GameNative games from published shortcuts."
                        }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    scanFeedback = "GameNative shortcut scan failed: ${error.message}"
                }
            }
        }
    }
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
        bulkScanJob = launchBulkScan {
            val entries = runCatching {
                withContext(Dispatchers.IO) {
                    val scanContext = coroutineContext
                    GameRecommendations.scanImportableMameRoms(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        games = games,
                        cancellationCheck = { scanContext.ensureActive() }
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
                if (error is CancellationException) throw error
                scanFeedback = "MAME scan failed: ${error.message ?: error.javaClass.simpleName}"
                emptyList()
            }
            bulkScanRunning = false
            pendingScanPlatform = null
            if (entries.isEmpty()) {
                scanFeedback = scanFeedback
                    ?: "No new MAME ROM sets were found. Existing games and duplicate copies are skipped; artwork is optional."
            } else {
                onAddCustomBatch(entries, selectedSystemChoice?.platform ?: "MAME Arcade")
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
        bulkScanJob = launchBulkScan {
            val entries = runCatching {
                withContext(Dispatchers.IO) {
                    val scanContext = coroutineContext
                    GameRecommendations.scanImportablePlatformRoms(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        games = games,
                        platform = scanPlatform,
                        packageName = flowDraft.packageName.ifBlank { null },
                        retroArchSystemId = flowOption.retroArchSystemId,
                        cancellationCheck = { scanContext.ensureActive() }
                    ).map { candidate ->
                        scanContext.ensureActive()
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
                if (error is CancellationException) throw error
                scanFeedback = "$activeScanSystemLabel scan failed: ${error.message ?: error.javaClass.simpleName}"
                emptyList()
            }
            bulkScanRunning = false
            pendingScanPlatform = null
            if (entries.isEmpty()) {
                scanFeedback = scanFeedback
                    ?: "No new $activeScanSystemLabel titles were found in the ROM folder."
            } else {
                onAddCustomBatch(entries, scanPlatform)
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
        bulkScanJob = launchBulkScan {
            val (entries, skippedFolders) = runCatching {
                withContext(Dispatchers.IO) {
                    val scanContext = coroutineContext
                    var skipped = 0
                    val importedEntries = GameRecommendations.scanImportableScummVmFolders(
                        context = context,
                        authorizedRomTreeUri = authorizedRomTreeUri,
                        games = games,
                        cancellationCheck = { scanContext.ensureActive() }
                    ).mapNotNull { candidate ->
                        scanContext.ensureActive()
                        val folderUri = Uri.parse(candidate.folderUri)
                        val registeredTarget = candidate.registeredTarget
                        if (registeredTarget.isNullOrBlank()) {
                            skipped += 1
                            null
                        } else {
                            GameEntry(
                                title = candidate.title,
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
                                extrasJson = com.ariy.retrolauncher.launch.ScummVmGames.extrasForTarget(registeredTarget, flowDraft.extras)
                            )
                        }
                    }
                    // Android redacts shortcut intents. Import opaque registered shortcuts through
                    // LauncherApps instead of attempting to infer target IDs from hidden payloads.
                    val installed = com.ariy.retrolauncher.launch.InstalledTargets(context.applicationContext as android.app.Application)
                    val sharedGames = installed.loadShortcuts("org.scummvm.scummvm").filterNot { shortcut ->
                        games.any { game -> game.allTargets().any {
                            it.packageName == shortcut.packageName && it.shortcutId == shortcut.shortcutId && it.userSerial == shortcut.userSerial
                        } }
                    }.map { shortcut ->
                        GameEntry(title = shortcut.title, subtitle = "ScummVM", platform = "ScummVM",
                            genre = collection.ifBlank { null }, sourceType = GameSourceType.SHORTCUT,
                            packageName = shortcut.packageName, shortcutId = shortcut.shortcutId, userSerial = shortcut.userSerial)
                    }
                    (importedEntries + sharedGames) to skipped
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                scanFeedback = "ScummVM scan failed: ${error.message ?: error.javaClass.simpleName}"
                emptyList<GameEntry>() to 0
            }
            bulkScanRunning = false
            pendingScanPlatform = null
            if (entries.isEmpty()) {
                val scanSucceeded = scanFeedback == null
                scanFeedback = scanFeedback ?: if (skippedFolders > 0) {
                    "$skippedFolders folders could not be linked to registered targets. Adding games inside ScummVM does not expose its private list here. Use Choose registered game below, or Import ScummVM config. An exported scummvm.ini in the ROM folder enables folder rescanning and repairs existing tiles."
                } else {
                    "No new shared ScummVM shortcuts or config-linked folders found. Use Choose registered game below to access ScummVM's private registered list, or import its exported config."
                }
                if (scanSucceeded) chooseRegisteredScummVm()
            } else {
                scanFeedback = "Found ${entries.size} shared or config-linked entries, not necessarily your full ScummVM library. Opening ScummVM's registered-game chooser for the remaining games. Import ScummVM config to add the full registered list in one batch."
                onAddCustomBatch(entries, selectedSystemChoice?.platform ?: "ScummVM")
                chooseRegisteredScummVm()
            }
        }
    }

    val editorFocusManager = LocalFocusManager.current
    val editorScroll = rememberScrollState()
    LaunchedEffect(step) {
        editorFocusManager.clearFocus()
        editorScroll.scrollTo(0)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stepLabels.forEachIndexed { index, label ->
                FilterChip(selected = step == index, onClick = { step = index }, label = { Text(label) })
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(editorScroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        val isGameNativeSystem = systemChoice.platform in setOf("PC (Windows)", "GameNative")
                        val hasFolderHint = !systemChoice.romFolderHint.isNullOrBlank()
                        val needsRomFolder = !isGameNativeSystem && (supportsScan || hasFolderHint) && authorizedRomTreeUri.isNullOrBlank()
                        SystemChoiceCard(
                            choice = systemChoice,
                            selected = selectedSystemId == systemChoice.id,
                            emulatorCount = emulatorsPerSystem[systemChoice.id] ?: 0,
                            enabled = systemSelectionPendingId == null && !bulkScanRunning && !needsRomFolder,
                            pending = systemSelectionPendingId == systemChoice.id,
                            secondaryActionLabel = if (isGameNativeSystem) {
                                "Scan GameNative"
                            } else if (supportsScan && !authorizedRomTreeUri.isNullOrBlank()) {
                                "Scan"
                            } else {
                                null
                            },
                            secondaryActionEnabled = (isGameNativeSystem || (supportsScan &&
                                !authorizedRomTreeUri.isNullOrBlank())) &&
                                systemSelectionPendingId == null &&
                                !bulkScanRunning,
                            onSecondaryAction = if (isGameNativeSystem) {
                                startGameNativeScan
                            } else if (supportsScan && !authorizedRomTreeUri.isNullOrBlank()) {
                                {
                                    if (authorizedRomTreeUri.isNullOrBlank()) return@SystemChoiceCard
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
                            tertiaryActionLabel = "Add missing",
                            tertiaryActionEnabled = systemSelectionPendingId == null && !bulkScanRunning,
                            onTertiaryAction = {
                                if (systemSelectionPendingId != null || bulkScanRunning) return@SystemChoiceCard
                                scanFeedback = "Queued missing artwork for ${systemChoice.label}."
                                onFillMissingArtworkForPlatform(systemChoice.platform)
                            },
                            quaternaryActionLabel = "Refresh all",
                            quaternaryActionEnabled = systemSelectionPendingId == null && !bulkScanRunning,
                            onQuaternaryAction = {
                                if (systemSelectionPendingId != null || bulkScanRunning) return@SystemChoiceCard
                                scanFeedback = "Queued full artwork refresh for ${systemChoice.label}."
                                onRefreshAllArtworkForPlatform(systemChoice.platform)
                            },
                            onClick = {
                                if (systemSelectionPendingId != null || bulkScanRunning || needsRomFolder) return@SystemChoiceCard
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
                scanFeedback?.let { HelpCard(title = "Scan", body = it) }
                if (authorizedRomTreeUri.isNullOrBlank()) {
                    when {
                        selectedSystemChoice?.platform.equals("MAME Arcade", ignoreCase = true) -> {
                            HelpCard(
                                title = "Scan unavailable",
                                body = "Authorize the ROM folder first, then tap Scan to bulk-add MAME ROM sets. Games do not need cabinet artwork to be imported."
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
                            "Select the emulator to use for the bulk scan. Retro Launcher will add every new MAME ROM set, skip duplicate copies, and use fallback artwork when a specific cabinet is unavailable."

                        scanningScummVmAfterEmulatorPick ->
                            "ScummVM must register games and authorize their folders first. Scan matches registered shortcuts or an exported scummvm.ini in your ScummVM ROM folder; a marker is not enough to launch a game. You can also choose a registered game or import the full config below."

                        scanningConfiguredPlatformAfterEmulatorPick != null ->
                            "Select the emulator to use for the bulk scan. Retro Launcher will use the validated ${activeScanSystemLabel} scanner, look in the suggested ROM folder, and add supported titles that are not already added."

                        else ->
                            "Select one of the compatible emulators for this system. The launch contract stays the same as today; only the order is different."
                    }
                )
                scanFeedback?.let { message ->
                    HelpCard(title = "Scan", body = message)
                }
                if (selectedSystemChoice?.platform == "ScummVM") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = openScummVmSetup) { Text("Open ScummVM") }
                        Button(onClick = chooseRegisteredScummVm) { Text("Choose registered game") }
                        OutlinedButton(onClick = { scummVmConfigPicker.launch(arrayOf("*/*")) }) { Text("Import ScummVM config") }
                    }
                }
                if (bulkScanRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Scanning ROM folder…", color = Color.White, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            bulkScanJob?.cancel()
                            scanFeedback = "Scan cancelled. Existing games were kept."
                        }) {
                            Text("Cancel")
                        }
                    }
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cardSpacing = 8.dp
                    val cardColumns = if (maxWidth < 380.dp) 1 else if (maxWidth < 640.dp) 2 else 3
                    val cardWidth = (maxWidth - cardSpacing * (cardColumns - 1)) / cardColumns
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = cardColumns,
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
                    if (isScummVmTemplate) "Choose a registered ScummVM game" else "Choose game ROM",
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
                    HelpCard(title = "ScummVM setup", body = "Use Open ScummVM to authorize the folder and Add Game (hold for Mass Add). Then choose a registered game here. Folder markers identify data but cannot register a game or grant ScummVM storage access. For bulk import, export its existing scummvm.ini using ScummVM's LAN server.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = openScummVmSetup) { Text("Open ScummVM") }
                        Button(onClick = chooseRegisteredScummVm) { Text("Choose registered game") }
                        OutlinedButton(onClick = { scummVmConfigPicker.launch(arrayOf("*/*")) }) { Text("Import ScummVM config") }
                    }
                    LabeledField(
                        value = scummVmGameId,
                        onValueChange = { scummVmGameId = it.trim().lowercase() },
                        label = "ScummVM game id",
                        help = "Detected data ID only (example: monkey, tentacle). This does not register a game. Use Choose registered game for a playable target."
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            Text("Check published target")
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
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(context.packageManager.getLaunchIntentForPackage("app.gamenative")
                                ?: error("GameNative is not installed"))
                        }.onFailure { scanFeedback = it.message }
                    }) { Text("Open GameNative to create game shortcuts") }
                    HelpCard(title = "GameNative discovery", body = "Scan imports games whose shortcuts GameNative shares with Retro Launcher. Set Retro Launcher as default Home, create a game shortcut inside GameNative, and allow the request. No ROM folder or manually typed app ID is needed. GameNative's private full library cannot be scanned.")
                    OutlinedButton(onClick = startGameNativeScan, enabled = !bulkScanRunning) {
                        Text(if (bulkScanRunning) "Scanning…" else "Scan GameNative shortcuts")
                    }
                    scanFeedback?.let { HelpCard(title = "GameNative scan", body = it) }
                    HelpCard(
                        title = "GameNative launch options",
                        body = "GameNative games aren't ROM files \u2014 there's no game folder to pick. Enter the numeric app ID and store this game is tracked under inside GameNative's own library."
                    )
                    LabeledField(
                        value = gameNativeAppId,
                        onValueChange = { gameNativeAppId = it.filter(Char::isDigit) },
                        label = "GameNative app ID",
                        help = "Use the app_id GameNative uses for this game and source. Steam games may use their Steam app ID; do not guess IDs for other sources. Publishing a shortcut avoids manual IDs."
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
        }

        HorizontalDivider(color = Color(0xFF293548))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                enabled = step > 0,
                onClick = { step-- }
            ) { Text("Back") }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (step < maxStep) {
                    Button(
                        enabled = !bulkScanRunning,
                        onClick = { if (!bulkScanRunning) step++ }
                    ) { Text("Next") }
                }
                Button(
                    enabled = canSave,
                    onClick = {
                        editorFocusManager.clearFocus()
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
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    tertiaryActionLabel: String? = null,
    tertiaryActionEnabled: Boolean = true,
    onTertiaryAction: (() -> Unit)? = null,
    quaternaryActionLabel: String? = null,
    quaternaryActionEnabled: Boolean = true,
    onQuaternaryAction: (() -> Unit)? = null,
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
            val actionRowItems = buildList {
                if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                    add(
                        Triple(secondaryActionLabel, secondaryActionEnabled, onSecondaryAction)
                    )
                }
                if (!tertiaryActionLabel.isNullOrBlank() && onTertiaryAction != null) {
                    add(
                        Triple(tertiaryActionLabel, tertiaryActionEnabled, onTertiaryAction)
                    )
                }
                if (!quaternaryActionLabel.isNullOrBlank() && onQuaternaryAction != null) {
                    add(
                        Triple(quaternaryActionLabel, quaternaryActionEnabled, onQuaternaryAction)
                    )
                }
            }
            if (actionRowItems.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actionRowItems.forEach { (label, enabled, action) ->
                        OutlinedButton(
                            onClick = action,
                            enabled = enabled
                        ) {
                            Text(label)
                        }
                    }
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

internal fun mergeEdenLaunchConfigIntoExtras(
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
    return com.ariy.retrolauncher.launch.detectScummVmFolder(context, uri)
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

internal fun inferEdenTitleIdFromUriString(context: Context, uriString: String?): String? {
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = label != "Extras JSON"
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
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions ?: KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus(); keyboard?.hide() },
            onSearch = { focusManager.clearFocus(); keyboard?.hide() }
        ),
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

@OptIn(ExperimentalLayoutApi::class)
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
    val sessionActive = LocalLauncherSessionActive.current
    var step by rememberSaveable(game.id) { mutableIntStateOf(0) }
    val primaryTargetKey = game.primaryTarget().stableKey()
    var title by rememberSaveable(game.id, game.title) { mutableStateOf(game.title) }
    var subtitle by rememberSaveable(game.id, game.subtitle, primaryTargetKey) { mutableStateOf(game.subtitle) }
    var collection by rememberSaveable(game.id, game.genre) { mutableStateOf(game.genre.orEmpty()) }
    var releaseYear by rememberSaveable(game.id, game.releaseYear) { mutableStateOf(game.releaseYear.orEmpty()) }
    var tagline by rememberSaveable(game.id, game.tagline) { mutableStateOf(game.tagline.orEmpty()) }
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
        if (!sessionActive()) return@rememberLauncherForActivityResult
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

    val editFocusManager = LocalFocusManager.current
    val editScroll = rememberScrollState()
    LaunchedEffect(step) {
        editFocusManager.clearFocus()
        editScroll.scrollTo(0)
    }
    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111927),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit tile", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFFDA29B)) }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Basics", "Launch", "Artwork").forEachIndexed { index, label ->
                        FilterChip(selected = step == index, onClick = { step = index }, label = { Text(label) })
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(editScroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                if (game.needsLaunchSetup) {
                    com.ariy.retrolauncher.ui.GameLaunchSetupChoices(game) { draft ->
                        packageName = draft.packageName
                        activityName = draft.activityName
                        action = draft.action
                        mimeType = draft.mimeType
                        categories = draft.categories
                        extras = if (draft.packageName == "org.scummvm.scummvm") {
                            com.ariy.retrolauncher.launch.ScummVmGames.registeredTarget(game.extrasJson)
                                ?.let { com.ariy.retrolauncher.launch.ScummVmGames.extrasForTarget(it, draft.extras) }
                                ?: draft.extras
                        } else draft.extras
                        subtitle = draft.subtitle
                        edenResolutionSetup = draft.edenResolutionSetup
                        step = 1
                    }
                }
                when (step) {
                    0 -> {
                        RetroTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        if (curatedDisplayTitle(title) != title.trim()) {
                            TextButton(onClick = { title = curatedDisplayTitle(title) }) { Text("Clean filename tags") }
                        }
                        RetroTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        RetroTextField(value = releaseYear, onValueChange = { releaseYear = it.take(4) }, label = { Text("Release year (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        RetroTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Game catchphrase (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Text("Shown beneath the game title. Leave blank to use known arcade metadata; emulator subtitle remains separate.", color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
                        CollectionPicker(
                            selectedCollection = collection,
                            onCollectionSelected = { collection = it },
                            suggestions = collectionSuggestions
                        )

                        if (supportsMameCabinetNostalgia(game.platform)) {
                            val metadata = com.ariy.retrolauncher.launch.ArcadeGameCatalog.find(game.arcadeRomShortNameForArtworkSearch())
                                ?: com.ariy.retrolauncher.launch.ArcadeGameCatalog.find(game.title)
                            metadata?.let { HelpCard(title = "About ${it.title}", body = it.description) }
                            val cabinet = com.ariy.retrolauncher.launch.ArcadeCabinetArtStore.find(context, game.arcadeRomShortNameForArtworkSearch() ?: game.title)
                            cabinet?.let {
                                HelpCard(
                                    title = if (it.kind == "period-style-conversion") "Cabinet: period-style conversion" else "Cabinet: photographic reference",
                                    body = if (it.kind == "period-style-conversion")
                                        "A designed operator-conversion alternative, not an authenticated original cabinet. The game sign is in the marquee; the screen is left as a CRT."
                                    else it.notes
                                )
                            }
                        }

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
                                help = "Use GameNative's app_id for the selected source. Steam may use its store app ID; publishing a shortcut avoids manual IDs."
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                }

                HorizontalDivider(color = Color(0xFF293548))
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            enabled = title.isNotBlank(),
                            onClick = {
                                editFocusManager.clearFocus()
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
                                        releaseYear = releaseYear.trim().takeIf { it.matches(Regex("[0-9]{4}")) },
                                        tagline = tagline.trim().takeIf(String::isNotBlank),
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
                    .aspectRatio(0.68f)
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
