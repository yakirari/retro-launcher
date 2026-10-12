package com.ariy.retrolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Tint and blur the real template alpha, not a rectangular card/slot outline. */
@Composable
internal fun TemplateDropShadow(template: ImageBitmap, modifier: Modifier = Modifier) {
    val blur = with(LocalDensity.current) { 6.dp.toPx() }
    val drop = with(LocalDensity.current) { 3.dp.toPx() }
    Image(bitmap = template, contentDescription = null, contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(Color.Black),
        modifier = modifier.fillMaxSize().graphicsLayer {
            alpha = 0.8f
            translationY = drop
            scaleX = 1.025f
            scaleY = 1.025f
            clip = false
            renderEffect = BlurEffect(blur, blur, TileMode.Decal)
        })
}