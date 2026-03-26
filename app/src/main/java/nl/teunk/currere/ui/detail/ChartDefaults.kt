package nl.teunk.currere.ui.detail

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.LayeredComponent
import com.patrykandpatrick.vico.compose.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import nl.teunk.currere.ui.theme.TextSecondary

internal object ChartDefaults {

    fun formatTimeAxis(totalSeconds: Long): String {
        val totalMinutes = (totalSeconds + 30) / 60
        return when {
            totalMinutes <= 0L -> "0m"
            totalMinutes < 60L -> "${totalMinutes}m"
            totalMinutes % 60L == 0L -> "${totalMinutes / 60}h"
            else -> "${totalMinutes / 60}h${totalMinutes % 60}m"
        }
    }

    private val niceIntervals = intArrayOf(1, 2, 5, 10, 15, 20, 30, 60)

    fun labelSpacingMinutes(totalDurationMinutes: Long): Int {
        val minSpacing = (totalDurationMinutes.toInt() / 4 + 1).coerceAtLeast(1)
        return niceIntervals.firstOrNull { it >= minSpacing } ?: minSpacing
    }

    fun centeredRangeProvider(
        values: List<Double>,
        padding: Double,
    ): CartesianLayerRangeProvider {
        val min = values.min()
        val max = values.max()
        val avg = values.average()
        val halfRange = maxOf(avg - min, max - avg) + padding
        return CartesianLayerRangeProvider.fixed(minY = avg - halfRange, maxY = avg + halfRange)
    }

    val yAxisSize: BaseAxis.Size = BaseAxis.Size.Fixed(32.dp)

    @Composable
    fun rememberGridLine() = rememberAxisGuidelineComponent(
        fill = Fill(TextSecondary.copy(alpha = 0.15f)),
        thickness = 0.5.dp,
        shape = RectangleShape,
    )

    @Composable
    fun rememberLabel() = rememberAxisLabelComponent(style = TextStyle(color = TextSecondary))

    @Composable
    fun rememberTimeFormatter() = remember {
        CartesianValueFormatter { _, value, _ -> formatTimeAxis(value.toLong()) }
    }

    @Composable
    fun rememberBottomTimeAxis(labelSpacingMinutes: Int) = HorizontalAxis.rememberBottom(
        label = rememberLabel(),
        valueFormatter = rememberTimeFormatter(),
        guideline = null,
        tick = null,
        line = null,
        itemPlacer = remember(labelSpacingMinutes) {
            HorizontalAxis.ItemPlacer.aligned(
                spacing = { labelSpacingMinutes * 60 },
                addExtremeLabelPadding = true,
            )
        },
    )

    @Composable
    fun rememberMarker(
        valueFormatter: DefaultCartesianMarker.ValueFormatter,
        guidelineColor: Color = TextSecondary,
    ): DefaultCartesianMarker {
        val labelBackground = rememberShapeComponent(
            fill = Fill(Color.White),
            shape = MarkerCornerBasedShape(
                base = RoundedCornerShape(14.dp),
                tickSize = 8.dp,
            ),
            strokeFill = Fill(Color(0x18000000)),
            strokeThickness = 0.5.dp,
        )
        val label = rememberTextComponent(
            style = TextStyle(color = Color(0xFF666666), fontSize = 14.sp),
            padding = Insets(horizontal = 14.dp, vertical = 10.dp),
            background = labelBackground,
        )
        val guideline = rememberLineComponent(
            fill = Fill(guidelineColor.copy(alpha = 0.25f)),
            thickness = 1.dp,
        )
        return rememberDefaultCartesianMarker(
            label = label,
            valueFormatter = valueFormatter,
            labelPosition = DefaultCartesianMarker.LabelPosition.Top,
            indicator = { color ->
                LayeredComponent(
                    back = ShapeComponent(fill = Fill(color.copy(alpha = 0.15f)), shape = CircleShape),
                    front = LayeredComponent(
                        back = ShapeComponent(fill = Fill(color), shape = CircleShape),
                        front = ShapeComponent(fill = Fill(Color.White), shape = CircleShape),
                        padding = Insets(all = 2.dp),
                    ),
                    padding = Insets(all = 4.dp),
                )
            },
            indicatorSize = 22.dp,
            guideline = guideline,
        )
    }

    const val LONG_PRESS_TIMEOUT_MS = 100L

    @Composable
    fun rememberShowOnLongPress(): CartesianMarkerController = remember {
        object : CartesianMarkerController {
            private var isActive = false

            override val acceptsLongPress = true
            override val consumeMoveEvents get() = isActive
            override val lock = CartesianMarkerController.Lock.Position

            override fun shouldAcceptInteraction(
                interaction: Interaction,
                targets: List<CartesianMarker.Target>,
            ) = when (interaction) {
                is Interaction.LongPress -> { isActive = true; true }
                is Interaction.Move -> isActive
                is Interaction.Release -> { isActive = false; true }
                else -> false
            }

            override fun shouldShowMarker(
                interaction: Interaction,
                targets: List<CartesianMarker.Target>,
            ) = interaction !is Interaction.Release
        }
    }

    @Composable
    fun rememberHapticMarkerVisibilityListener(): CartesianMarkerVisibilityListener {
        val haptic = LocalHapticFeedback.current
        return remember {
            object : CartesianMarkerVisibilityListener {
                override fun onShown(
                    marker: CartesianMarker,
                    targets: List<CartesianMarker.Target>,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                override fun onUpdated(
                    marker: CartesianMarker,
                    targets: List<CartesianMarker.Target>,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }
}
