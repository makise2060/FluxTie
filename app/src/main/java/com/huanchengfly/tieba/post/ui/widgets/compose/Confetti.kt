package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class ConfettiParticle(
    val startXFraction: Float,
    val startYFraction: Float,
    val angleDeg: Float,
    val speedFraction: Float,
    val sizeDp: Float,
    val rotationDeg: Float,
    val angularVelocity: Float,
    val color: Color,
)

/**
 * 五彩纸屑爆发:左上/右上双喷口喷出矩形纸片,上抛 + 重力 + 空气阻尼 + 旋转,约 1.1s 消散。
 * triggerKey 递增触发一次;闭式物理(时间参数化),Canvas 手写无依赖。
 */
@Composable
fun ConfettiBurst(
    triggerKey: Int,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
) {
    if (triggerKey <= 0) return
    val density = LocalDensity.current
    val colors = listOf(
        ExtendedTheme.colors.primary,
        ExtendedTheme.colors.accent,
        MaterialTheme.colors.secondary,
        ExtendedTheme.colors.primary,
        ExtendedTheme.colors.accent,
        MaterialTheme.colors.secondary,
        ExtendedTheme.colors.primary,
        ExtendedTheme.colors.accent
    )
    val particles = remember(triggerKey, particleCount) {
        val rng = Random(triggerKey)
        List(particleCount) { i ->
            val fromLeft = i % 2 == 0
            ConfettiParticle(
                startXFraction = if (fromLeft) 0.32f else 0.68f,
                startYFraction = 0.30f,
                angleDeg = if (fromLeft) -(40f + rng.nextFloat() * 40f)
                else -(100f + rng.nextFloat() * 40f),
                speedFraction = 0.55f + rng.nextFloat() * 0.45f,
                sizeDp = 5f + rng.nextFloat() * 5f,
                rotationDeg = rng.nextFloat() * 360f,
                angularVelocity = (rng.nextFloat() - 0.5f) * 720f,
                color = colors[rng.nextInt(colors.size)]
            )
        }
    }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1100, easing = LinearEasing))
    }
    if (progress.value >= 1f) return
    val t = progress.value * 1.1f // 秒
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val vBase = h * 1.05f // 初速量级(px/s),保证上抛越过 logo
        val gravity = h * 1.5f
        particles.forEach { p ->
            val rad = Math.toRadians(p.angleDeg.toDouble())
            val vx0 = (cos(rad) * p.speedFraction * vBase).toFloat()
            val vy0 = (sin(rad) * p.speedFraction * vBase).toFloat()
            val dragT = t * (1f - 0.35f * t) // 线性空气阻尼的近似位移系数
            val x = p.startXFraction * w + vx0 * dragT
            val y = p.startYFraction * h + vy0 * dragT + 0.5f * gravity * t * t
            val alpha = if (t > 0.8f) ((1.1f - t) / 0.3f).coerceIn(0f, 1f) else 1f
            val rot = p.rotationDeg + p.angularVelocity * t
            val sizePx = with(density) { p.sizeDp.dp.toPx() }
            rotate(degrees = rot, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - sizePx / 2f, y - sizePx / 4f),
                    size = Size(sizePx, sizePx / 2f)
                )
            }
        }
    }
}
