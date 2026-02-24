package nl.teunk.currere.data.api

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.teunk.currere.data.credentials.CredentialsManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val credentialsManager: CredentialsManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = runBlocking { credentialsManager.credentials.first() }
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .apply {
                if (credentials != null) {
                    addHeader("Authorization", "Bearer ${credentials.token}")
                }
            }
            .build()
        return chain.proceed(request)
    }
}
