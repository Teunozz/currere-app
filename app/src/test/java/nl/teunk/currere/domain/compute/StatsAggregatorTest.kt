package nl.teunk.currere.domain.compute

import nl.teunk.currere.domain.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class StatsAggregatorTest {

    private val utc = ZoneOffset.UTC

    @Test
    fun `timeOfDay morning at 05_00`() {
        val instant = Instant.parse("2025-06-21T05:00:00Z")
        assertEquals(TimeOfDay.MORNING, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay morning at 11_59`() {
        val instant = Instant.parse("2025-06-21T11:59:00Z")
        assertEquals(TimeOfDay.MORNING, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay afternoon at 12_00`() {
        val instant = Instant.parse("2025-06-21T12:00:00Z")
        assertEquals(TimeOfDay.AFTERNOON, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay afternoon at 16_59`() {
        val instant = Instant.parse("2025-06-21T16:59:00Z")
        assertEquals(TimeOfDay.AFTERNOON, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay evening at 17_00`() {
        val instant = Instant.parse("2025-06-21T17:00:00Z")
        assertEquals(TimeOfDay.EVENING, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay evening at 20_59`() {
        val instant = Instant.parse("2025-06-21T20:59:00Z")
        assertEquals(TimeOfDay.EVENING, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay night at 21_00`() {
        val instant = Instant.parse("2025-06-21T21:00:00Z")
        assertEquals(TimeOfDay.NIGHT, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay night at 04_59`() {
        val instant = Instant.parse("2025-06-21T04:59:00Z")
        assertEquals(TimeOfDay.NIGHT, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `timeOfDay night at midnight`() {
        val instant = Instant.parse("2025-06-21T00:00:00Z")
        assertEquals(TimeOfDay.NIGHT, StatsAggregator.timeOfDay(instant, utc))
    }

    @Test
    fun `formatDuration under 1 hour`() {
        assertEquals("59:30", StatsAggregator.formatDuration(Duration.ofSeconds(59 * 60 + 30)))
    }

    @Test
    fun `formatDuration over 1 hour`() {
        assertEquals("1:05:30", StatsAggregator.formatDuration(Duration.ofSeconds(3600 + 5 * 60 + 30)))
    }

    @Test
    fun `formatDuration zero`() {
        assertEquals("0:00", StatsAggregator.formatDuration(Duration.ZERO))
    }

    @Test
    fun `formatDuration exactly 1 hour`() {
        assertEquals("1:00:00", StatsAggregator.formatDuration(Duration.ofHours(1)))
    }

    @Test
    fun `formatDistanceKm normal`() {
        assertEquals("15.01", StatsAggregator.formatDistanceKm(15012.34))
    }

    @Test
    fun `formatDistanceKm sub-km`() {
        assertEquals("0.50", StatsAggregator.formatDistanceKm(500.0))
    }

    @Test
    fun `formatPace 5 min per km`() {
        assertEquals("5:00", StatsAggregator.formatPace(300.0))
    }

    @Test
    fun `formatPace 5 min 25 sec per km`() {
        assertEquals("5:25", StatsAggregator.formatPace(325.0))
    }

    @Test
    fun `formatPace sub-minute pace`() {
        assertEquals("0:45", StatsAggregator.formatPace(45.0))
    }

    @Test
    fun `activityTitle delegates to timeOfDay`() {
        val morning = Instant.parse("2025-06-21T07:30:00Z")
        assertEquals("Morning run", StatsAggregator.activityTitle(morning, utc))
    }
}
