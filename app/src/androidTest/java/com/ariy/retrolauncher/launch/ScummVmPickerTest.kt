package com.ariy.retrolauncher.launch

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test

class ScummVmPickerTest {
    @Suppress("DEPRECATION")
    private fun result(packageName: String = ScummVmGames.PACKAGE, target: String = "monkey2-fr") = Intent()
        .putExtra(Intent.EXTRA_SHORTCUT_NAME, "Monkey Island 2 (French)")
        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(packageName, "$packageName.ScummVMActivity"))
            .setData(Uri.fromParts("scummvm", target, null)))

    @Test fun registeredPickerRetainsExactTargetAndRejectsUnverifiedResults() {
        val entry = requireNotNull(scummVmEntryFromPicker(result()))
        assertEquals("monkey2-fr", ScummVmGames.registeredTarget(entry.extrasJson))
        assertEquals("scummvm:monkey2-fr", entry.intentDataUri)
        assertEquals("Monkey Island 2 (French)", entry.title)
        assertNull(scummVmEntryFromPicker(result("unverified.package")))
        assertNull(scummVmEntryFromPicker(result(target = "--path=/games monkey2")))
        assertNull(scummVmEntryFromPicker(null))
    }

    @Test fun bulkConfigCreatesEveryExistingRegisteredTargetWithoutGuessingEngineId() {
        val entries = scummVmEntriesFromConfig("""
            [monkey2-fr]
            gameid=monkey2
            description=Monkey Island 2 French
            path=/games/Monkey2
            [monkey2-en]
            gameid=monkey2
            description=Monkey Island 2 English
            path=/games/Monkey2English
        """.trimIndent())
        assertEquals(listOf("monkey2-fr", "monkey2-en"), entries.map { ScummVmGames.registeredTarget(it.extrasJson) })
        assertEquals(listOf("scummvm:monkey2-fr", "scummvm:monkey2-en"), entries.map { it.intentDataUri })
    }
}