package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateFloatAsState
import com.huanchengfly.tieba.post.ui.common.anim.MotionTokens
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * Material 3 规范的开关：52×32dp 实心轨道，选中态 24dp 圆点、未选中态 16dp 圆点 + 描边轨道。
 * 保持与旧版相同的公开 API（含拖动手势），所有调用点无需修改。
 *
 * 圆点位置与轨道色由 [checked] 经同一动画驱动（单一状态源），拖动时用本地覆盖层实时反馈，
 * 快速连点不会再出现颜色与位置脱节的残留。
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = MotionTokens.DurationFast)
    )
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null
                )
                .pointerInput(enabled, onCheckedChange, isRtl) {
                    if (!enabled) return@pointerInput
                    val travel = ThumbStartChecked.toPx() - ThumbStartUnchecked.toPx()
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onDragEnd = {
                            val f = dragFraction
                            dragFraction = null
                            if (f != null && (f > 0.5f) != checked) {
                                onCheckedChange(f > 0.5f)
                            }
                        },
                        onDragCancel = { dragFraction = null }
                    ) { change, dragAmount ->
                        change.consume()
                        val signed = if (isRtl) -dragAmount else dragAmount
                        accumulated += signed
                        val base = if (checked) 1f else 0f
                        val dir = if (checked) -1f else 1f
                        dragFraction = (base + dir * accumulated / travel).coerceIn(0f, 1f)
                    }
                }
        } else {
            Modifier
        }

    Box(
        modifier
            .then(toggleableModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchTrackWidth, SwitchTrackHeight)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            colors = colors,
            fraction = dragFraction ?: fraction,
        )
    }
}

@Composable
private fun BoxScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    fraction: Float,
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
        val progress = fraction.coerceIn(0f, 1f)
        val diameter = lerp(ThumbDiameterUnchecked, ThumbDiameterChecked, progress).toPx()
        val startX = lerp(ThumbStartUnchecked, ThumbStartChecked, progress).toPx()
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
 * Contains the default values used to create a [SwitchColors]
 */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used by a [Switch] in
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
@Stable
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
