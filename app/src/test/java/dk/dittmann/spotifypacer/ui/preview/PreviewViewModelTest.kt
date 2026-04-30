package dk.dittmann.spotifypacer.ui.preview

import dk.dittmann.spotifypacer.pacing.CandidateTrack
import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.save.SavePlaylistResult
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val config =
        RunConfig(distanceKm = 5.0, targetTimeSec = 28 * 60, strategy = PaceStrategy.LinearRamp)

    private fun candidates(count: Int = 50) =
        (1..count).map {
            CandidateTrack(
                id = "t$it",
                title = "Title $it",
                artist = "Artist $it",
                bpm = 165.0 + (it % 21),
                durationSec = 200,
            )
        }

    private fun loader(items: List<CandidateTrack>) = CandidateLoader { items }

    private fun viewModel(
        loader: CandidateLoader = loader(candidates()),
        save: suspend (RunConfig, Selection) -> SavePlaylistResult = { _, _ ->
            SavePlaylistResult.Success("https://open.spotify.com/playlist/p1")
        },
        randomFactory: () -> Random = { Random(0) },
    ) =
        PreviewViewModel(
            config = config,
            candidates = loader,
            save = save,
            ioDispatcher = UnconfinedTestDispatcher(dispatcher.scheduler),
            workDispatcher = UnconfinedTestDispatcher(dispatcher.scheduler),
            randomFactory = randomFactory,
        )

    @Test
    fun starts_in_loading_then_transitions_to_ready_with_selection() = runTest {
        val vm = viewModel()
        assertTrue(vm.state.value is PreviewState.Loading)

        advanceUntilIdle()

        val ready = vm.state.value as PreviewState.Ready
        assertTrue(ready.selection.tracks.isNotEmpty())
        assertEquals(config, ready.config)
        assertTrue(ready.curve.isNotEmpty())
    }

    @Test
    fun empty_pool_surfaces_empty_pool_error() = runTest {
        val vm = viewModel(loader = loader(emptyList()))
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.EmptyPool), vm.state.value)
    }

    @Test
    fun network_failure_surfaces_network_error() = runTest {
        val vm = viewModel(loader = { throw IOException("offline") })
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.Network), vm.state.value)
    }

    @Test
    fun unknown_failure_surfaces_unknown_error() = runTest {
        val vm = viewModel(loader = { throw IllegalStateException("boom") })
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.Unknown), vm.state.value)
    }

    @Test
    fun forbidden_http_error_surfaces_forbidden_error() = runTest {
        val vm = viewModel(loader = { throw httpException(403) })
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.Forbidden), vm.state.value)
    }

    @Test
    fun rate_limited_http_error_surfaces_rate_limited_error() = runTest {
        val vm = viewModel(loader = { throw httpException(429) })
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.RateLimited), vm.state.value)
    }

    @Test
    fun other_http_error_surfaces_unknown_error() = runTest {
        val vm = viewModel(loader = { throw httpException(500) })
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.Unknown), vm.state.value)
    }

    @Test
    fun reroll_keeps_curve_and_changes_selection_with_new_seed() = runTest {
        var seed = 0
        val vm = viewModel(randomFactory = { Random(seed++) })
        advanceUntilIdle()
        val before = (vm.state.value as PreviewState.Ready)

        vm.onReroll()
        advanceUntilIdle()

        val after = vm.state.value as PreviewState.Ready
        assertEquals(before.curve, after.curve)
        assertEquals(before.config, after.config)
        // With a 50-track pool and shuffled selection, seed change should produce a different
        // ordered list. Compare ids to avoid flaky reference equality.
        val beforeIds = before.selection.tracks.map { it.track.id }
        val afterIds = after.selection.tracks.map { it.track.id }
        assertTrue(beforeIds != afterIds || beforeIds.isEmpty())
    }

    @Test
    fun reroll_is_ignored_outside_ready_state() = runTest {
        val vm = viewModel(loader = loader(emptyList()))
        advanceUntilIdle()
        val before = vm.state.value
        vm.onReroll()
        advanceUntilIdle()
        assertEquals(before, vm.state.value)
    }

    @Test
    fun approve_transitions_through_saving_to_saved() = runTest {
        var observed: List<PreviewState> = emptyList()
        val vm =
            viewModel(
                save = { _, _ ->
                    SavePlaylistResult.Success("https://open.spotify.com/playlist/abc")
                }
            )
        advanceUntilIdle()
        val ready = vm.state.value as PreviewState.Ready

        vm.onApprove()
        observed = listOf(vm.state.value) // captures Saving
        advanceUntilIdle()

        assertTrue(observed.first() is PreviewState.Saving)
        assertEquals(PreviewState.Saved("https://open.spotify.com/playlist/abc"), vm.state.value)
        assertEquals(ready.selection, (observed.first() as PreviewState.Saving).selection)
    }

    @Test
    fun approve_failure_surfaces_save_failed_error() = runTest {
        val vm =
            viewModel(
                save = { _, _ ->
                    SavePlaylistResult.Failure(
                        stage = SavePlaylistResult.Stage.ADD_TRACKS,
                        cause = RuntimeException("nope"),
                    )
                }
            )
        advanceUntilIdle()

        vm.onApprove()
        advanceUntilIdle()

        assertEquals(PreviewState.Error(ErrorReason.SaveFailed), vm.state.value)
    }

    @Test
    fun approve_is_ignored_outside_ready_state() = runTest {
        val vm = viewModel(loader = { throw IOException("offline") })
        advanceUntilIdle()
        val before = vm.state.value

        vm.onApprove()
        advanceUntilIdle()

        assertEquals(before, vm.state.value)
    }

    @Test
    fun retry_reloads_after_failure() = runTest {
        var attempts = 0
        val vm =
            viewModel(
                loader =
                    CandidateLoader {
                        attempts++
                        if (attempts == 1) throw IOException("offline") else candidates()
                    }
            )
        advanceUntilIdle()
        assertEquals(PreviewState.Error(ErrorReason.Network), vm.state.value)

        vm.onRetry()
        advanceUntilIdle()

        assertTrue(vm.state.value is PreviewState.Ready)
        assertEquals(2, attempts)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))
}
