package dk.dittmann.spotifypacer.ui.preview

import dk.dittmann.spotifypacer.pacing.BpmSample
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.ui.setup.RunConfig

/**
 * Preview screen view state. The screen carries the generated [Selection] and BPM curve through
 * Ready/Saving so re-rolls and saves don't lose the data the user has been looking at.
 */
sealed interface PreviewState {

    data object Loading : PreviewState

    data class Ready(val config: RunConfig, val curve: List<BpmSample>, val selection: Selection) :
        PreviewState

    data class Saving(val config: RunConfig, val curve: List<BpmSample>, val selection: Selection) :
        PreviewState

    data class Saved(val playlistUrl: String) : PreviewState

    data class Error(val reason: ErrorReason) : PreviewState
}

enum class ErrorReason {
    Network,
    Forbidden,
    RateLimited,
    EmptyPool,
    SaveFailed,
    Unknown,
}
