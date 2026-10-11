package com.ariy.retrolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Preserve the cover's lettering; fill unmatched space with a subdued artwork backdrop. */
@Composable
internal fun CoverArtwork(
    image: ImageBitmap,
    contentDescription: String,
    colorFilter: ColorFilter? = null,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clipToBounds()) {
        Image(
            painter = BitmapPainter(image), contentDescription = null,
            contentScale = ContentScale.Crop, colorFilter = colorFilter, alpha = alpha * 0.3f,
            modifier = Modifier.fillMaxSize().blur(18.dp)
        )
        Image(
            painter = BitmapPainter(image), contentDescription = contentDescription,
            contentScale = ContentScale.Fit, colorFilter = colorFilter, alpha = alpha,
            modifier = Modifier.fillMaxSize()
        )
    }
}