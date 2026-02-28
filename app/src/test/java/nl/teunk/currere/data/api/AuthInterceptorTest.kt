package nl.teunk.currere.data.api

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.credentials.ServerCredentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
        server.shutdown()
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(credentialsManager))
            .build()
    }

    @Test
    fun `adds Accept json header always`() {
        every { credentialsManager.credentials } returns flowOf(null)
        server.enqueue(MockResponse().setBody("{}"))

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("application/json", recorded.getHeader("Accept"))
    }

    @Test
    fun `adds Authorization header when credentials present`() {
        val credentials = ServerCredentials(baseUrl = "https://example.com", token = "my-secret-token")
        every { credentialsManager.credentials } returns flowOf(credentials)
        server.enqueue(MockResponse().setBody("{}"))

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer my-secret-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `omits Authorization header when credentials null`() {
        every { credentialsManager.credentials } returns flowOf(null)
        server.enqueue(MockResponse().setBody("{}"))

        buildClient().newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `preserves original request URL and method`() {
        every { credentialsManager.credentials } returns flowOf(null)
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder()
            .url(server.url("/api/runs?page=1"))
            .get()
            .build()
        buildClient().newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/runs?page=1", recorded.path)
    }
}
