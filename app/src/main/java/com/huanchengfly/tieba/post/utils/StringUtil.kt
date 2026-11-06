package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.LruCache
import android.widget.TextView
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.request.execute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.spans.EmoticonSpanV2
import com.huanchengfly.tieba.post.components.spans.MemeImageSpan
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonDrawable
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonIdByName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.math.roundToInt

object StringUtil {
    @JvmStatic
    fun getEmoticonContent(
        tv: TextView,
        source: CharSequence?,
        emoticonType: Int = EmoticonUtil.EMOTICON_ALL_TYPE
    ): SpannableString {
        return try {
            if (source == null) {
                return SpannableString("")
            }
            val spannableString: SpannableString = if (source is SpannableString) {
                source
            } else {
                SpannableString(source)
            }
            val regexEmoticon = EmoticonUtil.getRegex(emoticonType)
            val patternEmoticon = Pattern.compile(regexEmoticon)
            val matcherEmoticon = patternEmoticon.matcher(spannableString)
            while (matcherEmoticon.find()) {
                val key = matcherEmoticon.group()
                val start = matcherEmoticon.start()
                val group1 = matcherEmoticon.group(1) ?: ""
                val emoticonDrawable = getEmoticonDrawable(tv.context, getEmoticonIdByName(group1))
                if (emoticonDrawable != null) {
                    val paint = tv.paint
                    val size = (-paint.ascent() + paint.descent()).roundToInt()
                    val span = EmoticonSpanV2(emoticonDrawable, size)
                    spannableString.setSpan(
                        span,
                        start,
                        start + key.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            applyMemeSpans(tv, spannableString)
            spannableString
        } catch (e: Exception) {
            e.printStackTrace()
            val spannableString: SpannableString = if (source is SpannableString) {
                source
            } else {
                SpannableString(source)
            }
            spannableString
        }
    }

    // ===== 个人表情（meme）图片预览 =====

    /** 个人表情图片缓存：URL → Drawable（仅主线程访问） */
    private val memeDrawableCache = LruCache<String, Drawable>(64)

    /** 加载中的 URL（去重，防止重复请求） */
    private val memeLoadingUrls: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val memeLoadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * 扫描 [text] 中的个人表情文本（`#(meme,...)`）并设置 [MemeImageSpan]。
     *
     * 已缓存的图片立即上屏；未缓存的触发一次异步加载，加载完成后经
     * [refreshMemeSpans] 重扫当前文本补上 span。移除旧 span 时严格按
     * [MemeImageSpan] 类型过滤，不影响经典表情 [EmoticonSpanV2]。
     */
    private fun applyMemeSpans(tv: TextView, text: Spannable) {
        text.getSpans(0, text.length, MemeImageSpan::class.java).forEach { text.removeSpan(it) }
        val matcher = Pattern.compile(EmoticonUtil.REGEX_MEME).matcher(text)
        while (matcher.find()) {
            val group = matcher.group(1) ?: continue
            val key = EmoticonManager.resolveMemeKey(group.substringBefore(',')) ?: continue
            val url = EmoticonManager.getMemeUrl(key) ?: continue
            val drawable = memeDrawableCache.get(url)
            if (drawable != null) {
                val (width, height) = EmoticonUtil.parseMemeSize(group)
                // 贴纸口径：与正文 Compose 渲染一致，按原始宽高比适配进上限方框
                val maxSide =
                    (EmoticonUtil.MEME_STICKER_SIZE_DP * tv.resources.displayMetrics.density)
                        .roundToInt()
                text.setSpan(
                    MemeImageSpan(drawable, width, height, maxSide),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                loadMemeDrawable(tv, url)
            }
        }
    }

    /** 异步加载个人表情图片，完成后重扫当前文本补 span（失败时保持纯文本降级） */
    private fun loadMemeDrawable(tv: TextView, url: String) {
        if (!memeLoadingUrls.add(url)) return
        val tvRef = WeakReference(tv)
        val context = tv.context
        val resources = tv.resources
        memeLoadScope.launch {
            try {
                val result = LoadRequest(context, url).execute()
                if (result is LoadResult.Success) {
                    memeDrawableCache.put(url, BitmapDrawable(resources, result.bitmap))
                    tvRef.get()?.post {
                        tvRef.get()?.let { refreshMemeSpans(it) }
                    }
                }
            } finally {
                memeLoadingUrls.remove(url)
            }
        }
    }

    /**
     * 重扫 [tv] 当前文本中的个人表情并刷新 span（异步加载完成回调）。
     * span 变化只触发 SpanWatcher，不触发 TextWatcher，不影响撤销栈与草稿。
     */
    private fun refreshMemeSpans(tv: TextView) {
        if (!tv.isAttachedToWindow) return
        val text = tv.text
        if (text is Spannable) {
            applyMemeSpans(tv, text)
            tv.invalidate()
        }
    }

    @JvmStatic
    fun getUsernameString(context: Context, username: String, nickname: String?): CharSequence {
        val showBoth = context.appPreferences.showBothUsernameAndNickname
        if (TextUtils.isEmpty(nickname)) {
            return if (TextUtils.isEmpty(username)) "" else username
        } else if (showBoth && !TextUtils.isEmpty(username) && !TextUtils.equals(
                username,
                nickname
            )
        ) {
            val builder = SpannableStringBuilder(nickname)
            builder.append(
                "($username)",
                ForegroundColorSpan(ThemeUtils.getColorByAttr(context, R.attr.color_text_disabled)),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return builder
        }
        return nickname ?: ""
    }

    @Stable
    fun getUsernameAnnotatedString(
        context: Context,
        username: String,
        nickname: String?,
        color: Color = Color.Unspecified
    ): AnnotatedString {
        val showBoth = App.isInitialized && context.appPreferences.showBothUsernameAndNickname
        return buildAnnotatedString {
            if (showBoth && !nickname.isNullOrBlank() && username != nickname && username.isNotBlank()) {
                append(nickname)
                withStyle(SpanStyle(color = color)) {
                    append("(${username})")
                }
            } else {
                append(nickname ?: username)
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Stable
    fun buildAnnotatedStringWithUser(
        userId: String,
        username: String,
        nickname: String?,
        content: String,
        context: Context = App.INSTANCE,
    ): AnnotatedString {
        return buildAnnotatedString {
            withAnnotation(tag = "user", annotation = userId) {
                withStyle(
                    SpanStyle(
                        color = Color(ThemeUtils.getColorByAttr(context, R.attr.colorNewPrimary))
                    )
                ) {
                    append("@")
                    append(
                        getUsernameAnnotatedString(
                            context,
                            username,
                            nickname,
                        )
                    )
                }
            }
            append(": ")
            append(content)
        }
    }

    @JvmStatic
    @Stable
    fun getAvatarUrl(portrait: String?): String {
        if (portrait.isNullOrEmpty()) {
            return ""
        }
        return if (portrait.startsWith("http://") || portrait.startsWith("https://")) {
            portrait
        } else "http://tb.himg.baidu.com/sys/portrait/item/$portrait"
    }

    @JvmStatic
    fun getBigAvatarUrl(portrait: String?): String {
        if (portrait.isNullOrEmpty()) {
            return ""
        }
        return if (portrait.startsWith("http://") || portrait.startsWith("https://")) {
            portrait
        } else "http://tb.himg.baidu.com/sys/portraith/item/$portrait"
    }

    fun String.getShortNumString(): String {
        val long = toLongOrNull() ?: return ""
        return long.getShortNumString()
    }

    fun Int.getShortNumString(): String {
        return toLong().getShortNumString()
    }

    fun Long.getShortNumString(): String {
        val long = this
        return if (long > 9999) {
            val longW = long * 10 / 10000L / 10F
            if (longW > 999) {
                val longKW = longW.toLong() * 10 / 1000L / 10F
                "${longKW}KW"
            } else {
                "${longW}W"
            }
        } else {
            "$this"
        }
    }
}