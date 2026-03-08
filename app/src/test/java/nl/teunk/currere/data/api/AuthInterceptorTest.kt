package nl.teunk.currere.data.api

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.credentials.ServerCredentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private val credentialsManager = mockk<CredentialsManager>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(credentialsManager))
            .build()
    }

    @Test
    fun `adds Accept json header always`() {
        every { credentialsManager.credentials } returns flowOf(null)
        server.enqueue(MockResponse(body = "{}"))

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("application/json", recorded.headers["Accept"])
    }

    @Test
    fun `adds Authorization header when credentials present`() {
        val credentials = ServerCredentials(baseUrl = "https://example.com", token = "my-secret-token")
        every { credentialsManager.credentials } returns flowOf(credentials)
        server.enqueue(MockResponse(body = "{}"))

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer my-secret-token", recorded.headers["Authorization"])
    }

    @Test
    fun `omits Authorization header when credentials null`() {
        every { credentialsManager.credentials } returns flowOf(null)
        server.enqueue(MockResponse(body = "{}"))

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun `preserves original request URL and method`() {
        every { credentialsManager.credentials } returns flowOf(null)
        server.enqueue(MockResponse(body = "{}"))

        val request = Request.Builder()
            .url(server.url("/api/runs?page=1"))
            .get()
            .build()
        buildClient().newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/runs?page=1", recorded.target)
    }
}
