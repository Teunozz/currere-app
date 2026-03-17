package nl.teunk.currere.domain.model

import nl.teunk.currere.data.db.RunningStatsRaw
import nl.teunk.currere.domain.compute.StatsAggregator
import java.time.Duration

data class RunningStats(
    val avgDistanceKm: String,
    val longestDistanceKm: String,
    val totalDistanceKm: String,
    val avgPace: String,
    val fastestPace: String,
    val avgHeartRate: String,
    val highestHeartRate: String,
    val totalRuns: String,
    val totalTime: String,
)

fun RunningStatsRaw.toFormatted(): RunningStats = RunningStats(
    avgDistanceKm = StatsAggregator.formatDistanceKm(avgDistance),
    longestDistanceKm = StatsAggregator.formatDistanceKm(maxDistance),
    totalDistanceKm = StatsAggregator.formatDistanceKm(totalDistance),
    avgPace = avgPace?.let { StatsAggregator.formatPace(it) } ?: "\u2014",
    fastestPace = fastestPace?.let { StatsAggregator.formatPace(it) } ?: "\u2014",
    avgHeartRate = avgHeartRate?.let { "${it.toLong()}" } ?: "\u2014",
    highestHeartRate = highestHeartRate?.let { "$it" } ?: "\u2014",
    totalRuns = "$totalRuns",
    totalTime = StatsAggregator.formatDurationLong(Duration.ofSeconds(totalDurationSeconds)),
)
