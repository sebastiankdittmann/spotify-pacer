package dk.dittmann.spotifypacer.bpm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Minimal client for the GetSongBPM search endpoint. We only use the song-by-name lookup; the rest
 * of the public API (artist, album, type=both) is irrelevant for our matching strategy.
 */
interface GetSongBpmApi {
    @GET("search/")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("type") type: String = "song",
        @Query("lookup") lookup: String,
    ): GetSongBpmSearchResponse
}

/**
 * GetSongBPM returns either: `{ "search": [ { ... }, ... ] }` when matches exist, or `{ "search":
 * "no results" }` when nothing matches.
 *
 * We deserialize as a raw [JsonElement] and decode in [parseSearchHits] so the no-results sentinel
 * doesn't blow up the converter.
 */
@Serializable
data class GetSongBpmSearchResponse(@SerialName("search") val search: JsonElement? = null)

@Serializable
data class GetSongBpmHit(
    @SerialName("song_id") val songId: String? = null,
    @SerialName("song_title") val songTitle: String? = null,
    @SerialName("artist") val artist: GetSongBpmArtist? = null,
    @SerialName("tempo") val tempo: String? = null,
    @SerialName("song_length") val songLength: String? = null,
)

@Serializable data class GetSongBpmArtist(@SerialName("name") val name: String? = null)
