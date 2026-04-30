package dk.dittmann.spotifypacer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.dittmann.spotifypacer.auth.AuthLauncher
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.ui.login.LoginScreen
import dk.dittmann.spotifypacer.ui.login.LoginViewModel
import dk.dittmann.spotifypacer.ui.login.SignInLauncher

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authService = (application as SpotifyPacerApplication).authService
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginRoute(
                        authService = authService,
                        signInLauncher = { AuthLauncher.launch(this, authService) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginRoute(authService: AuthService, signInLauncher: SignInLauncher) {
    val viewModel: LoginViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(
                        modelClass: Class<T>
                    ): T = LoginViewModel(authService = authService, launcher = signInLauncher) as T
                }
        )

    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAuthState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    LoginScreen(
        state = state,
        onSignIn = viewModel::onSignInClicked,
        onSignedIn = {
            // Temporary: navigation graph (#10) will replace this with a real destination.
            Toast.makeText(context, "Signed in to Spotify ✔", Toast.LENGTH_SHORT).show()
        },
    )
}
