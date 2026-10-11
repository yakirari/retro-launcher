package com.ariy.retrolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdaptiveLauncherDialogTest {
    @get:Rule val compose = createComposeRule()

    private fun showForm(onSave: () -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                AdaptiveLauncherDialog(onDismissRequest = {}) {
                    Surface {
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add games")
                            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                var title by remember { mutableStateOf("") }
                                OutlinedTextField(title, { title = it }, label = { Text("Title") })
                                repeat(30) { Text("Form section $it", modifier = Modifier.padding(vertical = 12.dp)) }
                            }
                            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                        }
                    }
                }
            }
        }
    }

    @Test fun longFormScrollDoesNotHideSave() {
        showForm()
        compose.onNodeWithText("Form section 29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test fun editingAFieldKeepsSaveReachable() {
        var saved = false
        showForm { saved = true }
        compose.onNodeWithText("Title").performClick().performTextInput("My game")
        compose.onNodeWithText("Save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(saved) }
    }
}