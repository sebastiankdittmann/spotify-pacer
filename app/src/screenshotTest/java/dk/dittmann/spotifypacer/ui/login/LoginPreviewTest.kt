package dk.dittmann.spotifypacer.ui.login

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "login_idle_light", showBackground = true)
@Composable
fun LoginIdlePreview() {
    MaterialTheme { Surface { LoginScreen(LoginState.Idle, onSignIn = {}, onSignedIn = {}) } }
}

@Preview(name = "login_idle_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoginIdleDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { LoginScreen(LoginState.Idle, onSignIn = {}, onSignedIn = {}) }
    }
}

@Preview(name = "login_loading_light", showBackground = true)
@Composable
fun LoginLoadingPreview() {
    MaterialTheme { Surface { LoginScreen(LoginState.Loading, onSignIn = {}, onSignedIn = {}) } }
}

@Preview(name = "login_error_light", showBackground = true)
@Composable
fun LoginErrorPreview() {
    MaterialTheme {
        Surface {
            LoginScreen(
                state = LoginState.Error("Couldn't reach Spotify. Try again."),
                onSignIn = {},
                onSignedIn = {},
            )
        }
    }
}

@Preview(name = "login_success_light", showBackground = true)
@Composable
fun LoginSuccessPreview() {
    MaterialTheme { Surface { LoginScreen(LoginState.Success, onSignIn = {}, onSignedIn = {}) } }
}
