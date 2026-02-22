package nl.teunk.currere.domain.model

import java.time.Instant

data class PaceSample(
    val time: Instant,
    val secondsPerKm: Double,
)
