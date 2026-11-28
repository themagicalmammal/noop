package com.noop.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToggleCheckboxTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clickTogglesAndPublishesCheckboxState() {
        var checked by mutableStateOf(false)

        compose.setContent {
            ToggleCheckbox(
                checked = checked,
                onCheckedChange = { checked = it },
                modifier = Modifier.testTag("toggle-checkbox"),
            )
        }

        compose.onNodeWithTag("toggle-checkbox")
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }
}
