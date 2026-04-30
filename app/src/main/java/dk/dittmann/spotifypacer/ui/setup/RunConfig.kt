package dk.dittmann.spotifypacer.ui.setup

import dk.dittmann.spotifypacer.pacing.PaceStrategy

data class RunConfig(val distanceKm: Double, val targetTimeSec: Int, val strategy: PaceStrategy)
