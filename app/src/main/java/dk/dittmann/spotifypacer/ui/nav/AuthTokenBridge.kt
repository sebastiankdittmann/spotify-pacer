package dk.dittmann.spotifypacer.ui.nav

import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.spotify.SpotifyTokenProvider
import kotlinx.coroutines.runBlocking

/**
 * Bridges [AuthService]'s suspend refresh API to the synchronous [SpotifyTokenProvider] expected by
 * the OkHttp auth interceptor. Refresh runs on the OkHttp I/O thread, so blocking is safe; we never
 * call this from the main thread.
 */
class AuthTokenBridge(private val authService: AuthService) : SpotifyTokenProvider {

    private val refreshLock = Any()

    override fun currentAccessToken(): String? = authService.currentAccessToken()

    override fun refreshAccessToken(): String =
        synchronized(refreshLock) { runBlocking { authService.refreshAccessToken() } }
}
