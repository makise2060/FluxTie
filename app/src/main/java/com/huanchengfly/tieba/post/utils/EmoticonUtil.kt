package com.huanchengfly.tieba.post.utils

import androidx.annotation.DrawableRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import com.huanchengfly.tieba.post.R
import org.intellij.lang.annotations.RegExp
import java.util.regex.Pattern

object EmoticonUtil {
    const val EMOTICON_ALL_TYPE = 0
    const val EMOTICON_CLASSIC_TYPE = 1
    const val EMOTICON_EMOJI_TYPE = 2
    const val EMOTICON_ALL_WEB_TYPE = 3
    const val EMOTICON_CLASSIC_WEB_TYPE = 4
    const val EMOTICON_EMOJI_WEB_TYPE = 5
    const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"

    /** 个人表情（贴吧表情包）InlineContent 键前缀，完整键为 `MemeEmoticon#{sharpKey}` */
    const val MEME_INLINE_PREFIX = "MemeEmoticon#"

    /**
     * 个人表情贴纸在正文中的展示上限（dp）：按原始宽高比适配进该正方形。
     * 贴纸类内容按经典小黄脸的行高渲染会小得离谱，统一放大到贴纸口径。
     */
    const val MEME_STICKER_SIZE_DP = 96

    @RegExp
    private val REGEX_WEB = "\\(#(([\u4e00-\u9fa5\\w\u007e])+)\\)"

    @RegExp
    private val REGEX = "#\\((([一-龥\\w~])+)\\)"

    /**
     * 个人表情文本：
     * - 本地 sharpText：`#(meme,collect_xxx,150,150,md5)` / `#(meme,diy_xxx,...)`
     * - 服务端下发（发送时前缀被去除）：`#(meme,xxx,150,150,md5)` / `#(meme,{pkgId}_xxx,...)`
     * 排除表情包表情（net_ 前缀）
     */
    @RegExp
    internal val REGEX_MEME = "#\\(meme,(?!net_)((collect_|diy_)?[a-zA-Z0-9_,]+)\\)"
    private val EMPTY_MAP: Map<String, Int> = emptyMap()
    private val EMOTICON_ALL_WEB_MAP: MutableMap<String, Int> = mutableMapOf()
    private val EMOTICON_CLASSIC_WEB_MAP: MutableMap<String, Int> = mutableMapOf()
    private val EMOTICON_EMOJI_WEB_MAP: MutableMap<String, Int> = mutableMapOf()

    init {
        EMOTICON_CLASSIC_WEB_MAP["(#滑稽)"] = R.drawable.image_emoticon25
        EMOTICON_CLASSIC_WEB_MAP["(#呵呵)"] = R.drawable.image_emoticon1
        EMOTICON_CLASSIC_WEB_MAP["(#哈哈)"] = R.drawable.image_emoticon2
        EMOTICON_CLASSIC_WEB_MAP["(#啊)"] = R.drawable.image_emoticon4
        EMOTICON_CLASSIC_WEB_MAP["(#开心)"] = R.drawable.image_emoticon7
        EMOTICON_CLASSIC_WEB_MAP["(#酷)"] = R.drawable.image_emoticon5
        EMOTICON_CLASSIC_WEB_MAP["(#汗)"] = R.drawable.image_emoticon8
        EMOTICON_CLASSIC_WEB_MAP["(#怒)"] = R.drawable.image_emoticon6
        EMOTICON_CLASSIC_WEB_MAP["(#鄙视)"] = R.drawable.image_emoticon11
        EMOTICON_CLASSIC_WEB_MAP["(#不高兴)"] = R.drawable.image_emoticon12
        EMOTICON_CLASSIC_WEB_MAP["(#泪)"] = R.drawable.image_emoticon9
        EMOTICON_CLASSIC_WEB_MAP["(#吐舌)"] = R.drawable.image_emoticon3
        EMOTICON_CLASSIC_WEB_MAP["(#黑线)"] = R.drawable.image_emoticon10
        EMOTICON_CLASSIC_WEB_MAP["(#乖)"] = R.drawable.image_emoticon28
        EMOTICON_CLASSIC_WEB_MAP["(#呼~)"] = R.drawable.image_emoticon21
        EMOTICON_CLASSIC_WEB_MAP["(#花心)"] = R.drawable.image_emoticon20
        EMOTICON_CLASSIC_WEB_MAP["(#惊哭)"] = R.drawable.image_emoticon30
        EMOTICON_CLASSIC_WEB_MAP["(#惊讶)"] = R.drawable.image_emoticon32
        EMOTICON_CLASSIC_WEB_MAP["(#狂汗)"] = R.drawable.image_emoticon27
        EMOTICON_CLASSIC_WEB_MAP["(#冷)"] = R.drawable.image_emoticon23
        EMOTICON_CLASSIC_WEB_MAP["(#勉强)"] = R.drawable.image_emoticon26
        EMOTICON_CLASSIC_WEB_MAP["(#喷)"] = R.drawable.image_emoticon33
        EMOTICON_CLASSIC_WEB_MAP["(#噗)"] = R.drawable.image_emoticon89
        EMOTICON_CLASSIC_WEB_MAP["(#钱)"] = R.drawable.image_emoticon14
        EMOTICON_CLASSIC_WEB_MAP["(#生气)"] = R.drawable.image_emoticon31
        EMOTICON_CLASSIC_WEB_MAP["(#睡觉)"] = R.drawable.image_emoticon29
        EMOTICON_CLASSIC_WEB_MAP["(#太开心)"] = R.drawable.image_emoticon24
        EMOTICON_CLASSIC_WEB_MAP["(#吐)"] = R.drawable.image_emoticon17
        EMOTICON_CLASSIC_WEB_MAP["(#委屈)"] = R.drawable.image_emoticon19
        EMOTICON_CLASSIC_WEB_MAP["(#笑眼)"] = R.drawable.image_emoticon22
        EMOTICON_CLASSIC_WEB_MAP["(#咦)"] = R.drawable.image_emoticon18
        EMOTICON_CLASSIC_WEB_MAP["(#阴险)"] = R.drawable.image_emoticon16
        EMOTICON_CLASSIC_WEB_MAP["(#疑问)"] = R.drawable.image_emoticon15
        EMOTICON_CLASSIC_WEB_MAP["(#真棒)"] = R.drawable.image_emoticon13
        EMOTICON_EMOJI_WEB_MAP["(#爱心)"] = R.drawable.image_emoticon34
        EMOTICON_EMOJI_WEB_MAP["(#心碎)"] = R.drawable.image_emoticon35
        EMOTICON_EMOJI_WEB_MAP["(#玫瑰)"] = R.drawable.image_emoticon36
        EMOTICON_EMOJI_WEB_MAP["(#礼物)"] = R.drawable.image_emoticon37
        EMOTICON_EMOJI_WEB_MAP["(#彩虹)"] = R.drawable.image_emoticon38
        EMOTICON_EMOJI_WEB_MAP["(#星星月亮)"] = R.drawable.image_emoticon39
        EMOTICON_EMOJI_WEB_MAP["(#太阳)"] = R.drawable.image_emoticon40
        EMOTICON_EMOJI_WEB_MAP["(#钱币)"] = R.drawable.image_emoticon41
        EMOTICON_EMOJI_WEB_MAP["(#灯泡)"] = R.drawable.image_emoticon42
        EMOTICON_EMOJI_WEB_MAP["(#茶杯)"] = R.drawable.image_emoticon43
        EMOTICON_EMOJI_WEB_MAP["(#蛋糕)"] = R.drawable.image_emoticon44
        EMOTICON_EMOJI_WEB_MAP["(#音乐)"] = R.drawable.image_emoticon45
        EMOTICON_EMOJI_WEB_MAP["(#haha)"] = R.drawable.image_emoticon46
        EMOTICON_EMOJI_WEB_MAP["(#胜利)"] = R.drawable.image_emoticon47
        EMOTICON_EMOJI_WEB_MAP["(#大拇指)"] = R.drawable.image_emoticon48
        EMOTICON_EMOJI_WEB_MAP["(#弱)"] = R.drawable.image_emoticon49
        EMOTICON_EMOJI_WEB_MAP["(#OK)"] = R.drawable.image_emoticon50
        EMOTICON_ALL_WEB_MAP.putAll(EMOTICON_CLASSIC_WEB_MAP)
        EMOTICON_ALL_WEB_MAP.putAll(EMOTICON_EMOJI_WEB_MAP)
    }

    @RegExp
    fun getRegex(type: Int): String {
        when (type) {
            EMOTICON_ALL_TYPE, EMOTICON_CLASSIC_TYPE, EMOTICON_EMOJI_TYPE -> return REGEX
            EMOTICON_ALL_WEB_TYPE, EMOTICON_CLASSIC_WEB_TYPE, EMOTICON_EMOJI_WEB_TYPE -> return REGEX_WEB
        }
        return REGEX
    }

    @JvmStatic
    @DrawableRes
    fun getImgByName(EmoticonType: Int, imgName: String): Int {
        var integer: Int? = null
        when (EmoticonType) {
            EMOTICON_ALL_WEB_TYPE -> integer = EMOTICON_ALL_WEB_MAP[imgName]
            EMOTICON_CLASSIC_WEB_TYPE -> integer = EMOTICON_CLASSIC_WEB_MAP[imgName]
            EMOTICON_EMOJI_WEB_TYPE -> integer = EMOTICON_EMOJI_WEB_MAP[imgName]
            else -> {}
        }
        return integer ?: -1
    }

    @JvmStatic
    fun getEmojiMap(emoticonType: Int): Map<String, Int> {
        return when (emoticonType) {
            EMOTICON_ALL_WEB_TYPE -> EMOTICON_ALL_WEB_MAP
            EMOTICON_CLASSIC_WEB_TYPE -> EMOTICON_CLASSIC_WEB_MAP
            EMOTICON_EMOJI_WEB_TYPE -> EMOTICON_EMOJI_WEB_MAP
            else -> EMPTY_MAP
        }
    }

    @OptIn(ExperimentalTextApi::class)
    val String.emoticonString: AnnotatedString
        get() {
            val regexPattern = Pattern.compile(getRegex(EMOTICON_ALL_TYPE))
            val matcher = regexPattern.matcher(this)
            return buildAnnotatedString {
                withAnnotation("Emoticon", "true") {
                    append(this@emoticonString)
                }
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    val emoticonName = matcher.group(1)
                    if (emoticonName != null) {
                        addStringAnnotation(
                            INLINE_CONTENT_TAG,
                            "Emoticon#${EmoticonManager.getEmoticonIdByName(emoticonName)}",
                            start,
                            end,
                        )
                    }
                }
                appendMemeAnnotations(this@emoticonString)
            }
        }

    @OptIn(ExperimentalTextApi::class)
    val AnnotatedString.emoticonString: AnnotatedString
        get() {
            if (hasStringAnnotations("Emoticon", 0, length)) {
                return this
            }
            val regexPattern = Pattern.compile(getRegex(EMOTICON_ALL_TYPE))
            val matcher = regexPattern.matcher(this.text)
            return buildAnnotatedString {
                withAnnotation("Emoticon", "true") {
                    append(this@emoticonString)
                }
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    val emoticonName = matcher.group(1)
                    if (emoticonName != null) {
                        addStringAnnotation(
                            INLINE_CONTENT_TAG,
                            "Emoticon#${EmoticonManager.getEmoticonIdByName(emoticonName)}",
                            start,
                            end,
                        )
                    }
                }
                appendMemeAnnotations(this@emoticonString)
            }
        }

    /** 扫描个人表情文本并添加 InlineContent 注解（与内置 `#(名称)` 表情互斥，独立扫描） */
    private fun AnnotatedString.Builder.appendMemeAnnotations(text: CharSequence) {
        val matcher = Pattern.compile(REGEX_MEME).matcher(text)
        while (matcher.find()) {
            val group = matcher.group(1) ?: continue
            val key = EmoticonManager.resolveMemeKey(group.substringBefore(',')) ?: continue
            // 注解携带原始宽高，供渲染端按贴纸口径定尺寸：`MemeEmoticon#{key}|{w}|{h}`
            val (width, height) = parseMemeSize(group)
            addStringAnnotation(
                INLINE_CONTENT_TAG,
                "$MEME_INLINE_PREFIX$key|$width|$height",
                matcher.start(),
                matcher.end(),
            )
        }
    }

    /**
     * 解析个人表情 sharpText 的原始宽高：`collect_xxx,150,150,md5` → 150 x 150。
     * 缺失或非法时返回 0，由渲染端按 1:1 兜底。
     */
    fun parseMemeSize(group: String): Pair<Int, Int> {
        val parts = group.substringAfter(',', "").split(',')
        val width = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val height = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return width to height
    }
}