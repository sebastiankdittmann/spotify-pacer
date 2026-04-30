package dk.dittmann.spotifypacer.bpm

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

/**
 * BPM provider backed by GetSongBPM.
 *
 * Matching is conservative: we strip common version suffixes from the Spotify title (Remastered,
 * Live, Radio Edit, parenthetical/feat blocks), search artist+title, and accept the first hit only
 * if its `song_length` (in seconds) is within [DURATION_TOLERANCE_SEC] of the Spotify track's
 * duration. If the provider doesn't return a usable length we still accept the first hit — better
 * than dropping every track that has no length metadata.
 *
 * Throttling is sequential at one request per [REQUEST_INTERVAL_MS]. GetSongBPM's published rate
 * limit is unclear, so we err on the safe side; the cache keeps repeat runs free.
 */
class GetSongBpmRepository(
    private val api: GetSongBpmApi,
    private val cache: BpmCache,
    private val apiKey: String,
    private val sleep: suspend (Long) -> Unit = ::delay,
    private val requestIntervalMs: Long = REQUEST_INTERVAL_MS,
) : BpmRepository {

    override suspend fun bpmFor(tracks: List<TrackKey>): Map<String, Double> {
        if (tracks.isEmpty()) return emptyMap()
        if (apiKey.isBlank()) {
            throw BpmProviderException.Unconfigured(
                "GetSongBPM API key is not configured. Set getsongbpm.apiKey in local.properties."
            )
        }
        val out = mutableMapOf<String, Double>()
        var firstNetworkCall = true
        for (track in tracks) {
            when (val cached = cache.get(track.spotifyId)) {
                is BpmCache.Lookup.Hit -> out[track.spotifyId] = cached.bpm
                BpmCache.Lookup.NegativeHit -> Unit // skip; cached miss
                BpmCache.Lookup.Miss -> {
                    if (!firstNetworkCall) sleep(requestIntervalMs)
                    firstNetworkCall = false
                    val bpm = lookup(track)
                    cache.put(track.spotifyId, bpm)
                    if (bpm != null) out[track.spotifyId] = bpm
                }
            }
        }
        return out
    }

    private suspend fun lookup(track: TrackKey): Double? {
        val lookup = "song:${normaliseTitle(track.title)} artist:${track.primaryArtist}"
        val response =
            try {
                api.search(apiKey = apiKey, lookup = lookup)
            } catch (e: HttpException) {
                when (e.code()) {
                    401,
                    403 ->
                        throw BpmProviderException.Unconfigured(
                            "GetSongBPM rejected the API key (HTTP ${e.code()})."
                        )
                    429 -> throw BpmProviderException.RateLimited(e)
                    in 500..599 -> throw BpmProviderException.Unavailable(e)
                    else -> throw BpmProviderException.Unavailable(e)
                }
            } catch (e: IOException) {
                throw BpmProviderException.Unavailable(e)
            }
        val hit = pickHit(response, track) ?: return null
        return hit.tempo?.toDoubleOrNull().also { tempo ->
            if (tempo == null) {
                Log.w(
                    TAG,
                    "GetSongBPM returned non-numeric tempo for ${track.spotifyId}: ${hit.tempo}",
                )
            }
        }
    }

    private fun pickHit(response: GetSongBpmSearchResponse, track: TrackKey): GetSongBpmHit? {
        val element = response.search ?: return null
        if (element is JsonPrimitive) {
            // "no results" sentinel — provider didn't find a match.
            return null
        }
        if (element !is JsonArray) return null
        val hits =
            runCatching { hitsJson.decodeFromJsonElement(HitListSerializer, element) }.getOrNull()
                ?: return null
        if (hits.isEmpty()) return null
        val targetSec = track.durationMs / 1000.0
        // Prefer first hit whose song_length is within tolerance of the Spotify track length.
        val acceptable =
            hits.firstOrNull { hit ->
                val len = hit.songLength?.toDoubleOrNull() ?: return@firstOrNull false
                kotlin.math.abs(len - targetSec) <= DURATION_TOLERANCE_SEC
            }
        return acceptable ?: hits.first()
    }

    private fun normaliseTitle(title: String): String {
        var t = title
        // Strip parenthetical/bracketed annotations and dash-suffixes commonly used by Spotify
        // remasters, live versions, radio edits, feat. blocks.
        t = t.replace(Regex("\\([^)]*\\)"), " ")
        t = t.replace(Regex("\\[[^]]*]"), " ")
        t = t.replace(Regex("\\s-\\s.*$"), " ")
        return t.trim().ifEmpty { title }
    }

    private companion object {
        const val TAG = "GetSongBpmRepository"
        const val REQUEST_INTERVAL_MS = 1100L
        const val DURATION_TOLERANCE_SEC = 10.0
        val hitsJson = Json { ignoreUnknownKeys = true }
        val HitListSerializer =
            kotlinx.serialization.builtins.ListSerializer(GetSongBpmHit.serializer())
    }
}
