package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.coroutines.delay

/**
 * FluxDo 风格应用内启动屏：logo + 应用名 + blob 形变动画 + 底部版本号。
 * 覆盖在页面内容之上，约 1.3s 后淡出。放在 Composable 树末尾以保证绘制在最上层。
 */
@Composable
fun AppSplashOverlay() {
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "splashAlpha"
    )
    LaunchedEffect(Unit) {
        delay(1800)
        visible = false
    }
    if (alpha > 0.01f) {
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(ExtendedTheme.colors.windowBackground)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Image(
                    painter = rememberDrawablePainter(
                        drawable = context.getDrawable(R.mipmap.ic_launcher_new_round)
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "FluxTie",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Black,
                    color = ExtendedTheme.colors.text
                )
                Spacer(modifier = Modifier.height(56.dp))
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
            .background(MaterialTheme.colors.primary)
    )
}

/** 圆角随脉冲在 30%~50% 间呼吸，模拟 blob 形变 */
private fun BlobShape(morph: Float): Shape =
    RoundedCornerShape(percent = (30 + 20 * morph).toInt())
