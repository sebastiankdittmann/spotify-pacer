package dk.dittmann.spotifypacer.pacing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceCurveTest {

    // ── Constant ─────────────────────────────────────────────────────────────

    @Test
    fun `Constant - all samples are the average of start and end BPM`() {
        val curve = generateCurve(PaceStrategy.Constant, 120, 150, 170, 60)
        val expected = (150 + 170) / 2.0
        curve.forEach { assertEquals(expected, it.bpm, 0.0) }
    }

    // ── LinearRamp ────────────────────────────────────────────────────────────

    @Test
    fun `LinearRamp - first sample is startBpm`() {
        val curve = generateCurve(PaceStrategy.LinearRamp, 120, 150, 170, 60)
        assertEquals(150.0, curve.first().bpm, 0.0)
    }

    @Test
    fun `LinearRamp - last sample is endBpm`() {
        val curve = generateCurve(PaceStrategy.LinearRamp, 120, 150, 170, 60)
        assertEquals(170.0, curve.last().bpm, 0.0)
    }

    @Test
    fun `LinearRamp - midpoint is the linear average`() {
        val curve = generateCurve(PaceStrategy.LinearRamp, 120, 150, 170, 60)
        val midSample = curve.first { it.timeSec == 60 }
        assertEquals(160.0, midSample.bpm, 1e-9)
    }

    // ── DelayedExponential ───────────────────────────────────────────────────

    @Test
    fun `DelayedExponential - first sample is startBpm`() {
        val curve = generateCurve(PaceStrategy.DelayedExponential(), 120, 150, 170, 60)
        assertEquals(150.0, curve.first().bpm, 1e-9)
    }

    @Test
    fun `DelayedExponential - last sample is endBpm`() {
        val curve = generateCurve(PaceStrategy.DelayedExponential(), 120, 150, 170, 60)
        assertEquals(170.0, curve.last().bpm, 1e-9)
    }

    @Test
    fun `DelayedExponential - midpoint is below the linear midpoint when bpm ramps up`() {
        val curve = generateCurve(PaceStrategy.DelayedExponential(k = 4.0), 120, 150, 170, 60)
        val expMid = curve.first { it.timeSec == 60 }.bpm
        val linearMid = (150.0 + 170.0) / 2.0
        assertTrue("Delayed exponential mid ($expMid) should be below linear mid ($linearMid)", expMid < linearMid)
    }

    // ── Input validation ─────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `validation - totalSeconds zero throws`() {
        generateCurve(PaceStrategy.Constant, 0, 150, 170)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - totalSeconds negative throws`() {
        generateCurve(PaceStrategy.Constant, -1, 150, 170)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - sampleIntervalSec zero throws`() {
        generateCurve(PaceStrategy.Constant, 120, 150, 170, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - sampleIntervalSec negative throws`() {
        generateCurve(PaceStrategy.Constant, 120, 150, 170, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - startBpm zero throws`() {
        generateCurve(PaceStrategy.Constant, 120, 0, 170)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - startBpm above 300 throws`() {
        generateCurve(PaceStrategy.Constant, 120, 301, 170)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - endBpm zero throws`() {
        generateCurve(PaceStrategy.Constant, 120, 150, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation - endBpm above 300 throws`() {
        generateCurve(PaceStrategy.Constant, 120, 150, 301)
    }
}
