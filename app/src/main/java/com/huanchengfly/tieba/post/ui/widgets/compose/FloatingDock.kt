package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 外壳高度：64dp 全胶囊。 */
private val DockShellHeight = 64.dp

/** 外壳水平外边距。 */
private val DockHorizontalMargin = 16.dp

/** 外壳距系统导航栏的底边距。 */
private val DockBottomMargin = 12.dp

/** 外壳内左右留白：首末槽位不贴胶囊边缘。 */
private val DockInnerHorizontalPadding = 8.dp

/** 静止指示器高度：壳高 64dp 上下各留 4dp（= 壳高 × 4/64）。 */
private val DockIndicatorRestHeight = 56.dp

/** 按下 bloom 目标高度 78dp（≈1.39×）。 */
private const val DockIndicatorPressScale = 78f / 56f

/** 毛玻璃模糊半径（平衡档）。 */
private val DockBlurRadius = 10.dp

/** 壳底色透明度：有模糊时更低（让模糊透出），无模糊时更高（替代纯色）。 */
private const val DockSurfaceAlphaBlur = 0.50f
private const val DockSurfaceAlphaFallback = 0.88f

/**
 * 指示器速度形变：把指示器位移速度换算为 ±0.2 的形变系数。
 * 快速切换时 scaleX 拉伸 / scaleY 压扁（约 ±20% 封顶），静止时无形变。
 */
private const val DockIndicatorVelocityDivisor = 10000f

/** 背景模糊（RenderEffect / haze）需要 API 31+。 */
fun supportsBackdropBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * 悬浮毛玻璃 Dock：64dp 全胶囊壳（背景模糊 + 高光 rim + 内阴影 + 投影）+
 * 弹性胶囊指示器（spring 位移 + 按下 bloom + 速度形变）。
 *
 * 条目图标常显；仅当前激活项展开名称（沿用旧悬浮底栏的交互）。
 * API < 31 自动降级为增强半透明纯色壳（不挂模糊）。
 */
@Composable
fun FloatingDock(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    hazeState: HazeState,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val blurSupported = supportsBackdropBlur()
    // 任一槽位按下 → 指示器 bloom
    var pressed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = DockBottomMargin)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DockHorizontalMargin)
                .height(DockShellHeight)
        ) {
            val slotWidth = (maxWidth - DockInnerHorizontalPadding * 2) / navigationItems.size
            val density = LocalDensity.current
            val slotWidthPx = with(density) { slotWidth.toPx() }
            val innerPaddingPx = with(density) { DockInnerHorizontalPadding.toPx() }

            val shape = CircleShape
            val surfaceTint = themeColors.bottomBarSurface.copy(
                alpha = if (blurSupported) DockSurfaceAlphaBlur else DockSurfaceAlphaFallback
            )
            val shadowAlpha = if (themeColors.isNightMode) 0.22f else 0.10f
            val rimTopAlpha = if (themeColors.isNightMode) 0.18f else 0.55f
            val rimBottomAlpha = if (themeColors.isNightMode) 0.05f else 0.12f
            val innerShadowAlpha = if (themeColors.isNightMode) 0.10f else 0.06f

            // 玻璃壳（裁切到胶囊；指示器 bloom 越界由指示器层负责，壳不参与）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 10.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = shadowAlpha),
                        spotColor = Color.Black.copy(alpha = shadowAlpha),
                    )
                    .clip(shape)
                    .then(
                        if (blurSupported) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurEffect {
                                    style = HazeBlurStyle(
                                        backgroundColor = Color.Transparent,
                                        colorEffects = emptyList(),
                                        blurRadius = DockBlurRadius,
                                        noiseFactor = 0f,
                                        fallbackColorEffect = HazeColorEffect.tint(surfaceTint),
                                    )
                                    blurEnabled = true
                                    blurredEdgeTreatment = BlurredEdgeTreatment(shape)
                                }
                            }
                        } else Modifier
                    )
                    .background(surfaceTint)
                    .drawBehind {
                        // 底部内阴影：给玻璃一点厚度暗示
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = innerShadowAlpha)
                                )
                            ),
                            topLeft = Offset(0f, size.height * 0.45f),
                            size = Size(size.width, size.height * 0.55f)
                        )
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = rimTopAlpha),
                                Color.White.copy(alpha = rimBottomAlpha)
                            )
                        ),
                        shape = shape,
                    )
            )

            // 选中指示器（可越界 bloom）
            DockIndicator(
                modifier = Modifier.align(Alignment.CenterStart),
                targetPosition = currentPosition,
                slotWidthPx = slotWidthPx,
                innerPaddingPx = innerPaddingPx,
                pressed = pressed,
                isNightMode = themeColors.isNightMode,
            )

            // 槽位内容
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DockInnerHorizontalPadding),
            ) {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    DockItem(
                        selected = index == currentPosition,
                        icon = navigationItem.icon(),
                        title = navigationItem.title(index == currentPosition),
                        badge = navigationItem.badge,
                        badgeText = navigationItem.badgeText,
                        selectedColor = MaterialTheme.colors.secondary,
                        unselectedColor = themeColors.unselected,
                        onPressedChange = { pressed = it },
                        onClick = {
                            if (index == currentPosition) {
                                onReselected(index)
                            } else {
                                onChangePosition(index)
                            }
                            navigationItem.onClick?.invoke()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DockIndicator(
    modifier: Modifier,
    targetPosition: Int,
    slotWidthPx: Float,
    innerPaddingPx: Float,
    pressed: Boolean,
    isNightMode: Boolean,
) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(targetPosition, slotWidthPx) {
        offset.animateTo(
            targetValue = targetPosition * slotWidthPx,
            animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
        )
    }
    val pressScaleX = remember { Animatable(1f) }
    val pressScaleY = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            launch { pressScaleX.animateTo(DockIndicatorPressScale, spring(0.6f, 250f)) }
            launch { pressScaleY.animateTo(DockIndicatorPressScale, spring(0.7f, 250f)) }
        } else {
            launch { pressScaleX.animateTo(1f, spring(1f, 300f)) }
            launch { pressScaleY.animateTo(1f, spring(1f, 300f)) }
        }
    }
    val indicatorColor = if (isNightMode) Color.White else Color.Black

    Box(
        modifier = modifier
            .offset { IntOffset((innerPaddingPx + offset.value).roundToInt(), 0) }
            .width(with(LocalDensity.current) { slotWidthPx.toDp() })
            .height(DockIndicatorRestHeight)
            .graphicsLayer {
                val velocity = offset.velocity
                val termX = (velocity / DockIndicatorVelocityDivisor * 0.75f)
                    .coerceIn(-0.2f, 0.2f)
                val termY = (velocity / DockIndicatorVelocityDivisor * 0.25f)
                    .coerceIn(-0.2f, 0.2f)
                scaleX = pressScaleX.value / (1f - termX)
                scaleY = pressScaleY.value * (1f - termY)
            }
            .drawBehind {
                val pressProgress =
                    ((pressScaleX.value - 1f) / (DockIndicatorPressScale - 1f)).coerceIn(0f, 1f)
                drawRoundRect(
                    color = indicatorColor.copy(alpha = 0.10f + 0.03f * pressProgress),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun DockItem(
    selected: Boolean,
    icon: AnimatedImageVector,
    title: String,
    badge: Boolean,
    badgeText: String?,
    selectedColor: Color,
    unselectedColor: Color,
    onPressedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "dockItemProgress"
    )
    val hapticFeedback = LocalHapticFeedback.current
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) { onPressedChange(pressed) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime >= 250) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                    lastClickTime = currentTime
                }
            )
    ) {
        Box {
            Icon(
                painter = rememberAnimatedVectorPainter(
                    animatedImageVector = icon,
                    atEnd = selected
                ),
                contentDescription = title,
                tint = if (progress > 0f) selectedColor else unselectedColor,
                modifier = Modifier.size(24.dp),
            )
            if (badge) {
                Text(
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = White,
                    text = badgeText ?: "",
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colors.secondary,
                            shape = CircleShape
                        ),
                )
            }
        }
        if (progress > 0f) {
            Text(
                text = title,
                maxLines = 1,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = selectedColor,
                modifier = Modifier
                    .alpha((progress * progress * progress).coerceIn(0f, 1f))
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints())
                        layout((placeable.width * progress).roundToInt(), placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .padding(start = 6.dp)
            )
        }
    }
}
