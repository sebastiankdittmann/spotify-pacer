package dk.dittmann.spotifypacer.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dk.dittmann.spotifypacer.ui.login.LoginScreen
import dk.dittmann.spotifypacer.ui.login.LoginState

/**
 * Top-level scaffold preview. Renders the login destination inside the same Surface used by
 * MainActivity so layout-level regressions on the host show up in screenshot validation.
 */
@Composable
private fun AppScaffold() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LoginScreen(state = LoginState.Idle, onSignIn = {}, onSignedIn = {})
        }
    }
}

@Preview(name = "scaffold_login", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun AppScaffoldPreview() {
    AppScaffold()
}
