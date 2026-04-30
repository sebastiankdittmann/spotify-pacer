package dk.dittmann.spotifypacer.auth

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeTokenStore
    private lateinit var authService: AuthService
    private var now: Long = FIXED_NOW
    private var nextState: String = "state-0"

    @Before
    fun setup() {
        server = MockWebServer().also { it.start() }
        tokenStore = FakeTokenStore()
        authService =
            AuthService(
                clientId = CLIENT_ID,
                redirectUri = REDIRECT_URI,
                tokenStore = tokenStore,
                api = AuthApiFactory.create(baseUrl = server.url("/").toString()),
                authBaseUrl = server.url("/"),
                clock = { now },
                stateGenerator = { nextState },
            )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun buildAuthUrl_assembles_required_query_params() {
        val url = authService.buildAuthUrl(codeChallenge = "challenge-xyz", state = "state-abc")

        assertEquals("authorize", url.pathSegments.last())
        assertEquals(CLIENT_ID, url.queryParameter("client_id"))
        assertEquals("code", url.queryParameter("response_type"))
        assertEquals(REDIRECT_URI, url.queryParameter("redirect_uri"))
        assertEquals("S256", url.queryParameter("code_challenge_method"))
        assertEquals("challenge-xyz", url.queryParameter("code_challenge"))
        assertEquals("state-abc", url.queryParameter("state"))
        assertEquals(
            "user-library-read playlist-read-private playlist-modify-private",
            url.queryParameter("scope"),
        )
    }

    @Test
    fun exchangeCode_parses_response_and_persists_refresh_token() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "access-1",
                      "token_type": "Bearer",
                      "expires_in": 3600,
                      "refresh_token": "refresh-1",
                      "scope": "user-library-read"
                    }
                    """
                        .trimIndent()
                )
        )

        authService.exchangeCode(code = "auth-code", codeVerifier = "verifier-1")

        assertEquals("refresh-1", tokenStore.readRefreshToken())
        assertEquals("access-1", authService.currentAccessToken())

        val recorded: RecordedRequest = server.takeRequest()
        assertEquals("/api/token", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("grant_type=authorization_code"))
        assertTrue(body.contains("code=auth-code"))
        assertTrue(body.contains("code_verifier=verifier-1"))
        assertTrue(body.contains("client_id=$CLIENT_ID"))
    }

    @Test
    fun refreshAccessToken_uses_stored_refresh_token() = runTest {
        tokenStore.saveRefreshToken("stored-refresh")
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "access-2",
                      "token_type": "Bearer",
                      "expires_in": 1800
                    }
                    """
                        .trimIndent()
                )
        )

        val access = authService.refreshAccessToken()

        assertEquals("access-2", access)
        assertEquals("access-2", authService.currentAccessToken())
        assertEquals("stored-refresh", tokenStore.readRefreshToken())

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("grant_type=refresh_token"))
        assertTrue(body.contains("refresh_token=stored-refresh"))
    }

    @Test
    fun refreshAccessToken_rotates_stored_refresh_token_when_response_includes_one() = runTest {
        tokenStore.saveRefreshToken("stored-refresh")
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "access-3",
                      "token_type": "Bearer",
                      "expires_in": 1800,
                      "refresh_token": "rotated-refresh"
                    }
                    """
                        .trimIndent()
                )
        )

        authService.refreshAccessToken()

        assertEquals("rotated-refresh", tokenStore.readRefreshToken())
    }

    @Test(expected = IllegalStateException::class)
    fun refreshAccessToken_throws_when_no_refresh_token_stored() = runTest {
        authService.refreshAccessToken()
    }

    @Test
    fun currentAccessToken_returns_null_when_expired() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "access-4",
                      "token_type": "Bearer",
                      "expires_in": 60
                    }
                    """
                        .trimIndent()
                )
        )
        authService.exchangeCode(code = "c", codeVerifier = "v")
        assertNotNull(authService.currentAccessToken())

        now += 60_000L

        assertNull(authService.currentAccessToken())
    }

    @Test
    fun logout_clears_memory_and_persisted_tokens() = runTest {
        tokenStore.saveRefreshToken("stored-refresh")
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "access-5",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }
                    """
                        .trimIndent()
                )
        )
        authService.exchangeCode(code = "c", codeVerifier = "v")

        authService.logout()

        assertNull(authService.currentAccessToken())
        assertNull(tokenStore.readRefreshToken())
    }

    @Test
    fun isAuthenticated_initial_value_reflects_stored_refresh_token() {
        val withToken = FakeTokenStore().also { it.saveRefreshToken("r") }
        val service =
            AuthService(
                clientId = "cid",
                redirectUri = "app://cb",
                tokenStore = withToken,
                api = AuthApiFactory.create(baseUrl = server.url("/").toString()),
            )
        assertEquals(true, service.isAuthenticated.value)

        val empty =
            AuthService(
                clientId = "cid",
                redirectUri = "app://cb",
                tokenStore = FakeTokenStore(),
                api = AuthApiFactory.create(baseUrl = server.url("/").toString()),
            )
        assertEquals(false, empty.isAuthenticated.value)
    }

    @Test
    fun isAuthenticated_flips_true_after_successful_token_exchange() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "a",
                      "token_type": "Bearer",
                      "expires_in": 3600,
                      "refresh_token": "r"
                    }
                    """
                        .trimIndent()
                )
        )

        assertEquals(false, authService.isAuthenticated.value)
        authService.exchangeCode(code = "c", codeVerifier = "v")
        assertEquals(true, authService.isAuthenticated.value)
    }

    @Test
    fun isAuthenticated_flips_false_after_logout() = runTest {
        tokenStore.saveRefreshToken("r")
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "a",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }
                    """
                        .trimIndent()
                )
        )
        authService.exchangeCode(code = "c", codeVerifier = "v")
        assertEquals(true, authService.isAuthenticated.value)

        authService.logout()

        assertEquals(false, authService.isAuthenticated.value)
    }

    @Test
    fun prepareAuthorize_generates_pkce_and_state_and_returns_url() {
        nextState = "state-prep"

        val url = authService.prepareAuthorize()

        assertEquals("state-prep", url.queryParameter("state"))
        assertEquals("S256", url.queryParameter("code_challenge_method"))
        assertNotNull(url.queryParameter("code_challenge"))
    }

    @Test
    fun completeAuthorize_verifies_state_then_exchanges_code() = runTest {
        nextState = "expected-state"
        authService.prepareAuthorize()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "access_token": "a",
                      "token_type": "Bearer",
                      "expires_in": 3600,
                      "refresh_token": "r"
                    }
                    """
                        .trimIndent()
                )
        )

        authService.completeAuthorize(
            code = "auth-code",
            state = "expected-state",
            spotifyError = null,
        )

        assertEquals("a", authService.currentAccessToken())
        assertEquals("r", tokenStore.readRefreshToken())
    }

    @Test(expected = IllegalStateException::class)
    fun completeAuthorize_throws_on_state_mismatch() = runTest {
        nextState = "expected-state"
        authService.prepareAuthorize()

        authService.completeAuthorize(code = "c", state = "attacker-state", spotifyError = null)
    }

    @Test(expected = IllegalStateException::class)
    fun completeAuthorize_throws_when_spotify_returned_error() = runTest {
        nextState = "s"
        authService.prepareAuthorize()

        authService.completeAuthorize(code = null, state = "s", spotifyError = "access_denied")
    }

    @Test(expected = IllegalStateException::class)
    fun completeAuthorize_throws_when_no_prepare_in_flight() = runTest {
        authService.completeAuthorize(code = "c", state = "s", spotifyError = null)
    }

    @Test
    fun authBaseUrl_defaults_to_spotify_accounts() {
        val service =
            AuthService(
                clientId = CLIENT_ID,
                redirectUri = REDIRECT_URI,
                tokenStore = FakeTokenStore(),
                api = AuthApiFactory.create(),
            )
        val url = service.buildAuthUrl("c", "s")
        assertEquals("https://accounts.spotify.com/authorize", url.toString().substringBefore("?"))
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

    private companion object {
        const val CLIENT_ID = "test-client-id"
        const val REDIRECT_URI = "spotifypacer://callback"
        const val FIXED_NOW = 1_000_000_000L
    }
}
