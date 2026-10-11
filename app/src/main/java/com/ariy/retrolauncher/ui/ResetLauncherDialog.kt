package com.ariy.retrolauncher.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color

@Composable
internal fun ResetLauncherDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Reset everything in Retro Launcher?") },
        text = {
            Text("This permanently clears your library, custom titles/collections/launch targets, play history, " +
                "imported/downloaded artwork, cache and all launcher settings, including saved ROM folder, " +
                "artwork and background access. Running scans and artwork downloads stop.\n\n" +
                "ROMs, shared images/backgrounds and installed apps are NOT deleted. Other apps' permissions " +
                "and Android's default Home selection are unchanged.\n\n" +
                "Welcome returns with Grid, 2 tiles and transparent backgrounds. Installed emulator/hub " +
                "app tiles can reappear automatically; shared hub games return only after setup or a new " +
                "deliberate refresh/import. Choose your ROM folder again to scan.\n\n" +
                "There is no undo or in-app backup/export. Continue only if you accept losing your launcher data.")
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.focusRequester(cancelFocus)) { Text("Cancel") }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))) {
                Text("Reset launcher")
            }
        }
    )
    // Controller/keyboard activation starts on the non-destructive choice.
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
}