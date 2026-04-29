package dk.dittmann.spotifypacer.pacing

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSelectorTest {

    private fun track(id: String, bpm: Double, durationSec: Int = 60) =
        CandidateTrack(id, "Track $id", "Artist", bpm, durationSec)

    private fun constantCurve(totalSec: Int, bpm: Int) =
        generateCurve(PaceStrategy.Constant, totalSec, bpm, bpm)

    // ── Exact BPM match ───────────────────────────────────────────────────────

    @Test
    fun `exact BPM match - all selected tracks match target BPM`() {
        val curve = constantCurve(300, 160)
        val pool = (1..10).map { track("t$it", 160.0) }
        val result = select(curve, pool)
        assertTrue(result.tracks.isNotEmpty())
        result.tracks.forEach { assertEquals(160.0, it.track.bpm, 0.0) }
    }

    // ── Tolerance widening ────────────────────────────────────────────────────

    @Test
    fun `tolerance widening - selects tracks within wider window when no exact match`() {
        val curve = constantCurve(120, 160)
        val pool = (1..5).map { track("t$it", 165.0) }
        val result = select(curve, pool)
        assertTrue(
            "Should select at least one track via tolerance widening",
            result.tracks.isNotEmpty(),
        )
        result.tracks.forEach { assertEquals(165.0, it.track.bpm, 0.0) }
    }

    @Test
    fun `tolerance widening - falls back to nearest BPM when outside all tolerance windows`() {
        val curve = constantCurve(60, 160)
        val pool = listOf(track("far", 250.0))
        val result = select(curve, pool)
        assertEquals(1, result.tracks.size)
        assertEquals("far", result.tracks[0].track.id)
    }

    // ── Half/double tempo substitution ───────────────────────────────────────

    @Test
    fun `half tempo substitution - 80 BPM track substitutes for 160 BPM target`() {
        val curve = constantCurve(60, 160)
        val pool = listOf(track("half", 80.0))
        val result = select(curve, pool)
        assertEquals(1, result.tracks.size)
        assertEquals("half", result.tracks[0].track.id)
    }

    @Test
    fun `double tempo substitution - 180 BPM track substitutes for 90 BPM target`() {
        val curve = constantCurve(60, 90)
        val pool = listOf(track("double", 180.0))
        val result = select(curve, pool)
        assertEquals(1, result.tracks.size)
        assertEquals("double", result.tracks[0].track.id)
    }

    // ── No repetition ─────────────────────────────────────────────────────────

    @Test
    fun `no track is repeated in a selection`() {
        val curve = constantCurve(300, 160)
        val pool = (1..5).map { track("t$it", 160.0) }
        val result = select(curve, pool)
        val ids = result.tracks.map { it.track.id }
        assertEquals(ids.distinct(), ids)
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun `same seed produces identical selection`() {
        val curve = generateCurve(PaceStrategy.LinearRamp, 300, 150, 170)
        val pool = (1..10).map { track("t$it", 160.0) }
        val r1 = select(curve, pool, Random(42))
        val r2 = select(curve, pool, Random(42))
        assertEquals(r1.tracks.map { it.track.id }, r2.tracks.map { it.track.id })
    }

    @Test
    fun `different seeds can produce different selections`() {
        val curve = constantCurve(300, 160)
        val pool = (1..10).map { track("t$it", 160.0) }
        val results =
            (0..20).map { seed -> select(curve, pool, Random(seed)).tracks.map { it.track.id } }
        assertTrue(
            "Different seeds should produce at least two distinct orderings",
            results.distinct().size > 1,
        )
    }

    // ── Small-pool degradation ────────────────────────────────────────────────

    @Test
    fun `small pool - returns partial selection without throwing`() {
        val curve = constantCurve(300, 160)
        val pool = listOf(track("t1", 160.0), track("t2", 160.0))
        val result = select(curve, pool)
        assertEquals(2, result.tracks.size)
        assertEquals(120, result.totalSec)
    }

    @Test
    fun `empty candidate pool - returns empty selection without throwing`() {
        val curve = constantCurve(300, 160)
        val result = select(curve, emptyList())
        assertTrue(result.tracks.isEmpty())
        assertEquals(0, result.totalSec)
    }

    @Test
    fun `empty curve - returns empty selection without throwing`() {
        val pool = listOf(track("t1", 160.0))
        val result = select(emptyList(), pool)
        assertTrue(result.tracks.isEmpty())
        assertEquals(0, result.totalSec)
    }
}
