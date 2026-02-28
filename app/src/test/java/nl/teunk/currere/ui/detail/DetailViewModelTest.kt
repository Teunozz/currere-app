package nl.teunk.currere.ui.detail

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val healthConnectSource = mockk<HealthConnectSource>()

    private val sessionId = "session-123"
    private val startTime = Instant.parse("2024-01-01T10:00:00Z")
    private val endTime = Instant.parse("2024-01-01T10:30:00Z")

    private val sampleDetail = RunDetail(
        session = RunSession(
            id = sessionId,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = 5000.0,
            activeDuration = Duration.ofSeconds(1800),
            averagePaceSecondsPerKm = 360.0,
            averageHeartRateBpm = 150,
            title = "Morning run",
        ),
        totalSteps = 5000,
        heartRateSamples = listOf(
            HeartRateSample(time = startTime, bpm = 120),
        ),
        paceSamples = emptyList(),
        splits = emptyList(),
    )

    @Test
    fun `emits Success when loadRunDetail succeeds`() {
        coEvery { healthConnectSource.loadRunDetail(sessionId, startTime, endTime) } returns sampleDetail

        val viewModel = DetailViewModel(healthConnectSource, sessionId, startTime, endTime)

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        assertEquals(sessionId, (state as DetailUiState.Success).detail.session.id)
    }

    @Test
    fun `emits Error when loadRunDetail throws`() {
        coEvery { healthConnectSource.loadRunDetail(sessionId, startTime, endTime) } throws
            RuntimeException("Health Connect unavailable")

        val viewModel = DetailViewModel(healthConnectSource, sessionId, startTime, endTime)

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Error)
        assertEquals("Health Connect unavailable", (state as DetailUiState.Error).message)
    }

    @Test
    fun `emits Error with default message when exception has no message`() {
        coEvery { healthConnectSource.loadRunDetail(sessionId, startTime, endTime) } throws
            RuntimeException()

        val viewModel = DetailViewModel(healthConnectSource, sessionId, startTime, endTime)

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Error)
        assertEquals("Failed to load run detail", (state as DetailUiState.Error).message)
    }
}
