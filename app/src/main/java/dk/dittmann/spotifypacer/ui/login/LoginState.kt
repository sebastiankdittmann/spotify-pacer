package dk.dittmann.spotifypacer.ui.login

sealed interface LoginState {
    data object Idle : LoginState

    data object Loading : LoginState

    data class Error(val message: String) : LoginState

    data object Success : LoginState
}
