package nl.teunk.currere.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CurrereApiService {

    @POST("runs/batch")
    suspend fun createRunsBatch(@Body batch: BatchRunRequest): Response<ApiResponse<BatchRunResponseData>>

    @GET("runs")
    suspend fun getRuns(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): Response<PaginatedResponse<List<RunResponse>>>
}
