package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import androidx.core.graphics.drawable.toBitmap
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

/** 启动屏字体:Roboto(Google Material 官方字体) */
private val SplashFontFamily = FontFamily(
    Font(R.font.roboto_regular),
    Font(R.font.roboto_medium, FontWeight.Medium),
)

/**
 * 应用内启动屏（单屏）：应用图标 + 标语 + FluxTie + 液态 blob Loading + 底部版本号。
 * 视觉上从系统启动屏（应用图标屏）无缝延续：同底色、图标同位。
 * 编排：图标轻柔入场；450ms 后标语淡入上浮；随后 FluxTie 淡入；720ms 后 blob 淡入持续流动形变。
 * 退场：前景内容先行收束（缩放+上浮+快淡出），背景色慢速退隐，与底层页面交叉溶解。
 * 背景与系统启动屏同源（colorSplashBg）。
 */
@Composable
fun AppSplashOverlay() {
    // 初始即满屏:与系统启动屏同色无缝衔接,避免淡入期露出底层页面
    var visible by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val contentTranslatePx = with(density) { 12.dp.toPx() }

    val transition = updateTransition(targetState = visible, label = "splash")
    val bgAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 320, easing = LinearEasing) },
        label = "splashBgAlpha"
    ) { if (it) 1f else 0f }
    val contentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 220, easing = EaseInCubic) },
        label = "splashContentAlpha"
    ) { if (it) 1f else 0f }
    val contentScale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 260, easing = FastOutSlowInEasing) },
        label = "splashContentScale"
    ) { if (it) 1f else 0.96f }
    val contentTranslate by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 260, easing = FastOutSlowInEasing) },
        label = "splashContentTranslate"
    ) { if (it) 0f else contentTranslatePx }

    LaunchedEffect(Unit) {
        SplashState.reset()
        val start = System.currentTimeMillis()
        // 循环播放直到：加载完成且达到最短展示时长；6s 超时兜底
        val minDuration = 2400L
        val timeout = 6000L
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            if ((SplashState.ready.value && elapsed >= minDuration) || elapsed >= timeout) break
            delay(100)
        }
        visible = false
    }
    if (bgAlpha > 0.01f) {
        val splashBg = colorResource(id = R.color.colorSplashBg)
        val context = LocalContext.current
        val appIconBitmap = remember {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bgAlpha }
                .background(splashBg)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = contentAlpha
                        scaleX = contentScale
                        scaleY = contentScale
                        translationY = contentTranslate
                    }
            ) {
                // 应用图标:与系统启动屏同源同位延续,轻柔缩放入场
                SplashIconEntrance {
                    AppIcon(bitmap = appIconBitmap)
                }
                Spacer(modifier = Modifier.height(24.dp))
                // 标语：微延时淡入上浮
                SplashFadeIn(delayMillis = 450) {
                    Text(
                        text = stringResource(id = R.string.splash_tagline),
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        fontFamily = SplashFontFamily,
                        color = ExtendedTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                // 应用名:标语之后、Loading 之前
                SplashFadeIn(delayMillis = 580) {
                    Text(
                        text = "FluxTie",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        fontFamily = SplashFontFamily,
                        color = ExtendedTheme.colors.text
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                // 液态 blob Loading：应用名出现后微延时淡入
                SplashFadeIn(delayMillis = 720) {
                    BlobLoading()
                }
            }
            Text(
                text = stringResource(id = R.string.summary_about_version, BuildConfig.VERSION_NAME),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SplashFontFamily,
                color = ExtendedTheme.colors.textSecondary.copy(alpha = contentAlpha),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

/** 应用图标渲染:自适应图标经 PackageManager 取真实位图(与系统启动屏一致) */
@Composable
private fun AppIcon(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(96.dp)
    )
}

/** 图标入场:缩放 0.92→1 + 淡入,轻柔不抢戏 */
@Composable
private fun SplashIconEntrance(content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.92f) }
    val iconAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, tween(durationMillis = 500, easing = FastOutSlowInEasing))
        }
        iconAlpha.animateTo(1f, tween(durationMillis = 400, easing = EaseOutCubic))
    }
    Box(modifier = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        alpha = iconAlpha.value
    }) {
        content()
    }
}

/** 延时淡入 + 上浮入场 */
@Composable
private fun SplashFadeIn(delayMillis: Int, content: @Composable () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        entered = true
    }
    AnimatedVisibility(
        visible = entered,
        enter = fadeIn(tween(durationMillis = 280)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) { it / 2 },
    ) {
        content()
    }
}

/**
 * 液态 blob Loading：8 点谐波半径驱动闭合曲线持续流动形变（液滴/水球效果），
 * 叠加光泽渐变。零额外依赖，Canvas 手绘。
 */
@Composable
private fun BlobLoading() {
    val transition = rememberInfiniteTransition(label = "blob")
    // 相位 0..2π 线性推进,双频谐波叠加产生非重复的流动形变
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "blobPhase"
    )
    val sheen by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobSheen"
    )
    val base = ExtendedTheme.colors.primary
    Canvas(modifier = Modifier.size(36.dp)) {
        drawLiquidBlob(
            phase = phase,
            brush = Brush.linearGradient(
                colors = listOf(base, lerpColor(base, Color.White, 0.22f * sheen))
            )
        )
    }
}

private fun DrawScope.drawLiquidBlob(phase: Float, brush: Brush) {
    val points = 8
    val radius = size.minDimension / 2f * 0.84f
    val vertices = (0 until points).map { i ->
        val angle = 2.0 * PI * i / points
        val wobble = 0.10f * sin(phase * 1.0f + i * 0.9f) +
                0.06f * sin(phase * 1.7f + i * 1.9f)
        val r = radius * (1f + wobble)
        Offset(
            center.x + (cos(angle) * r).toFloat(),
            center.y + (sin(angle) * r).toFloat()
        )
    }
    // 平滑闭合曲线:相邻点中点作为锚,顶点作为控制点
    val path = Path()
    val mid = { a: Offset, b: Offset -> Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f) }
    val firstMid = mid(vertices[0], vertices[1])
    path.moveTo(firstMid.x, firstMid.y)
    for (i in 1..points) {
        val control = vertices[i % points]
        val anchor = mid(control, vertices[(i + 1) % points])
        path.quadraticBezierTo(control.x, control.y, anchor.x, anchor.y)
    }
    path.close()
    drawPath(path, brush)
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color =
    Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
