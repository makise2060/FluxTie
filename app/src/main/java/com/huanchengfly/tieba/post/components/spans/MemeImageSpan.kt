package com.huanchengfly.tieba.post.components.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import kotlin.math.roundToInt

/**
 * 个人表情（贴吧表情包）图片 Span，仿 [EmoticonSpanV2] 基线对齐绘制。
 *
 * 贴纸口径展示：按原始宽高比适配进 [maxSide] 正方形（宽高缺失按 1:1），
 * 行顶随绘制高度自动抬高（行距/底部保持文字口径），贴纸不再被压成小黄脸尺寸。
 *
 * @param width 表情原始宽度（解析自 sharpText，<=0 时按 1:1 处理）
 * @param height 表情原始高度（解析自 sharpText，<=0 时按 1:1 处理）
 * @param maxSide 展示上限（px），即适配方框的边长
 */
class MemeImageSpan(
    drawable: Drawable,
    width: Int,
    height: Int,
    private val maxSide: Int,
) : ImageSpan(drawable, ImageSpan.ALIGN_BASELINE) {

    private val aspectRatio: Float =
        if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f

    /** 真实宽高比下的绘制宽度 */
    private val drawWidth: Int =
        (if (aspectRatio >= 1f) maxSide.toFloat() else maxSide * aspectRatio)
            .roundToInt().coerceAtLeast(1)

    /** 真实宽高比下的绘制高度 */
    private val drawHeight: Int =
        (if (aspectRatio >= 1f) maxSide / aspectRatio else maxSide.toFloat())
            .roundToInt().coerceAtLeast(1)

    override fun getDrawable(): Drawable {
        val drawable = super.getDrawable()
        drawable.setBounds(0, 0, drawWidth, drawHeight)
        return drawable
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            val fontFm = paint.fontMetricsInt
            fm.descent = fontFm.descent
            fm.bottom = fontFm.bottom
            // 行顶抬高以容纳整张贴纸，避免溢出压到上一行
            fm.ascent = minOf(fontFm.ascent, fontFm.descent - drawHeight)
            fm.top = minOf(fontFm.top, fm.ascent)
        }
        return drawWidth
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val fm = paint.fontMetricsInt
        val drawable = getDrawable()
        val transY = y - drawable.bounds.bottom + fm.descent
        val transX = x
        canvas.save()
        canvas.translate(transX, transY.toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }
}
