package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R

/** 空态插画口径：内置三套 Lottie，新增空态从中取用，不再各自拼装。 */
enum class EmptyIllustration(val rawRes: Int, val aspectRatio: Float) {
    /** 宇航员漂浮（首页未登录等「大空态」）。 */
    Astronaut(R.raw.lottie_astronaut, 2f),

    /** 空纸箱（列表无数据）。 */
    Box(R.raw.lottie_empty_box, 2f),

    /** 遮罩（内容被隐藏/屏蔽）。 */
    Hide(R.raw.lottie_hide, 2.5f),
}

/**
 * 统一空态组件：插画 + 标题 + 副文 + 动作槽。
 *
 * 全站空态的单一入口——视觉口径（层级、留白、按钮风格）集中在此，
 * 替代各页面自行拼装 TipScreen + Lottie 的写法。
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    illustration: EmptyIllustration = EmptyIllustration.Astronaut,
    scrollable: Boolean = true,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(illustration.rawRes)
    )
    TipScreen(
        title = { Text(text = title) },
        modifier = modifier,
        image = {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(illustration.aspectRatio)
            )
        },
        message = {
            if (subtitle != null) {
                Text(text = subtitle)
            }
        },
        actions = actions,
        scrollable = scrollable,
    )
}
