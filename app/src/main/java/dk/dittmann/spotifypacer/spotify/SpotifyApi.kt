package dk.dittmann.spotifypacer.spotify

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyApi {

    @GET("v1/me") suspend fun me(): UserProfile

    @GET("v1/me/tracks")
    suspend fun savedTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SavedTracksPage

    @POST("v1/users/{user_id}/playlists")
    suspend fun createPlaylist(
        @Path("user_id") userId: String,
        @Body body: CreatePlaylistBody,
    ): Playlist

    @POST("v1/playlists/{playlist_id}/tracks")
    suspend fun addTracks(
        @Path("playlist_id") playlistId: String,
        @Body body: AddTracksBody,
    ): AddTracksResponse
}
