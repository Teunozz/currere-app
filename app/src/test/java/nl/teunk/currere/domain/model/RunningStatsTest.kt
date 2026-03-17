package nl.teunk.currere.domain.model

import nl.teunk.currere.data.db.RunningStatsRaw
import org.junit.Assert.assertEquals
import org.junit.Test

class RunningStatsTest {

    @Test
    fun `toFormatted formats normal values correctly`() {
        val raw = RunningStatsRaw(
            avgDistance = 7800.0,
            maxDistance = 15012.34,
            totalDistance = 234500.0,
            avgPace = 312.0,
            fastestPace = 255.0,
            avgHeartRate = 152.0,
            highestHeartRate = 189,
            totalRuns = 30,
            totalDurationSeconds = 100000,
        )
        val stats = raw.toFormatted()

        assertEquals("7.80", stats.avgDistanceKm)
        assertEquals("15.01", stats.longestDistanceKm)
        assertEquals("234.50", stats.totalDistanceKm)
        assertEquals("5:12", stats.avgPace)
        assertEquals("4:15", stats.fastestPace)
        assertEquals("152", stats.avgHeartRate)
        assertEquals("189", stats.highestHeartRate)
        assertEquals("30", stats.totalRuns)
        assertEquals("1d 3h 46m", stats.totalTime)
    }

    @Test
    fun `toFormatted shows dash for null pace and heart rate`() {
        val raw = RunningStatsRaw(
            avgDistance = 5000.0,
            maxDistance = 5000.0,
            totalDistance = 5000.0,
            avgPace = null,
            fastestPace = null,
            avgHeartRate = null,
            highestHeartRate = null,
            totalRuns = 1,
            totalDurationSeconds = 1800,
        )
        val stats = raw.toFormatted()

        assertEquals("\u2014", stats.avgPace)
        assertEquals("\u2014", stats.fastestPace)
        assertEquals("\u2014", stats.avgHeartRate)
        assertEquals("\u2014", stats.highestHeartRate)
    }

    @Test
    fun `toFormatted handles zero total duration`() {
        val raw = RunningStatsRaw(
            avgDistance = 0.0,
            maxDistance = 0.0,
            totalDistance = 0.0,
            avgPace = null,
            fastestPace = null,
            avgHeartRate = null,
            highestHeartRate = null,
            totalRuns = 0,
            totalDurationSeconds = 0,
        )
        val stats = raw.toFormatted()

        assertEquals("0.00", stats.avgDistanceKm)
        assertEquals("0", stats.totalRuns)
        assertEquals("0m", stats.totalTime)
    }
}
