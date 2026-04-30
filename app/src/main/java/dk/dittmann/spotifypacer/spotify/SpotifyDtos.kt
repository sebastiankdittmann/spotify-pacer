package dk.dittmann.spotifypacer.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class SavedTracksPage(
    @SerialName("items") val items: List<SavedTrackItem>,
    @SerialName("total") val total: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("offset") val offset: Int,
    @SerialName("next") val next: String? = null,
)

@Serializable
data class SavedTrackItem(
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("track") val track: Track,
)

@Serializable
data class Track(
    @SerialName("id") val id: String,
    @SerialName("uri") val uri: String,
    @SerialName("name") val name: String,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("artists") val artists: List<Artist> = emptyList(),
    @SerialName("album") val album: Album? = null,
)

@Serializable
data class Artist(@SerialName("id") val id: String? = null, @SerialName("name") val name: String)

@Serializable
data class Album(@SerialName("id") val id: String? = null, @SerialName("name") val name: String)

@Serializable
data class AudioFeaturesResponse(
    @SerialName("audio_features") val audioFeatures: List<AudioFeatures?>
)

@Serializable
data class AudioFeatures(
    @SerialName("id") val id: String,
    @SerialName("uri") val uri: String? = null,
    @SerialName("tempo") val tempo: Float,
    @SerialName("energy") val energy: Float? = null,
    @SerialName("danceability") val danceability: Float? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
)

@Serializable
data class CreatePlaylistBody(
    @SerialName("name") val name: String,
    @SerialName("public") val public: Boolean = false,
    @SerialName("description") val description: String? = null,
)

@Serializable
data class Playlist(
    @SerialName("id") val id: String,
    @SerialName("uri") val uri: String,
    @SerialName("name") val name: String,
)

@Serializable data class AddTracksBody(@SerialName("uris") val uris: List<String>)

@Serializable data class AddTracksResponse(@SerialName("snapshot_id") val snapshotId: String)
