package nl.teunk.currere.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// region QR Payload

@Serializable
data class QrPayload(
    val token: String,
    @SerialName("base_url") val baseUrl: String,
)

// endregion

// region Request DTOs

@Serializable
data class HeartRateSampleRequest(
    val timestamp: String,
    val bpm: Long,
)

@Serializable
data class PaceSplitRequest(
    @SerialName("kilometer_number") val kilometerNumber: Int,
    @SerialName("split_time_seconds") val splitTimeSeconds: Long,
    @SerialName("pace_seconds_per_km") val paceSecondsPerKm: Long,
    @SerialName("is_partial") val isPartial: Boolean,
    @SerialName("partial_distance_km") val partialDistanceKm: Double? = null,
)

@Serializable
data class RunRequest(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("distance_km") val distanceKm: Double,
    @SerialName("duration_seconds") val durationSeconds: Long,
    val steps: Long? = null,
    @SerialName("avg_heart_rate") val avgHeartRate: Long? = null,
    @SerialName("avg_pace_seconds_per_km") val avgPaceSecondsPerKm: Long? = null,
    @SerialName("heart_rate_samples") val heartRateSamples: List<HeartRateSampleRequest>? = null,
    @SerialName("pace_splits") val paceSplits: List<PaceSplitRequest>? = null,
)

@Serializable
data class BatchRunRequest(
    val runs: List<RunRequest>,
)

// endregion

// region Response DTOs

@Serializable
data class ApiResponse<T>(
    val data: T,
)

@Serializable
data class RunResponse(
    val id: Long,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("distance_km") val distanceKm: Double,
    @SerialName("duration_seconds") val durationSeconds: Long? = null,
    val steps: Long? = null,
    @SerialName("avg_heart_rate") val avgHeartRate: Long? = null,
    @SerialName("avg_pace_seconds_per_km") val avgPaceSecondsPerKm: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("already_synced") val alreadySynced: Boolean? = null,
)

@Serializable
data class RunDetailResponse(
    val id: Long,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("distance_km") val distanceKm: Double,
    @SerialName("duration_seconds") val durationSeconds: Long,
    val steps: Long? = null,
    @SerialName("avg_heart_rate") val avgHeartRate: Long? = null,
    @SerialName("avg_pace_seconds_per_km") val avgPaceSecondsPerKm: Long? = null,
    @SerialName("heart_rate_samples") val heartRateSamples: List<HeartRateSampleRequest>? = null,
    @SerialName("pace_splits") val paceSplits: List<PaceSplitRequest>? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class BatchResultItem(
    val index: Int,
    val status: String,
    val id: Long? = null,
    @SerialName("already_synced") val alreadySynced: Boolean? = null,
)

@Serializable
data class BatchRunResponseData(
    val created: Int = 0,
    val skipped: Int = 0,
    val results: List<BatchResultItem> = emptyList(),
)

@Serializable
data class PaginationMeta(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("last_page") val lastPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
)

@Serializable
data class PaginationLinks(
    val first: String? = null,
    val last: String? = null,
    val prev: String? = null,
    val next: String? = null,
)

@Serializable
data class PaginatedResponse<T>(
    val data: T,
    val meta: PaginationMeta? = null,
    val links: PaginationLinks? = null,
)

@Serializable
data class ApiErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>? = null,
)

// endregion
