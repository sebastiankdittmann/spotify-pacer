package dk.dittmann.spotifypacer.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal const val SIGN_IN_LABEL = "Sign in with Spotify"
internal const val LOADING_DESCRIPTION = "Signing you in"

@Composable
fun LoginScreen(
    state: LoginState,
    onSignIn: () -> Unit,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state) { if (state is LoginState.Success) onSignedIn() }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "spotify-pacer", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Build a playlist paced to your run.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        Button(onClick = onSignIn, enabled = state !is LoginState.Loading) { Text(SIGN_IN_LABEL) }

        when (state) {
            is LoginState.Loading -> {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = LOADING_DESCRIPTION }
                )
            }
            is LoginState.Error -> {
                Spacer(Modifier.height(16.dp))
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
            else -> Unit
        }
    }
}
