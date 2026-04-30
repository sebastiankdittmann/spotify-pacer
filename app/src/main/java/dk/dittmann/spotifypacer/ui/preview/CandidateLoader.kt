package dk.dittmann.spotifypacer.ui.preview

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
 * Default implementation: pages through `/me/tracks` (the user's liked songs), then fetches their
 * audio features via [SpotifyClient]. Tracks without an `audio_features` entry are dropped — the
 * selector needs a tempo to match them against the curve.
 */
class SpotifyCandidateLoader(
    private val client: SpotifyClient,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
) : CandidateLoader {

    override suspend fun load(): List<CandidateTrack> {
        val tracks = mutableListOf<dk.dittmann.spotifypacer.spotify.Track>()
        var offset = 0
        while (true) {
            val page = client.savedTracks(limit = pageSize, offset = offset)
            tracks += page.items.map { it.track }
            if (page.next == null || page.items.isEmpty()) break
            offset += page.items.size
        }
        if (tracks.isEmpty()) return emptyList()

        val featuresById =
            client.audioFeatures(tracks.map { it.id }).filterNotNull().associateBy { it.id }

        return tracks.mapNotNull { track ->
            val feature = featuresById[track.id] ?: return@mapNotNull null
            CandidateTrack(
                id = track.id,
                title = track.name,
                artist = track.artists.joinToString(", ") { it.name }.ifBlank { "Unknown" },
                bpm = feature.tempo.toDouble(),
                durationSec = (track.durationMs / 1000L).toInt(),
            )
        }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
