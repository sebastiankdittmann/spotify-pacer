package dk.dittmann.spotifypacer

import android.app.Application
import dk.dittmann.spotifypacer.auth.AuthApiFactory
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.auth.EncryptedTokenStore
import dk.dittmann.spotifypacer.auth.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SpotifyPacerApplication : Application() {

    val tokenStore: TokenStore by lazy { EncryptedTokenStore(applicationContext) }

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val authService: AuthService by lazy {
        AuthService(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
            tokenStore = tokenStore,
            api = AuthApiFactory.create(),
        )
    }
}
