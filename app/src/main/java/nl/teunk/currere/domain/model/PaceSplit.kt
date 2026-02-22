package nl.teunk.currere.domain.model

import java.time.Duration

data class PaceSplit(
    val kilometerNumber: Int,
    val distanceMeters: Double,
    val splitDuration: Duration,
    val splitPaceSecondsPerKm: Double,
    val cumulativeDuration: Duration,
    val isPartial: Boolean,
)
