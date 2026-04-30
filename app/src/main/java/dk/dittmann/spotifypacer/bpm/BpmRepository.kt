package dk.dittmann.spotifypacer.bpm

/**
 * Resolves BPM (tempo) for a batch of Spotify tracks. The map's key is the Spotify track id; tracks
 * that the provider could not match are simply absent from the result.
 *
 * Implementations are expected to honour cancellation cooperatively and to throw
 * [BpmProviderException] for provider-wide failures (auth, rate-limit, transport). Per-track
 * matching failures are not exceptional — they're modelled as missing keys.
 */
fun interface BpmRepository {
    suspend fun bpmFor(tracks: List<TrackKey>): Map<String, Double>
}

/** Just enough of a Spotify track to drive a GetSongBPM lookup. */
data class TrackKey(
    val spotifyId: String,
    val title: String,
    val primaryArtist: String,
    val durationMs: Long,
)

sealed class BpmProviderException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    /** API key missing or rejected — surfaces as a configuration error to the user. */
    class Unconfigured(message: String) : BpmProviderException(message)

    /** Provider returned 429 or otherwise asked us to back off. */
    class RateLimited(cause: Throwable? = null) :
        BpmProviderException("BPM provider rate limited", cause)

    /** Provider was unreachable or returned an unexpected error. */
    class Unavailable(cause: Throwable? = null) :
        BpmProviderException("BPM provider unavailable", cause)
}
