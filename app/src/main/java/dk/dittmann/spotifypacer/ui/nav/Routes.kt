package dk.dittmann.spotifypacer.ui.nav

import android.net.Uri
import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Routes {
    const val Login = "login"
    const val Setup = "setup"
    const val Preview = "preview"

    /** Path-segment argument key on the [Preview] destination carrying a JSON-encoded config. */
    const val PreviewArgConfig = "config"

    /** NavHost route pattern with the required config path argument. */
    const val PreviewPattern = "$Preview/{$PreviewArgConfig}"

    fun preview(config: RunConfig): String = "$Preview/${Uri.encode(RunConfigArg.encode(config))}"
}

@Serializable
data class RunConfigArg(
    val distanceKm: Double,
    val targetTimeSec: Int,
    val strategy: StrategyArg,
    val k: Double = DEFAULT_K,
) {
    fun toRunConfig(): RunConfig =
        RunConfig(
            distanceKm = distanceKm,
            targetTimeSec = targetTimeSec,
            strategy =
                when (strategy) {
                    StrategyArg.Constant -> PaceStrategy.Constant
                    StrategyArg.LinearRamp -> PaceStrategy.LinearRamp
                    StrategyArg.DelayedExponential -> PaceStrategy.DelayedExponential(k)
                },
        )

    companion object {
        const val DEFAULT_K = 4.0
        private val json = Json { ignoreUnknownKeys = true }

        fun fromRunConfig(config: RunConfig): RunConfigArg =
            RunConfigArg(
                distanceKm = config.distanceKm,
                targetTimeSec = config.targetTimeSec,
                strategy =
                    when (config.strategy) {
                        is PaceStrategy.Constant -> StrategyArg.Constant
                        is PaceStrategy.LinearRamp -> StrategyArg.LinearRamp
                        is PaceStrategy.DelayedExponential -> StrategyArg.DelayedExponential
                    },
                k = (config.strategy as? PaceStrategy.DelayedExponential)?.k ?: DEFAULT_K,
            )

        fun encode(config: RunConfig): String =
            json.encodeToString(serializer(), fromRunConfig(config))

        fun decode(encoded: String): RunConfigArg = json.decodeFromString(serializer(), encoded)
    }
}

@Serializable
enum class StrategyArg {
    Constant,
    LinearRamp,
    DelayedExponential,
}
