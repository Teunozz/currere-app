package nl.teunk.currere.domain.model

import java.time.Duration
import java.time.Instant

data class RunSession(
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
    val activeDuration: Duration,
    val averagePaceSecondsPerKm: Double?,
    val averageHeartRateBpm: Long?,
    val title: String,
)
