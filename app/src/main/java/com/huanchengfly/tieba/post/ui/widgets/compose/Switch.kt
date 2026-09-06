package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.roundToInt

/**
 * Material 3 规范的开关：52×32dp 实心轨道，选中态 24dp 圆点、未选中态 16dp 圆点 + 描边轨道。
 * 保持与旧版相同的公开 API（含拖动手势），所有调用点无需修改。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    val thumbStart = with(LocalDensity.current) { ThumbStartUnchecked.toPx() }
    val thumbEnd = with(LocalDensity.current) { ThumbStartChecked.toPx() }
    val swipeableState = rememberSwipeableStateFor(checked, onCheckedChange ?: {}, AnimationSpec)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            )
        } else {
            Modifier
        }

    Box(
        modifier
            .then(toggleableModifier)
            .swipeable(
                state = swipeableState,
                anchors = mapOf(thumbStart to false, thumbEnd to true),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = enabled && onCheckedChange != null,
                reverseDirection = isRtl,
                interactionSource = interactionSource,
                resistance = null
            )
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchTrackWidth, SwitchTrackHeight)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            colors = colors,
            thumbOffset = swipeableState.offset,
            thumbStart = thumbStart,
            thumbEnd = thumbEnd,
        )
    }
}

@Composable
private fun BoxScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    thumbOffset: State<Float>,
    thumbStart: Float,
    thumbEnd: Float,
) {
    val trackColor by colors.trackColor(enabled, checked)
    val thumbColor by colors.thumbColor(enabled, checked)
    val borderColor by colors.borderColor(enabled, checked)

    Canvas(Modifier.align(Alignment.Center).fillMaxSize()) {
        val trackHeightPx = SwitchTrackHeight.toPx()
        val trackTop = center.y - trackHeightPx / 2
        val cornerRadiusPx = CornerRadius(trackHeightPx / 2, trackHeightPx / 2)

        // 轨道
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeightPx),
            cornerRadius = cornerRadiusPx,
            style = Fill
        )
        // 未选中态描边
        if (!checked) {
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(1.dp.toPx(), trackTop + 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), trackHeightPx - 2.dp.toPx()),
                cornerRadius = cornerRadiusPx,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 圆点：16dp ↔ 24dp，位置 8dp ↔ 24dp
        val fraction = ((thumbOffset.value - thumbStart) / (thumbEnd - thumbStart)).coerceIn(0f, 1f)
        val diameter = lerp(ThumbDiameterUnchecked, ThumbDiameterChecked, fraction).toPx()
        val startX = lerp(ThumbStartUnchecked, ThumbStartChecked, fraction).toPx()
        drawCircle(
            color = thumbColor,
            radius = diameter / 2,
            center = Offset(startX + diameter / 2, center.y)
        )
    }
}

internal val SwitchTrackWidth = 52.dp
internal val SwitchTrackHeight = 32.dp
internal val ThumbDiameterChecked = 24.dp
internal val ThumbDiameterUnchecked = 16.dp
internal val ThumbStartUnchecked = 8.dp
internal val ThumbStartChecked = SwitchTrackWidth - ThumbDiameterChecked - 4.dp

private val AnimationSpec = TweenSpec<Float>(durationMillis = 150)

@Stable
interface SwitchColors {
    @Composable
    fun thumbColor(enabled: Boolean, checked: Boolean): State<Color>

    @Composable
    fun trackColor(enabled: Boolean, checked: Boolean): State<Color>

    @Composable
    fun borderColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Contains the default values used by [Switch]
 */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in
     * different states.
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = MaterialTheme.colors.onSecondary,
        checkedTrackColor: Color = MaterialTheme.colors.secondary,
        checkedTrackAlpha: Float = 1f,
        uncheckedThumbColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.55f),
        uncheckedTrackColor: Color = Color.Transparent,
        @Suppress("UNUSED_PARAMETER") uncheckedTrackAlpha: Float = 1f,
        uncheckedBorderColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.38f),
        disabledCheckedThumbColor: Color = checkedThumbColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledCheckedTrackColor: Color = checkedTrackColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledUncheckedThumbColor: Color = uncheckedThumbColor
            .copy(alpha = ContentAlpha.disabled),
        disabledUncheckedTrackColor: Color = Color.Transparent,
        disabledUncheckedBorderColor: Color = uncheckedBorderColor.copy(alpha = ContentAlpha.disabled)
    ): SwitchColors = DefaultSwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor.copy(alpha = checkedTrackAlpha),
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedBorderColor = uncheckedBorderColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor.copy(alpha = checkedTrackAlpha),
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor,
        disabledUncheckedBorderColor = disabledUncheckedBorderColor
    )
}

/**
 * Default [SwitchColors] implementation.
 */
@Immutable
private class DefaultSwitchColors(
    private val checkedThumbColor: Color,
    private val checkedTrackColor: Color,
    private val uncheckedThumbColor: Color,
    private val uncheckedTrackColor: Color,
    private val uncheckedBorderColor: Color,
    private val disabledCheckedThumbColor: Color,
    private val disabledCheckedTrackColor: Color,
    private val disabledUncheckedThumbColor: Color,
    private val disabledUncheckedTrackColor: Color,
    private val disabledUncheckedBorderColor: Color
) : SwitchColors {
    @Composable
    override fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedThumbColor else uncheckedThumbColor
            } else {
                if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
            }
        )
    }

    @Composable
    override fun trackColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedTrackColor else uncheckedTrackColor
            } else {
                if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
            }
        )
    }

    @Composable
    override fun borderColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                uncheckedBorderColor
            } else {
                disabledUncheckedBorderColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSwitchColors

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedBorderColor != other.uncheckedBorderColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false
        if (disabledUncheckedBorderColor != other.disabledUncheckedBorderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedBorderColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedBorderColor.hashCode()
        return result
    }
}
