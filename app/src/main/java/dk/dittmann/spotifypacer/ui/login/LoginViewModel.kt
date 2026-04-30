package dk.dittmann.spotifypacer.ui.login

import androidx.lifecycle.ViewModel
import dk.dittmann.spotifypacer.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the login screen. Owns the screen state but delegates the actual sign-in launch to a
 * [SignInLauncher] so the auth flow's Activity dependency stays out of the ViewModel.
 *
 * The auth dependency is a small token-snapshot lambda so tests don't need to reach into
 * [AuthService] internals.
 */
class LoginViewModel(
    private val accessTokenSource: () -> String?,
    private val launcher: SignInLauncher,
) : ViewModel() {

    constructor(
        authService: AuthService,
        launcher: SignInLauncher,
    ) : this(accessTokenSource = authService::currentAccessToken, launcher = launcher)

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onSignInClicked() {
        _state.value = LoginState.Loading
        runCatching { launcher.launch() }
            .onFailure { _state.value = LoginState.Error(it.message ?: "Sign-in failed.") }
    }

    /**
     * Re-evaluates the sign-in result. Call from the screen's lifecycle resume — by then the OAuth
     * redirect has either completed (token present) or been cancelled (still loading, no token).
     */
    fun refreshAuthState() {
        if (accessTokenSource() != null) {
            _state.value = LoginState.Success
            return
        }
        if (_state.value == LoginState.Loading) {
            _state.value = LoginState.Idle
        }
    }
}

/**
 * Launches the Spotify sign-in flow. The default impl bridges to
 * [dk.dittmann.spotifypacer.auth.AuthLauncher].
 */
fun interface SignInLauncher {
    fun launch()
}
