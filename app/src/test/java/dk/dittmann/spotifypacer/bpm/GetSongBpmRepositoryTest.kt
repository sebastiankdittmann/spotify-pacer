package dk.dittmann.spotifypacer.bpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import retrofit2.Retrofit

@RunWith(AndroidJUnit4::class)
class GetSongBpmRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var api: GetSongBpmApi
    private lateinit var cache: BpmCache
    private val sleeps = mutableListOf<Long>()

    @Before
    fun setup() {
        server = MockWebServer().also { it.start() }
        val json = Json { ignoreUnknownKeys = true }
        api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GetSongBpmApi::class.java)
        cache = BpmCache(File(tmp.newFolder(), "bpm-cache.json"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun repository(apiKey: String = "key") =
        GetSongBpmRepository(
            api = api,
            cache = cache,
            apiKey = apiKey,
            sleep = { sleeps += it },
            requestIntervalMs = 17L,
        )

    private fun trackKey(id: String, durationMs: Long = 200_000) =
        TrackKey(
            spotifyId = id,
            title = "Title $id",
            primaryArtist = "Artist",
            durationMs = durationMs,
        )

    private fun hitBody(tempo: String, length: String?): String {
        val len = length?.let { """"song_length":"$it",""" }.orEmpty()
        return """{"search":[{"song_id":"x","song_title":"t","artist":{"name":"a"},$len"tempo":"$tempo"}]}"""
    }

    @Test
    fun happy_path_returns_tempo_and_caches_it() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(hitBody(tempo = "172.5", length = "200"))
        )

        val out = repository().bpmFor(listOf(trackKey("t1")))

        assertEquals(mapOf("t1" to 172.5), out)
        assertEquals(BpmCache.Lookup.Hit(172.5), cache.get("t1"))

        sleeps.clear()
        val again = repository().bpmFor(listOf(trackKey("t1")))
        assertEquals(mapOf("t1" to 172.5), again)
        assertEquals(1, server.requestCount)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun no_results_sentinel_caches_negative() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"search":"no results"}""")
        )

        val out = repository().bpmFor(listOf(trackKey("nope")))
        assertTrue(out.isEmpty())
        assertEquals(BpmCache.Lookup.NegativeHit, cache.get("nope"))
    }

    @Test
    fun unauthorized_throws_unconfigured() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        try {
            repository().bpmFor(listOf(trackKey("t1")))
            fail("expected Unconfigured")
        } catch (e: BpmProviderException.Unconfigured) {
            // expected
        }
    }

    @Test
    fun rate_limited_throws_rate_limited() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        try {
            repository().bpmFor(listOf(trackKey("t1")))
            fail("expected RateLimited")
        } catch (e: BpmProviderException.RateLimited) {
            // expected
        }
    }

    @Test
    fun server_error_throws_unavailable() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            repository().bpmFor(listOf(trackKey("t1")))
            fail("expected Unavailable")
        } catch (e: BpmProviderException.Unavailable) {
            // expected
        }
    }

    @Test
    fun blank_api_key_throws_unconfigured_without_calling_network() = runTest {
        try {
            repository(apiKey = " ").bpmFor(listOf(trackKey("t1")))
            fail("expected Unconfigured")
        } catch (e: BpmProviderException.Unconfigured) {
            // expected
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun malformed_tempo_caches_negative() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(hitBody(tempo = "not-a-number", length = "200"))
        )

        val out = repository().bpmFor(listOf(trackKey("t1")))
        assertTrue(out.isEmpty())
        assertEquals(BpmCache.Lookup.NegativeHit, cache.get("t1"))
    }

    @Test
    fun duration_mismatch_falls_back_to_first_hit() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(hitBody(tempo = "120", length = "60"))
        )
        val out = repository().bpmFor(listOf(trackKey("t1", durationMs = 240_000)))
        assertEquals(mapOf("t1" to 120.0), out)
    }

    @Test
    fun sequential_lookups_sleep_between_network_calls() = runTest {
        repeat(3) {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(hitBody(tempo = "150", length = "200"))
            )
        }

        repository().bpmFor(listOf(trackKey("t1"), trackKey("t2"), trackKey("t3")))

        assertEquals(listOf(17L, 17L), sleeps)
    }

    @Test
    fun empty_track_list_returns_empty_without_network() = runTest {
        val out = repository().bpmFor(emptyList())
        assertTrue(out.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun negative_cached_track_skips_network() = runTest {
        cache.put("t1", null)
        val out = repository().bpmFor(listOf(trackKey("t1")))
        assertTrue(out.isEmpty())
        assertEquals(0, server.requestCount)
        assertNull(out["t1"])
    }
}
