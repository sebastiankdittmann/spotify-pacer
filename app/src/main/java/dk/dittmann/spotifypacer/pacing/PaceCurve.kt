package dk.dittmann.spotifypacer.pacing

import kotlin.math.exp

data class BpmSample(val timeSec: Int, val bpm: Double)

fun generateCurve(
    strategy: PaceStrategy,
    totalSeconds: Int,
    startBpm: Int,
    endBpm: Int,
    sampleIntervalSec: Int = 60,
): List<BpmSample> {
    require(totalSeconds > 0) { "totalSeconds must be > 0" }
    require(sampleIntervalSec > 0) { "sampleIntervalSec must be > 0" }
    require(startBpm in 1..300) { "startBpm must be in (0, 300]" }
    require(endBpm in 1..300) { "endBpm must be in (0, 300]" }

    val b0 = startBpm.toDouble()
    val b1 = endBpm.toDouble()
    val T = totalSeconds.toDouble()

    val times = (0..totalSeconds step sampleIntervalSec).toMutableList()
    if (times.last() != totalSeconds) times.add(totalSeconds)

    return times.map { t ->
        val bpm =
            when (strategy) {
                PaceStrategy.Constant -> (b0 + b1) / 2.0
                PaceStrategy.LinearRamp -> b0 + (b1 - b0) * (t / T)
                is PaceStrategy.DelayedExponential -> {
                    val k = strategy.k
                    b0 + (b1 - b0) * (exp(k * t / T) - 1) / (exp(k) - 1)
                }
            }
        BpmSample(t, bpm)
    }
}
