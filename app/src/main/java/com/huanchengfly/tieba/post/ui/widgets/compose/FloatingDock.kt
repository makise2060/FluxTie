package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

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

/** 按下时壳整体放大的宽度量（64dp 壳基准 16dp，按壳宽比例缩放）。 */
private val DockPressBloomDp = 16.dp

/** 按下时条目内容放大比例（1 + 0.2）。 */
private const val DockTabPressScaleExtra = 0.2f

/** 毛玻璃模糊半径（平衡档）。 */
private val DockBlurRadius = 10.dp

/** 壳底色透明度：有模糊时更低（让模糊透出），无模糊时更高（替代纯色）。 */
private const val DockSurfaceAlphaBlur = 0.50f
private const val DockSurfaceAlphaFallback = 0.88f

/**
 * 指示器速度形变：速度口径为「归一化槽位速度」（槽位/秒 ÷ (count-1)）。
 * 快速切换/拖拽时 scaleX 拉伸 / scaleY 压扁（约 ±20% 封顶），静止时无形变。
 */
private const val DockVelocityDivisor = 10f
private const val DockVelocityScaleXMultiplier = 0.75f
private const val DockVelocityClamp = 0.2f

/** 拖拽到边界后整个面板的 rubber-band 跟手位移上限。 */
private val DockRubberBand = 4.dp

/** 背景模糊（RenderEffect / haze）需要 API 31+。 */
fun supportsBackdropBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * 悬浮毛玻璃 Dock：64dp 全胶囊壳（背景模糊 + 高光 rim + 投影）+
 * 弹性胶囊指示器（spring 位移 + 按下 bloom + 速度形变 + 拖拽跟手）。
 *
 * 交互：
 * - 单击条目：切换 / 再次点击当前项触发刷新；
 * - 按住当前选中项不放：指示器跟随手指在槽位间滑动，滑到哪个条目哪个条目展开名称，
 *   松手落定并切换（面板整体带 4dp rubber-band 跟手位移）；
 * - 条目图标常显，仅指示器所在的条目展开名称。
 *
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
    val itemCount = navigationItems.size
    val maxIndex = (itemCount - 1).coerceAtLeast(0)
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    // pointerInput 的 block 只在 key 变化时重启，回调与选中值需经 rememberUpdatedState 取最新
    val currentPositionLatest = rememberUpdatedState(currentPosition)
    val onChangePositionLatest = rememberUpdatedState(onChangePosition)
    val hapticFeedbackLatest = rememberUpdatedState(hapticFeedback)

    // 指示器位置（浮点槽位索引）：拖拽中直接跟随手指，其余 spring 落定。
    val dragPosition = remember { Animatable(currentPosition.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    // 任一槽位按下 → 按压反馈（指示器 bloom + 壳 bloom + 内容放大）
    var pressed by remember { mutableStateOf(false) }
    val indicatorScaleX = remember { Animatable(1f) }
    val indicatorScaleY = remember { Animatable(1f) }
    val pressProgress = remember { Animatable(0f) }
    // 速度形变（归一化槽位速度）
    val velocity = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    // rubber-band 面板跟手位移（累计拖拽量，松手回弹）
    val panelOffsetRaw = remember { Animatable(0f) }
    var lastReselectTime by remember { mutableLongStateOf(0L) }

    // 外部选中变化（点击切换 / 返回键回首页）→ 指示器 spring 落定
    LaunchedEffect(currentPosition, maxIndex) {
        if (!isDragging) {
            dragPosition.animateTo(
                targetValue = currentPosition.coerceIn(0, maxIndex).toFloat(),
                animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
            )
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            launch { indicatorScaleX.animateTo(DockIndicatorPressScale, spring(0.6f, 250f)) }
            launch { indicatorScaleY.animateTo(DockIndicatorPressScale, spring(0.7f, 250f)) }
            launch { pressProgress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 1000f)) }
        } else {
            launch { indicatorScaleX.animateTo(1f, spring(0.6f, 250f)) }
            launch { indicatorScaleY.animateTo(1f, spring(0.7f, 250f)) }
            launch { pressProgress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 1000f)) }
        }
    }

    // 未选中色：高对比主前景色（浅色近黑 / 夜间近白），避免浅色下过淡看不清
    val unselectedColor = MaterialTheme.colors.onSurface

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
            val slotWidth = (maxWidth - DockInnerHorizontalPadding * 2) / itemCount
            val density = LocalDensity.current
            val slotWidthPx = with(density) { slotWidth.toPx() }
            val innerPaddingPx = with(density) { DockInnerHorizontalPadding.toPx() }
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val rubberBandPx = with(density) { DockRubberBand.toPx() }

            // rubber-band 面板偏移：拖拽越远偏移越大（4dp 上限，EaseOut 缓动）
            val panelOffsetPx: () -> Float = {
                if (totalWidthPx <= 0f) {
                    0f
                } else {
                    val fraction = (panelOffsetRaw.value / totalWidthPx).coerceIn(-1f, 1f)
                    rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }

            val shape = CircleShape
            val surfaceTint = themeColors.bottomBarSurface.copy(
                alpha = if (blurSupported) DockSurfaceAlphaBlur else DockSurfaceAlphaFallback
            )
            val shadowAlpha = if (themeColors.isNightMode) 0.22f else 0.10f
            val rimTopAlpha = if (themeColors.isNightMode) 0.18f else 0.55f
            val rimBottomAlpha = if (themeColors.isNightMode) 0.05f else 0.12f

            // 壳按压 bloom：按壳宽比例整体放大（含投影；draw phase 读取进度避免整树重组）
            val pressBloomPx = with(density) { DockPressBloomDp.toPx() }

            // 玻璃壳（均匀材质；裁切到胶囊。指示器 bloom 越界由指示器层负责，壳不参与）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val s = 1f + pressBloomPx / totalWidthPx.coerceAtLeast(1f) *
                            pressProgress.value
                        scaleX = s
                        scaleY = s
                        translationX = panelOffsetPx()
                    }
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

            // 选中指示器（可越界 bloom，跟随拖拽）
            DockIndicator(
                positionPx = innerPaddingPx + dragPosition.value * slotWidthPx,
                widthPx = slotWidthPx,
                pressedScaleX = indicatorScaleX.value,
                pressedScaleY = indicatorScaleY.value,
                velocity = velocity.value,
                panelOffsetPx = panelOffsetPx,
                isNightMode = themeColors.isNightMode,
            )

            // 槽位内容：名称展开与配色由指示器位置驱动（拖拽经过时实时切换）
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DockInnerHorizontalPadding)
                    .graphicsLayer {
                        val s = 1f + DockTabPressScaleExtra * pressProgress.value
                        scaleX = s
                        scaleY = s
                        translationX = panelOffsetPx()
                    },
            ) {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    val itemProgress =
                        (1f - abs(index - dragPosition.value)).coerceIn(0f, 1f)
                    DockItem(
                        progress = itemProgress,
                        icon = navigationItem.icon(),
                        title = navigationItem.title(index == currentPosition),
                        badge = navigationItem.badge,
                        badgeText = navigationItem.badgeText,
                        selectedColor = MaterialTheme.colors.secondary,
                        unselectedColor = unselectedColor,
                        onPressedChange = {
                            // 当前项的按压由拖拽手势层统一管理（拖出槽位不应提前结束按压反馈）
                            if (index != currentPosition) {
                                pressed = it
                            }
                        },
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

            // 拖拽手势层：覆盖当前指示器槽位（z 序最上）。
            // 按住即进入拖拽（无 touch slop），指示器跟随手指滑动；
            // 单击（未移动）触发当前项 reselect；拖拽手势消费移动事件后点击自动失效。
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset(
                            (innerPaddingPx + dragPosition.value * slotWidthPx + panelOffsetPx())
                                .roundToInt(),
                            0
                        )
                    }
                    .width(slotWidth)
                    .height(DockIndicatorRestHeight)
                    .pointerInput(itemCount, slotWidthPx, maxIndex) {
                        if (itemCount <= 1) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            isDragging = true
                            velocityTracker.resetTracking()
                            var pointerId = down.id
                            var moved = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.fastFirstOrNull { it.id == pointerId }
                                if (change == null) {
                                    // 手势被系统中断：复位按压/拖拽状态，不触发切换
                                    pressed = false
                                    isDragging = false
                                    break
                                }
                                if (change.changedToUpIgnoreConsumed()) {
                                    pressed = false
                                    isDragging = false
                                    val target =
                                        dragPosition.value.roundToInt().coerceIn(0, maxIndex)
                                    scope.launch {
                                        dragPosition.animateTo(
                                            target.toFloat(),
                                            spring(dampingRatio = 1f, stiffness = 1000f)
                                        )
                                    }
                                    scope.launch {
                                        velocity.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.5f, stiffness = 300f)
                                        )
                                    }
                                    scope.launch {
                                        panelOffsetRaw.animateTo(
                                            0f,
                                            spring(dampingRatio = 1f, stiffness = 300f)
                                        )
                                    }
                                    if (moved && target != currentPositionLatest.value) {
                                        hapticFeedbackLatest.value.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                        onChangePositionLatest.value(target)
                                    }
                                    break
                                }
                                val delta = change.positionChange().x
                                if (delta != 0f) {
                                    moved = true
                                    val next = (dragPosition.value + delta / slotWidthPx)
                                        .coerceIn(0f, maxIndex.toFloat())
                                    // AwaitPointerEventScope 受限，不能直接调 Animatable.snapTo；
                                    // UNDISPATCHED 确保位置在本次事件处理内同步落地
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        dragPosition.snapTo(next)
                                    }
                                    velocityTracker.addPosition(
                                        SystemClock.uptimeMillis(),
                                        Offset(next, 0f)
                                    )
                                    val slotVelocity = velocityTracker.calculateVelocity().x /
                                        maxIndex.coerceAtLeast(1)
                                    scope.launch {
                                        velocity.animateTo(
                                            slotVelocity,
                                            spring(dampingRatio = 0.5f, stiffness = 300f)
                                        )
                                    }
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        panelOffsetRaw.snapTo(panelOffsetRaw.value + delta)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastReselectTime >= 250) {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                                onReselected(currentPosition)
                            }
                            lastReselectTime = currentTime
                        }
                    )
                    .clearAndSetSemantics {}
            )
        }
    }
}

@Composable
private fun DockIndicator(
    positionPx: Float,
    widthPx: Float,
    pressedScaleX: Float,
    pressedScaleY: Float,
    velocity: Float,
    panelOffsetPx: () -> Float,
    isNightMode: Boolean,
) {
    val indicatorColor = if (isNightMode) Color.White else Color.Black
    Box(
        modifier = Modifier
            .offset {
                IntOffset((positionPx + panelOffsetPx()).roundToInt(), 0)
            }
            .width(with(LocalDensity.current) { widthPx.toDp() })
            .height(DockIndicatorRestHeight)
            .graphicsLayer {
                val termX = (velocity / DockVelocityDivisor * DockVelocityScaleXMultiplier)
                    .coerceIn(-DockVelocityClamp, DockVelocityClamp)
                val termY = (velocity / DockVelocityDivisor * 0.25f)
                    .coerceIn(-DockVelocityClamp, DockVelocityClamp)
                scaleX = pressedScaleX / (1f - termX)
                scaleY = pressedScaleY * (1f - termY)
            }
            .drawBehind {
                val bloomProgress =
                    ((pressedScaleX - 1f) / (DockIndicatorPressScale - 1f)).coerceIn(0f, 1f)
                drawRoundRect(
                    color = indicatorColor.copy(alpha = 0.10f + 0.03f * bloomProgress),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun DockItem(
    progress: Float,
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
    val color = lerp(unselectedColor, selectedColor, progress)
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
                    atEnd = progress >= 0.5f
                ),
                contentDescription = title,
                tint = color,
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
                color = color,
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
