package nl.teunk.currere.domain.compute

import nl.teunk.currere.domain.model.PaceSample
import java.time.Duration
import java.time.Instant

object PaceCalculator {

    /**
     * Convert speed in m/s to pace in seconds per km.
     * Returns null for zero or negative speed (stationary).
     */
    fun speedToPace(metersPerSecond: Double): Double? {
        if (metersPerSecond <= 0.0) return null
        return 1000.0 / metersPerSecond
    }

    /**
     * Compute average pace from total duration and total distance.
     * Returns null if distance is zero or negative.
     */
    fun averagePace(activeDuration: Duration, distanceMeters: Double): Double? {
        if (distanceMeters <= 0.0) return null
        val totalSeconds = activeDuration.seconds.toDouble() + activeDuration.nano / 1_000_000_000.0
        return totalSeconds / (distanceMeters / 1000.0)
    }

    /**
     * Convert a list of timestamped speed values (m/s) to pace samples.
     * Filters out stationary (0 m/s) samples.
     */
    fun toPaceSamples(speedSamples: List<Pair<Instant, Double>>): List<PaceSample> {
        return speedSamples.mapNotNull { (time, speed) ->
            speedToPace(speed)?.let { pace ->
                PaceSample(time = time, secondsPerKm = pace)
            }
        }
    }
}
