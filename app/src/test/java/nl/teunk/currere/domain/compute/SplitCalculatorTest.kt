package nl.teunk.currere.domain.compute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SplitCalculatorTest {

    private val baseTime: Instant = Instant.parse("2025-06-21T07:00:00Z")

    /**
     * Generate speed samples at a constant speed over a given duration.
     * Samples are generated every [intervalSec] seconds.
     */
    private fun constantSpeedSamples(
        startOffsetSec: Long,
        metersPerSecond: Double,
        durationSec: Long,
        intervalSec: Long = 10,
    ): List<Pair<Instant, Double>> {
        val samples = mutableListOf<Pair<Instant, Double>>()
        var t = 0L
        while (t <= durationSec) {
            samples.add(baseTime.plusSeconds(startOffsetSec + t) to metersPerSecond)
            t += intervalSec
        }
        return samples
    }

    @Test
    fun `exact 5km run produces 5 full splits`() {
        // 5.0 m/s for 1000s = 5000m, each 10s interval = 50m, 20 intervals = 1km
        val samples = constantSpeedSamples(0, 5.0, 1000)
        val splits = SplitCalculator.computeSplits(samples)

        assertEquals(5, splits.size)
        splits.forEach { split ->
            assertFalse(split.isPartial)
            assertEquals(1000.0, split.distanceMeters, 0.01)
            assertEquals(200.0, split.splitPaceSecondsPerKm, 1.0)
        }
        assertEquals(1, splits[0].kilometerNumber)
        assertEquals(5, splits[4].kilometerNumber)
    }

    @Test
    fun `varying speed produces different split paces`() {
        // km1 at 5.0 m/s (200s/km), km2 at 4.0 m/s (250s/km)
        val samples1 = constantSpeedSamples(0, 5.0, 200)
        val samples2 = constantSpeedSamples(200, 4.0, 250)
        val splits = SplitCalculator.computeSplits(samples1 + samples2)

        assertEquals(2, splits.size)
        assertEquals(200.0, splits[0].splitPaceSecondsPerKm, 2.0)
        assertEquals(250.0, splits[1].splitPaceSecondsPerKm, 2.0)
        assertTrue(
            "Splits should have different paces",
            kotlin.math.abs(splits[0].splitPaceSecondsPerKm - splits[1].splitPaceSecondsPerKm) > 10,
        )
    }

    @Test
    fun `sub-1km run produces single partial split`() {
        // 5.0 m/s for 60s = 300m
        val samples = constantSpeedSamples(0, 5.0, 60)
        val splits = SplitCalculator.computeSplits(samples)

        assertEquals(1, splits.size)
        assertTrue(splits[0].isPartial)
        assertEquals(1, splits[0].kilometerNumber)
        assertEquals(300.0, splits[0].distanceMeters, 1.0)
    }

    @Test
    fun `empty samples return empty splits`() {
        assertEquals(0, SplitCalculator.computeSplits(emptyList()).size)
    }

    @Test
    fun `single sample returns empty splits`() {
        val samples = listOf(baseTime to 3.5)
        assertEquals(0, SplitCalculator.computeSplits(samples).size)
    }

    @Test
    fun `splits cumulative duration increases`() {
        val samples = constantSpeedSamples(0, 5.0, 600)
        val splits = SplitCalculator.computeSplits(samples)

        assertTrue(splits.size >= 2)
        for (i in 1 until splits.size) {
            assertTrue(splits[i].cumulativeDuration > splits[i - 1].cumulativeDuration)
        }
    }

    @Test
    fun `no negative pace values`() {
        val samples = constantSpeedSamples(0, 5.0, 3000)
        val splits = SplitCalculator.computeSplits(samples)

        assertTrue(splits.size >= 15)
        splits.forEach { split ->
            assertTrue(
                "Pace should be positive but was ${split.splitPaceSecondsPerKm}",
                split.splitPaceSecondsPerKm > 0,
            )
        }
    }

    @Test
    fun `15km with partial produces 15 full splits plus 1 partial`() {
        // 5.0 m/s for 3010s = 15050m (301 intervals of 50m each)
        val samples = constantSpeedSamples(0, 5.0, 3010)
        val splits = SplitCalculator.computeSplits(samples)

        assertEquals(16, splits.size)
        for (i in 0 until 15) {
            assertFalse(splits[i].isPartial)
            assertEquals(i + 1, splits[i].kilometerNumber)
        }
        assertTrue(splits[15].isPartial)
        assertEquals(16, splits[15].kilometerNumber)
    }

    @Test
    fun `exact km boundary produces no partial split`() {
        // 5.0 m/s for 400s = exactly 2000m
        val samples = constantSpeedSamples(0, 5.0, 400)
        val splits = SplitCalculator.computeSplits(samples)

        assertEquals(2, splits.size)
        assertFalse(splits[0].isPartial)
        assertFalse(splits[1].isPartial)
    }
}
