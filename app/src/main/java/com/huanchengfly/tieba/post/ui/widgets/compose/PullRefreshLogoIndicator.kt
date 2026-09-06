package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 下拉刷新指示器：Material 圆球中心叠加 MD3 刷新图标，随下拉进度旋转浮现。
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PullRefreshIndicator(
            refreshing = refreshing,
            state = state,
            modifier = Modifier.align(Alignment.Center),
            backgroundColor = backgroundColor,
            contentColor = contentColor,
        )
        val progress = state.progress
        if (progress > 0.01f || refreshing) {
            Icon(
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(18.dp)
                    .graphicsLayer {
                        rotationZ = progress * 300f
                        alpha = progress.coerceIn(0f, 1f)
                    }
            )
        }
    }
}
