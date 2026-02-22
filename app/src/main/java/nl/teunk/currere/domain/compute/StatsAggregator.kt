package nl.teunk.currere.domain.compute

import nl.teunk.currere.domain.model.TimeOfDay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object StatsAggregator {

    /**
     * Format a Duration as h:mm:ss or mm:ss.
     * Uses h:mm:ss when duration >= 1 hour, otherwise mm:ss.
     */
    fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format distance in meters as km with 2 decimal places.
     * e.g. 15012.34 → "15.01"
     */
    fun formatDistanceKm(distanceMeters: Double): String {
        val km = distanceMeters / 1000.0
        return String.format(Locale.US, "%.2f", km)
    }

    /**
     * Format pace in seconds/km as m:ss.
     * e.g. 300.0 → "5:00", 325.0 → "5:25"
     */
    fun formatPace(secondsPerKm: Double): String {
        val totalSeconds = secondsPerKm.toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    /**
     * Derive TimeOfDay from an Instant using the device's default timezone.
     * Morning: 05:00–11:59, Afternoon: 12:00–16:59,
     * Evening: 17:00–20:59, Night: 21:00–04:59
     */
    fun timeOfDay(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): TimeOfDay {
        val hour = instant.atZone(zoneId).hour
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    /**
     * Derive activity title from start time.
     */
    fun activityTitle(startTime: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return timeOfDay(startTime, zoneId).label
    }
}
