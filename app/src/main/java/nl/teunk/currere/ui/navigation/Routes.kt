package nl.teunk.currere.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object PermissionRoute

@Serializable
object DiaryRoute

@Serializable
data class DetailRoute(
    val sessionId: String,
    val startTimeEpochMilli: Long,
    val endTimeEpochMilli: Long,
)
