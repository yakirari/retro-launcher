package com.ariy.retrolauncher.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.model.GameEntry
import com.ariy.retrolauncher.model.HandheldCartridgeSettings

/** Immediate artwork edits, independent of the cover and the dialog's unsaved launch fields. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HandheldCartridgeControls(
    game: GameEntry,
    sessionActive: () -> Boolean,
    onUpdate: (labelUri: String?, color: String?) -> Unit
) {
    if (!com.ariy.retrolauncher.model.CartridgeLabelSettings.supportsPlatform(game.platform)) return
    val handheld = HandheldCartridgeSettings.supportsPlatform(game.platform)
    val context = LocalContext.current
    val currentGame by rememberUpdatedState(game)
    val currentUpdate by rememberUpdatedState(onUpdate)
    val currentSessionActive by rememberUpdatedState(sessionActive)
    var error by remember(game.id) { mutableStateOf<String?>(null) }
    val labelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && currentSessionActive()) {
            val permission = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (permission.isSuccess) {
                currentUpdate(uri.toString(), currentGame.cartridgeColor)
                error = null
            } else {
                error = "Could not keep access to this label. Choose an image from another document provider."
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (handheld) "Handheld cartridge" else "NES cartridge label", color = Color.White, style = MaterialTheme.typography.titleSmall)
        Text(
            if (handheld) "Choose a full cartridge sticker, or fit the complete cover without extra text. Auto shell color follows the artwork; colors are decorative, not verified original hardware."
            else "Choose a flat scan of the printed sticker for this game and region, not a photograph of the whole cartridge. The complete image is fitted without cropping or recoloring; ordinary cover views stay unchanged.",
            color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall
        )
        Text(
            if (game.cartridgeLabelUri != null) "Label: custom image"
            else if (handheld) "Label: exact-ROM cartridge sticker when available; cover fallback"
            else "Label: reviewed exact-ROM sticker when available (limited NES coverage); complete cover fallback",
            color = Color.White, style = MaterialTheme.typography.bodySmall
        )
        if (handheld) Text("GB / GBC / GBA label art: Slot art set / ScreenScraper · CC BY-NC-SA 4.0\n" +
            "art.slot-cfw.fyi · screenscraper.fr · creativecommons.org/licenses/by-nc-sa/4.0/",
            color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
        if (!handheld) Text("NES scan source: Internet Archive. Download/cache only; source media is not MIT-licensed or bundled. No regional substitution.",
            color = Color(0xFF98A2B3), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { labelPicker.launch(arrayOf("image/*")) }) { Text("Choose cartridge label") }
            OutlinedButton(
                enabled = game.cartridgeLabelUri != null,
                onClick = {
                    if (sessionActive()) onUpdate(null, game.cartridgeColor)
                    error = null
                }
            ) { Text("Use automatic label") }
        }
        error?.let { Text(it, color = Color(0xFFFDA29B), style = MaterialTheme.typography.bodySmall) }
        if (handheld) Text("Shell color", color = Color.White, style = MaterialTheme.typography.labelLarge)
        if (handheld) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HandheldCartridgeSettings.colorChoices.forEach { (key, label) ->
                FilterChip(
                    selected = HandheldCartridgeSettings.normalizeColor(game.cartridgeColor) == key,
                    onClick = { if (sessionActive()) onUpdate(game.cartridgeLabelUri, key) },
                    label = { Text(label) }
                )
            }
        }
    }
}