package nl.teunk.currere.domain.model

import java.time.Instant

data class HeartRateSample(
    val time: Instant,
    val bpm: Long,
)
