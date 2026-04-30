package dk.dittmann.spotifypacer.spotify

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class SpotifyApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SpotifyApi
    private val tokens =
        object : SpotifyTokenProvider {
            override fun currentAccessToken(): String? = "access-token"

            override fun refreshAccessToken(): String = "access-token"
        }

    @Before
    fun setup() {
        server = MockWebServer().also { it.start() }
        api = SpotifyApiFactory.create(tokens = tokens, baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun me_parses_user_profile_and_sends_bearer() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":"user-1","display_name":"Seb"}""")
        )

        val profile = api.me()

        assertEquals("user-1", profile.id)
        assertEquals("Seb", profile.displayName)
        val recorded = server.takeRequest()
        assertEquals("/v1/me", recorded.path)
        assertEquals("Bearer access-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun savedTracks_sends_pagination_and_parses_page() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "items": [
                        {
                          "added_at": "2024-01-01T00:00:00Z",
                          "track": {
                            "id": "t1",
                            "uri": "spotify:track:t1",
                            "name": "Run",
                            "duration_ms": 240000,
                            "artists": [{"id":"a1","name":"Artist"}],
                            "album": {"id":"al1","name":"Album"}
                          }
                        }
                      ],
                      "total": 100,
                      "limit": 50,
                      "offset": 0,
                      "next": "https://api.spotify.com/v1/me/tracks?offset=50&limit=50"
                    }
                    """
                        .trimIndent()
                )
        )

        val page = api.savedTracks(limit = 50, offset = 0)

        assertEquals(100, page.total)
        assertEquals(1, page.items.size)
        assertEquals("spotify:track:t1", page.items[0].track.uri)
        assertEquals("Artist", page.items[0].track.artists[0].name)
        val recorded = server.takeRequest()
        assertEquals("/v1/me/tracks?limit=50&offset=0", recorded.path)
    }

    @Test
    fun audioFeatures_sends_comma_separated_ids() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "audio_features": [
                        {"id":"t1","tempo":172.5,"energy":0.8,"duration_ms":240000},
                        null
                      ]
                    }
                    """
                        .trimIndent()
                )
        )

        val response = api.audioFeatures(ids = "t1,t2")

        assertEquals(2, response.audioFeatures.size)
        assertEquals(172.5f, response.audioFeatures[0]!!.tempo, 0.001f)
        assertNull(response.audioFeatures[1])
        val recorded = server.takeRequest()
        assertEquals("/v1/audio-features?ids=t1%2Ct2", recorded.path)
    }

    @Test
    fun createPlaylist_posts_body_and_parses_response() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody("""{"id":"p1","uri":"spotify:playlist:p1","name":"5km pacer"}""")
        )

        val playlist =
            api.createPlaylist(
                userId = "user-1",
                body = CreatePlaylistBody(name = "5km pacer", description = "desc"),
            )

        assertEquals("p1", playlist.id)
        assertEquals("spotify:playlist:p1", playlist.uri)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/users/user-1/playlists", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"name\":\"5km pacer\""))
        assertTrue(body.contains("\"description\":\"desc\""))
    }

    @Test
    fun addTracks_posts_uris_and_parses_snapshot() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody("""{"snapshot_id":"snap-1"}""")
        )

        val response =
            api.addTracks(
                playlistId = "p1",
                body = AddTracksBody(uris = listOf("spotify:track:t1", "spotify:track:t2")),
            )

        assertEquals("snap-1", response.snapshotId)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/playlists/p1/tracks", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("spotify:track:t1"))
        assertTrue(body.contains("spotify:track:t2"))
    }

    @Test
    fun rate_limit_429_is_retried_after_sleeping() = runTest {
        val sleeps = mutableListOf<Long>()
        val testApi =
            buildApi(tokens = tokens, rateLimit = RateLimitInterceptor(sleeper = { sleeps += it }))

        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2"))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":"u","display_name":null}""")
        )

        val profile = testApi.me()

        assertEquals("u", profile.id)
        assertEquals(listOf(2_000L), sleeps)
    }

    @Test
    fun rate_limit_429_without_retry_after_is_not_retried() = runTest {
        val sleeps = mutableListOf<Long>()
        val testApi =
            buildApi(tokens = tokens, rateLimit = RateLimitInterceptor(sleeper = { sleeps += it }))

        server.enqueue(MockResponse().setResponseCode(429))

        try {
            testApi.me()
            org.junit.Assert.fail("Expected HttpException for unretried 429")
        } catch (expected: retrofit2.HttpException) {
            assertEquals(429, expected.code())
        }
        assertTrue(sleeps.isEmpty())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun unauthorized_401_triggers_one_refresh_and_retry() = runTest {
        var current: String? = "stale-token"
        val refreshes = mutableListOf<String>()
        val provider =
            object : SpotifyTokenProvider {
                override fun currentAccessToken(): String? = current

                override fun refreshAccessToken(): String {
                    val fresh = "fresh-token"
                    current = fresh
                    refreshes += fresh
                    return fresh
                }
            }
        val refreshingApi =
            SpotifyApiFactory.create(tokens = provider, baseUrl = server.url("/").toString())

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":"u","display_name":null}""")
        )

        val profile = refreshingApi.me()

        assertEquals("u", profile.id)
        assertEquals(listOf("fresh-token"), refreshes)
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertEquals("Bearer stale-token", first.getHeader("Authorization"))
        assertEquals("Bearer fresh-token", second.getHeader("Authorization"))
    }

    private fun buildApi(
        tokens: SpotifyTokenProvider,
        rateLimit: RateLimitInterceptor = RateLimitInterceptor(),
    ): SpotifyApi {
        val client =
            OkHttpClient.Builder()
                .addInterceptor(SpotifyAuthInterceptor(tokens))
                .addInterceptor(rateLimit)
                .build()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotifyApi::class.java)
    }
}
