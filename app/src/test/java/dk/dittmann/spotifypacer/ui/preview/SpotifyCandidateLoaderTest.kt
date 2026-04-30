package dk.dittmann.spotifypacer.ui.preview

import dk.dittmann.spotifypacer.bpm.BpmRepository
import dk.dittmann.spotifypacer.bpm.TrackKey
import dk.dittmann.spotifypacer.spotify.AddTracksBody
import dk.dittmann.spotifypacer.spotify.AddTracksResponse
import dk.dittmann.spotifypacer.spotify.Artist
import dk.dittmann.spotifypacer.spotify.CreatePlaylistBody
import dk.dittmann.spotifypacer.spotify.Playlist
import dk.dittmann.spotifypacer.spotify.SavedTrackItem
import dk.dittmann.spotifypacer.spotify.SavedTracksPage
import dk.dittmann.spotifypacer.spotify.SpotifyApi
import dk.dittmann.spotifypacer.spotify.SpotifyClient
import dk.dittmann.spotifypacer.spotify.Track
import dk.dittmann.spotifypacer.spotify.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCandidateLoaderTest {

    private fun fakeApi(totalTracks: Int, pageSize: Int = 50) =
        object : SpotifyApi {
            var pagesServed = 0

            override suspend fun me(): UserProfile = UserProfile("u")

            override suspend fun savedTracks(limit: Int, offset: Int): SavedTracksPage {
                pagesServed++
                val items =
                    (offset until (offset + pageSize).coerceAtMost(totalTracks)).map { i ->
                        SavedTrackItem(
                            track =
                                Track(
                                    id = "t$i",
                                    uri = "spotify:track:t$i",
                                    name = "Title $i",
                                    durationMs = 200_000,
                                    artists = listOf(Artist(name = "Artist")),
                                )
                        )
                    }
                val nextOffset = offset + items.size
                return SavedTracksPage(
                    items = items,
                    total = totalTracks,
                    limit = limit,
                    offset = offset,
                    next = if (nextOffset < totalTracks) "next" else null,
                )
            }

            override suspend fun createPlaylist(
                userId: String,
                body: CreatePlaylistBody,
            ): Playlist = error("not used")

            override suspend fun addTracks(
                playlistId: String,
                body: AddTracksBody,
            ): AddTracksResponse = error("not used")
        }

    private fun bpm(returnFor: (TrackKey) -> Double?): BpmRepository = BpmRepository { tracks ->
        tracks.mapNotNull { key -> returnFor(key)?.let { key.spotifyId to it } }.toMap()
    }

    @Test
    fun stops_paging_once_min_pool_reached() = runTest {
        val api = fakeApi(totalTracks = 1000)
        val loader =
            SpotifyCandidateLoader(
                client = SpotifyClient(api),
                bpmRepository = bpm { 150.0 },
                pageSize = 50,
                minPool = 60,
                maxLookupBudget = 1000,
            )

        val pool = loader.load()

        // Two pages of 50 = 100 candidates; pool >= 60 reached, so we stop after the second page.
        assertEquals(100, pool.size)
        assertEquals(2, api.pagesServed)
    }

    @Test
    fun stops_when_lookup_budget_is_exhausted() = runTest {
        val api = fakeApi(totalTracks = 1000)
        val loader =
            SpotifyCandidateLoader(
                client = SpotifyClient(api),
                bpmRepository = bpm { null }, // no matches → pool stays empty
                pageSize = 50,
                minPool = 1000,
                maxLookupBudget = 75,
            )

        val pool = loader.load()

        assertTrue(pool.isEmpty())
        // 50 lookups on page 1, 25 remaining on page 2; budget exhausted → stop.
        assertEquals(2, api.pagesServed)
    }

    @Test
    fun stops_when_pagination_exhausted() = runTest {
        val api = fakeApi(totalTracks = 30)
        val loader =
            SpotifyCandidateLoader(
                client = SpotifyClient(api),
                bpmRepository = bpm { 150.0 },
                pageSize = 50,
                minPool = 1000,
                maxLookupBudget = 1000,
            )

        val pool = loader.load()
        assertEquals(30, pool.size)
        assertEquals(1, api.pagesServed)
    }

    @Test
    fun maps_track_metadata_into_candidate() = runTest {
        val api = fakeApi(totalTracks = 1)
        val loader =
            SpotifyCandidateLoader(
                client = SpotifyClient(api),
                bpmRepository = bpm { 165.0 },
                pageSize = 50,
                minPool = 1,
                maxLookupBudget = 10,
            )
        val pool = loader.load()
        val candidate = pool.single()
        assertEquals("t0", candidate.id)
        assertEquals("Title 0", candidate.title)
        assertEquals("Artist", candidate.artist)
        assertEquals(165.0, candidate.bpm, 0.0001)
        assertEquals(200, candidate.durationSec)
    }
}
