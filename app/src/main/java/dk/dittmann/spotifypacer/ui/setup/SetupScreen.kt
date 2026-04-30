package dk.dittmann.spotifypacer.ui.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dk.dittmann.spotifypacer.R
import dk.dittmann.spotifypacer.pacing.generateCurve

private const val PREVIEW_START_BPM = 160
private const val PREVIEW_END_BPM = 180
private const val PREVIEW_TOTAL_SECONDS = 1800

@Composable
fun SetupScreen(
    state: SetupState,
    onDistanceChange: (String) -> Unit,
    onTargetTimeChange: (String) -> Unit,
    onStrategyChange: (StrategyChoice) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        DistanceField(state = state, onChange = onDistanceChange)
        TargetTimeField(state = state, onChange = onTargetTimeChange)

        state.paceWarning?.let { warning ->
            Text(
                text = stringResource(warning.messageRes()),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        StrategyPicker(state.strategy, onStrategyChange)

        Text(
            text = stringResource(R.string.setup_curve_preview_label),
            style = MaterialTheme.typography.labelMedium,
        )
        BpmCurvePreview(
            strategy = state.strategy,
            modifier = Modifier.fillMaxWidth().height(120.dp),
        )

        Spacer(Modifier.height(8.dp))
        Button(onClick = onSubmit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_submit_button))
        }
    }
}

@Composable
private fun DistanceField(state: SetupState, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = state.distanceText,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.setup_distance_label)) },
        placeholder = { Text(stringResource(R.string.setup_distance_placeholder)) },
        singleLine = true,
        isError = state.distanceError != null,
        supportingText = { state.distanceError?.let { Text(stringResource(it.messageRes())) } },
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    )
}

@Composable
private fun TargetTimeField(state: SetupState, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = state.targetTimeText,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.setup_target_time_label)) },
        placeholder = { Text(stringResource(R.string.setup_target_time_placeholder)) },
        singleLine = true,
        isError = state.targetTimeError != null,
        supportingText = { state.targetTimeError?.let { Text(stringResource(it.messageRes())) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}

@Composable
private fun StrategyPicker(selected: StrategyChoice, onSelect: (StrategyChoice) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text(
            text = stringResource(R.string.setup_strategy_label),
            style = MaterialTheme.typography.labelMedium,
        )
        StrategyChoice.entries.forEach { choice ->
            val label = stringResource(choice.labelRes())
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .selectable(
                            selected = selected == choice,
                            onClick = { onSelect(choice) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == choice, onClick = null)
                Text(text = label, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
internal fun BpmCurvePreview(strategy: StrategyChoice, modifier: Modifier = Modifier) {
    val samples =
        generateCurve(
            strategy = strategy.toStrategy(),
            totalSeconds = PREVIEW_TOTAL_SECONDS,
            startBpm = PREVIEW_START_BPM,
            endBpm = PREVIEW_END_BPM,
        )
    val color = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.setup_curve_preview_content_description)
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        if (samples.size < 2) return@Canvas
        val bpmRange = (PREVIEW_END_BPM - PREVIEW_START_BPM).toDouble()
        val totalT = samples.last().timeSec.toDouble().takeIf { it > 0.0 } ?: 1.0

        val path = Path()
        samples.forEachIndexed { index, sample ->
            val x = (sample.timeSec / totalT * size.width).toFloat()
            val y = (size.height * (1.0 - (sample.bpm - PREVIEW_START_BPM) / bpmRange)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 4f))
    }
}

private fun DistanceError.messageRes(): Int =
    when (this) {
        DistanceError.NotANumber -> R.string.setup_distance_error_not_a_number
        DistanceError.NotPositive -> R.string.setup_distance_error_not_positive
    }

private fun TargetTimeError.messageRes(): Int =
    when (this) {
        TargetTimeError.Malformed -> R.string.setup_target_time_error_malformed
        TargetTimeError.NotPositive -> R.string.setup_target_time_error_not_positive
    }

private fun PaceWarning.messageRes(): Int =
    when (this) {
        PaceWarning.TooFast -> R.string.setup_pace_warning_too_fast
        PaceWarning.TooSlow -> R.string.setup_pace_warning_too_slow
    }

private fun StrategyChoice.labelRes(): Int =
    when (this) {
        StrategyChoice.Constant -> R.string.setup_strategy_constant
        StrategyChoice.LinearRamp -> R.string.setup_strategy_linear
        StrategyChoice.DelayedExponential -> R.string.setup_strategy_delayed_exponential
    }
