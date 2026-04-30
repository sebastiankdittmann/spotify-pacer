package dk.dittmann.spotifypacer.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.save.SavePlaylistResult
import dk.dittmann.spotifypacer.ui.preview.CandidateLoader
import dk.dittmann.spotifypacer.ui.preview.PreviewScreen
import dk.dittmann.spotifypacer.ui.preview.PreviewViewModel
import dk.dittmann.spotifypacer.ui.setup.RunConfig

/**
 * Preview destination wrapper. Composes a [PreviewViewModel] with the supplied dependencies and
 * forwards UI events. Save dependency is passed as a suspend lambda so the ViewModel stays
 * decoupled from `SavePlaylistUseCase`.
 */
@Composable
fun PreviewRoute(
    config: RunConfig,
    candidateLoader: CandidateLoader,
    save: suspend (RunConfig, Selection) -> SavePlaylistResult,
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PreviewViewModel =
        viewModel(
            factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(
                        modelClass: Class<T>
                    ): T =
                        PreviewViewModel(config = config, candidates = candidateLoader, save = save)
                            as T
                }
        )

    val state by viewModel.state.collectAsStateWithLifecycle()
    PreviewScreen(
        state = state,
        onReroll = viewModel::onReroll,
        onApprove = viewModel::onApprove,
        onRetry = viewModel::onRetry,
        onOpenPlaylist = onOpenPlaylist,
        modifier = modifier,
    )
}
