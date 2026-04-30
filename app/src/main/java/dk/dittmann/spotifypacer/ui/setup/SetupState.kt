package dk.dittmann.spotifypacer.ui.setup

import dk.dittmann.spotifypacer.pacing.PaceStrategy

enum class StrategyChoice {
    Constant,
    LinearRamp,
    DelayedExponential;

    fun toStrategy(): PaceStrategy =
        when (this) {
            Constant -> PaceStrategy.Constant
            LinearRamp -> PaceStrategy.LinearRamp
            DelayedExponential -> PaceStrategy.DelayedExponential()
        }
}

data class SetupState(
    val distanceText: String = "",
    val targetTimeText: String = "",
    val strategy: StrategyChoice = StrategyChoice.LinearRamp,
    val distanceError: DistanceError? = null,
    val targetTimeError: TargetTimeError? = null,
    val paceWarning: PaceWarning? = null,
) {
    val canSubmit: Boolean
        get() =
            distanceText.isNotBlank() &&
                targetTimeText.isNotBlank() &&
                distanceError == null &&
                targetTimeError == null
}

enum class DistanceError {
    NotANumber,
    NotPositive,
}

enum class TargetTimeError {
    Malformed,
    NotPositive,
}

enum class PaceWarning {
    TooFast,
    TooSlow,
}
