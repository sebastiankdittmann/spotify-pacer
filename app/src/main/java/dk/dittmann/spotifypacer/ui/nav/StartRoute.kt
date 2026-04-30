package dk.dittmann.spotifypacer.ui.nav

import dk.dittmann.spotifypacer.auth.TokenStore

/**
 * Auth-guarded start destination: skip the login screen if a refresh token is already stored. Token
 * validity is not checked here — an expired refresh fails later via [AuthTokenBridge] and surfaces
 * as a network error in the calling screen.
 */
fun chooseStartRoute(tokenStore: TokenStore): String =
    if (tokenStore.readRefreshToken() != null) Routes.Setup else Routes.Login
