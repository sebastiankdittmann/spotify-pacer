package dk.dittmann.spotifypacer.save

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dk.dittmann.spotifypacer.pacing.CandidateTrack
import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.pacing.SelectedTrack
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.spotify.RateLimitInterceptor
import dk.dittmann.spotifypacer.spotify.SpotifyApi
import dk.dittmann.spotifypacer.spotify.SpotifyAuthInterceptor
import dk.dittmann.spotifypacer.spotify.SpotifyClient
import dk.dittmann.spotifypacer.spotify.SpotifyTokenProvider
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class SavePlaylistUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var useCase: SavePlaylistUseCase
    private val sleeps = mutableListOf<Long>()

    private val tokens =
        object : SpotifyTokenProvider {
            override fun currentAccessToken(): String? = "t"

            override fun refreshAccessToken(): String = "t"
        }

    @Before
    fun setup() {
        server = MockWebServer().also { it.start() }
        // Build the API directly (instead of via SpotifyApiFactory) so we can swap in a no-op
        // sleeper for the rate-limit interceptor and keep tests fast.
        val okHttp =
            OkHttpClient.Builder()
                .addInterceptor(SpotifyAuthInterceptor(tokens))
                .addInterceptor(RateLimitInterceptor(sleeper = { ms -> sleeps += ms }))
                .build()
        val json = Json { ignoreUnknownKeys = true }
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/").toString())
                .client(okHttp)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(SpotifyApi::class.java)
        useCase = SavePlaylistUseCase(SpotifyClient(api))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun selection(count: Int): Selection {
        val tracks =
            (1..count).map {
                SelectedTrack(
                    track =
                        CandidateTrack(
                            id = "t$it",
                            title = "T$it",
                            artist = "A",
                            bpm = 170.0,
                            durationSec = 200,
                        ),
                    startSec = (it - 1) * 200,
                )
            }
        return Selection(tracks = tracks, totalSec = count * 200)
    }

    private val config =
        RunConfig(distanceKm = 5.0, targetTimeSec = 28 * 60, strategy = PaceStrategy.LinearRamp)

    private fun jsonResponse(code: Int, body: String) =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setResponseCode(code)
            .setBody(body)

    @Test
    fun happy_path_creates_playlist_then_adds_tracks_in_batches() = runTest {
        server.enqueue(jsonResponse(200, """{"id":"u1"}"""))
        server.enqueue(jsonResponse(201, """{"id":"p1","uri":"spotify:playlist:p1","name":"n"}"""))
        // 150 tracks → 2 batches (100 + 50).
        server.enqueue(jsonResponse(201, """{"snapshot_id":"s1"}"""))
        server.enqueue(jsonResponse(201, """{"snapshot_id":"s2"}"""))

        val result = useCase(config, selection(150))

        assertEquals(SavePlaylistResult.Success("https://open.spotify.com/playlist/p1"), result)

        assertEquals("/v1/me", server.takeRequest().path)

        val createReq = server.takeRequest()
        assertEquals("/v1/users/u1/playlists", createReq.path)
        val createBody = Json.parseToJsonElement(createReq.body.readUtf8()).jsonObject
        assertEquals("Pacer · 5km · 28:00 · linear", createBody["name"]!!.jsonPrimitive.content)
        assertTrue(createBody["description"]!!.jsonPrimitive.content.contains("linear pace, total"))

        val firstAdd = server.takeRequest()
        assertEquals("/v1/playlists/p1/tracks", firstAdd.path)
        assertEquals(100, "spotify:track:".toRegex().findAll(firstAdd.body.readUtf8()).count())

        val secondAdd = server.takeRequest()
        assertEquals(50, "spotify:track:".toRegex().findAll(secondAdd.body.readUtf8()).count())
    }

    @Test
    fun batch_failure_returns_failure_with_partial_playlist_url() = runTest {
        server.enqueue(jsonResponse(200, """{"id":"u1"}"""))
        server.enqueue(jsonResponse(201, """{"id":"p1","uri":"spotify:playlist:p1","name":"n"}"""))
        server.enqueue(jsonResponse(201, """{"snapshot_id":"s1"}"""))
        server.enqueue(jsonResponse(500, """{"error":{"status":500,"message":"boom"}}"""))

        val result = useCase(config, selection(150))

        result as SavePlaylistResult.Failure
        assertEquals(SavePlaylistResult.Stage.ADD_TRACKS, result.stage)
        assertEquals("https://open.spotify.com/playlist/p1", result.playlistUrl)
    }

    @Test
    fun create_playlist_failure_has_no_playlist_url() = runTest {
        server.enqueue(jsonResponse(200, """{"id":"u1"}"""))
        server.enqueue(jsonResponse(403, """{"error":{"status":403,"message":"forbidden"}}"""))

        val result = useCase(config, selection(3))

        result as SavePlaylistResult.Failure
        assertEquals(SavePlaylistResult.Stage.CREATE_PLAYLIST, result.stage)
        assertEquals(null, result.playlistUrl)
    }

    @Test
    fun fetch_user_failure_short_circuits_before_create() = runTest {
        server.enqueue(jsonResponse(500, """{"error":{"status":500,"message":"boom"}}"""))

        val result = useCase(config, selection(3))

        result as SavePlaylistResult.Failure
        assertEquals(SavePlaylistResult.Stage.FETCH_USER, result.stage)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun rate_limited_request_is_retried_after_retry_after_header() = runTest {
        server.enqueue(jsonResponse(200, """{"id":"u1"}"""))
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "1")
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"status":429,"message":"slow down"}}""")
        )
        server.enqueue(jsonResponse(201, """{"id":"p1","uri":"spotify:playlist:p1","name":"n"}"""))
        server.enqueue(jsonResponse(201, """{"snapshot_id":"s1"}"""))

        val result = useCase(config, selection(3))

        assertEquals(SavePlaylistResult.Success("https://open.spotify.com/playlist/p1"), result)
        assertEquals(listOf(1000L), sleeps)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun empty_selection_skips_add_tracks_call() = runTest {
        server.enqueue(jsonResponse(200, """{"id":"u1"}"""))
        server.enqueue(jsonResponse(201, """{"id":"p1","uri":"spotify:playlist:p1","name":"n"}"""))

        val result = useCase(config, Selection(emptyList(), totalSec = 0))

        assertEquals(SavePlaylistResult.Success("https://open.spotify.com/playlist/p1"), result)
        assertEquals(2, server.requestCount)
    }
}
