package dk.dittmann.spotifypacer.ui.nav

import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutesTest {

    @Test
    fun preview_route_round_trips_through_url_encoding() {
        val config =
            RunConfig(
                distanceKm = 5.5,
                targetTimeSec = 1800,
                strategy = PaceStrategy.DelayedExponential(k = 3.0),
            )
        val route = Routes.preview(config)
        assertTrue(route.startsWith("${Routes.Preview}?${Routes.PreviewArgConfig}="))

        val encoded = route.substringAfter("=")
        val decoded = RunConfigArg.decode(android.net.Uri.decode(encoded)).toRunConfig()
        assertEquals(config.distanceKm, decoded.distanceKm, 0.0)
        assertEquals(config.targetTimeSec, decoded.targetTimeSec)
        assertEquals(3.0, (decoded.strategy as PaceStrategy.DelayedExponential).k, 0.0)
    }
}
