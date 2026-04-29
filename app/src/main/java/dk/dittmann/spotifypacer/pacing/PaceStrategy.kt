package dk.dittmann.spotifypacer.pacing

sealed interface PaceStrategy {
    object Constant : PaceStrategy

    object LinearRamp : PaceStrategy

    data class DelayedExponential(val k: Double = 4.0) : PaceStrategy
}
