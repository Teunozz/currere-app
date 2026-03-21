package nl.teunk.currere.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.R
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.ui.preview.SamplePaceRunSessions
import nl.teunk.currere.ui.theme.ChartPace
import nl.teunk.currere.ui.theme.CurrereTheme
import nl.teunk.currere.ui.theme.SplitFast
import nl.teunk.currere.ui.theme.SplitSlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SLOT_COUNT = 5
private val BAR_WIDTH_DP = 12.dp

private data class BarSlot(
    val paceSeconds: Double?,
    val dateLabel: String?,
    val paceFormatted: String?,
)

private fun formatDateLabel(startTime: Instant, todayLabel: String): String {
    val runDate = startTime.atZone(ZoneId.systemDefault()).toLocalDate()
    if (runDate == LocalDate.now()) return todayLabel
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(startTime)
}

private enum class PaceTrend { FASTER, SLOWER, SAME }

@Composable
fun PaceBarChart(
    runs: List<RunSession>,
    modifier: Modifier = Modifier,
) {
    val todayLabel = stringResource(R.string.today)

    // Build exactly SLOT_COUNT slots, newest on the right
    val slots = remember(runs, todayLabel) {
        val recent = runs.take(SLOT_COUNT)
        val reversed = recent.reversed() // oldest first → newest last (rightmost)
        List(SLOT_COUNT) { index ->
            val offset = SLOT_COUNT - reversed.size
            val session = reversed.getOrNull(index - offset)
            BarSlot(
                paceSeconds = session?.averagePaceSecondsPerKm,
                dateLabel = session?.let { formatDateLabel(it.startTime, todayLabel) },
                paceFormatted = session?.averagePaceSecondsPerKm?.let {
                    StatsAggregator.formatPace(it)
                },
            )
        }
    }

    // Don't show if no pace data at all
    val hasAnyPace = remember(slots) { slots.any { it.paceSeconds != null } }
    if (!hasAnyPace) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val haptic = LocalHapticFeedback.current
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // The latest run with pace data (rightmost filled slot)
    val latestSlot = remember(slots) { slots.lastOrNull { it.paceSeconds != null } }

    // Trend: compare two most recent runs with pace
    val trend = remember(slots) {
        val withPace = slots.filter { it.paceSeconds != null }
        if (withPace.size < 2) null
        else {
            val latest = withPace.last().paceSeconds!!
            val previous = withPace[withPace.size - 2].paceSeconds!!
            val diff = latest - previous
            when {
                diff < -3 -> PaceTrend.FASTER  // lower seconds = faster
                diff > 3 -> PaceTrend.SLOWER
                else -> PaceTrend.SAME
            }
        }
    }

    // Display values: selected bar overrides the hero on the left
    val displayPace by remember(selectedIndex, slots, latestSlot) {
        derivedStateOf {
            if (selectedIndex in slots.indices) {
                slots[selectedIndex].paceFormatted ?: "0:00"
            } else {
                latestSlot?.paceFormatted ?: "—"
            }
        }
    }
    val displayDate by remember(selectedIndex, slots, latestSlot) {
        derivedStateOf {
            if (selectedIndex in slots.indices) {
                slots[selectedIndex].dateLabel ?: ""
            } else {
                latestSlot?.dateLabel ?: ""
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.recent_pace),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: hero pace value (fixed width to prevent bar reflow)
                Column(modifier = Modifier.width(100.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayPace,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChartPace,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.unit_per_km),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = onSurfaceVariant,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant,
                        )
                        // Trend indicator (only when not selecting a bar)
                        if (selectedIndex == -1 && trend != null) {
                            Spacer(Modifier.width(4.dp))
                            val (icon, tint) = when (trend) {
                                PaceTrend.FASTER -> Icons.AutoMirrored.Filled.TrendingDown to SplitFast
                                PaceTrend.SLOWER -> Icons.AutoMirrored.Filled.TrendingUp to SplitSlow
                                PaceTrend.SAME -> Icons.AutoMirrored.Filled.TrendingFlat to onSurfaceVariant
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = when (trend) {
                                    PaceTrend.FASTER -> stringResource(R.string.pace_improving)
                                    PaceTrend.SLOWER -> stringResource(R.string.pace_declining)
                                    PaceTrend.SAME -> stringResource(R.string.pace_stable)
                                },
                                tint = tint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Right: bar chart
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .pointerInput(slots) {
                            val bw = BAR_WIDTH_DP.toPx()
                            val g = BAR_WIDTH_DP.toPx() * 1.5f
                            detectTapGestures(
                                onPress = { offset ->
                                    val idx = hitTestSlot(offset.x, size.width.toFloat(), bw, g)
                                    if (idx != selectedIndex) {
                                        selectedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    tryAwaitRelease()
                                    selectedIndex = -1
                                },
                            )
                        }
                        .pointerInput(slots) {
                            val bw = BAR_WIDTH_DP.toPx()
                            val g = BAR_WIDTH_DP.toPx() * 1.5f
                            detectHorizontalDragGestures(
                                onDragEnd = { selectedIndex = -1 },
                                onDragCancel = { selectedIndex = -1 },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val idx = hitTestSlot(change.position.x, size.width.toFloat(), bw, g)
                                    if (idx != selectedIndex) {
                                        selectedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                            )
                        },
                ) {
                    drawBars(
                        slots = slots,
                        selectedIndex = selectedIndex,
                    )
                }
            }
        }
    }
}

private fun hitTestSlot(x: Float, canvasWidth: Float, barWidth: Float, gap: Float): Int {
    val slotWidth = barWidth + gap
    val totalChartWidth = SLOT_COUNT * slotWidth - gap
    val offsetX = canvasWidth - totalChartWidth // right-align
    val localX = x - offsetX
    if (localX < 0) return -1
    val index = (localX / slotWidth).toInt().coerceIn(0, SLOT_COUNT - 1)
    return index
}

private fun DrawScope.drawBars(
    slots: List<BarSlot>,
    selectedIndex: Int,
) {
    val labelAreaHeight = 18.dp.toPx()
    val barsAreaHeight = size.height - labelAreaHeight
    val barCornerRadius = 3.dp.toPx()

    val barWidth = BAR_WIDTH_DP.toPx()
    val gap = barWidth * 1.5f
    val slotWidth = barWidth + gap
    val totalChartWidth = SLOT_COUNT * slotWidth - gap
    val offsetX = size.width - totalChartWidth // right-align

    val paces = slots.mapNotNull { it.paceSeconds }
    if (paces.isEmpty()) return

    val fastestPace = paces.min()
    val slowestPace = paces.max()
    val paceRange = slowestPace - fastestPace
    val minBarFraction = 0.2f

    slots.forEachIndexed { index, slot ->
        val slotCenterX = offsetX + slotWidth * index + barWidth / 2f
        val isSelected = index == selectedIndex
        val barLeft = slotCenterX - barWidth / 2f

        if (slot.paceSeconds != null) {
            // Fastest = full height, slowest = minBarFraction, linear spread
            val normalized = if (paceRange > 0) {
                ((slowestPace - slot.paceSeconds) / paceRange).toFloat()
            } else {
                1f
            }
            val barFraction = minBarFraction + normalized * (1f - minBarFraction)
            val barHeight = (barsAreaHeight * barFraction).coerceAtLeast(4.dp.toPx())
            val barTop = barsAreaHeight - barHeight

            val barColor = if (isSelected) ChartPace else ChartPace.copy(alpha = 0.5f)

            drawRoundRect(
                color = barColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barCornerRadius, barCornerRadius),
            )

        }

        // Bottom dot for all slots
        drawCircle(
            color = if (isSelected) ChartPace else ChartPace.copy(alpha = 0.3f),
            radius = 2.dp.toPx(),
            center = Offset(slotCenterX, barsAreaHeight + labelAreaHeight / 2f),
        )
    }
}

@Preview
@Composable
private fun PaceBarChartPreview() {
    val runs = SamplePaceRunSessions
    CurrereTheme {
        PaceBarChart(runs = runs)
    }
}
