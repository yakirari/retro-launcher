package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.LibraryScanProgress
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import com.ariy.retrolauncher.resolveEmulatorSelectionDraft
import kotlinx.coroutines.delay

@Composable
internal fun LibrarySetupDialog(
    firstRun: Boolean, progress: LibraryScanProgress, hasRoot: Boolean,
    isDefaultLauncher: Boolean, installedCount: Int, sharedGameCount: Int,
    onChooseRoot: () -> Unit, onScan: () -> Unit, onCancel: () -> Unit,
    onChooseEmulator: (String?) -> Unit,
    onSetHome: () -> Unit, onDismiss: () -> Unit,
    onChooseScummVmGame: () -> Unit = {}, onImportScummVmConfig: () -> Unit = {},
    onSkipScummVm: () -> Unit = {}
) {
    var showDetails by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(progress.running) {
        elapsed = 0
        while (progress.running) {
            delay(1000)
            elapsed++
        }
    }
    val muted = Color(0xFF98A2B3)
    val choice = progress.emulatorChoice
    AdaptiveLauncherDialog(onDismissRequest = onDismiss) {
        val compact = LocalCompactLauncherDialog.current
        // A new emulator prompt must not inherit a scroll position deep in scan details.
        val scrollState = key(choice?.folder, progress.scummVmSetupRequired) { rememberScrollState() }
        Surface(color = Color(0xFF111927), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxSize().padding(if (compact) 8.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp)) {
                Text(if (firstRun) "Welcome to Retro Launcher" else "Library setup",
                    style = MaterialTheme.typography.titleLarge, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress.running || progress.totalPlatforms > 0) {
                    Text(progress.statusText(), color = Color.White,
                        style = MaterialTheme.typography.bodySmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(1f).fillMaxWidth().testTag("library-setup-content")
                    .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
                    if (progress.scummVmSetupRequired) {
                        Text("Link your registered ScummVM games", color = Color.White)
                        Text("ScummVM's private game list needs its own picker or an exported config. Folder names alone cannot create working launch targets.",
                            color = muted, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onChooseScummVmGame, modifier = Modifier.fillMaxWidth()) { Text("Choose registered game") }
                        OutlinedButton(onClick = onImportScummVmConfig, modifier = Modifier.fillMaxWidth()) { Text("Import ScummVM config") }
                        Text("Import the existing scummvm.ini to add all registered games in one batch. Choosing one game adds only that game.",
                            color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (choice != null) {
                        Text(if (choice.options.size == 1)
                            "Choose the emulator for ${choice.folder}; each game is added only once."
                        else "Choose one emulator for ${choice.folder}; each game is added only once.",
                            color = Color.White)
                        choice.options.forEach { option ->
                            val template = CustomIntentTemplates.all[option.templateIndex]
                            val core = resolveEmulatorSelectionDraft(CustomIntentTemplates.all, option)
                                ?.retroArchSystem?.coreSoFile
                            OutlinedButton(onClick = { onChooseEmulator(option.key) }, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    InstalledEmulatorIcon(template.packageName, template.label.removeSuffix(" preset"))
                                    Column(Modifier.weight(1f)) {
                                        Text("${template.label.removeSuffix(" preset")} · ${option.systemLabel}")
                                        if (core != null) Text(
                                            "Requires installed core $core and a compatible ROM. Core availability is not verified.",
                                            style = MaterialTheme.typography.bodySmall)
                                        else if (template.packageName == "com.retroarch" || template.packageName == "com.retroarch.aarch64")
                                            Text("Install the required RetroArch core and use a compatible ROM. Core availability is not verified.",
                                                style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Text("Prefer manual setup? Skip this platform now. No games from this folder will be imported. Use Add games to choose the platform and configure its emulator or RetroArch core later.", color = muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (progress.running || progress.totalPlatforms > 0) {
                        Surface(color = Color(0xFF1D2939), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (progress.running && choice == null) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                } else {
                                    LinearProgressIndicator(progress = {
                                        progress.completedPlatforms.toFloat() / progress.totalPlatforms.coerceAtLeast(1)
                                    }, modifier = Modifier.fillMaxWidth())
                                }
                                Text("${progress.completedPlatforms}/${progress.totalPlatforms} platforms checked · ${progress.imported} added / repaired · ${progress.discovered} found",
                                    color = Color.White, style = MaterialTheme.typography.bodySmall)
                                if (progress.running && choice == null) {
                                    Text(if (progress.currentGame.isNotBlank()) progress.currentGame else "Reading folders — large libraries can take a while.",
                                        color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("${elapsed / 60}m ${elapsed % 60}s elapsed · Games are saved as they are added. Artwork comes afterward.",
                                        color = muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (choice == null && !progress.running) {
                        Text("Choose the folder containing your platform folders. Games are added automatically; covers download afterward.", color = Color.White)
                        Text("$installedCount emulators / hubs detected · $sharedGameCount shared hub games", color = muted,
                            style = MaterialTheme.typography.bodyMedium)
                        if (hasRoot) OutlinedButton(onClick = onChooseRoot, modifier = Modifier.fillMaxWidth()) { Text("Change ROM folder") }
                        if (progress.unconfigured > 0) Text("${progress.unconfigured} games need launch setup. Tile menu → Edit → Launch targets.", color = muted)
                        if (progress.deferredPlatforms.isNotEmpty()) Text(
                            "Skipped: ${progress.deferredPlatforms.joinToString()}. Set these up later through Add games.",
                            color = muted, style = MaterialTheme.typography.bodySmall)
                        Text("No ROMs? Skip for now and use installed apps or shared hub games.", color = muted,
                            style = MaterialTheme.typography.bodySmall)
                    } else if (choice == null && !progress.scummVmSetupRequired) {
                        Text("Your library is being built. You can cancel safely — games already added are kept.", color = muted,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { showDetails = !showDetails }) {
                        Text(if (showDetails) "Hide setup details" else "Setup help / scan details")
                    }
                    if (showDetails) {
                        if (!isDefaultLauncher) OutlinedButton(onClick = onSetHome, modifier = Modifier.fillMaxWidth()) { Text("Set as Home app") }
                        Text("For GameNative, GameHub and GameHub Lite: set Retro Launcher as Home, then create / approve game shortcuts inside the hub. Private libraries and shortcuts pinned only to another launcher cannot be read.", color = muted,
                            style = MaterialTheme.typography.bodySmall)
                        Text("${progress.skipped} skipped / already present · ${progress.errors} platform errors. Return anytime through Options → Scan library / Setup.", color = muted,
                            style = MaterialTheme.typography.bodySmall)
                        progress.messages.forEach { Text(it, color = muted, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (progress.running) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel scan") }
                        if (choice != null) {
                            Button(onClick = { onChooseEmulator(null) }, modifier = Modifier.weight(1f)) { Text("Skip platform") }
                        } else if (progress.scummVmSetupRequired) {
                            Button(onClick = onSkipScummVm, modifier = Modifier.weight(1f)) { Text("Skip platform") }
                        } else {
                            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("View library") }
                        }
                    } else {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(if (progress.totalPlatforms > 0) "Done" else "Skip for now")
                        }
                        Button(onClick = if (hasRoot) onScan else onChooseRoot, modifier = Modifier.weight(1f)) {
                            Text(if (hasRoot) "Scan library" else "Choose ROM folder")
                        }
                    }
                }
            }
        }
    }
}