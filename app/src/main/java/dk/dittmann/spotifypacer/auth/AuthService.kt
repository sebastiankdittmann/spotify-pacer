package dk.dittmann.spotifypacer.auth

import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class AuthService(
    private val clientId: String,
    private val redirectUri: String,
    private val tokenStore: TokenStore,
    private val api: SpotifyAuthApi,
    private val authBaseUrl: HttpUrl = DEFAULT_AUTH_BASE_URL.toHttpUrl(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
    private val stateGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    @Volatile private var accessToken: String? = null
    @Volatile private var accessTokenExpiresAt: Long = 0L
    private var pending: PendingAuth? = null

    private val _isAuthenticated = MutableStateFlow(tokenStore.readRefreshToken() != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun prepareAuthorize(): HttpUrl {
        val verifier = PkceGenerator.generateCodeVerifier(random)
        val challenge = PkceGenerator.codeChallenge(verifier)
        val state = stateGenerator()
        pending = PendingAuth(verifier = verifier, state = state)
        return buildAuthUrl(codeChallenge = challenge, state = state)
    }

    fun buildAuthUrl(codeChallenge: String, state: String): HttpUrl =
        authBaseUrl
            .newBuilder()
            .addPathSegment("authorize")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("code_challenge", codeChallenge)
            .addQueryParameter("state", state)
            .addQueryParameter("scope", SCOPES.joinToString(" "))
            .build()

    suspend fun completeAuthorize(code: String?, state: String?, spotifyError: String?) {
        val pending = pending ?: error("No pending authorization. Call prepareAuthorize() first.")
        try {
            check(state == pending.state) { "State mismatch on auth redirect." }
            if (spotifyError != null) error("Spotify authorization failed: $spotifyError")
            checkNotNull(code) { "Missing authorization code in redirect." }
            exchangeCode(code = code, codeVerifier = pending.verifier)
        } finally {
            this.pending = null
        }
    }

    suspend fun exchangeCode(code: String, codeVerifier: String) {
        val response =
            api.exchangeCode(
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = codeVerifier,
            )
        applyTokenResponse(response)
        response.refreshToken?.let(tokenStore::saveRefreshToken)
    }

    suspend fun refreshAccessToken(): String {
        val refresh =
            tokenStore.readRefreshToken()
                ?: error("No refresh token stored. User must sign in again.")
        val response = api.refreshToken(refreshToken = refresh, clientId = clientId)
        applyTokenResponse(response)
        response.refreshToken?.let(tokenStore::saveRefreshToken)
        return response.accessToken
    }

    fun currentAccessToken(): String? =
        accessToken?.takeIf { clock() < accessTokenExpiresAt - EXPIRY_SKEW_MS }

    fun logout() {
        tokenStore.clear()
        accessToken = null
        accessTokenExpiresAt = 0L
        pending = null
        _isAuthenticated.value = false
    }

    private fun applyTokenResponse(response: TokenResponse) {
        accessToken = response.accessToken
        accessTokenExpiresAt = clock() + response.expiresIn * 1000L
        _isAuthenticated.value = true
    }

    companion object {
        const val DEFAULT_AUTH_BASE_URL = "https://accounts.spotify.com/"
        private const val EXPIRY_SKEW_MS = 30_000L

        val SCOPES = listOf("user-library-read", "playlist-read-private", "playlist-modify-private")
    }

    private data class PendingAuth(val verifier: String, val state: String)
}
