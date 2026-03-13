package nl.teunk.currere.data.db

import nl.teunk.currere.domain.model.HeartRateSample
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HeartRateSampleEntityTest {

    @Test
    fun `toDomain maps fields correctly`() {
        val entity = HeartRateSampleEntity(
            id = 1,
            sessionId = "s1",
            timeEpochMillis = 1704067200000L,
            bpm = 145,
        )
        val domain = entity.toDomain()
        assertEquals(Instant.ofEpochMilli(1704067200000L), domain.time)
        assertEquals(145L, domain.bpm)
    }

    @Test
    fun `fromDomain maps fields correctly`() {
        val sample = HeartRateSample(
            time = Instant.ofEpochMilli(1704067200000L),
            bpm = 145,
        )
        val entity = HeartRateSampleEntity.fromDomain("s1", sample)
        assertEquals("s1", entity.sessionId)
        assertEquals(1704067200000L, entity.timeEpochMillis)
        assertEquals(145L, entity.bpm)
        assertEquals(0L, entity.id)
    }

    @Test
    fun `round trip preserves data`() {
        val original = HeartRateSample(
            time = Instant.ofEpochMilli(1704067200000L),
            bpm = 160,
        )
        val roundTripped = HeartRateSampleEntity.fromDomain("s1", original).toDomain()
        assertEquals(original, roundTripped)
    }
}
