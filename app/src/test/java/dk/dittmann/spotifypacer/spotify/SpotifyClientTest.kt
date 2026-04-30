package dk.dittmann.spotifypacer.spotify

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SpotifyClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: SpotifyClient

    private val tokens =
        object : SpotifyTokenProvider {
            override fun currentAccessToken(): String? = "t"

            override fun refreshAccessToken(): String = "t"
        }

    @Before
    fun setup() {
        server = MockWebServer().also { it.start() }
        val api = SpotifyApiFactory.create(tokens = tokens, baseUrl = server.url("/").toString())
        client = SpotifyClient(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun addTracks_chunks_uris_into_batches_of_100_and_returns_snapshot_per_batch() = runTest {
        val uris = (1..150).map { "spotify:track:t$it" }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody("""{"snapshot_id":"s1"}""")
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody("""{"snapshot_id":"s2"}""")
        )

        val snapshots = client.addTracks(playlistId = "p1", uris = uris)

        assertEquals(listOf("s1", "s2"), snapshots)
        val firstBody = server.takeRequest().body.readUtf8()
        val secondBody = server.takeRequest().body.readUtf8()
        // Crude size check: each batch should serialize 100 / 50 uris respectively.
        assertEquals(100, "spotify:track:".toRegex().findAll(firstBody).count())
        assertEquals(50, "spotify:track:".toRegex().findAll(secondBody).count())
    }
}
