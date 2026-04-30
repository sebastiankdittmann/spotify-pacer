package dk.dittmann.spotifypacer.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.auth.SpotifyAuthApi
import dk.dittmann.spotifypacer.auth.TokenStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginRouteTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeTokenStore : TokenStore {
        override fun saveRefreshToken(token: String) = Unit

        override fun readRefreshToken(): String? = null

        override fun clear() = Unit
    }

    private val unusedApi: SpotifyAuthApi =
        object : SpotifyAuthApi {
            override suspend fun exchangeCode(
                grantType: String,
                code: String,
                redirectUri: String,
                clientId: String,
                codeVerifier: String,
            ) = throw UnsupportedOperationException()

            override suspend fun refreshToken(
                grantType: String,
                refreshToken: String,
                clientId: String,
            ) = throw UnsupportedOperationException()
        }

    private fun authService() =
        AuthService(
            clientId = "client-id",
            redirectUri = "spotifypacer://callback",
            tokenStore = FakeTokenStore(),
            api = unusedApi,
            authBaseUrl = "https://accounts.spotify.com/".toHttpUrl(),
        )

    @Test
    fun sign_in_button_invokes_launcher() {
        var launched = 0
        composeRule.setContent {
            MaterialTheme {
                LoginRoute(
                    authService = authService(),
                    signInLauncher = { launched++ },
                    onSignedIn = {},
                )
            }
        }
        composeRule.onNodeWithText("Sign in with Spotify").performClick()
        assertEquals(1, launched)
    }

    @Test
    fun launcher_failure_renders_error_message() {
        composeRule.setContent {
            MaterialTheme {
                LoginRoute(
                    authService = authService(),
                    signInLauncher = { error("no browser") },
                    onSignedIn = {},
                )
            }
        }
        composeRule.onNodeWithText("Sign in with Spotify").performClick()
        composeRule.onNodeWithText("no browser").assertExists()
    }

    @Test
    fun on_signed_in_not_invoked_without_token() {
        var signedIn = 0
        composeRule.setContent {
            MaterialTheme {
                LoginRoute(
                    authService = authService(),
                    signInLauncher = {},
                    onSignedIn = { signedIn++ },
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue(signedIn == 0)
    }
}
