package nl.teunk.currere.domain.compute

import nl.teunk.currere.domain.model.PaceSplit
import java.time.Duration
import java.time.Instant

/**
 * A distance segment from Health Connect, representing incremental distance
 * covered between startTime and endTime.
 */
data class DistanceSegment(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
)

object SplitCalculator {

    /**
     * Compute per-km splits from sorted incremental distance segments.
     *
     * Walks through segments accumulating distance. When cumulative distance
     * crosses a km boundary, interpolates the crossing time and records the split.
     * The final partial split (if any) covers remaining distance after the last full km.
     */
    fun computeSplits(segments: List<DistanceSegment>): List<PaceSplit> {
        if (segments.isEmpty()) return emptyList()

        val sorted = segments.sortedBy { it.startTime }
        val splits = mutableListOf<PaceSplit>()

        var cumulativeDistance = 0.0
        var splitStartTime = sorted.first().startTime
        var splitStartDistance = 0.0
        var currentKm = 1
        var cumulativeDuration = Duration.ZERO

        for (segment in sorted) {
            val segmentDurationMs = Duration.between(segment.startTime, segment.endTime).toMillis()
            val prevCumulative = cumulativeDistance
            cumulativeDistance += segment.distanceMeters

            // Check if this segment crosses one or more km boundaries
            while (cumulativeDistance >= currentKm * 1000.0) {
                val boundaryDistance = currentKm * 1000.0
                val distanceIntoSegment = boundaryDistance - prevCumulative
                val fraction = if (segment.distanceMeters > 0) {
                    distanceIntoSegment / segment.distanceMeters
                } else {
                    0.0
                }
                val boundaryTime = segment.startTime.plusMillis((segmentDurationMs * fraction).toLong())

                val splitDuration = Duration.between(splitStartTime, boundaryTime)
                cumulativeDuration = cumulativeDuration.plus(splitDuration)
                val splitDistance = 1000.0
                val splitPace = if (splitDistance > 0) {
                    splitDuration.toMillis() / 1000.0 / (splitDistance / 1000.0)
                } else {
                    0.0
                }

                splits.add(
                    PaceSplit(
                        kilometerNumber = currentKm,
                        distanceMeters = splitDistance,
                        splitDuration = splitDuration,
                        splitPaceSecondsPerKm = splitPace,
                        cumulativeDuration = cumulativeDuration,
                        isPartial = false,
                    )
                )

                splitStartTime = boundaryTime
                splitStartDistance = boundaryDistance
                currentKm++
            }
        }

        // Final partial split (remaining distance after last full km)
        val remainingDistance = cumulativeDistance - (currentKm - 1) * 1000.0
        if (remainingDistance > 0.1) { // Threshold to avoid floating point noise
            val lastSegmentEnd = sorted.last().endTime
            val splitDuration = Duration.between(splitStartTime, lastSegmentEnd)
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
}
