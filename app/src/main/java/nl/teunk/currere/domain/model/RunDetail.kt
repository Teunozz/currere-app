package nl.teunk.currere.domain.model

data class RunDetail(
    val session: RunSession,
    val totalSteps: Long,
    val heartRateSamples: List<HeartRateSample>,
    val paceSamples: List<PaceSample>,
    val splits: List<PaceSplit>,
)
