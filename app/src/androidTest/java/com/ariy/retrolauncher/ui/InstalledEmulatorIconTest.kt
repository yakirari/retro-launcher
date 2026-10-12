package com.ariy.retrolauncher.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class InstalledEmulatorIconTest {
    @get:Rule val compose = createComposeRule()

    @Test fun installedApplicationIconLoads() {
        compose.setContent { InstalledEmulatorIcon(LocalContext.current.packageName, "Installed app") }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Installed app icon").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Installed app icon").assertIsDisplayed()
    }

    @Test fun unavailablePackageGetsFallbackIcon() {
        compose.setContent { InstalledEmulatorIcon("invalid.missing.emulator", "Missing emulator") }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Missing emulator icon").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Missing emulator icon").assertIsDisplayed()
    }
}