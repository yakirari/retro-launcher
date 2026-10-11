package com.ariy.retrolauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ariy.retrolauncher.GamePickerKind
import com.ariy.retrolauncher.GameSystemEmulatorOption
import com.ariy.retrolauncher.LibraryEmulatorChoice
import com.ariy.retrolauncher.LibraryScanProgress
import com.ariy.retrolauncher.LibraryScanStage
import com.ariy.retrolauncher.launch.CustomIntentTemplates
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibrarySetupDialogTest {
    @get:Rule val compose = createComposeRule()

    private fun show(progress: LibraryScanProgress = LibraryScanProgress(), compact: Boolean = false,
        onCancel: () -> Unit = {}, onChoose: (String?) -> Unit = {}) {
        compose.setContent {
            val content: @Composable () -> Unit = {
                MaterialTheme {
                    LibrarySetupDialog(firstRun = true, progress = progress, hasRoot = progress.running,
                        isDefaultLauncher = false, installedCount = 4, sharedGameCount = 2,
                        onChooseRoot = {}, onScan = {}, onCancel = onCancel, onChooseEmulator = onChoose,
                        onSetHome = {}, onDismiss = {})
                }
            }
            if (compact) CompactDialogTestViewport(content) else content()
        }
    }

    @Test fun firstRunPrimaryActionsStayVisibleWithoutScrollingThroughHelp() {
        show()
        compose.onNodeWithText("Choose ROM folder").assertIsDisplayed()
        compose.onNodeWithText("Skip for now").assertIsDisplayed()
        compose.onNodeWithText("Setup help / scan details").performScrollTo().performClick()
        compose.onNodeWithText("Set as Home app").performScrollTo()
        compose.onNodeWithText("Choose ROM folder").assertIsDisplayed()
        compose.onNodeWithText("Skip for now").assertIsDisplayed()
    }

    @Test fun compactLongScanDetailsDoNotHideLiveStatusOrCancel() {
        show(LibraryScanProgress(running = true, stage = LibraryScanStage.ADDING,
            platform = "MAME Arcade", totalPlatforms = 24, imported = 64,
            messages = List(30) { "Scan message $it" }), compact = true)
        compose.onNodeWithText("Setup help / scan details").performScrollTo().performClick()
        compose.onNodeWithText("Scan message 29").performScrollTo()
        compose.onNodeWithText("Adding MAME Arcade…").assertIsDisplayed()
        compose.onNodeWithText("Cancel scan").assertIsDisplayed()
        compose.onNodeWithText("View library").assertIsDisplayed()
    }

    @Test fun emulatorPromptOffersSkipWithoutSelectingAnEmulator() {
        val options = CustomIntentTemplates.all.take(2).mapIndexed { index, _ ->
            GameSystemEmulatorOption("choice-$index", index, "gc", "Nintendo GameCube",
                "Nintendo GameCube", "gamecube", GamePickerKind.FILE)
        }
        var skipped = false
        show(LibraryScanProgress(running = true, stage = LibraryScanStage.CHOOSING_EMULATOR,
            platform = "Nintendo GameCube", totalPlatforms = 24,
            emulatorChoice = LibraryEmulatorChoice("GameCube", options))) { skipped = it == null }
        compose.onNodeWithText("Skip platform").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(skipped) }
    }

    @Test fun compactEmulatorChoicePrioritizesOptionsAndKeepsFooterReachable() {
        val options = List(12) { index ->
            GameSystemEmulatorOption("choice-$index", 0, "gc", "Option $index",
                "Nintendo GameCube", "gamecube", GamePickerKind.FILE)
        }
        var selected: String? = null
        var cancelled = false
        show(LibraryScanProgress(running = true, stage = LibraryScanStage.CHOOSING_EMULATOR,
            platform = "Nintendo GameCube", totalPlatforms = 24,
            emulatorChoice = LibraryEmulatorChoice("GameCube", options)), compact = true,
            onCancel = { cancelled = true }) { selected = it }
        val label = CustomIntentTemplates.all[0].label.removeSuffix(" preset")
        compose.onNodeWithText("$label · Option 0").assertIsDisplayed()
        val scrollBounds = compose.onNodeWithTag("library-setup-content").fetchSemanticsNode().boundsInRoot
        val titleBounds = compose.onNodeWithText("Welcome to Retro Launcher").fetchSemanticsNode().boundsInRoot
        val footerBounds = compose.onNodeWithText("Cancel scan").fetchSemanticsNode().boundsInRoot
        assertTrue("Options should occupy more than half the dialog height",
            scrollBounds.height > (footerBounds.bottom - titleBounds.top) / 2f)
        compose.onNodeWithText("$label · Option 11").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(selected == "choice-11") }
        compose.onNodeWithText("Skip platform").assertIsDisplayed()
        compose.onNodeWithText("Cancel scan").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }

    @Test fun singleRetroArchChoiceNamesRequiredCoreWithoutClaimingItIsInstalled() {
        val index = CustomIntentTemplates.all.indexOfFirst { it.packageName == "com.retroarch.aarch64" }
        val option = GameSystemEmulatorOption("retroarch-mame", index, "mame", "MAME Arcade",
            "MAME Arcade", "mame", GamePickerKind.FILE, retroArchSystemId = "mame")
        var selected: String? = null
        show(LibraryScanProgress(running = true, stage = LibraryScanStage.CHOOSING_EMULATOR,
            platform = "MAME Arcade", totalPlatforms = 24,
            emulatorChoice = LibraryEmulatorChoice("MAME", listOf(option))), compact = true) { selected = it }
        compose.onNodeWithText("Choose the emulator for MAME; each game is added only once.").assertIsDisplayed()
        compose.onNodeWithText(
            "Requires installed core mame2003_plus_libretro_android.so and a compatible ROM. Core availability is not verified."
        ).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(selected == "retroarch-mame") }
        compose.onNodeWithText("Skip platform").assertIsDisplayed()
        compose.onNodeWithText("Cancel scan").assertIsDisplayed()
    }

    @Test fun initialScummVmSetupOffersRegisteredPickerBulkConfigAndSafeSkip() {
        var pickerOpened = false
        var configOpened = false
        var skipped = false
        compose.setContent {
            CompactDialogTestViewport {
                MaterialTheme {
                    LibrarySetupDialog(firstRun = true,
                        progress = LibraryScanProgress(running = true, stage = LibraryScanStage.LINKING_SCUMMVM,
                            platform = "ScummVM", scummVmSetupRequired = true), hasRoot = true,
                        isDefaultLauncher = true, installedCount = 1, sharedGameCount = 0,
                        onChooseRoot = {}, onScan = {}, onCancel = {}, onChooseEmulator = {},
                        onSetHome = {}, onDismiss = {},
                        onChooseScummVmGame = { pickerOpened = true },
                        onImportScummVmConfig = { configOpened = true }, onSkipScummVm = { skipped = true })
                }
            }
        }
        compose.onNodeWithText("Choose registered game").performScrollTo().performClick()
        compose.onNodeWithText("Import ScummVM config").performScrollTo().performClick()
        compose.onNodeWithText("Skip platform").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(pickerOpened && configOpened && skipped) }
    }
}