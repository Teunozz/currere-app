package nl.teunk.currere.domain.compute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PaceCalculatorTest {

    @Test
    fun `speedToPace converts normal running speed`() {
        // 3.33 m/s ≈ 5:00 min/km = 300 s/km
        val pace = PaceCalculator.speedToPace(3.333333)
        assertEquals(300.0, pace!!, 0.1)
    }

    @Test
    fun `speedToPace converts fast speed`() {
        // 5.0 m/s = 200 s/km ≈ 3:20 min/km
        val pace = PaceCalculator.speedToPace(5.0)
        assertEquals(200.0, pace!!, 0.01)
    }

    @Test
    fun `speedToPace returns null for zero speed`() {
        assertNull(PaceCalculator.speedToPace(0.0))
    }

    @Test
    fun `speedToPace returns null for negative speed`() {
        assertNull(PaceCalculator.speedToPace(-1.0))
    }

    @Test
    fun `averagePace computes correctly`() {
        // 30 minutes for 6 km = 300 s/km = 5:00 min/km
        val pace = PaceCalculator.averagePace(Duration.ofMinutes(30), 6000.0)
        assertEquals(300.0, pace!!, 0.01)
    }

    @Test
    fun `averagePace returns null for zero distance`() {
        assertNull(PaceCalculator.averagePace(Duration.ofMinutes(30), 0.0))
    }

    @Test
    fun `averagePace returns null for negative distance`() {
        assertNull(PaceCalculator.averagePace(Duration.ofMinutes(30), -100.0))
    }

    @Test
    fun `toPaceSamples filters out stationary samples`() {
        val now = Instant.now()
        val samples = listOf(
            now to 3.0,
            now.plusSeconds(10) to 0.0, // stationary — should be filtered
            now.plusSeconds(20) to 4.0,
        )
        val result = PaceCalculator.toPaceSamples(samples)
        assertEquals(2, result.size)
        assertEquals(now, result[0].time)
        assertEquals(now.plusSeconds(20), result[1].time)
    }

    @Test
    fun `toPaceSamples converts speeds correctly`() {
        val now = Instant.now()
        val samples = listOf(now to 5.0) // 5 m/s = 200 s/km
        val result = PaceCalculator.toPaceSamples(samples)
        assertEquals(200.0, result[0].secondsPerKm, 0.01)
    }
}
