package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefreshIndicatorTransform
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.min

private val IndicatorSize = 40.dp

/**
 * MD3 风格下拉刷新指示器：白色圆底 + 主题色弧线。
 * 下拉时弧线随进度增长，刷新时弧线旋转（CircularProgressIndicator 观感）。
 *
 * 位置/动效与 Material 默认指示器同源：
 * - [pullRefreshIndicatorTransform] 让圆环跟随下拉手势从页面上缘滑入，而不是钉死在固定位置；
 * - 出现/消失走 AnimatedVisibility 淡入淡出 + 缩放，避免瞬时闪现/闪现消失；
 * - zIndex 保证指示器始终绘制在页面内容之上（issue #38）。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullRefreshLogoIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    contentColor: Color = Color.Black,
) {
    val progress = state.progress
    // 仅手势下拉过的刷新才展开指示器;程序性刷新(进页预载/静默刷新)不显示,
    // 避免进入页面时顶部凭空悬一只刷新圆
    var gesturePulled by remember { mutableStateOf(false) }
    LaunchedEffect(progress, refreshing) {
        if (progress > 0f) gesturePulled = true
        else if (!refreshing) gesturePulled = false
    }
    val visible = progress > 0f || (refreshing && gesturePulled)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(120, easing = LinearOutSlowInEasing)) +
                scaleIn(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    initialScale = 0.6f
                ),
        exit = fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                scaleOut(
                    animationSpec = tween(200, easing = FastOutLinearInEasing),
                    targetScale = 0.6f
                ),
        modifier = modifier.zIndex(1f),
    ) {
        Box(
            modifier = Modifier
                .size(IndicatorSize)
                .pullRefreshIndicatorTransform(state),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = backgroundColor,
                elevation = 4.dp,
                modifier = Modifier.size(IndicatorSize)
            ) {
                val transition = rememberInfiniteTransition(label = "pullArc")
                val spin by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = LinearEasing)
                    ),
                    label = "pullSpin"
                )
                Canvas(modifier = Modifier.size(IndicatorSize)) {
                    val stroke = 3.5.dp.toPx()
                    val inset = stroke
                    val arcSize = size.minDimension - inset * 2
                    if (refreshing) {
                        // 刷新中：旋转弧线
                        drawArc(
                            color = contentColor,
                            startAngle = spin,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    } else {
                        // 下拉中：弧线随进度增长
                        val sweep = 360f * min(progress, 1f)
                        if (sweep > 2f) {
                            drawArc(
                                color = contentColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}
