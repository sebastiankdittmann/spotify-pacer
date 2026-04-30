package dk.dittmann.spotifypacer.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupRouteTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun submitting_valid_inputs_invokes_on_confirm_with_run_config() {
        var captured: RunConfig? = null
        composeRule.setContent { MaterialTheme { SetupRoute(onConfirm = { captured = it }) } }

        composeRule.onNodeWithText("Distance (km)").performTextInput("5")
        composeRule.onNodeWithText("Target time").performTextInput("28:00")
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        assertNotNull(captured)
        assertEquals(5.0, captured!!.distanceKm, 0.0)
        assertEquals(28 * 60, captured!!.targetTimeSec)
        assertEquals(PaceStrategy.LinearRamp, captured!!.strategy)
    }

    @Test
    fun submit_button_disabled_with_blank_inputs_and_does_not_invoke_callback() {
        var calls = 0
        composeRule.setContent { MaterialTheme { SetupRoute(onConfirm = { calls++ }) } }

        composeRule.onNodeWithText("Continue").performScrollTo().assertIsNotEnabled()
        assertEquals(0, calls)
    }
}
