package dk.dittmann.spotifypacer.save

import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistNamingTest {

    @Test
    fun name_uses_dot_separator_and_human_strategy_label() {
        val name =
            PlaylistNaming.name(
                RunConfig(
                    distanceKm = 5.0,
                    targetTimeSec = 28 * 60,
                    strategy = PaceStrategy.DelayedExponential(),
                )
            )
        assertEquals("Pacer · 5km · 28:00 · delayed-exp", name)
    }

    @Test
    fun name_renders_decimal_distance_with_two_places() {
        val name =
            PlaylistNaming.name(
                RunConfig(
                    distanceKm = 5.5,
                    targetTimeSec = 30 * 60 + 5,
                    strategy = PaceStrategy.LinearRamp,
                )
            )
        assertEquals("Pacer · 5.50km · 30:05 · linear", name)
    }

    @Test
    fun name_handles_constant_strategy_label() {
        val name =
            PlaylistNaming.name(
                RunConfig(distanceKm = 10.0, targetTimeSec = 3600, strategy = PaceStrategy.Constant)
            )
        assertEquals("Pacer · 10km · 60:00 · constant", name)
    }

    @Test
    fun description_includes_strategy_and_total_duration() {
        val description = PlaylistNaming.description(PaceStrategy.LinearRamp, totalSec = 1685)
        assertEquals("linear pace, total 28:05", description)
    }

    @Test
    fun trackUri_prefixes_with_spotify_track() {
        assertEquals("spotify:track:abc", PlaylistNaming.trackUri("abc"))
    }

    @Test
    fun playlistUrl_uses_open_spotify_host() {
        assertEquals("https://open.spotify.com/playlist/xyz", PlaylistNaming.playlistUrl("xyz"))
    }
}
