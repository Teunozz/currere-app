package nl.teunk.currere.domain.compute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SplitCalculatorTest {

    private val baseTime: Instant = Instant.parse("2025-06-21T07:00:00Z")

    private fun segment(startOffsetSec: Long, endOffsetSec: Long, meters: Double) =
        DistanceSegment(
            startTime = baseTime.plusSeconds(startOffsetSec),
            endTime = baseTime.plusSeconds(endOffsetSec),
            distanceMeters = meters,
        )

    @Test
    fun `exact 5km run produces 5 full splits`() {
        // 5 segments of 1000m each, 5 minutes per km
        val segments = (0 until 5).map { i ->
            segment(i * 300L, (i + 1) * 300L, 1000.0)
        }
        val splits = SplitCalculator.computeSplits(segments)

        assertEquals(5, splits.size)
        splits.forEach { split ->
            assertFalse(split.isPartial)
            assertEquals(1000.0, split.distanceMeters, 0.01)
            assertEquals(300.0, split.splitPaceSecondsPerKm, 1.0)
        }
        assertEquals(1, splits[0].kilometerNumber)
        assertEquals(5, splits[4].kilometerNumber)
    }

    @Test
    fun `15_01km run produces 15 full splits plus 1 partial`() {
        // 15 full km + 10m partial, 5 min/km pace
        val segments = (0 until 15).map { i ->
            segment(i * 300L, (i + 1) * 300L, 1000.0)
        } + segment(4500L, 4503L, 10.0) // 10m in 3 seconds

        val splits = SplitCalculator.computeSplits(segments)

        assertEquals(16, splits.size)
        // First 15 should be full
        for (i in 0 until 15) {
            assertFalse(splits[i].isPartial)
            assertEquals(i + 1, splits[i].kilometerNumber)
        }
        // Last one is partial
        assertTrue(splits[15].isPartial)
        assertEquals(16, splits[15].kilometerNumber)
        assertEquals(10.0, splits[15].distanceMeters, 0.5)
    }

    @Test
    fun `sub-1km run produces single partial split`() {
        val segments = listOf(segment(0, 180, 500.0)) // 500m in 3 minutes
        val splits = SplitCalculator.computeSplits(segments)

        assertEquals(1, splits.size)
        assertTrue(splits[0].isPartial)
        assertEquals(1, splits[0].kilometerNumber)
        assertEquals(500.0, splits[0].distanceMeters, 0.01)
    }

    @Test
    fun `exact km boundary produces no partial split`() {
        val segments = listOf(
            segment(0, 300, 1000.0),
            segment(300, 600, 1000.0),
        )
        val splits = SplitCalculator.computeSplits(segments)

        assertEquals(2, splits.size)
        assertFalse(splits[0].isPartial)
        assertFalse(splits[1].isPartial)
    }

    @Test
    fun `empty segments return empty splits`() {
        assertEquals(0, SplitCalculator.computeSplits(emptyList()).size)
    }

    @Test
    fun `splits cumulative duration increases`() {
        val segments = (0 until 3).map { i ->
            segment(i * 300L, (i + 1) * 300L, 1000.0)
        }
        val splits = SplitCalculator.computeSplits(segments)

        for (i in 1 until splits.size) {
            assertTrue(splits[i].cumulativeDuration > splits[i - 1].cumulativeDuration)
        }
    }

    @Test
    fun `split distances sum to total distance`() {
        val segments = listOf(
            segment(0, 300, 1000.0),
            segment(300, 600, 1000.0),
            segment(600, 750, 500.0),
        )
        val splits = SplitCalculator.computeSplits(segments)
        val totalSplitDistance = splits.sumOf { it.distanceMeters }
        val totalSegmentDistance = segments.sumOf { it.distanceMeters }

        assertEquals(totalSegmentDistance, totalSplitDistance, 1.0)
    }

    @Test
    fun `duplicate overlapping segments are deduplicated`() {
        // Simulate two data sources recording the same 5km run
        val sourceA = (0 until 5).map { i ->
            segment(i * 300L, (i + 1) * 300L, 1000.0)
        }
        val sourceB = (0 until 5).map { i ->
            segment(i * 300L, (i + 1) * 300L, 1000.0)
        }
        val segments = sourceA + sourceB

        val splits = SplitCalculator.computeSplits(segments)

        // Should produce 5 splits, not 10
        assertEquals(5, splits.size)
        splits.forEach { split ->
            assertFalse(split.isPartial)
            assertEquals(1000.0, split.distanceMeters, 0.01)
            assertEquals(300.0, split.splitPaceSecondsPerKm, 1.0)
        }
    }

    @Test
    fun `partially overlapping segments are deduplicated`() {
        // Source A has coarser segments, source B has finer segments in the same window
        val segments = listOf(
            segment(0, 300, 1000.0),    // Source A: 0-300s
            segment(0, 150, 500.0),     // Source B: 0-150s (overlaps with A)
            segment(150, 300, 500.0),   // Source B: 150-300s (overlaps with A)
            segment(300, 600, 1000.0),  // Source A: 300-600s
            segment(300, 450, 500.0),   // Source B: 300-450s (overlaps with A)
            segment(450, 600, 500.0),   // Source B: 450-600s (overlaps with A)
        )

        val splits = SplitCalculator.computeSplits(segments)

        // Should produce 2 full splits from source A's data only
        assertEquals(2, splits.size)
        splits.forEach { split ->
            assertFalse(split.isPartial)
            assertEquals(1000.0, split.distanceMeters, 0.01)
        }
    }

    @Test
    fun `no negative pace values with overlapping segments`() {
        // Simulate the exact scenario from the bug: two sources for a 15km run
        val sourceA = (0 until 15).map { i ->
            segment(i * 312L, (i + 1) * 312L, 1000.0)
        }
        val sourceB = (0 until 15).map { i ->
            segment(i * 312L, (i + 1) * 312L, 1000.0)
        }
        val segments = sourceA + sourceB

        val splits = SplitCalculator.computeSplits(segments)

        assertEquals(15, splits.size)
        splits.forEach { split ->
            assertTrue(
                "Pace should be positive but was ${split.splitPaceSecondsPerKm}",
                split.splitPaceSecondsPerKm > 0,
            )
        }
    }
}
