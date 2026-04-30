package dk.dittmann.spotifypacer.save

sealed interface SavePlaylistResult {

    data class Success(val playlistUrl: String) : SavePlaylistResult

    /**
     * Something failed mid-flow. [playlistUrl] is set when the playlist itself was created before
     * the failure (e.g. a track-add batch failed) so the UI can still link the user to the partial
     * playlist.
     */
    data class Failure(val stage: Stage, val cause: Throwable, val playlistUrl: String? = null) :
        SavePlaylistResult

    enum class Stage {
        FETCH_USER,
        CREATE_PLAYLIST,
        ADD_TRACKS,
    }
}
