package dk.dittmann.spotifypacer.spotify

/**
 * High-level facade over [SpotifyApi] that handles batching constraints (Spotify enforces 100-item
 * limits on `audio-features` ids and `playlists/{id}/tracks` uris).
 */
class SpotifyClient(private val api: SpotifyApi) {

    suspend fun me(): UserProfile = api.me()

    suspend fun savedTracks(limit: Int = 50, offset: Int = 0): SavedTracksPage =
        api.savedTracks(limit = limit, offset = offset)

    /** Fetches audio features for any number of track ids in 100-id chunks. */
    suspend fun audioFeatures(ids: List<String>): List<AudioFeatures?> {
        if (ids.isEmpty()) return emptyList()
        val results = mutableListOf<AudioFeatures?>()
        ids.chunked(MAX_AUDIO_FEATURES_BATCH).forEach { chunk ->
            results += api.audioFeatures(chunk.joinToString(",")).audioFeatures
        }
        return results
    }

    suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String? = null,
        public: Boolean = false,
    ): Playlist =
        api.createPlaylist(
            userId = userId,
            body = CreatePlaylistBody(name = name, public = public, description = description),
        )

    /** Adds any number of track URIs to a playlist in 100-uri chunks. Returns each snapshot id. */
    suspend fun addTracks(playlistId: String, uris: List<String>): List<String> {
        if (uris.isEmpty()) return emptyList()
        val snapshots = mutableListOf<String>()
        uris.chunked(MAX_ADD_TRACKS_BATCH).forEach { chunk ->
            snapshots += api.addTracks(playlistId, AddTracksBody(uris = chunk)).snapshotId
        }
        return snapshots
    }

    private companion object {
        const val MAX_AUDIO_FEATURES_BATCH = 100
        const val MAX_ADD_TRACKS_BATCH = 100
    }
}
