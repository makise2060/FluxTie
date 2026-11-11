package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.DampedDragAnimation
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.InteractiveHighlight
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.InnerShadow
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.innerShadow
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.lens
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.rememberCombinedBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.dock.vibrancy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

internal val DockShellHeight = 64.dp

internal val DockBottomMargin = 12.dp

/** 底栏滚动自动隐藏的位移距离：胶囊高 + 底边距（调用方另加系统导航栏 inset）。 */
internal val DockAutoHideTranslate = DockShellHeight + DockBottomMargin

private val indicatorSpecular = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f
private const val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState) {
        derivedStateOf {
            val tilt = tiltState.value
            val gravityMagnitudeSquared = tilt.gravityX * tilt.gravityX + tilt.gravityY * tilt.gravityY
            if (gravityMagnitudeSquared > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(tilt.gravityY, tilt.gravityX) / GRAVITY_ANGLE_STEP_RAD).roundToInt() *
                        GRAVITY_ANGLE_STEP_RAD
            } else {
                (-PI / 2).toFloat()
            }
        }
    }
}

@Composable
private fun rememberGravityRotatedHighlight(base: Highlight, extraDegrees: Float = 0f): State<Highlight> {
    val gravityAngle = rememberQuantizedGravityAngle()
    return remember(gravityAngle, base, extraDegrees) {
        derivedStateOf {
            val baseStyle = base.style as BloomStroke
            val radians = gravityAngle.value + (extraDegrees * PI / 180.0).toFloat()
            base.copy(
                style = baseStyle.copy(
                    primaryLight = baseStyle.primaryLight.copy(
                        position = LightPosition(
                            x = LIGHT_REF_X + cos(radians),
                            y = LIGHT_REF_Y + sin(radians),
                            z = baseStyle.primaryLight.position.z,
                        ),
                    ),
                ),
            )
        }
    }
}

/**
 * 悬浮液态玻璃 Dock（IosLiquidGlassNavigationBar 全特性版）：
 * 壳体 vibrancy + 4dp 模糊 + 24dp 边缘折射透镜 + 重力感高光；
 * 选中指示器胶囊经「隐藏槽位捕获」把选中项内容透进透镜（含色散、
 * 按压 bloom、速度形变、内阴影），支持按住拖拽与橡皮筋跟手。
 */
@Composable
fun FloatingDock(
    currentPosition: Int,
    onChangePosition: (Int) -> Unit,
    onReselected: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    // 条目数变化时整体重建：DampedDragAnimation.modifier 的 pointerInput(Unit)
    // 不随实例重启，否则手势沿用旧的 valueRange，指示器与宽度计算全部错位
    key(navigationItems.size) {
        FloatingDockImpl(
            currentPosition = currentPosition.coerceIn(0, (navigationItems.size - 1).coerceAtLeast(0)),
            onChangePosition = onChangePosition,
            onReselected = onReselected,
            navigationItems = navigationItems,
            backdrop = backdrop,
            modifier = modifier,
            themeColors = themeColors,
            isBlurEnabled = isRuntimeShaderSupported(),
        )
    }
}

@Composable
private fun FloatingDockImpl(
    currentPosition: Int,
    onChangePosition: (Int) -> Unit,
    onReselected: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    backdrop: Backdrop,
    modifier: Modifier,
    themeColors: ExtendedColors,
    isBlurEnabled: Boolean,
) {
    val isDark = themeColors.isNightMode
    val pillShape = remember { CircleShape }
    val accentColor = MaterialTheme.colors.secondary
    val tabContentColor = MaterialTheme.colors.onSurface
    val surfaceContainer = themeColors.bottomBarSurface
    val containerColor = if (isBlurEnabled) surfaceContainer.copy(alpha = 0.4f) else surfaceContainer
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }
    var currentIndex by remember { mutableIntStateOf(currentPosition) }
    val currentPositionUpdated by rememberUpdatedState(currentPosition)
    val onChangePositionUpdated by rememberUpdatedState(onChangePosition)
    val onReselectedUpdated by rememberUpdatedState(onReselected)
    val navigationItemsUpdated by rememberUpdatedState(navigationItems)

    fun select(index: Int) {
        val items = navigationItemsUpdated
        if (items.isEmpty()) return
        val safeIndex = index.coerceIn(0, items.lastIndex)
        if (safeIndex == currentPositionUpdated) {
            onReselectedUpdated(safeIndex)
        } else {
            onChangePositionUpdated(safeIndex)
        }
        items[safeIndex].onClick?.invoke()
    }

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, navigationItems.size - 1)
    }

    val dampedDragAnimation = remember(animationScope, navigationItems.size, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = currentPosition.toFloat(),
            valueRange = 0f..(navigationItems.size - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position -> position.x in 0f..totalWidthPx },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, navigationItems.lastIndex)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    select(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (navigationItems.size - 1).toFloat()),
                    )
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
                }
            },
        )
    }

    LaunchedEffect(currentPosition) {
        if (currentIndex != currentPosition) {
            currentIndex = currentPosition
            dampedDragAnimation.animateToValue(currentPosition.toFloat())
        }
    }

    val interactiveHighlight = remember(animationScope, isLtr, dampedDragAnimation) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    x = if (isLtr) {
                        (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = size.height / 2f,
                )
            },
        )
    }
    val baseHighlight = rememberGravityRotatedHighlight(indicatorSpecular, extraDegrees = -45f)
    val pillHighlight = rememberGravityRotatedHighlight(indicatorSpecular, extraDegrees = 90f)
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    // 槽位内容（可见层与隐藏捕获层共用）：图标 + 常显名称 + 角标
    val tabContent: @Composable (Int, Boolean) -> Unit = { index, selected ->
        val item = navigationItems[index]
        val icon: @Composable () -> Unit = {
            Icon(
                painter = rememberAnimatedVectorPainter(item.icon(), selected),
                contentDescription = item.title(selected),
                modifier = Modifier.size(22.dp),
            )
        }
        if (item.badge) {
            BadgedBox(badge = {
                Badge(backgroundColor = themeColors.accent, contentColor = themeColors.background) {
                    Text(
                        text = item.badgeText.orEmpty(),
                        style = MaterialTheme.typography.caption,
                    )
                }
            }) { icon() }
        } else {
            icon()
        }
        Text(
            text = item.title(selected),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Box(modifier = modifier.width(IntrinsicSize.Min), contentAlignment = Alignment.CenterStart) {
        // 壳体：玻璃材质 + 槽位内容
        Row(
            modifier = Modifier
                .selectableGroup()
                .onSizeChanged { size ->
                    totalWidthPx = size.width.toFloat()
                    tabWidthPx = ((totalWidthPx - with(density) { 8.dp.toPx() }) / navigationItems.size).coerceAtLeast(0f)
                }
                .graphicsLayer { translationX = panelOffset }
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(
                        radius = 10.dp,
                        color = Color.Black,
                        alpha = if (isDark) 0.2f else 0.1f,
                    ),
                )
                .then(
                    if (isBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                padding = maxOf(padding, 40.dp.toPx())
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
                            },
                            highlight = { baseHighlight.value.copy(alpha = 0.75f) },
                            layerBlock = {
                                val width = size.width.coerceAtLeast(1f)
                                val scale = lerp(1f, 1f + 16.dp.toPx() / width, dampedDragAnimation.pressProgress)
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else {
                        Modifier.background(containerColor, pillShape)
                    },
                )
                .then(
                    if (isBlurEnabled) {
                        interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    },
                )
                .then(dampedDragAnimation.modifier)
                .height(DockShellHeight)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEachIndexed { index, _ ->
                val selected = index == currentPosition
                Column(
                    modifier = Modifier
                        // 点击已选中 tab 触发 onReselected（刷新）；未选中则切换 tab。
                        // 底栏重建（液态玻璃 Dock）前每个条目都有 clickable，
                        // 重建后只留了 semantics onClick（无障碍），触摸点击丢失——此处恢复。
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            select(index)
                        }
                        .semantics(mergeDescendants = true) {
                            this.selected = selected
                            role = Role.Tab
                            onClick {
                                select(index)
                                true
                            }
                        }
                        .focusable()
                        .defaultMinSize(minWidth = if (navigationItems.size > 4) 56.dp else 76.dp)
                        .fillMaxHeight()
                        .weight(1f)
                        .graphicsLayer {
                            val value = lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                            scaleX = value
                            scaleY = value
                        },
                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompositionLocalProvider(LocalContentColor provides if (selected) accentColor else tabContentColor) {
                        tabContent(index, selected)
                    }
                }
            }
        }

        // 隐藏槽位捕获：不可见的选中项内容经 layerBackdrop 供给指示器透镜
        if (isBlurEnabled) {
            Row(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx(), 4.dp.toPx())
                            lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationItems.forEachIndexed { index, _ ->
                    Column(
                        modifier = Modifier
                            .defaultMinSize(minWidth = if (navigationItems.size > 4) 56.dp else 76.dp)
                            .fillMaxHeight()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides accentColor) {
                            tabContent(index, true)
                        }
                    }
                }
            }
        }

        // 选中指示器胶囊：透镜折射 + 色散 + 内阴影 + 速度形变
        if (tabWidthPx > 0f) {
            val tabWidth = with(density) { tabWidthPx.toDp() }
            if (isBlurEnabled) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { pillShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    refractionHeight = 10.dp.toPx() * progress,
                                    refractionAmount = 14.dp.toPx() * progress,
                                    depthEffect = true,
                                    chromaticAberration = 0.5f,
                                )
                            },
                            highlight = { pillHighlight.value.copy(alpha = dampedDragAnimation.pressProgress) },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = 0.15f),
                                alpha = dampedDragAnimation.pressProgress,
                            )
                        }
                        .height(56.dp)
                        .width(tabWidth),
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .clip(pillShape)
                        .background(accentColor.copy(alpha = 0.15f), pillShape)
                        .height(56.dp)
                        .width(tabWidth),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .wrapContentWidth(align = Alignment.Start, unbounded = true)
                            .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                            .height(56.dp)
                            .graphicsLayer {
                                val progressOffset = dampedDragAnimation.value * tabWidthPx
                                translationX = if (isLtr) -progressOffset else progressOffset
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navigationItems.forEachIndexed { index, _ ->
                            Column(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = if (navigationItems.size > 4) 56.dp else 76.dp)
                                    .fillMaxHeight()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CompositionLocalProvider(LocalContentColor provides accentColor) {
                                    tabContent(index, true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}