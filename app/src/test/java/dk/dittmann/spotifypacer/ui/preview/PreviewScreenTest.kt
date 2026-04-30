package dk.dittmann.spotifypacer.ui.preview

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dk.dittmann.spotifypacer.pacing.BpmSample
import dk.dittmann.spotifypacer.pacing.CandidateTrack
import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.pacing.SelectedTrack
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val config =
        RunConfig(distanceKm = 5.0, targetTimeSec = 28 * 60, strategy = PaceStrategy.LinearRamp)

    private fun curve(): List<BpmSample> = listOf(BpmSample(0, 165.0), BpmSample(1680, 185.0))

    private fun selection() =
        Selection(
            tracks =
                listOf(
                    SelectedTrack(
                        track =
                            CandidateTrack(
                                id = "t1",
                                title = "Runner's High",
                                artist = "The Pacers",
                                bpm = 170.0,
                                durationSec = 200,
                            ),
                        startSec = 0,
                    ),
                    SelectedTrack(
                        track =
                            CandidateTrack(
                                id = "t2",
                                title = "Negative Split",
                                artist = "Cadence",
                                bpm = 180.0,
                                durationSec = 220,
                            ),
                        startSec = 200,
                    ),
                ),
            totalSec = 420,
        )

    private fun render(
        state: PreviewState,
        onReroll: () -> Unit = {},
        onApprove: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenPlaylist: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                PreviewScreen(
                    state = state,
                    onReroll = onReroll,
                    onApprove = onApprove,
                    onRetry = onRetry,
                    onOpenPlaylist = onOpenPlaylist,
                )
            }
        }
    }

    @Test
    fun loading_shows_progress_message() {
        render(PreviewState.Loading)
        composeRule.onNodeWithText("Building your pacer playlist…").assertIsDisplayed()
    }

    @Test
    fun ready_renders_track_list_and_actions() {
        render(PreviewState.Ready(config, curve(), selection()))
        composeRule.onNodeWithText("Runner's High").assertIsDisplayed()
        composeRule.onNodeWithText("Negative Split").assertIsDisplayed()
        composeRule.onNodeWithText("Re-roll").assertIsDisplayed()
        composeRule.onNodeWithText("Approve & save").assertIsDisplayed()
    }

    @Test
    fun reroll_button_invokes_callback() {
        var rerolls = 0
        render(PreviewState.Ready(config, curve(), selection()), onReroll = { rerolls++ })
        composeRule.onNodeWithText("Re-roll").performClick()
        assertEquals(1, rerolls)
    }

    @Test
    fun approve_button_invokes_callback() {
        var approves = 0
        render(PreviewState.Ready(config, curve(), selection()), onApprove = { approves++ })
        composeRule.onNodeWithText("Approve & save").performClick()
        assertEquals(1, approves)
    }

    @Test
    fun saving_state_disables_actions_and_shows_progress() {
        var rerolls = 0
        render(PreviewState.Saving(config, curve(), selection()), onReroll = { rerolls++ })
        composeRule.onNodeWithText("Saving to Spotify…").assertIsDisplayed()
        composeRule.onNodeWithText("Re-roll").assertIsNotEnabled()
        composeRule.onNodeWithText("Approve & save").assertIsNotEnabled()
        assertEquals(0, rerolls)
    }

    @Test
    fun saved_shows_open_link_button_with_url() {
        var openedWith: String? = null
        render(
            PreviewState.Saved("https://open.spotify.com/playlist/abc"),
            onOpenPlaylist = { openedWith = it },
        )
        composeRule.onNodeWithText("Saved!").assertIsDisplayed()
        composeRule.onNodeWithText("Open in Spotify").performClick()
        assertEquals("https://open.spotify.com/playlist/abc", openedWith)
    }

    @Test
    fun error_network_renders_retry() {
        var retries = 0
        render(PreviewState.Error(ErrorReason.Network), onRetry = { retries++ })
        composeRule
            .onNodeWithText("Couldn't reach Spotify. Check your connection.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun error_empty_pool_renders_message() {
        render(PreviewState.Error(ErrorReason.EmptyPool))
        composeRule
            .onNodeWithText("No tracks in your library matched the curve.")
            .assertIsDisplayed()
    }

    @Test
    fun error_save_failed_renders_message() {
        render(PreviewState.Error(ErrorReason.SaveFailed))
        composeRule.onNodeWithText("Saving the playlist failed. Try again.").assertIsDisplayed()
    }

    @Test
    fun error_forbidden_renders_message() {
        render(PreviewState.Error(ErrorReason.Forbidden))
        composeRule
            .onNodeWithText("Spotify refused access. Try signing in again or retry.")
            .assertIsDisplayed()
    }

    @Test
    fun error_rate_limited_renders_message() {
        render(PreviewState.Error(ErrorReason.RateLimited))
        composeRule
            .onNodeWithText("The BPM provider is throttling requests. Wait a moment and retry.")
            .assertIsDisplayed()
    }

    @Test
    fun error_bpm_unconfigured_renders_message() {
        render(PreviewState.Error(ErrorReason.BpmProviderUnconfigured))
        composeRule
            .onNodeWithText("BPM lookup is not configured. The app needs a GetSongBPM API key.")
            .assertIsDisplayed()
    }

    @Test
    fun error_bpm_unavailable_renders_message() {
        render(PreviewState.Error(ErrorReason.BpmProviderUnavailable))
        composeRule
            .onNodeWithText("Couldn't reach the BPM provider. Try again later.")
            .assertIsDisplayed()
    }

    @Test
    fun attribution_link_is_visible_in_every_state() {
        render(PreviewState.Loading)
        composeRule.onNodeWithText("BPM data powered by GetSongBPM").assertIsDisplayed()
    }
}
