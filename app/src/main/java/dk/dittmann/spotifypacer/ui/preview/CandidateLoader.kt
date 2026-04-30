package dk.dittmann.spotifypacer.ui.preview

import dk.dittmann.spotifypacer.bpm.BpmRepository
import dk.dittmann.spotifypacer.bpm.TrackKey
import dk.dittmann.spotifypacer.pacing.CandidateTrack
import dk.dittmann.spotifypacer.spotify.SpotifyClient

/**
 * Loads the user's candidate track pool. Abstracted so unit tests can supply a fixed pool and the
 * production app can swap in an implementation that hits Spotify, all without touching the
 * ViewModel.
 */
fun interface CandidateLoader {
    suspend fun load(): List<CandidateTrack>
}

/**
 * Default implementation: pages through `/me/tracks` (the user's liked songs) and resolves BPM via
 * [BpmRepository] page-by-page. Unlike the previous version that loaded the whole library before
 * hitting the audio-features endpoint, this loader stops early once it has enough candidates with
 * matched tempo data — important now that BPM lookups are sequential and rate-limited.
 *
 * Stop conditions (whichever first):
 * - `minPool` valid candidates accumulated, or
 * - `maxLookupBudget` external BPM lookups consumed (cache hits don't count), or
 * - saved-tracks pagination exhausted.
 */
class SpotifyCandidateLoader(
    private val client: SpotifyClient,
    private val bpmRepository: BpmRepository,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val minPool: Int = DEFAULT_MIN_POOL,
    private val maxLookupBudget: Int = DEFAULT_MAX_LOOKUP_BUDGET,
) : CandidateLoader {

    override suspend fun load(): List<CandidateTrack> {
        val pool = mutableListOf<CandidateTrack>()
        var offset = 0
        var lookupsRemaining = maxLookupBudget
        while (true) {
            val page = client.savedTracks(limit = pageSize, offset = offset)
            if (page.items.isEmpty()) break
            val tracks = page.items.map { it.track }
            val keys =
                tracks.map { track ->
                    TrackKey(
                        spotifyId = track.id,
                        title = track.name,
                        primaryArtist = track.artists.firstOrNull()?.name.orEmpty(),
                        durationMs = track.durationMs,
                    )
                }
            val budgeted = keys.take(lookupsRemaining)
            val bpm = bpmRepository.bpmFor(budgeted)
            lookupsRemaining = (lookupsRemaining - budgeted.size).coerceAtLeast(0)
            tracks.forEach { track ->
                val tempo = bpm[track.id] ?: return@forEach
                pool +=
                    CandidateTrack(
                        id = track.id,
                        title = track.name,
                        artist = track.artists.joinToString(", ") { it.name }.ifBlank { "Unknown" },
                        bpm = tempo,
                        durationSec = (track.durationMs / 1000L).toInt(),
                    )
            }
            if (pool.size >= minPool) break
            if (lookupsRemaining == 0) break
            if (page.next == null) break
            offset += page.items.size
        }
        return pool
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val DEFAULT_MIN_POOL = 200
        const val DEFAULT_MAX_LOOKUP_BUDGET = 300
    }
}
