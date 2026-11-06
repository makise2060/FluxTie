package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString

private val RankBadgeShape = RoundedCornerShape(10.dp)

/**
 * 名次徽章（热榜/话题榜统一口径）：
 * No.1 强调色实心 + 轻投影，No.2/3 用 MaterialKolor 容器色对（对比度由色板生成保证，
 * 任意种子色/深浅色安全），4+ 中性 chip。数字等宽（tnum），不支持时退化为普通数字。
 */
@Composable
fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ExtendedTheme.colors
    val (bg, fg, elevated) = when (rank) {
        1 -> Triple(
            colors.accent takeUnlessSpecified colors.chip,
            colors.onAccent takeUnlessSpecified colors.onChip,
            true
        )

        2 -> Triple(
            colors.primaryContainer takeUnlessSpecified colors.chip,
            colors.onPrimaryContainer takeUnlessSpecified colors.onChip,
            false
        )

        3 -> Triple(
            colors.secondaryContainer takeUnlessSpecified colors.chip,
            colors.onSecondaryContainer takeUnlessSpecified colors.onChip,
            false
        )

        else -> Triple(colors.divider.copy(alpha = 0.12f), colors.textSecondary, false)
    }
    Text(
        text = rank.toString(),
        style = MaterialTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Bold,
        color = fg,
        textAlign = TextAlign.Center,
        modifier = modifier
            .then(if (elevated) Modifier.shadow(elevation = 2.dp, shape = RankBadgeShape) else Modifier)
            .clip(RankBadgeShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** 容器色在旧动态色板管线下可能未指定，退回中性 chip 色对。 */
private infix fun Color.takeUnlessSpecified(fallback: Color): Color =
    takeIf { it.isSpecified } ?: fallback

private val Color.isSpecified: Boolean
    get() = this != Color.Unspecified

/**
 * 话题榜卡片（单列）：名次独立成列 + 标题/简介/热度。
 * 容器复用 FeedCard 口径：圆角 20dp / hairline 描边 / 左右 8dp，紧凑模式 16dp/6dp。
 */
@Composable
fun TopicCard(
    rank: Int,
    title: String,
    intro: String,
    discussNum: Long,
    tag: Int,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = LocalContext.current.appPreferences.compactFeedCard
    val corner = if (compact) 16.dp else 20.dp
    val outerPadding = if (compact) 6.dp else 8.dp
    val rowSpacing = if (compact) 8.dp else 10.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rowSpacing),
        modifier = modifier
            .padding(horizontal = outerPadding)
            .clip(RoundedCornerShape(corner))
            .background(ExtendedTheme.colors.card)
            .border(
                width = 0.5.dp,
                color = ExtendedTheme.colors.divider.copy(alpha = 0.6f),
                shape = RoundedCornerShape(corner)
            )
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier.widthIn(min = 40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            RankBadge(rank = rank)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TopicTagChip(tag = tag)
            }
            if (intro.isNotBlank()) {
                Text(
                    text = intro,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TopicHeatRow(discussNum = discussNum)
        }
        // 话题头图：64dp 尾图保留视觉辨识；无图降级为容器色渐变
        TopicThumb(imageUrl = imageUrl)
    }
}

/** 话题头图缩略块。 */
@Composable
private fun TopicThumb(imageUrl: String, modifier: Modifier = Modifier) {
    val colors = ExtendedTheme.colors
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        if (imageUrl.isNotBlank()) {
            NetworkImage(
                imageUri = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.primaryContainer takeUnlessSpecified colors.chip,
                                colors.secondaryContainer takeUnlessSpecified colors.chip
                            )
                        )
                    )
            )
        }
    }
}

/** 热/新 标签小 chip，语义色保留，形状与卡片同圆角口径。 */
@Composable
fun TopicTagChip(tag: Int, modifier: Modifier = Modifier) {
    val colors = ExtendedTheme.colors
    when (tag) {
        2 -> TopicTagChipImpl(
            text = stringResource(id = R.string.topic_tag_hot),
            color = colors.accent takeUnlessSpecified colors.chip,
            onColor = colors.onAccent takeUnlessSpecified colors.onChip,
            modifier = modifier
        )

        1 -> TopicTagChipImpl(
            text = stringResource(id = R.string.topic_tag_new),
            color = colors.primaryContainer takeUnlessSpecified colors.chip,
            onColor = colors.onPrimaryContainer takeUnlessSpecified colors.onChip,
            modifier = modifier
        )
    }
}

@Composable
private fun TopicTagChipImpl(
    text: String,
    color: Color,
    onColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = onColor,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

/** 热度行：火苗图标 + 短数字。 */
@Composable
fun TopicHeatRow(
    discussNum: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Rounded.Whatshot,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(id = R.string.hot_num, discussNum.getShortNumString()),
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary
        )
    }
}

/**
 * 话题横排卡片（热榜首页前三）：话题图打底（16:9）+ 名次徽章 + 名称 + 热度。
 * 固定宽度横向滚动展示；无图时降级为容器色渐变打底。
 */
@Composable
fun TopicHorizontalCard(
    rank: Int,
    title: String,
    imageUrl: String,
    discussNum: Long,
    tag: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExtendedTheme.colors
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(
                width = 0.5.dp,
                color = colors.divider.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .debounceClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            if (imageUrl.isNotBlank()) {
                NetworkImage(
                    imageUri = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 无图降级：容器色渐变打底，排版不变
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    colors.primaryContainer takeUnlessSpecified colors.chip,
                                    colors.secondaryContainer takeUnlessSpecified colors.chip
                                )
                            )
                        )
                )
            }
            RankBadge(
                rank = rank,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TopicTagChip(tag = tag)
            }
            TopicHeatRow(discussNum = discussNum)
        }
    }
}
