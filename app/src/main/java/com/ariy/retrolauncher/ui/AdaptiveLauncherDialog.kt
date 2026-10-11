package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Compact is based on the usable dialog viewport, including system bars and the IME. */
internal val LocalCompactLauncherDialog = staticCompositionLocalOf { false }

/** One viewport for utility screens: near-full-screen on phones, bounded on larger displays. */
@Composable
internal fun AdaptiveLauncherDialog(
    onDismissRequest: () -> Unit,
    expanded: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            contentAlignment = Alignment.Center
        ) {
            val compact = maxHeight < 480.dp
            CompositionLocalProvider(LocalCompactLauncherDialog provides compact) {
                Box(Modifier.padding(if (compact) 4.dp else 8.dp)) {
                    Box(
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()
                            .then(if (expanded) Modifier.fillMaxHeight(if (compact) 1f else 0.96f) else Modifier)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}