package dk.dittmann.spotifypacer.spotify

/**
 * Synchronous token source for the Spotify API client. Implementations bridge to the auth module —
 * keeping this interface here decouples the API client from auth wiring.
 */
interface SpotifyTokenProvider {
    /** Returns a non-expired access token, or null if a refresh is required. */
    fun currentAccessToken(): String?

    /** Forces a refresh and returns the new access token. May block on I/O. */
    fun refreshAccessToken(): String
}
