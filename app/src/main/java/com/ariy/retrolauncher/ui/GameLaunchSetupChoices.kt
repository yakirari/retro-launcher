package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.ResolvedEmulatorSelectionDraft
import com.ariy.retrolauncher.buildGameSystemEmulatorOptions
import com.ariy.retrolauncher.resolveEmulatorSelectionDraft
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import com.ariy.retrolauncher.model.GameEntry

/** Explicit repair uses exactly the same system/core presets as the manual Add flow. */
@Composable
internal fun GameLaunchSetupChoices(game: GameEntry, onSelect: (ResolvedEmulatorSelectionDraft) -> Unit) {
    val context = LocalContext.current
    val templates = CustomIntentTemplates.all
    val options = remember(context, game.platform) {
        buildGameSystemEmulatorOptions(templates).filter {
            it.platform == game.platform && runCatching {
                context.packageManager.getApplicationInfo(templates[it.templateIndex].packageName, 0)
            }.isSuccess
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Needs emulator setup", color = Color(0xFFF2AA4C))
        Text("Choose a verified launch preset, then Save. For RetroArch you must install the named core inside RetroArch and grant it access to your ROMs. Core availability cannot be inspected from this launcher.", color = Color(0xFF98A2B3))
        if (options.isEmpty()) Text("No supported preset is installed for this platform. Install an emulator and retry Scan library, or configure a verified contract in the Launch tab.", color = Color(0xFF98A2B3))
        options.forEach { option ->
            val draft = resolveEmulatorSelectionDraft(templates, option) ?: return@forEach
            OutlinedButton(onClick = { onSelect(draft) }) {
                Text(draft.subtitle + (draft.retroArchSystem?.let { " · ${it.coreSoFile}" } ?: ""))
            }
        }
    }
}