package com.ariy.retrolauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResetLauncherDialogTest {
    @get:Rule val compose = createComposeRule()
    private var confirmations = 0
    private var cancellations = 0

    private fun showConfirmation() {
        compose.setContent {
            val open = androidx.compose.runtime.remember { mutableStateOf(true) }
            MaterialTheme {
                if (open.value) ResetLauncherDialog(
                    onCancel = { cancellations++; open.value = false },
                    onConfirm = { confirmations++; open.value = false }
                )
            }
        }
    }

    @Test fun cancelIsInitiallyFocusedAndDoesNotReset() {
        showConfirmation()
        compose.onNodeWithText("Cancel").assertIsFocused().performClick()
        compose.runOnIdle {
            assertEquals(0, confirmations)
            assertEquals(1, cancellations)
        }
    }

    @Test fun backDismissesWithoutReset() {
        showConfirmation()
        pressBack()
        compose.runOnIdle {
            assertEquals(0, confirmations)
            assertEquals(1, cancellations)
        }
    }

    @Test fun onlyExplicitConfirmationResets() {
        showConfirmation()
        compose.runOnIdle { assertEquals(0, confirmations) }
        compose.onNodeWithText("Reset launcher").performClick()
        compose.runOnIdle {
            assertEquals(1, confirmations)
            assertEquals(0, cancellations)
        }
    }
}