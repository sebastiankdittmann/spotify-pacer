package dk.dittmann.spotifypacer.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dk.dittmann.spotifypacer.R
import dk.dittmann.spotifypacer.pacing.SelectedTrack
import dk.dittmann.spotifypacer.pacing.Selection

@Composable
fun PreviewScreen(
    state: PreviewState,
    onReroll: () -> Unit,
    onApprove: () -> Unit,
    onRetry: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        when (state) {
            PreviewState.Loading -> LoadingBody(stringResource(R.string.preview_loading))
            is PreviewState.Ready ->
                ReadyBody(
                    selection = state.selection,
                    onReroll = onReroll,
                    onApprove = onApprove,
                    actionsEnabled = true,
                )
            is PreviewState.Saving ->
                ReadyBody(
                    selection = state.selection,
                    onReroll = onReroll,
                    onApprove = onApprove,
                    actionsEnabled = false,
                    overlayText = stringResource(R.string.preview_saving),
                )
            is PreviewState.Saved -> SavedBody(state.playlistUrl, onOpenPlaylist)
            is PreviewState.Error -> ErrorBody(state.reason, onRetry)
        }
    }
}

@Composable
private fun LoadingBody(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(message)
    }
}

@Composable
private fun ReadyBody(
    selection: Selection,
    onReroll: () -> Unit,
    onApprove: () -> Unit,
    actionsEnabled: Boolean,
    overlayText: String? = null,
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.preview_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text =
                stringResource(
                    R.string.preview_track_count,
                    selection.tracks.size,
                    formatDuration(selection.totalSec),
                ),
            style = MaterialTheme.typography.bodyMedium,
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(selection.tracks, key = { it.track.id }) { TrackRow(it) }
        }

        if (overlayText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Text(overlayText)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onReroll,
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.preview_reroll_button))
            }
            Button(onClick = onApprove, enabled = actionsEnabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.preview_approve_button))
            }
        }
    }
}

@Composable
private fun TrackRow(selected: SelectedTrack) {
    val track = selected.track
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = track.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                stringResource(
                    R.string.preview_track_meta,
                    track.bpm.toInt(),
                    formatDuration(track.durationSec),
                    formatDuration(selected.startSec),
                ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedBody(playlistUrl: String, onOpen: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.preview_saved_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = playlistUrl, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onOpen(playlistUrl) }) {
            Text(stringResource(R.string.preview_saved_open_link))
        }
    }
}

@Composable
private fun ErrorBody(reason: ErrorReason, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(reason.messageRes()), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(stringResource(R.string.preview_retry_button)) }
    }
}

private fun ErrorReason.messageRes(): Int =
    when (this) {
        ErrorReason.Network -> R.string.preview_error_network
        ErrorReason.EmptyPool -> R.string.preview_error_empty_pool
        ErrorReason.SaveFailed -> R.string.preview_error_save_failed
        ErrorReason.Unknown -> R.string.preview_error_unknown
    }

internal fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(java.util.Locale.ROOT, m, s)
}
