package dk.dittmann.spotifypacer.ui.setup

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun empty_state_disables_submit() {
        renderSetup(SetupState())

        composeRule
            .onNodeWithText("Continue")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun valid_inputs_enable_submit() {
        renderSetup(SetupState(distanceText = "5", targetTimeText = "28:00"))

        composeRule.onNodeWithText("Continue").performScrollTo().assertIsEnabled()
    }

    @Test
    fun distance_error_renders_message() {
        renderSetup(SetupState(distanceText = "abc", distanceError = DistanceError.NotANumber))

        composeRule.onNodeWithText("Enter a number, e.g. 5.0").assertIsDisplayed()
    }

    @Test
    fun target_time_error_renders_message() {
        renderSetup(SetupState(targetTimeText = "xx", targetTimeError = TargetTimeError.Malformed))

        composeRule.onNodeWithText("Use mm:ss or hh:mm:ss").assertIsDisplayed()
    }

    @Test
    fun pace_warning_renders_message() {
        renderSetup(
            SetupState(
                distanceText = "5",
                targetTimeText = "10:00",
                paceWarning = PaceWarning.TooFast,
            )
        )

        composeRule
            .onNodeWithText("That pace is faster than 3:00 min/km — sure?")
            .assertIsDisplayed()
    }

    @Test
    fun strategy_radios_invoke_callback() {
        var picked: StrategyChoice? = null
        composeRule.setContent {
            MaterialTheme {
                SetupScreen(
                    state = SetupState(),
                    onDistanceChange = {},
                    onTargetTimeChange = {},
                    onStrategyChange = { picked = it },
                    onSubmit = {},
                )
            }
        }

        composeRule.onNodeWithText("Constant").performClick()

        assertEquals(StrategyChoice.Constant, picked)
    }

    @Test
    fun typing_distance_invokes_callback() {
        var typed: String? = null
        composeRule.setContent {
            MaterialTheme {
                SetupScreen(
                    state = SetupState(),
                    onDistanceChange = { typed = it },
                    onTargetTimeChange = {},
                    onStrategyChange = {},
                    onSubmit = {},
                )
            }
        }

        composeRule.onNodeWithText("Distance (km)").performTextInput("5")

        assertEquals("5", typed)
    }

    @Test
    fun submit_invokes_callback_when_enabled() {
        var submitted = 0
        composeRule.setContent {
            MaterialTheme {
                SetupScreen(
                    state = SetupState(distanceText = "5", targetTimeText = "28:00"),
                    onDistanceChange = {},
                    onTargetTimeChange = {},
                    onStrategyChange = {},
                    onSubmit = { submitted++ },
                )
            }
        }

        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        assertEquals(1, submitted)
    }

    private fun renderSetup(state: SetupState) {
        composeRule.setContent {
            MaterialTheme {
                SetupScreen(
                    state = state,
                    onDistanceChange = {},
                    onTargetTimeChange = {},
                    onStrategyChange = {},
                    onSubmit = {},
                )
            }
        }
    }
}
