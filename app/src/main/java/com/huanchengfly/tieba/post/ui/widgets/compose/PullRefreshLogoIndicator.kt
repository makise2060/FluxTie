package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R

/**
 * 带 logo 的下拉刷新指示器：Material 圆圈中心叠加应用小 logo。
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
            Image(
                painter = rememberDrawablePainter(
                    drawable = LocalContext.current.getDrawable(R.mipmap.ic_launcher_new_round)
                ),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = progress
                        scaleY = progress
                        alpha = progress
                    }
                    .clip(CircleShape)
            )
        }
    }
}
