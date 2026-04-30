package dk.dittmann.spotifypacer.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import dk.dittmann.spotifypacer.ui.setup.SetupScreen
import dk.dittmann.spotifypacer.ui.setup.SetupViewModel

/**
 * Setup destination wrapper. Submits a validated [RunConfig] to the host so it can navigate to the
 * preview destination. The ViewModel itself remains nav-unaware.
 */
@Composable
fun SetupRoute(onConfirm: (RunConfig) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: SetupViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SetupScreen(
        state = state,
        onDistanceChange = viewModel::onDistanceChanged,
        onTargetTimeChange = viewModel::onTargetTimeChanged,
        onStrategyChange = viewModel::onStrategyChanged,
        onSubmit = { viewModel.toRunConfig()?.let(onConfirm) },
        modifier = modifier,
    )
}
