package nl.teunk.currere.domain.compute

import nl.teunk.currere.domain.model.PaceSplit
import java.time.Duration
import java.time.Instant

object SplitCalculator {

    /**
     * Compute per-km splits from speed samples using trapezoidal integration,
     * calibrated against the known GPS-based total distance.
     *
     * @param speedSamples list of (timestamp, speed in m/s) pairs
     * @param totalDistanceMeters known total distance to calibrate against
     * @param sessionStartTime actual session start to anchor km1 timing
     */
    fun computeSplits(
        speedSamples: List<Pair<Instant, Double>>,
        totalDistanceMeters: Double? = null,
        sessionStartTime: Instant? = null,
    ): List<PaceSplit> {
        if (speedSamples.size < 2) return emptyList()

        val sorted = speedSamples.sortedBy { it.first }

        // Prepend synthetic zero-velocity sample at session start if it precedes
        // the first speed sample, to anchor km1 timing correctly.
        val corrected = if (sessionStartTime != null && sessionStartTime < sorted.first().first) {
            listOf(sessionStartTime to 0.0) + sorted
        } else {
            sorted
        }

        // First pass: compute raw integrated distance to determine scale factor
        val scaleFactor = if (totalDistanceMeters != null && totalDistanceMeters > 0) {
            val rawTotal = integrateDistance(corrected)
            if (rawTotal > 0) totalDistanceMeters / rawTotal else 1.0
        } else {
            1.0
        }

        // Second pass: find km boundaries using scaled distances
        val splits = mutableListOf<PaceSplit>()
        var cumulativeDistance = 0.0
        var splitStartTime = corrected.first().first
        var currentKm = 1
        var cumulativeDuration = Duration.ZERO

        for (i in 0 until corrected.size - 1) {
            val (t0, _) = corrected[i]
            val (t1, v1) = corrected[i + 1]
            val intervalMs = Duration.between(t0, t1).toMillis()
            if (intervalMs <= 0) continue

            val intervalSeconds = intervalMs / 1000.0
            val avgSpeed = (corrected[i].second + v1) / 2.0
            val intervalDistance = avgSpeed * intervalSeconds * scaleFactor
            val prevCumulative = cumulativeDistance
            cumulativeDistance += intervalDistance

            while (cumulativeDistance >= currentKm * 1000.0) {
                val boundaryDistance = currentKm * 1000.0
                val distanceIntoBoundary = boundaryDistance - prevCumulative
                val fraction = if (intervalDistance > 0) {
                    distanceIntoBoundary / intervalDistance
                } else {
                    0.0
                }
                val boundaryTime = t0.plusMillis((intervalMs * fraction).toLong())

                val splitDuration = Duration.between(splitStartTime, boundaryTime)
                cumulativeDuration = cumulativeDuration.plus(splitDuration)
                val splitPace = splitDuration.toMillis() / 1000.0 // seconds for 1 km

                splits.add(
                    PaceSplit(
                        kilometerNumber = currentKm,
                        distanceMeters = 1000.0,
                        splitDuration = splitDuration,
                        splitPaceSecondsPerKm = splitPace,
                        cumulativeDuration = cumulativeDuration,
                        isPartial = false,
                    )
                )

                splitStartTime = boundaryTime
                currentKm++
            }
        }

        // Final partial split
        val remainingDistance = cumulativeDistance - (currentKm - 1) * 1000.0
        if (remainingDistance > 0.1) {
            val lastTime = corrected.last().first
            val splitDuration = Duration.between(splitStartTime, lastTime)
            cumulativeDuration = cumulativeDuration.plus(splitDuration)
            val splitPace = if (remainingDistance > 0) {
                splitDuration.toMillis() / 1000.0 / (remainingDistance / 1000.0)
            } else {
                0.0
            }

            splits.add(
                PaceSplit(
                    kilometerNumber = currentKm,
                    distanceMeters = remainingDistance,
                    splitDuration = splitDuration,
                    splitPaceSecondsPerKm = splitPace,
                    cumulativeDuration = cumulativeDuration,
                    isPartial = true,
                )
            )
        }

        return splits
    }

    /** Integrate speed samples to get total distance using trapezoidal rule. */
    private fun integrateDistance(sorted: List<Pair<Instant, Double>>): Double {
        var total = 0.0
        for (i in 0 until sorted.size - 1) {
            val (t0, v0) = sorted[i]
            val (t1, v1) = sorted[i + 1]
            val intervalSeconds = Duration.between(t0, t1).toMillis() / 1000.0
            if (intervalSeconds <= 0) continue
            total += (v0 + v1) / 2.0 * intervalSeconds
        }
        return total
    }
}
