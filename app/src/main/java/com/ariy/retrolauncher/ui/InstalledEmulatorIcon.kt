package com.ariy.retrolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Load the installed application's icon off the UI thread, including adaptive icons. */
@Composable
internal fun InstalledEmulatorIcon(packageName: String, emulatorName: String) {
    val context = LocalContext.current.applicationContext
    val icon by produceState<ImageBitmap?>(null, context, packageName) {
        value = withContext(Dispatchers.IO) {
            val manager = context.packageManager
            val drawable = runCatching { manager.getApplicationIcon(packageName) }
                .getOrElse { manager.defaultActivityIcon }
            runCatching { drawable.toBitmap(width = 128, height = 128).asImageBitmap() }.getOrNull()
        }
    }
    Box(Modifier.size(40.dp).testTag("emulator-icon-$packageName")) {
        icon?.let {
            Image(bitmap = it, contentDescription = "$emulatorName icon",
                modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
        }
    }
}