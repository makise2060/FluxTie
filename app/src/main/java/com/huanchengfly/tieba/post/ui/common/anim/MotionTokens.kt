package com.huanchengfly.tieba.post.ui.common.anim

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * 全项目动效口径单一来源（MotionTokens）。
 *
 * 规则：新增动效一律从此处取时长与曲线，不允许散点硬编码；
 * 曲线采用 M3 emphasized 家族（进入减速 / 退出加速）。
 */
object MotionTokens {
    /** 快速微动效（开关、指示器小幅反馈）。 */
    const val DurationFast = 150

    /** 常规过渡（Tab 淡入、退场淡出）。 */
    const val DurationNormal = 220

    /** 短促淡出（Tab 旧内容移出）。 */
    const val DurationExit = 140

    /** 慢速过渡（启动屏背景、交接类动画）。 */
    const val DurationSlow = 300

    /** 交接位移/缩放（启动屏 Logo 收束）。 */
    const val DurationHandover = 260

    /** 进入：强调减速（快起步、缓收尾）。 */
    val EasingEnter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** 退出：强调加速（缓起步、快离场）。 */
    val EasingExit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** 标准曲线（位移/缩放类）。 */
    val EasingStandard = FastOutSlowInEasing

    /** 无回弹快速落定（Dock 指示器、槽位对齐）。 */
    fun <T> springSnap(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1000f)
}
