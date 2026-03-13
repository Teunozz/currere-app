package nl.teunk.currere.data.db

import nl.teunk.currere.domain.model.PaceSample
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PaceSampleEntityTest {

    @Test
    fun `toDomain maps fields correctly`() {
        val entity = PaceSampleEntity(
            id = 1,
            sessionId = "s1",
            timeEpochMillis = 1704067200000L,
            secondsPerKm = 350.5,
        )
        val domain = entity.toDomain()
        assertEquals(Instant.ofEpochMilli(1704067200000L), domain.time)
        assertEquals(350.5, domain.secondsPerKm, 0.001)
    }

    @Test
    fun `fromDomain maps fields correctly`() {
        val sample = PaceSample(
            time = Instant.ofEpochMilli(1704067200000L),
            secondsPerKm = 350.5,
        )
        val entity = PaceSampleEntity.fromDomain("s1", sample)
        assertEquals("s1", entity.sessionId)
        assertEquals(1704067200000L, entity.timeEpochMillis)
        assertEquals(350.5, entity.secondsPerKm, 0.001)
    }

    @Test
    fun `round trip preserves data`() {
        val original = PaceSample(
            time = Instant.ofEpochMilli(1704067200000L),
            secondsPerKm = 360.0,
        )
        val roundTripped = PaceSampleEntity.fromDomain("s1", original).toDomain()
        assertEquals(original, roundTripped)
    }
}
