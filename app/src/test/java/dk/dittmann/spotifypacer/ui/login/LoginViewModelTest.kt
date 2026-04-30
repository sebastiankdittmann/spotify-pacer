package dk.dittmann.spotifypacer.ui.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private var token: String? = null
    private var launches = 0
    private var launchError: Throwable? = null
    private val authState = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        token = null
        launches = 0
        launchError = null
        authState.value = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_idle() {
        assertEquals(LoginState.Idle, newViewModel().state.value)
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
        token = "fresh-token"

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

    @Test
    fun authStateChanges_flip_loading_to_success_when_authenticated() {
        val vm = newViewModel(authStateChanges = authState)
        vm.onSignInClicked()
        assertEquals(LoginState.Loading, vm.state.value)

        authState.value = true

        assertEquals(LoginState.Success, vm.state.value)
    }

    @Test
    fun authStateChanges_emitting_false_does_not_change_state() {
        val vm = newViewModel(authStateChanges = authState)

        authState.value = false

        assertEquals(LoginState.Idle, vm.state.value)
    }

    private fun newViewModel(authStateChanges: Flow<Boolean> = emptyFlow()): LoginViewModel =
        LoginViewModel(
            accessTokenSource = { token },
            launcher = {
                launches++
                launchError?.let { throw it }
            },
            authStateChanges = authStateChanges,
        )
}
