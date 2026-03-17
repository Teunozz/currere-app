package nl.teunk.currere.data.db

data class RunningStatsRaw(
    val avgDistance: Double,
    val maxDistance: Double,
    val totalDistance: Double,
    val avgPace: Double?,
    val fastestPace: Double?,
    val avgHeartRate: Double?,
    val highestHeartRate: Long?,
    val totalRuns: Int,
    val totalDurationSeconds: Long,
)
