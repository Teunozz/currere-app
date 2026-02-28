package nl.teunk.currere.data.db

import nl.teunk.currere.domain.model.RunSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RunSessionEntityTest {

    private val sampleEntity = RunSessionEntity(
        id = "session-123",
        startTimeEpochMillis = 1704067200000L, // 2024-01-01T08:00:00Z
        endTimeEpochMillis = 1704069000000L,   // 2024-01-01T08:30:00Z
        distanceMeters = 5000.0,
        activeDurationSeconds = 1800L,
        averagePaceSecondsPerKm = 360.0,
        averageHeartRateBpm = 155L,
        title = "Morning run",
    )

    private val sampleDomain = RunSession(
        id = "session-123",
        startTime = Instant.ofEpochMilli(1704067200000L),
        endTime = Instant.ofEpochMilli(1704069000000L),
        distanceMeters = 5000.0,
        activeDuration = Duration.ofSeconds(1800L),
        averagePaceSecondsPerKm = 360.0,
        averageHeartRateBpm = 155L,
        title = "Morning run",
    )

    @Test
    fun `toDomain maps epoch millis to Instant`() {
        val domain = sampleEntity.toDomain()
        assertEquals(Instant.ofEpochMilli(1704067200000L), domain.startTime)
        assertEquals(Instant.ofEpochMilli(1704069000000L), domain.endTime)
    }

    @Test
    fun `toDomain maps seconds to Duration`() {
        val domain = sampleEntity.toDomain()
        assertEquals(Duration.ofSeconds(1800L), domain.activeDuration)
    }

    @Test
    fun `toDomain preserves all fields`() {
        val domain = sampleEntity.toDomain()
        assertEquals("session-123", domain.id)
        assertEquals(5000.0, domain.distanceMeters, 0.001)
        assertEquals(360.0, domain.averagePaceSecondsPerKm!!, 0.001)
        assertEquals(155L, domain.averageHeartRateBpm)
        assertEquals("Morning run", domain.title)
    }

    @Test
    fun `fromDomain maps Instant to epoch millis`() {
        val entity = RunSessionEntity.fromDomain(sampleDomain)
        assertEquals(1704067200000L, entity.startTimeEpochMillis)
        assertEquals(1704069000000L, entity.endTimeEpochMillis)
    }

    @Test
    fun `round trip entity to domain and back preserves all fields`() {
        val domain = sampleEntity.toDomain()
        val backToEntity = RunSessionEntity.fromDomain(domain)
        assertEquals(sampleEntity, backToEntity)
    }

    @Test
    fun `nullable fields preserved through toDomain and fromDomain`() {
        val entityWithNulls = sampleEntity.copy(
            averagePaceSecondsPerKm = null,
            averageHeartRateBpm = null,
        )
        val domain = entityWithNulls.toDomain()
        assertNull(domain.averagePaceSecondsPerKm)
        assertNull(domain.averageHeartRateBpm)

        val backToEntity = RunSessionEntity.fromDomain(domain)
        assertNull(backToEntity.averagePaceSecondsPerKm)
        assertNull(backToEntity.averageHeartRateBpm)
    }
}
