package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min

/**
 * 启动屏就绪信号：首页数据加载完成后置位，启动屏随之淡出。
 */
object SplashState {
    val ready = MutableStateFlow(false)

    fun markReady() {
        ready.value = true
    }

    fun reset() {
        ready.value = false
    }
}

/**
 * FluxDo 风格应用内启动屏：顶部品牌动画 + 应用名 + blob 加载图形 + 底部版本号。
 * 背景与系统启动屏同源（colorSplashBg）；动画循环播放，直到首页加载完成（含最短展示与超时保护）后淡出。
 */
@Composable
fun AppSplashOverlay() {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "splashAlpha"
    )
    LaunchedEffect(Unit) {
        SplashState.reset()
        visible = true
        val start = System.currentTimeMillis()
        // 循环播放直到：加载完成且达到最短展示时长；6s 超时兜底
        val minDuration = 1600L
        val timeout = 6000L
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            if ((SplashState.ready.value && elapsed >= minDuration) || elapsed >= timeout) break
            delay(100)
        }
        visible = false
    }
    if (alpha > 0.01f) {
        val splashBg = colorResource(id = R.color.colorSplashBg)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(splashBg)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                BrandAnimation()
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "FluxTie",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Black,
                    color = ExtendedTheme.colors.text
                )
                Spacer(modifier = Modifier.height(72.dp))
                BlobLoading()
            }
            Text(
                text = stringResource(id = R.string.summary_about_version, BuildConfig.VERSION_NAME),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

/** 品牌动画：圆环描边 + 内部横线上下往返（FluxDo 式极简图形） */
@Composable
private fun BrandAnimation() {
    val transition = rememberInfiniteTransition(label = "brand")
    val bounce by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brandBounce"
    )
    val color = ExtendedTheme.colors.primary
    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 6.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        // 外圆
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // 内部横线：在圆内上下往返
        val lineHalfWidth = radius * 0.72f
        val lineY = center.y + bounce * (radius * 0.55f)
        drawLine(
            color = color,
            start = Offset(center.x - lineHalfWidth, lineY),
            end = Offset(center.x + lineHalfWidth, lineY),
            strokeWidth = strokeWidth * 0.75f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BlobLoading() {
    val transition = rememberInfiniteTransition(label = "blob")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobPulse"
    )
    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = 1f + 0.22f * pulse
                scaleY = 1f - 0.18f * pulse
            }
            .clip(BlobShape(morph = pulse))
            .background(ExtendedTheme.colors.primary)
    )
}

/** 圆角随脉冲在 30%~50% 间呼吸，模拟 blob 形变 */
private fun BlobShape(morph: Float): Shape =
    RoundedCornerShape(percent = (30 + 20 * morph).toInt())
