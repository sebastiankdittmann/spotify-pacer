package dk.dittmann.spotifypacer.ui.nav

import dk.dittmann.spotifypacer.auth.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Test

class StartRouteTest {

    private class FakeTokenStore(private val token: String?) : TokenStore {
        override fun saveRefreshToken(token: String) = Unit

        override fun readRefreshToken(): String? = token

        override fun clear() = Unit
    }

    @Test
    fun returns_login_when_no_refresh_token() {
        assertEquals(Routes.Login, chooseStartRoute(FakeTokenStore(null)))
    }

    @Test
    fun returns_setup_when_refresh_token_present() {
        assertEquals(Routes.Setup, chooseStartRoute(FakeTokenStore("rt-123")))
    }
}
