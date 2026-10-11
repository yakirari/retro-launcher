package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()
                    .then(if (expanded) Modifier.fillMaxHeight(0.96f) else Modifier)
            ) {
                content()
            }
        }
    }
}