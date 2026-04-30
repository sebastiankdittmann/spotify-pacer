package dk.dittmann.spotifypacer.ui.login

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// The loading state is intentionally not screenshot-tested — its progress indicator is dynamic
// and not portable across rendering hosts. LoginScreenTest covers the loading state behaviorally.

@Composable
private fun LoginPreviewBody(state: LoginState) {
    LoginScreen(state = state, onSignIn = {}, onSignedIn = {})
}

@Preview(name = "login_idle_light", showBackground = true)
@Composable
fun LoginIdlePreview() {
    MaterialTheme { Surface { LoginPreviewBody(LoginState.Idle) } }
}

@Preview(name = "login_idle_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoginIdleDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) { Surface { LoginPreviewBody(LoginState.Idle) } }
}

@Preview(name = "login_error_light", showBackground = true)
@Composable
fun LoginErrorPreview() {
    MaterialTheme {
        Surface { LoginPreviewBody(LoginState.Error("Couldn't reach Spotify. Try again.")) }
    }
}

@Preview(name = "login_error_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoginErrorDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { LoginPreviewBody(LoginState.Error("Couldn't reach Spotify. Try again.")) }
    }
}

@Preview(name = "login_success_light", showBackground = true)
@Composable
fun LoginSuccessPreview() {
    MaterialTheme { Surface { LoginPreviewBody(LoginState.Success) } }
}

@Preview(name = "login_success_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoginSuccessDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { LoginPreviewBody(LoginState.Success) }
    }
}
