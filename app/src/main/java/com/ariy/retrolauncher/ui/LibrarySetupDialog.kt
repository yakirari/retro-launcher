package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.LibraryScanProgress

@Composable
internal fun LibrarySetupDialog(
    firstRun: Boolean, progress: LibraryScanProgress, hasRoot: Boolean,
    isDefaultLauncher: Boolean, installedCount: Int, sharedGameCount: Int,
    onChooseRoot: () -> Unit, onScan: () -> Unit, onCancel: () -> Unit,
    onSetHome: () -> Unit, onDismiss: () -> Unit
) {
    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        Surface(color = Color(0xFF111927), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (firstRun) "Welcome to Retro Launcher" else "Scan library / Setup",
                    style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose your ROM root once. Recognized platform folders are scanned automatically, games are imported, and missing artwork is fetched afterward when online.", color = Color.White)
                    Text("$installedCount installed emulators / hubs detected · $sharedGameCount accessible shared game shortcuts",
                        color = Color(0xFF98A2B3))
                    Text("GameNative, GameHub and GameHub Lite games shared through Android shortcuts are imported automatically on return to Home. Private hub libraries and shortcuts pinned only to another launcher are not accessible.", color = Color(0xFF98A2B3))
                    if (!isDefaultLauncher) {
                        OutlinedButton(onClick = onSetHome, modifier = Modifier.fillMaxWidth()) { Text("Set as Home app") }
                        Text("Set Retro Launcher as Home, then create / approve game shortcuts inside your hub. Installed hub launchers work without Home status.", color = Color(0xFF98A2B3))
                    }
                    if (progress.running || progress.totalPlatforms > 0) {
                        Text(if (progress.running) "Scanning ${progress.platform.ifBlank { "platform folders" }}…"
                            else if (progress.cancelled) "Scan cancelled — imported games kept" else "Scan finished", color = Color.White)
                        LinearProgressIndicator(progress = {
                            progress.completedPlatforms.toFloat() / progress.totalPlatforms.coerceAtLeast(1)
                        }, modifier = Modifier.fillMaxWidth())
                        Text("${progress.completedPlatforms}/${progress.totalPlatforms} platforms · ${progress.discovered} discovered · ${progress.imported} imported / repaired · ${progress.skipped} skipped / already present · ${progress.errors} platform errors", color = Color.White)
                        Text("${progress.unconfigured} discovered tiles need emulator setup. These are library entries, not yet launchable. Open the tile menu → Edit → Launch targets, or install a supported standalone emulator and scan again. RetroArch requires your explicit system/core choice.", color = Color(0xFF98A2B3))
                        progress.messages.forEach { Text(it, color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall) }
                    }
                    Button(onClick = onChooseRoot, enabled = !progress.running, modifier = Modifier.fillMaxWidth()) {
                        Text(if (hasRoot) "Change ROM root and scan" else "Choose ROM folder")
                    }
                    if (hasRoot) OutlinedButton(onClick = onScan, enabled = !progress.running, modifier = Modifier.fillMaxWidth()) {
                        Text("Scan library / Retry")
                    }
                    if (progress.running) OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel scan") }
                    Text("No ROMs? Continue with installed apps and shared hub games. You can return through Options → Scan library / Setup. Closing this screen does not stop a scan.", color = Color(0xFF98A2B3))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(if (firstRun) "Skip / I have no ROMs" else "Close")
                }
            }
        }
    }
}