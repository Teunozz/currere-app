package nl.teunk.currere.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CurrereApiService {

    @POST("runs/batch")
    suspend fun createRunsBatch(@Body batch: BatchRunRequest): Response<ApiResponse<BatchRunResponseData>>

    @GET("ping")
    suspend fun ping(): Response<PingResponse>
}
