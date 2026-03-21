package nl.teunk.currere.domain.compute

object OutlierFilter {

    fun clampOutliers(values: List<Double>, multiplier: Double = 1.5): List<Double> {
        if (values.size < 2) return values

        val sorted = values.sorted()
        val q1 = percentile(sorted, 0.25)
        val q3 = percentile(sorted, 0.75)
        val iqr = q3 - q1
        val lower = q1 - multiplier * iqr
        val upper = q3 + multiplier * iqr

        return values.map { it.coerceIn(lower, upper) }
    }

    fun movingAverage(values: List<Double>, window: Int): List<Double> {
        if (values.size < window * 3) return values
        val half = window / 2
        return values.indices.map { i ->
            val from = (i - half).coerceAtLeast(0)
            val to = (i + half).coerceAtMost(values.lastIndex)
            values.subList(from, to + 1).average()
        }
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        val index = p * (sorted.size - 1)
        val lower = index.toInt()
        val upper = lower + 1
        if (upper >= sorted.size) return sorted.last()
        val fraction = index - lower
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
    }
}
