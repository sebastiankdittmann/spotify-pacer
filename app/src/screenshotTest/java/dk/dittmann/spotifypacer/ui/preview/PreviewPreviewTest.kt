package dk.dittmann.spotifypacer.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dk.dittmann.spotifypacer.pacing.BpmSample
import dk.dittmann.spotifypacer.pacing.CandidateTrack
import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.pacing.SelectedTrack
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.ui.setup.RunConfig

private val SampleConfig =
    RunConfig(distanceKm = 5.0, targetTimeSec = 28 * 60, strategy = PaceStrategy.LinearRamp)

private val SampleCurve = listOf(BpmSample(0, 165.0), BpmSample(840, 175.0), BpmSample(1680, 185.0))

private val SampleSelection =
    Selection(
        tracks =
            listOf(
                track("t1", "Runner's High", "The Pacers", 168.0, 195),
                track("t2", "Steady Rhythm", "Cadence Crew", 172.0, 215),
                track("t3", "Tempo Rising", "Stride", 176.0, 200),
                track("t4", "Negative Split", "Cadence", 182.0, 210),
            ),
        totalSec = 820,
    )

private fun track(id: String, title: String, artist: String, bpm: Double, durationSec: Int) =
    SelectedTrack(
        track =
            CandidateTrack(
                id = id,
                title = title,
                artist = artist,
                bpm = bpm,
                durationSec = durationSec,
            ),
        startSec = 0,
    )

@Composable
private fun PreviewBody(state: PreviewState) {
    PreviewScreen(state = state, onReroll = {}, onApprove = {}, onRetry = {}, onOpenPlaylist = {})
}

@Preview(name = "preview_loading_light", showBackground = true)
@Composable
fun PreviewLoading() {
    MaterialTheme { Surface { PreviewBody(PreviewState.Loading) } }
}

@Preview(name = "preview_ready_light", showBackground = true)
@Composable
fun PreviewReady() {
    MaterialTheme {
        Surface { PreviewBody(PreviewState.Ready(SampleConfig, SampleCurve, SampleSelection)) }
    }
}

@Preview(name = "preview_saving_light", showBackground = true)
@Composable
fun PreviewSaving() {
    MaterialTheme {
        Surface { PreviewBody(PreviewState.Saving(SampleConfig, SampleCurve, SampleSelection)) }
    }
}

@Preview(name = "preview_saved_light", showBackground = true)
@Composable
fun PreviewSaved() {
    MaterialTheme {
        Surface { PreviewBody(PreviewState.Saved("https://open.spotify.com/playlist/abc123")) }
    }
}

@Preview(name = "preview_error_network_light", showBackground = true)
@Composable
fun PreviewErrorNetwork() {
    MaterialTheme { Surface { PreviewBody(PreviewState.Error(ErrorReason.Network)) } }
}

@Preview(name = "preview_error_empty_pool_light", showBackground = true)
@Composable
fun PreviewErrorEmptyPool() {
    MaterialTheme { Surface { PreviewBody(PreviewState.Error(ErrorReason.EmptyPool)) } }
}

@Preview(name = "preview_ready_dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewReadyDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { PreviewBody(PreviewState.Ready(SampleConfig, SampleCurve, SampleSelection)) }
    }
}
