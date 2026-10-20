package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.ariy.retrolauncher.GamePickerKind
import com.ariy.retrolauncher.GameSystemChoice
import com.ariy.retrolauncher.SystemChoiceCard
import com.ariy.retrolauncher.buildGameSystemEmulatorOptions
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SystemChoiceCardTest {
    @get:Rule val compose = createComposeRule()

    private fun waitForBadge(label: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("$label platform badge", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun everyAddGameSystemRendersItsBadgeOnTheRight() {
        val choices = buildGameSystemEmulatorOptions(CustomIntentTemplates.all).distinctBy { it.systemId }
        val selected = mutableStateOf(choices.first())
        compose.setContent {
            val option = selected.value
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    SystemChoiceCard(
                        choice = GameSystemChoice(option.systemId, option.systemLabel, option.platform,
                            option.romFolderHint, option.pickerKind),
                        selected = false, emulatorCount = 1, onClick = {})
                }
            }
        }
        choices.forEach { option ->
            compose.runOnIdle { selected.value = option }
            waitForBadge(option.systemLabel)
            val badge = compose.onNodeWithContentDescription("${option.systemLabel} platform badge",
                useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText(option.systemLabel, useUnmergedTree = true)
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Badge must sit to the right of ${option.systemLabel}", badge.left > title.right)
        }
    }

    @Test fun narrowCardKeepsScanBelowBadgeAndClickable() {
        var scanned = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(280.dp)) {
                    SystemChoiceCard(
                        choice = GameSystemChoice("ps3", "Sony PlayStation 3", "Sony PlayStation 3",
                            "ps3", GamePickerKind.FILE),
                        selected = true, emulatorCount = 1,
                        secondaryActionLabel = "Scan ISO + shortcuts",
                        onSecondaryAction = { scanned = true }, onClick = {})
                }
            }
        }
        waitForBadge("Sony PlayStation 3")
        val badge = compose.onNodeWithContentDescription("Sony PlayStation 3 platform badge",
            useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val scan = compose.onNodeWithText("Scan ISO + shortcuts").assertIsDisplayed()
        assertTrue(scan.fetchSemanticsNode().boundsInRoot.top >= badge.bottom)
        scan.performClick()
        compose.runOnIdle { assertTrue(scanned) }
    }
}