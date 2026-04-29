package dk.dittmann.spotifypacer.pacing

import kotlin.math.abs
import kotlin.random.Random

data class CandidateTrack(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Double,
    val durationSec: Int,
)

data class SelectedTrack(val track: CandidateTrack, val startSec: Int)

data class Selection(val tracks: List<SelectedTrack>, val totalSec: Int)

private val TOLERANCE_STEPS = listOf(3.0, 6.0, 12.0, 24.0)

fun select(
    curve: List<BpmSample>,
    candidates: List<CandidateTrack>,
    random: Random = Random.Default,
): Selection {
    if (curve.isEmpty() || candidates.isEmpty()) return Selection(emptyList(), 0)

    val totalSec = curve.last().timeSec
    val unused = candidates.toMutableList()
    val selected = mutableListOf<SelectedTrack>()
    var cursor = 0

    while (cursor < totalSec && unused.isNotEmpty()) {
        val targetBpm = bpmAt(curve, cursor)
        val remaining = totalSec - cursor
        val track = pickTrack(unused, targetBpm, remaining, random) ?: break
        selected.add(SelectedTrack(track, cursor))
        unused.remove(track)
        if (track.durationSec <= 0) break
        cursor += track.durationSec
    }

    return Selection(selected, cursor)
}

private fun bpmAt(curve: List<BpmSample>, timeSec: Int): Double {
    if (curve.size == 1) return curve[0].bpm
    if (timeSec <= curve.first().timeSec) return curve.first().bpm
    if (timeSec >= curve.last().timeSec) return curve.last().bpm
    val idx = curve.indexOfLast { it.timeSec <= timeSec }
    val lo = curve[idx]
    val hi = curve[idx + 1]
    val frac = (timeSec - lo.timeSec).toDouble() / (hi.timeSec - lo.timeSec)
    return lo.bpm + (hi.bpm - lo.bpm) * frac
}

private fun bpmDistance(trackBpm: Double, targetBpm: Double): Double {
    val direct = abs(trackBpm - targetBpm)
    val half = abs(trackBpm / 2.0 - targetBpm)
    val double = abs(trackBpm * 2.0 - targetBpm)
    return minOf(direct, half, double)
}

private fun pickTrack(
    unused: List<CandidateTrack>,
    targetBpm: Double,
    remaining: Int,
    random: Random,
): CandidateTrack? {
    if (unused.isEmpty()) return null
    for (tolerance in TOLERANCE_STEPS) {
        val matches = unused.filter { bpmDistance(it.bpm, targetBpm) <= tolerance }
        if (matches.isNotEmpty()) return pickBest(matches, remaining, random)
    }
    val nearest = unused.minByOrNull { bpmDistance(it.bpm, targetBpm) } ?: return null
    return pickBest(listOf(nearest), remaining, random)
}

private fun pickBest(
    candidates: List<CandidateTrack>,
    remaining: Int,
    random: Random,
): CandidateTrack {
    val fitting = candidates.filter { it.durationSec <= remaining }
    val pool = if (fitting.isNotEmpty()) fitting else candidates
    return pool[random.nextInt(pool.size)]
}
