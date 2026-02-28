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

        val scaleFactor = computeScaleFactor(corrected, totalDistanceMeters)

        return findKmSplits(corrected, scaleFactor)
    }

    private fun computeScaleFactor(
        corrected: List<Pair<Instant, Double>>,
        totalDistanceMeters: Double?,
    ): Double {
        if (totalDistanceMeters == null || totalDistanceMeters <= 0) return 1.0
        val rawTotal = integrateDistance(corrected)
        return if (rawTotal > 0) totalDistanceMeters / rawTotal else 1.0
    }

    private fun findKmSplits(
        corrected: List<Pair<Instant, Double>>,
        scaleFactor: Double,
    ): List<PaceSplit> {
        val splits = mutableListOf<PaceSplit>()
        var cumulativeDistance = 0.0
        var splitStartTime = corrected.first().first
        var currentKm = 1
        var cumulativeDuration = Duration.ZERO

        for (i in 0 until corrected.size - 1) {
            val (t0, v0) = corrected[i]
            val (t1, v1) = corrected[i + 1]
            val intervalMs = Duration.between(t0, t1).toMillis()
            if (intervalMs <= 0) continue

            val intervalDistance = (v0 + v1) / 2.0 * (intervalMs / 1000.0) * scaleFactor
            val prevCumulative = cumulativeDistance
            cumulativeDistance += intervalDistance

            while (cumulativeDistance >= currentKm * 1000.0) {
                val boundary = interpolateBoundaryTime(
                    t0, intervalMs, prevCumulative, intervalDistance,
                    boundaryDistance = currentKm * 1000.0,
                )
                val splitDuration = Duration.between(splitStartTime, boundary)
                cumulativeDuration += splitDuration

                splits += PaceSplit(
                    kilometerNumber = currentKm,
                    distanceMeters = 1000.0,
                    splitDuration = splitDuration,
                    splitPaceSecondsPerKm = splitDuration.toMillis() / 1000.0,
                    cumulativeDuration = cumulativeDuration,
                    isPartial = false,
                )

                splitStartTime = boundary
                currentKm++
            }
        }

        addPartialSplit(
            splits, corrected, cumulativeDistance, currentKm, splitStartTime, cumulativeDuration,
        )

        return splits
    }

    private fun interpolateBoundaryTime(
        intervalStart: Instant,
        intervalMs: Long,
        prevCumulative: Double,
        intervalDistance: Double,
        boundaryDistance: Double,
    ): Instant {
        val fraction = if (intervalDistance > 0) {
            (boundaryDistance - prevCumulative) / intervalDistance
        } else {
            0.0
        }
        return intervalStart.plusMillis((intervalMs * fraction).toLong())
    }

    private fun addPartialSplit(
        splits: MutableList<PaceSplit>,
        corrected: List<Pair<Instant, Double>>,
        cumulativeDistance: Double,
        currentKm: Int,
        splitStartTime: Instant,
        cumulativeDuration: Duration,
    ) {
        val remainingDistance = cumulativeDistance - (currentKm - 1) * 1000.0
        if (remainingDistance <= 0.1) return

        val lastTime = corrected.last().first
        val splitDuration = Duration.between(splitStartTime, lastTime)

        splits += PaceSplit(
            kilometerNumber = currentKm,
            distanceMeters = remainingDistance,
            splitDuration = splitDuration,
            splitPaceSecondsPerKm = splitDuration.toMillis() / 1000.0 / (remainingDistance / 1000.0),
            cumulativeDuration = cumulativeDuration + splitDuration,
            isPartial = true,
        )
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
