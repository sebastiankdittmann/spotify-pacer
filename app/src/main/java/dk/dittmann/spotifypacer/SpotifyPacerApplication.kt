package dk.dittmann.spotifypacer

import android.app.Application
import dk.dittmann.spotifypacer.auth.AuthApiFactory
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.auth.EncryptedTokenStore

class SpotifyPacerApplication : Application() {

    val authService: AuthService by lazy {
        AuthService(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
            tokenStore = EncryptedTokenStore(applicationContext),
            api = AuthApiFactory.create(),
        )
    }
}
