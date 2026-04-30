package dk.dittmann.spotifypacer.save

import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.spotify.SpotifyClient
import dk.dittmann.spotifypacer.ui.setup.RunConfig

/**
 * Saves an approved [Selection] as a new private Spotify playlist for the signed-in user.
 *
 * The flow is sequential — fetch user → create playlist → add tracks — and each stage maps to a
 * [SavePlaylistResult.Stage] so callers can show targeted error messages. Track-add failures keep
 * the partial playlist URL because the playlist is already visible in the user's library at that
 * point.
 */
class SavePlaylistUseCase(private val client: SpotifyClient) {

    suspend operator fun invoke(config: RunConfig, selection: Selection): SavePlaylistResult {
        val userId =
            runCatching { client.me().id }
                .getOrElse { e ->
                    return SavePlaylistResult.Failure(SavePlaylistResult.Stage.FETCH_USER, e)
                }

        val playlist =
            runCatching {
                    client.createPlaylist(
                        userId = userId,
                        name = PlaylistNaming.name(config),
                        description =
                            PlaylistNaming.description(config.strategy, selection.totalSec),
                        public = false,
                    )
                }
                .getOrElse { e ->
                    return SavePlaylistResult.Failure(SavePlaylistResult.Stage.CREATE_PLAYLIST, e)
                }

        val playlistUrl = PlaylistNaming.playlistUrl(playlist.id)
        val uris = selection.tracks.map { PlaylistNaming.trackUri(it.track.id) }

        return runCatching { client.addTracks(playlist.id, uris) }
            .fold(
                onSuccess = { SavePlaylistResult.Success(playlistUrl) },
                onFailure = { e ->
                    SavePlaylistResult.Failure(
                        stage = SavePlaylistResult.Stage.ADD_TRACKS,
                        cause = e,
                        playlistUrl = playlistUrl,
                    )
                },
            )
    }
}
