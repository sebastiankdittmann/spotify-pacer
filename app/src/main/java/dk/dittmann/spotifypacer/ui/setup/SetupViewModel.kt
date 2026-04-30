package dk.dittmann.spotifypacer.ui.setup

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val MIN_PACE_SEC_PER_KM = 180
private const val MAX_PACE_SEC_PER_KM = 600

class SetupViewModel : ViewModel() {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    fun onDistanceChanged(text: String) {
        _state.update { current ->
            val parsed = parseDistance(text)
            current.copy(
                distanceText = text,
                distanceError = parsed.error,
                paceWarning = computePaceWarning(parsed.km, current.parsedTargetTimeSec()),
            )
        }
    }

    fun onTargetTimeChanged(text: String) {
        _state.update { current ->
            val parsed = parseTargetTime(text)
            current.copy(
                targetTimeText = text,
                targetTimeError = parsed.error,
                paceWarning = computePaceWarning(current.parsedDistanceKm(), parsed.seconds),
            )
        }
    }

    fun onStrategyChanged(choice: StrategyChoice) {
        _state.update { it.copy(strategy = choice) }
    }

    fun toRunConfig(): RunConfig? {
        val current = _state.value
        if (!current.canSubmit) return null
        val km = current.parsedDistanceKm() ?: return null
        val seconds = current.parsedTargetTimeSec() ?: return null
        return RunConfig(km, seconds, current.strategy.toStrategy())
    }
}

private fun SetupState.parsedDistanceKm(): Double? =
    if (distanceError == null) parseDistance(distanceText).km else null

private fun SetupState.parsedTargetTimeSec(): Int? =
    if (targetTimeError == null) parseTargetTime(targetTimeText).seconds else null

internal data class ParsedDistance(val km: Double?, val error: DistanceError?)

internal data class ParsedTargetTime(val seconds: Int?, val error: TargetTimeError?)

internal fun parseDistance(text: String): ParsedDistance {
    if (text.isBlank()) return ParsedDistance(null, null)
    val normalized = text.trim().replace(',', '.')
    val km = normalized.toDoubleOrNull() ?: return ParsedDistance(null, DistanceError.NotANumber)
    if (km <= 0.0) return ParsedDistance(null, DistanceError.NotPositive)
    return ParsedDistance(km, null)
}

internal fun parseTargetTime(text: String): ParsedTargetTime {
    if (text.isBlank()) return ParsedTargetTime(null, null)
    val parts = text.trim().split(":")
    if (parts.size !in 2..3 || parts.any { it.isBlank() }) {
        return ParsedTargetTime(null, TargetTimeError.Malformed)
    }
    val numbers =
        parts.map { it.toIntOrNull() ?: return ParsedTargetTime(null, TargetTimeError.Malformed) }
    if (numbers.any { it < 0 }) return ParsedTargetTime(null, TargetTimeError.Malformed)
    // Minutes/seconds segments must be < 60. The leading segment (hours, or minutes when only
    // mm:ss)
    // has no upper bound — runners may legitimately enter 90:00 for a 90-minute run.
    if (numbers.drop(1).any { it >= 60 }) return ParsedTargetTime(null, TargetTimeError.Malformed)
    val seconds =
        when (numbers.size) {
            2 -> numbers[0] * 60 + numbers[1]
            else -> numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        }
    if (seconds <= 0) return ParsedTargetTime(null, TargetTimeError.NotPositive)
    return ParsedTargetTime(seconds, null)
}

internal fun computePaceWarning(km: Double?, seconds: Int?): PaceWarning? {
    if (km == null || seconds == null || km <= 0.0 || seconds <= 0) return null
    val paceSecPerKm = seconds / km
    return when {
        paceSecPerKm < MIN_PACE_SEC_PER_KM -> PaceWarning.TooFast
        paceSecPerKm > MAX_PACE_SEC_PER_KM -> PaceWarning.TooSlow
        else -> null
    }
}
