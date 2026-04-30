package dk.dittmann.spotifypacer.ui.nav

import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunConfigArgTest {

    @Test
    fun roundtrip_constant() {
        val original =
            RunConfig(distanceKm = 5.0, targetTimeSec = 1800, strategy = PaceStrategy.Constant)
        val decoded = RunConfigArg.decode(RunConfigArg.encode(original)).toRunConfig()
        assertEquals(original.distanceKm, decoded.distanceKm, 0.0)
        assertEquals(original.targetTimeSec, decoded.targetTimeSec)
        assertTrue(decoded.strategy is PaceStrategy.Constant)
    }

    @Test
    fun roundtrip_linear_ramp() {
        val original =
            RunConfig(distanceKm = 7.5, targetTimeSec = 2700, strategy = PaceStrategy.LinearRamp)
        val decoded = RunConfigArg.decode(RunConfigArg.encode(original)).toRunConfig()
        assertEquals(original.distanceKm, decoded.distanceKm, 0.0)
        assertTrue(decoded.strategy is PaceStrategy.LinearRamp)
    }

    @Test
    fun roundtrip_delayed_exponential_preserves_k() {
        val original =
            RunConfig(
                distanceKm = 10.0,
                targetTimeSec = 3600,
                strategy = PaceStrategy.DelayedExponential(k = 2.5),
            )
        val decoded = RunConfigArg.decode(RunConfigArg.encode(original)).toRunConfig()
        val strategy = decoded.strategy as PaceStrategy.DelayedExponential
        assertEquals(2.5, strategy.k, 0.0)
    }
}
