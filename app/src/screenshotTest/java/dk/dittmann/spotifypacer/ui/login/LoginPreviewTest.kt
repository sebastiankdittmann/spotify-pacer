package dk.dittmann.spotifypacer.ui.login

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Static placeholder for the spinner so the screenshot is deterministic across hosts. The runtime
 * indicator stays indeterminate; this override is preview-only.
 */
@Composable
private fun PreviewLoadingIndicator() {
    Box(
        modifier =
            Modifier.size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .semantics { contentDescription = "Signing you in" }
    )
}

@Composable
private fun LoginPreviewBody(state: LoginState) {
    LoginScreen(
        state = state,
        onSignIn = {},
        onSignedIn = {},
        loadingIndicator = { PreviewLoadingIndicator() },
    )
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

@Preview(name = "login_loading_light", showBackground = true)
@Composable
fun LoginLoadingPreview() {
    MaterialTheme { Surface { LoginPreviewBody(LoginState.Loading) } }
}

@Preview(name = "login_loading_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoginLoadingDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { LoginPreviewBody(LoginState.Loading) }
    }
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
