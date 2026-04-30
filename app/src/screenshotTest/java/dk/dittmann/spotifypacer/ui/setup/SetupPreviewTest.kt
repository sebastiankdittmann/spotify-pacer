package dk.dittmann.spotifypacer.ui.setup

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun SetupPreviewBody(state: SetupState) {
    SetupScreen(
        state = state,
        onDistanceChange = {},
        onTargetTimeChange = {},
        onStrategyChange = {},
        onSubmit = {},
    )
}

@Preview(name = "setup_empty_light", showBackground = true)
@Composable
fun SetupEmptyPreview() {
    MaterialTheme { Surface { SetupPreviewBody(SetupState()) } }
}

@Preview(name = "setup_constant_light", showBackground = true)
@Composable
fun SetupConstantPreview() {
    MaterialTheme {
        Surface {
            SetupPreviewBody(
                SetupState(
                    distanceText = "5",
                    targetTimeText = "28:00",
                    strategy = StrategyChoice.Constant,
                )
            )
        }
    }
}

@Preview(name = "setup_linear_light", showBackground = true)
@Composable
fun SetupLinearPreview() {
    MaterialTheme {
        Surface {
            SetupPreviewBody(
                SetupState(
                    distanceText = "5",
                    targetTimeText = "28:00",
                    strategy = StrategyChoice.LinearRamp,
                )
            )
        }
    }
}

@Preview(name = "setup_delayed_exp_light", showBackground = true)
@Composable
fun SetupDelayedExponentialPreview() {
    MaterialTheme {
        Surface {
            SetupPreviewBody(
                SetupState(
                    distanceText = "10",
                    targetTimeText = "1:00:00",
                    strategy = StrategyChoice.DelayedExponential,
                )
            )
        }
    }
}

@Preview(name = "setup_distance_error_light", showBackground = true)
@Composable
fun SetupDistanceErrorPreview() {
    MaterialTheme {
        Surface {
            SetupPreviewBody(
                SetupState(distanceText = "five", distanceError = DistanceError.NotANumber)
            )
        }
    }
}

@Preview(name = "setup_pace_warning_light", showBackground = true)
@Composable
fun SetupPaceWarningPreview() {
    MaterialTheme {
        Surface {
            SetupPreviewBody(
                SetupState(
                    distanceText = "5",
                    targetTimeText = "10:00",
                    strategy = StrategyChoice.LinearRamp,
                    paceWarning = PaceWarning.TooFast,
                )
            )
        }
    }
}

@Preview(name = "setup_linear_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SetupLinearDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            SetupPreviewBody(
                SetupState(
                    distanceText = "5",
                    targetTimeText = "28:00",
                    strategy = StrategyChoice.LinearRamp,
                )
            )
        }
    }
}
