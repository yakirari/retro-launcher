package com.ariy.retrolauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle

/** Android validates user-initiated shortcut pin requests delivered to the current Home app. */
class ConfirmGameShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = getSystemService(LauncherApps::class.java).getPinItemRequest(intent)
        val shortcut = request?.shortcutInfo
        if (request?.isValid != true || request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT ||
            shortcut == null) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Add game shortcut")
            .setMessage("Add ${shortcut.shortLabel} from ${shortcut.`package`} to Retro Launcher?")
            .setPositiveButton("Allow") { _, _ ->
                if (runCatching { request.accept() }.getOrDefault(false)) {
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra("pinnedShortcutPackage", shortcut.`package`)
                        .putExtra("pinnedShortcutId", shortcut.id)
                        .putExtra("pinnedShortcutUser", getSystemService(android.os.UserManager::class.java)
                            .getSerialNumberForUser(shortcut.userHandle)))
                }
                finish()
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}