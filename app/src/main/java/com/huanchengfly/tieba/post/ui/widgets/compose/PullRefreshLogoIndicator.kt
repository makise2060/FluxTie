package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlin.math.min

/**
 * MD3 风格下拉刷新指示器：白色圆底 + 主题色弧线。
 * 下拉时弧线随进度增长，刷新时弧线旋转（CircularProgressIndicator 观感）。
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
    if (progress <= 0f && !refreshing) return

    val transition = rememberInfiniteTransition(label = "pullArc")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "pullSpin"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            elevation = 4.dp,
            modifier = Modifier.size(40.dp)
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
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
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
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
                            size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
