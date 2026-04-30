package dk.dittmann.spotifypacer.save

import dk.dittmann.spotifypacer.pacing.PaceStrategy
import dk.dittmann.spotifypacer.ui.setup.RunConfig

/**
 * Single source of truth for user-facing playlist names and descriptions, per ticket #9. Anything
 * that needs to format a strategy, distance, or duration goes through here so the wording stays
 * consistent.
 */
object PlaylistNaming {

    fun name(config: RunConfig): String =
        "Pacer · ${formatDistance(config.distanceKm)} · ${formatTime(config.targetTimeSec)} · ${strategyLabel(config.strategy)}"

    fun description(strategy: PaceStrategy, totalSec: Int): String =
        "${strategyLabel(strategy)} pace, total ${formatTime(totalSec)}"

    fun trackUri(trackId: String): String = "spotify:track:$trackId"

    fun playlistUrl(playlistId: String): String = "https://open.spotify.com/playlist/$playlistId"

    private fun formatDistance(km: Double): String {
        val whole = km.toLong()
        return if (km == whole.toDouble()) "${whole}km" else "%.2fkm".format(km)
    }

    private fun formatTime(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    private fun strategyLabel(strategy: PaceStrategy): String =
        when (strategy) {
            is PaceStrategy.Constant -> "constant"
            is PaceStrategy.LinearRamp -> "linear"
            is PaceStrategy.DelayedExponential -> "delayed-exp"
        }
}
