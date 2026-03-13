package nl.teunk.currere.data.db

import nl.teunk.currere.domain.model.PaceSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class PaceSplitEntityTest {

    @Test
    fun `toDomain maps fields correctly`() {
        val entity = PaceSplitEntity(
            id = 1,
            sessionId = "s1",
            kilometerNumber = 1,
            distanceMeters = 1000.0,
            splitDurationMs = 360000L,
            splitPaceSecondsPerKm = 360.0,
            cumulativeDurationMs = 360000L,
            isPartial = false,
        )
        val domain = entity.toDomain()
        assertEquals(1, domain.kilometerNumber)
        assertEquals(1000.0, domain.distanceMeters, 0.001)
        assertEquals(Duration.ofMillis(360000L), domain.splitDuration)
        assertEquals(360.0, domain.splitPaceSecondsPerKm, 0.001)
        assertEquals(Duration.ofMillis(360000L), domain.cumulativeDuration)
        assertFalse(domain.isPartial)
    }

    @Test
    fun `fromDomain maps fields correctly`() {
        val split = PaceSplit(
            kilometerNumber = 2,
            distanceMeters = 500.0,
            splitDuration = Duration.ofSeconds(180),
            splitPaceSecondsPerKm = 360.0,
            cumulativeDuration = Duration.ofSeconds(540),
            isPartial = true,
        )
        val entity = PaceSplitEntity.fromDomain("s1", split)
        assertEquals("s1", entity.sessionId)
        assertEquals(2, entity.kilometerNumber)
        assertEquals(500.0, entity.distanceMeters, 0.001)
        assertEquals(180000L, entity.splitDurationMs)
        assertEquals(360.0, entity.splitPaceSecondsPerKm, 0.001)
        assertEquals(540000L, entity.cumulativeDurationMs)
        assertTrue(entity.isPartial)
    }

    @Test
    fun `round trip preserves data`() {
        val original = PaceSplit(
            kilometerNumber = 1,
            distanceMeters = 1000.0,
            splitDuration = Duration.ofSeconds(360),
            splitPaceSecondsPerKm = 360.0,
            cumulativeDuration = Duration.ofSeconds(360),
            isPartial = false,
        )
        val roundTripped = PaceSplitEntity.fromDomain("s1", original).toDomain()
        assertEquals(original, roundTripped)
    }
}
