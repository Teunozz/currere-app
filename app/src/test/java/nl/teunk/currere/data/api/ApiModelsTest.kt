package nl.teunk.currere.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // region QrPayload

    @Test
    fun `QrPayload serializes base_url with snake_case`() {
        val payload = QrPayload(token = "abc123", baseUrl = "https://example.com")
        val encoded = json.encodeToString(payload)
        assert(encoded.contains("\"base_url\"")) { "Expected snake_case key 'base_url' but got: $encoded" }
        assert(!encoded.contains("\"baseUrl\"")) { "Should not contain camelCase key 'baseUrl'" }
    }

    @Test
    fun `QrPayload round trip preserves values`() {
        val original = QrPayload(token = "tok_abc", baseUrl = "https://api.example.com")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<QrPayload>(encoded)
        assertEquals(original, decoded)
    }

    // endregion

    // region RunRequest

    @Test
    fun `RunRequest serializes with correct snake_case keys`() {
        val request = RunRequest(
            startTime = "2024-01-01T10:00:00Z",
            endTime = "2024-01-01T10:30:00Z",
            distanceKm = 5.0,
            durationSeconds = 1800,
        )
        val encoded = json.encodeToString(request)
        assert(encoded.contains("\"start_time\""))
        assert(encoded.contains("\"end_time\""))
        assert(encoded.contains("\"distance_km\""))
        assert(encoded.contains("\"duration_seconds\""))
    }

    @Test
    fun `RunRequest encodes defaults for nullable fields`() {
        val request = RunRequest(
            startTime = "2024-01-01T10:00:00Z",
            endTime = "2024-01-01T10:30:00Z",
            distanceKm = 5.0,
            durationSeconds = 1800,
        )
        val encoded = json.encodeToString(request)
        // encodeDefaults = true means null fields are included
        assert(encoded.contains("\"steps\""))
        assert(encoded.contains("\"avg_heart_rate\""))
    }

    @Test
    fun `RunRequest round trip with all fields`() {
        val original = RunRequest(
            startTime = "2024-01-01T10:00:00Z",
            endTime = "2024-01-01T10:30:00Z",
            distanceKm = 5.123,
            durationSeconds = 1800,
            steps = 5000,
            avgHeartRate = 145,
            avgPaceSecondsPerKm = 300,
            heartRateSamples = listOf(
                HeartRateSampleRequest(timestamp = "2024-01-01T10:00:00Z", bpm = 120),
            ),
            paceSplits = listOf(
                PaceSplitRequest(
                    kilometerNumber = 1,
                    splitTimeSeconds = 300,
                    paceSecondsPerKm = 300,
                    isPartial = false,
                ),
            ),
        )
        val decoded = json.decodeFromString<RunRequest>(json.encodeToString(original))
        assertEquals(original, decoded)
    }

    // endregion

    // region BatchRunRequest

    @Test
    fun `BatchRunRequest round trip`() {
        val original = BatchRunRequest(
            runs = listOf(
                RunRequest("2024-01-01T10:00:00Z", "2024-01-01T10:30:00Z", 5.0, 1800),
                RunRequest("2024-01-02T10:00:00Z", "2024-01-02T10:45:00Z", 7.5, 2700),
            ),
        )
        val decoded = json.decodeFromString<BatchRunRequest>(json.encodeToString(original))
        assertEquals(original, decoded)
    }

    // endregion

    // region Response DTOs

    @Test
    fun `PingResponse deserializes correctly`() {
        val jsonStr = """{"status":"ok"}"""
        val response = json.decodeFromString<PingResponse>(jsonStr)
        assertEquals("ok", response.status)
    }

    @Test
    fun `BatchRunResponseData deserializes with defaults`() {
        val jsonStr = """{"created":2,"skipped":1,"results":[{"index":0,"status":"created","id":10},{"index":1,"status":"skipped","id":11},{"index":2,"status":"created","id":12}]}"""
        val data = json.decodeFromString<BatchRunResponseData>(jsonStr)
        assertEquals(2, data.created)
        assertEquals(1, data.skipped)
        assertEquals(3, data.results.size)
        assertEquals("created", data.results[0].status)
        assertEquals(10L, data.results[0].id)
    }

    @Test
    fun `BatchRunResponseData handles empty defaults`() {
        val jsonStr = """{}"""
        val data = json.decodeFromString<BatchRunResponseData>(jsonStr)
        assertEquals(0, data.created)
        assertEquals(0, data.skipped)
        assertEquals(emptyList<BatchResultItem>(), data.results)
    }

    // endregion

    // region PaceSplitRequest

    @Test
    fun `PaceSplitRequest serializes partial_distance_km only when present`() {
        val full = PaceSplitRequest(
            kilometerNumber = 1,
            splitTimeSeconds = 300,
            paceSecondsPerKm = 300,
            isPartial = false,
        )
        val partial = PaceSplitRequest(
            kilometerNumber = 6,
            splitTimeSeconds = 150,
            paceSecondsPerKm = 310,
            isPartial = true,
            partialDistanceKm = 0.48,
        )

        val fullJson = json.encodeToString(full)
        val partialJson = json.encodeToString(partial)

        // With encodeDefaults=true, null is encoded
        assert(fullJson.contains("\"partial_distance_km\":null"))
        assert(partialJson.contains("\"partial_distance_km\":0.48"))
    }

    // endregion
}
