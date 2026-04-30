package dk.dittmann.spotifypacer.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.ui.login.LoginScreen
import dk.dittmann.spotifypacer.ui.login.LoginViewModel
import dk.dittmann.spotifypacer.ui.login.SignInLauncher

/**
 * Login destination wrapper. Owns lifecycle observation that re-checks the auth state on resume —
 * after the OAuth redirect returns the user to the activity.
 */
@Composable
fun LoginRoute(
    authService: AuthService,
    signInLauncher: SignInLauncher,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

    LoginScreen(
        state = state,
        onSignIn = viewModel::onSignInClicked,
        onSignedIn = onSignedIn,
        modifier = modifier,
    )
}
