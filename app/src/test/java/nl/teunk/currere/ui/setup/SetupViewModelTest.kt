package nl.teunk.currere.ui.setup

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.teunk.currere.data.api.ApiClient
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.sync.SyncWorker
import nl.teunk.currere.util.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiClient = mockk<ApiClient>()
    private val credentialsManager = mockk<CredentialsManager>(relaxed = true)
    private val appContext = mockk<Context>()

    @Before
    fun setUp() {
        mockkObject(SyncWorker.Companion)
        every { SyncWorker.enqueueOneTime(any()) } returns Unit
        every { SyncWorker.schedulePeriodicSync(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(SyncWorker.Companion)
    }

    private fun createViewModel() = SetupViewModel(apiClient, credentialsManager, appContext)

    @Test
    fun `initial state is Idle`() {
        val viewModel = createViewModel()
        assertEquals(SetupState.Idle, viewModel.state.value)
    }

    @Test
    fun `connectWithCredentials with blank url returns Error`() {
        val viewModel = createViewModel()

        viewModel.connectWithCredentials("", "some-token")

        assertTrue(viewModel.state.value is SetupState.Error)
        assertEquals(
            "Server URL and token are required",
            (viewModel.state.value as SetupState.Error).message,
        )
    }

    @Test
    fun `connectWithCredentials with blank token returns Error`() {
        val viewModel = createViewModel()

        viewModel.connectWithCredentials("https://example.com", "  ")

        assertTrue(viewModel.state.value is SetupState.Error)
    }

    @Test
    fun `connectWithCredentials success saves credentials and schedules sync`() {
        coEvery { apiClient.testConnection(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.connectWithCredentials("https://example.com", "my-token")

        assertEquals(SetupState.Success, viewModel.state.value)
        coVerify { credentialsManager.save("https://example.com", "my-token") }
        coVerify { SyncWorker.schedulePeriodicSync(appContext) }
        coVerify { SyncWorker.enqueueOneTime(appContext) }
    }

    @Test
    fun `connectWithCredentials failure sets Error state`() {
        coEvery { apiClient.testConnection(any(), any()) } returns
            Result.failure(Exception("Authentication failed"))

        val viewModel = createViewModel()
        viewModel.connectWithCredentials("https://example.com", "bad-token")

        assertTrue(viewModel.state.value is SetupState.Error)
        assertEquals("Authentication failed", (viewModel.state.value as SetupState.Error).message)
    }

    @Test
    fun `resetState returns to Idle`() {
        coEvery { apiClient.testConnection(any(), any()) } returns
            Result.failure(Exception("fail"))

        val viewModel = createViewModel()
        viewModel.connectWithCredentials("https://example.com", "token")
        assertTrue(viewModel.state.value is SetupState.Error)

        viewModel.resetState()
        assertEquals(SetupState.Idle, viewModel.state.value)
    }
}
