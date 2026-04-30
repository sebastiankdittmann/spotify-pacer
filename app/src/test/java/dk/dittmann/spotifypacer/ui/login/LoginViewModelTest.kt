package dk.dittmann.spotifypacer.ui.login

import dk.dittmann.spotifypacer.auth.AuthApiFactory
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.auth.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginViewModelTest {

    private lateinit var authService: AuthService
    private var launches = 0
    private var launchError: Throwable? = null

    @Before
    fun setup() {
        authService =
            AuthService(
                clientId = "client",
                redirectUri = "spotifypacer://callback",
                tokenStore = FakeTokenStore(),
                api = AuthApiFactory.create(),
            )
        launches = 0
        launchError = null
    }

    @Test
    fun initial_state_is_idle() {
        val vm = newViewModel()
        assertEquals(LoginState.Idle, vm.state.value)
    }

    @Test
    fun click_transitions_to_loading_and_invokes_launcher() {
        val vm = newViewModel()

        vm.onSignInClicked()

        assertEquals(LoginState.Loading, vm.state.value)
        assertEquals(1, launches)
    }

    @Test
    fun launcher_failure_surfaces_as_error_state() {
        launchError = IllegalStateException("Custom Tabs unavailable")
        val vm = newViewModel()

        vm.onSignInClicked()

        val state = vm.state.value
        assertTrue(state is LoginState.Error)
        assertEquals("Custom Tabs unavailable", (state as LoginState.Error).message)
    }

    @Test
    fun refreshAuthState_marks_success_when_access_token_present() {
        val vm = newViewModel()
        vm.onSignInClicked()
        // Simulate successful redirect: AuthService now holds a valid access token.
        injectAccessToken(authService)

        vm.refreshAuthState()

        assertEquals(LoginState.Success, vm.state.value)
    }

    @Test
    fun refreshAuthState_resets_loading_to_idle_when_no_token() {
        val vm = newViewModel()
        vm.onSignInClicked()

        vm.refreshAuthState()

        assertEquals(LoginState.Idle, vm.state.value)
    }

    @Test
    fun refreshAuthState_does_not_clobber_idle_or_error() {
        val vm = newViewModel()
        vm.refreshAuthState()
        assertEquals(LoginState.Idle, vm.state.value)

        launchError = RuntimeException("nope")
        vm.onSignInClicked()
        val errorBefore = vm.state.value
        vm.refreshAuthState()
        assertEquals(errorBefore, vm.state.value)
    }

    private fun newViewModel(): LoginViewModel =
        LoginViewModel(
            authService = authService,
            launcher = {
                launches++
                launchError?.let { throw it }
            },
        )

    /**
     * Sets a valid access token on the AuthService via reflection — the property is private and
     * normally written by the OAuth callback. Test-only shortcut.
     */
    private fun injectAccessToken(service: AuthService) {
        val tokenField =
            AuthService::class.java.getDeclaredField("accessToken").apply { isAccessible = true }
        val expiryField =
            AuthService::class.java.getDeclaredField("accessTokenExpiresAt").apply {
                isAccessible = true
            }
        tokenField.set(service, "fresh-token")
        expiryField.set(service, Long.MAX_VALUE)
    }

    private class FakeTokenStore : TokenStore {
        private var token: String? = null

        override fun saveRefreshToken(token: String) {
            this.token = token
        }

        override fun readRefreshToken(): String? = token

        override fun clear() {
            token = null
        }
    }
}
