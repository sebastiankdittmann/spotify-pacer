package dk.dittmann.spotifypacer.ui.preview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dittmann.spotifypacer.pacing.BpmSample
import dk.dittmann.spotifypacer.pacing.CandidateTrack
import dk.dittmann.spotifypacer.pacing.Selection
import dk.dittmann.spotifypacer.pacing.generateCurve
import dk.dittmann.spotifypacer.pacing.select
import dk.dittmann.spotifypacer.save.SavePlaylistResult
import dk.dittmann.spotifypacer.ui.setup.RunConfig
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Drives the preview screen. Loads the candidate pool, generates the BPM curve, runs the selector,
 * and exposes the result through [PreviewState]. Re-rolling re-runs the selector with a fresh seed
 * against the cached pool — we don't refetch from Spotify. Approving routes through
 * [SavePlaylistUseCase] and surfaces the resulting playlist URL.
 */
class PreviewViewModel(
    private val config: RunConfig,
    private val candidates: CandidateLoader,
    private val save: suspend (RunConfig, Selection) -> SavePlaylistResult,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val randomFactory: () -> Random = { Random.Default },
) : ViewModel() {

    private val _state = MutableStateFlow<PreviewState>(PreviewState.Loading)
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    private var pool: List<CandidateTrack> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onReroll() {
        val current = _state.value as? PreviewState.Ready ?: return
        viewModelScope.launch {
            val selection = withContext(workDispatcher) { runSelection(current.curve, pool) }
            if (selection.tracks.isEmpty()) {
                _state.value = PreviewState.Error(ErrorReason.EmptyPool)
                return@launch
            }
            _state.value = current.copy(selection = selection)
        }
    }

    fun onApprove() {
        val current = _state.value as? PreviewState.Ready ?: return
        _state.value =
            PreviewState.Saving(
                config = current.config,
                curve = current.curve,
                selection = current.selection,
            )
        viewModelScope.launch {
            _state.value =
                try {
                    val result =
                        withContext(ioDispatcher) { save(current.config, current.selection) }
                    when (result) {
                        is SavePlaylistResult.Success -> PreviewState.Saved(result.playlistUrl)
                        is SavePlaylistResult.Failure -> {
                            Log.w(TAG, "Save failed at stage=${result.stage}", result.cause)
                            PreviewState.Error(ErrorReason.SaveFailed)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Save threw", e)
                    PreviewState.Error(errorReasonFor(e, fallback = ErrorReason.SaveFailed))
                }
        }
    }

    fun onRetry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = PreviewState.Loading
        loadJob =
            viewModelScope.launch {
                try {
                    val loaded = withContext(ioDispatcher) { candidates.load() }
                    pool = loaded
                    if (loaded.isEmpty()) {
                        _state.value = PreviewState.Error(ErrorReason.EmptyPool)
                        return@launch
                    }
                    val (curve, selection) =
                        withContext(workDispatcher) {
                            val curve =
                                generateCurve(
                                    strategy = config.strategy,
                                    totalSeconds = config.targetTimeSec,
                                    startBpm = START_BPM,
                                    endBpm = END_BPM,
                                )
                            curve to runSelection(curve, loaded)
                        }
                    _state.value =
                        if (selection.tracks.isEmpty()) PreviewState.Error(ErrorReason.EmptyPool)
                        else
                            PreviewState.Ready(
                                config = config,
                                curve = curve,
                                selection = selection,
                            )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Candidate load failed", e)
                    _state.value = PreviewState.Error(errorReasonFor(e))
                }
            }
    }

    private fun runSelection(curve: List<BpmSample>, pool: List<CandidateTrack>): Selection =
        select(curve = curve, candidates = pool, random = randomFactory())

    private companion object {
        private const val TAG = "PreviewViewModel"
        // Per docs/DESIGN.md §pace curves: avg ≈ 175 spm typical recreational cadence, ±10 BPM.
        const val START_BPM = 165
        const val END_BPM = 185
    }
}

internal fun errorReasonFor(
    e: Throwable,
    fallback: ErrorReason = ErrorReason.Unknown,
): ErrorReason =
    when (e) {
        is IOException -> ErrorReason.Network
        is HttpException ->
            when (e.code()) {
                403 -> ErrorReason.Forbidden
                429 -> ErrorReason.RateLimited
                else -> fallback
            }
        else -> fallback
    }
