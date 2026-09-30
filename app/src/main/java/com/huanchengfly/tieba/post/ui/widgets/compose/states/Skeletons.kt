package com.huanchengfly.tieba.post.ui.widgets.compose.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material.placeholder
import com.eygraber.compose.placeholder.material.shimmer
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeaderPlaceholder

@Composable
private fun Modifier.skeleton() = this.then(
    Modifier.placeholder(
        visible = true,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
        highlight = PlaceholderHighlight.shimmer(),
    )
)

@Composable
private fun PlaceholderLine(
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .skeleton()
    )
}

/**
 * 通用楼层骨架卡:头像 + 名字 + 若干正文行 + 可选图片块。
 * 逐块模拟真实楼层内容,供详情页首屏/回复区分阶段骨架使用。
 */
@Composable
fun PostSkeletonCard(
    textLines: Int = 2,
    withImage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val lineWeights = listOf(0.95f, 1f, 0.88f, 0.6f)
    Card(
        modifier = modifier,
        header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
        content = {
            repeat(textLines) { i ->
                PlaceholderLine(
                    widthFraction = lineWeights[i % lineWeights.size],
                    height = 14.dp
                )
            }
            if (withImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .skeleton()
                )
            }
        }
    )
}

/** 帖子详情页加载骨架:主贴卡(标题/正文/图片)+ 楼层卡,逐块模拟真实内容 */
@Composable
fun ThreadPageSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
            content = {
                PlaceholderLine(widthFraction = 0.55f, height = 16.dp)
                PlaceholderLine(widthFraction = 1f, height = 14.dp)
                PlaceholderLine(widthFraction = 0.92f, height = 14.dp)
                PlaceholderLine(widthFraction = 0.68f, height = 14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .skeleton()
                )
            }
        )
        repeat(2) {
            PostSkeletonCard()
        }
    }
}

/** 楼中楼(更多回复)加载骨架:主贴摘要卡 + 扁平回复行,与目标布局一致 */
@Composable
fun SubPostsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Card(
            header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
            content = {
                PlaceholderLine(widthFraction = 1f, height = 14.dp)
                PlaceholderLine(widthFraction = 0.72f, height = 14.dp)
            }
        )
        repeat(5) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AvatarPlaceholder(size = Sizes.Small)
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaceholderLine(widthFraction = 0.35f, height = 13.dp)
                    PlaceholderLine(
                        widthFraction = if (index % 2 == 0) 0.95f else 0.62f,
                        height = 13.dp
                    )
                }
            }
            if (index < 4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(0.5.dp)
                        .skeleton()
                )
            }
        }
    }
}
