package nl.teunk.currere.domain.compute

import org.junit.Assert.assertEquals
import org.junit.Test

class OutlierFilterTest {

    @Test
    fun `normal data is unchanged`() {
        val values = listOf(10.0, 12.0, 11.0, 13.0, 12.0, 11.0, 14.0, 10.0)
        val result = OutlierFilter.clampOutliers(values)
        assertEquals(values, result)
    }

    @Test
    fun `outliers are clamped to boundaries`() {
        val values = listOf(10.0, 12.0, 11.0, 13.0, 12.0, 11.0, 100.0, 10.0)
        val result = OutlierFilter.clampOutliers(values)
        // 100.0 should be clamped down to the upper boundary
        val sorted = values.sorted()
        val q1 = sorted[1] + 0.75 * (sorted[2] - sorted[1]) // index 1.75
        val q3 = sorted[5] + 0.25 * (sorted[6] - sorted[5]) // index 5.25
        val iqr = q3 - q1
        val upper = q3 + 1.5 * iqr
        assertEquals(upper, result[6], 0.001)
        // Other values should remain unchanged
        assertEquals(10.0, result[0], 0.001)
        assertEquals(12.0, result[1], 0.001)
    }

    @Test
    fun `empty list returns empty`() {
        assertEquals(emptyList<Double>(), OutlierFilter.clampOutliers(emptyList()))
    }

    @Test
    fun `single element returns unchanged`() {
        val values = listOf(42.0)
        assertEquals(values, OutlierFilter.clampOutliers(values))
    }

    @Test
    fun `all same values returns unchanged`() {
        val values = listOf(5.0, 5.0, 5.0, 5.0, 5.0)
        assertEquals(values, OutlierFilter.clampOutliers(values))
    }

    @Test
    fun `low outliers are clamped up`() {
        val values = listOf(10.0, 12.0, 11.0, 13.0, 12.0, 11.0, -50.0, 10.0)
        val result = OutlierFilter.clampOutliers(values)
        val sorted = values.sorted()
        val q1 = sorted[1] + 0.75 * (sorted[2] - sorted[1])
        val q3 = sorted[5] + 0.25 * (sorted[6] - sorted[5])
        val iqr = q3 - q1
        val lower = q1 - 1.5 * iqr
        assertEquals(lower, result[6], 0.001)
    }
}
