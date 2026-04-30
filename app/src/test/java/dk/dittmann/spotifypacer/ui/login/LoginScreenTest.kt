package dk.dittmann.spotifypacer.ui.login

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun idle_shows_sign_in_button_enabled() {
        composeRule.setContent {
            MaterialTheme { LoginScreen(state = LoginState.Idle, onSignIn = {}, onSignedIn = {}) }
        }

        composeRule.onNodeWithText(SIGN_IN_LABEL).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun loading_disables_button_and_shows_spinner() {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(state = LoginState.Loading, onSignIn = {}, onSignedIn = {})
            }
        }

        composeRule.onNodeWithText(SIGN_IN_LABEL).assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(LOADING_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun error_renders_message() {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(
                    state = LoginState.Error("Couldn't reach Spotify."),
                    onSignIn = {},
                    onSignedIn = {},
                )
            }
        }

        composeRule.onNodeWithText("Couldn't reach Spotify.").assertIsDisplayed()
        composeRule.onNodeWithText(SIGN_IN_LABEL).assertIsEnabled()
    }

    @Test
    fun tapping_sign_in_invokes_callback() {
        var clicks = 0
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(state = LoginState.Idle, onSignIn = { clicks++ }, onSignedIn = {})
            }
        }

        composeRule.onNodeWithText(SIGN_IN_LABEL).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun success_state_emits_signed_in_event() {
        var signedIn = 0
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(state = LoginState.Success, onSignIn = {}, onSignedIn = { signedIn++ })
            }
        }

        composeRule.waitForIdle()
        assertEquals(1, signedIn)
    }
}
