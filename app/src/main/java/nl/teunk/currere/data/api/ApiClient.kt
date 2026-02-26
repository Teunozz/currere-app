package nl.teunk.currere.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import nl.teunk.currere.data.credentials.CredentialsManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiClient(
    private val credentialsManager: CredentialsManager,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val authInterceptor = AuthInterceptor(credentialsManager)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun createService(): CurrereApiService? {
        val credentials = credentialsManager.credentials.first() ?: return null
        return buildService(credentials.baseUrl)
    }

    suspend fun testConnection(baseUrl: String, token: String): Result<Unit> {
        return try {
            val testClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val service = buildService(baseUrl, testClient)
            val response = service.getRuns(page = 1, perPage = 1)

            when {
                response.isSuccessful -> Result.success(Unit)
                response.code() == 401 -> Result.failure(Exception("Authentication failed. Check your token."))
                else -> Result.failure(Exception("Server returned ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    private fun buildService(baseUrl: String, client: OkHttpClient = okHttpClient): CurrereApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CurrereApiService::class.java)
    }
}
