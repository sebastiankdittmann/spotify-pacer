package dk.dittmann.spotifypacer.spotify

/**
 * High-level facade over [SpotifyApi] that handles batching constraints (Spotify enforces a 100-uri
 * limit on `playlists/{id}/tracks`).
 */
class SpotifyClient(private val api: SpotifyApi) {

    suspend fun me(): UserProfile = api.me()

    suspend fun savedTracks(limit: Int = 50, offset: Int = 0): SavedTracksPage =
        api.savedTracks(limit = limit, offset = offset)

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
        const val MAX_ADD_TRACKS_BATCH = 100
    }
}
