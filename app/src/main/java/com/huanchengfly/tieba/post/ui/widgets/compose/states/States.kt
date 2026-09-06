package com.huanchengfly.tieba.post.ui.widgets.compose.states

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen

val DefaultLoadingScreen: @Composable StateScreenScope.() -> Unit = {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ThreeDotsLoading()
    }
}

/** MD3 风格加载动画：三点波浪脉冲 */
@Composable
fun ThreeDotsLoading(
    modifier: Modifier = Modifier,
    color: Color = ExtendedTheme.colors.primary,
    dotSize: Dp = 12.dp,
) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val pulse by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = 0.6f + 0.4f * pulse
                        scaleY = 0.6f + 0.4f * pulse
                        alpha = 0.35f + 0.65f * pulse
                        translationY = -8f * pulse
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

val DefaultEmptyScreen: @Composable StateScreenScope.() -> Unit = {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_empty_box))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        actions = {
            if (canReload) {
                Button(onClick = { reload() }) {
                    Text(text = stringResource(id = R.string.btn_refresh))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

val DefaultErrorScreen: @Composable StateScreenScope.() -> Unit = {
    Text(
        text = stringResource(id = R.string.error_tip),
        style = MaterialTheme.typography.body1,
        color = ExtendedTheme.colors.textSecondary
    )
}

@Composable
fun StateScreen(
    isEmpty: Boolean,
    isError: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onReload: (() -> Unit)? = null,
    clickToReload: Boolean = false,
    emptyScreen: @Composable StateScreenScope.() -> Unit = DefaultEmptyScreen,
    errorScreen: @Composable StateScreenScope.() -> Unit = DefaultErrorScreen,
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    content: @Composable StateScreenScope.() -> Unit,
) {
    val stateScreenScope = remember(key1 = onReload) { StateScreenScope(onReload) }
    val clickableModifier = if (onReload != null && clickToReload) Modifier.clickable(
        enabled = isEmpty && !isLoading,
        onClick = onReload
    ) else Modifier
    Box(
        modifier = modifier
                then clickableModifier,
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            stateScreenScope.content()
        } else {
            if (isLoading) {
                stateScreenScope.loadingScreen()
            } else if (isError) {
                stateScreenScope.errorScreen()
            } else {
                stateScreenScope.emptyScreen()
            }
        }
    }
}

class StateScreenScope(
    private val onReload: (() -> Unit)? = null
) {
    val canReload: Boolean
        get() = onReload != null

    fun reload() {
        onReload?.invoke()
    }
}