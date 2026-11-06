package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.request.execute
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.EmoticonCache
import com.huanchengfly.tieba.post.models.database.MemeEmoticon
import com.huanchengfly.tieba.post.pxToDp
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.repository.MemeRepository
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

@Composable
fun calcLineHeightPx(style: TextStyle): Int {
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = textMeasurer.measure(
        AnnotatedString(stringResource(id = R.string.single_chinese_char)),
        style
    )
    return textLayoutResult.size.height
}

data class Emoticon(
    val id: String,
    val name: String
)

object EmoticonManager {
    private val DEFAULT_EMOTICON_MAPPING: Map<String, String> = mapOf(
        "呵呵" to "image_emoticon1",
        "哈哈" to "image_emoticon2",
        "吐舌" to "image_emoticon3",
        "啊" to "image_emoticon4",
        "酷" to "image_emoticon5",
        "怒" to "image_emoticon6",
        "开心" to "image_emoticon7",
        "汗" to "image_emoticon8",
        "泪" to "image_emoticon9",
        "黑线" to "image_emoticon10",
        "鄙视" to "image_emoticon11",
        "不高兴" to "image_emoticon12",
        "真棒" to "image_emoticon13",
        "钱" to "image_emoticon14",
        "疑问" to "image_emoticon15",
        "阴险" to "image_emoticon16",
        "吐" to "image_emoticon17",
        "咦" to "image_emoticon18",
        "委屈" to "image_emoticon19",
        "花心" to "image_emoticon20",
        "呼~" to "image_emoticon21",
        "笑眼" to "image_emoticon22",
        "冷" to "image_emoticon23",
        "太开心" to "image_emoticon24",
        "滑稽" to "image_emoticon25",
        "勉强" to "image_emoticon26",
        "狂汗" to "image_emoticon27",
        "乖" to "image_emoticon28",
        "睡觉" to "image_emoticon29",
        "惊哭" to "image_emoticon30",
        "生气" to "image_emoticon31",
        "惊讶" to "image_emoticon32",
        "喷" to "image_emoticon33",
        "爱心" to "image_emoticon34",
        "心碎" to "image_emoticon35",
        "玫瑰" to "image_emoticon36",
        "礼物" to "image_emoticon37",
        "彩虹" to "image_emoticon38",
        "星星月亮" to "image_emoticon39",
        "太阳" to "image_emoticon40",
        "钱币" to "image_emoticon41",
        "灯泡" to "image_emoticon42",
        "茶杯" to "image_emoticon43",
        "蛋糕" to "image_emoticon44",
        "音乐" to "image_emoticon45",
        "haha" to "image_emoticon46",
        "胜利" to "image_emoticon47",
        "大拇指" to "image_emoticon48",
        "弱" to "image_emoticon49",
        "OK" to "image_emoticon50",
        "生气" to "image_emoticon61",
        "沙发" to "image_emoticon77",
        "手纸" to "image_emoticon78",
        "香蕉" to "image_emoticon79",
        "便便" to "image_emoticon80",
        "药丸" to "image_emoticon81",
        "红领巾" to "image_emoticon82",
        "蜡烛" to "image_emoticon83",
        "三道杠" to "image_emoticon84",
        // 61 之后的新一批表情：名字来自服务端 rich_abstract 的 name/id 对照，早期只存在缓存里，
        // 这里固化为默认表，缓存作废后也不会从面板消失
        "小乖" to "image_emoticon66",
        "捂嘴笑" to "image_emoticon67",
        "你懂的" to "image_emoticon68",
        "what" to "image_emoticon69",
        "笑尿" to "image_emoticon72",
        "挖鼻" to "image_emoticon73",
        "吃瓜" to "image_emoticon86",
        "喝酒" to "image_emoticon87",
        "欢呼" to "image_emoticon95",
        "奥特曼" to "image_emoticon125",
        "不听" to "image_emoticon126",
        "望远镜" to "image_emoticon128",
        "鼠2" to "image_emoticon137",
    )

    /**
     * 表情映射表版本。
     *
     * 旧版本的缓存把「首次见到的 name/id」写死并永久覆盖默认表，一旦某次登记错了
     * （例如把 `呵呵` 记成 `image_emoticon11`），后续所有帖子都会按错误 id 渲染，
     * 且没有任何自愈路径。版本号递增后，旧缓存的 mapping 不再被信任，只保留 ids，
     * 默认表重建后再接受运行时观察到的对照。
     */
    private const val MAPPING_VERSION = 2

    private lateinit var contextRef: WeakReference<Context>
    private val emoticonIds: MutableList<String> = mutableListOf()
    private val emoticonMapping: MutableMap<String, String> = mutableMapOf()
    private val drawableCache: MutableMap<String, Drawable> = mutableMapOf()
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /** 个人表情包内存映射：sharpKey（collect_xxx / diy_xxx）→ 图片 URL */
    private val memeEmoticonMap: MutableMap<String, String> = mutableMapOf()

    /** 表情包数据版本号，供 Compose 感知 [memeEmoticonMap] 更新 */
    var memeVersion by mutableStateOf(0)
        private set

    /** 更新个人表情包内存映射（同步 / 增删后调用） */
    fun updateMemeEmoticons(list: List<MemeEmoticon>) {
        memeEmoticonMap.clear()
        list.forEach { m ->
            val url = m.thumbnail?.takeIf { it.isNotEmpty() } ?: m.picUrl
            if (url.isNotEmpty()) {
                memeEmoticonMap[MemeRepository.sharpKeyOf(m)] = url
            }
        }
        memeVersion++
    }

    /** 按 sharpKey 获取个人表情图片 URL */
    fun getMemeUrl(key: String): String? = memeEmoticonMap[key]

    /**
     * 将文本中的表情标识归一化为本地映射 key。
     *
     * 服务端下发的文本可能不带 `collect_`/`diy_` 前缀（发送时被去除），
     * 此时依次尝试收藏 / DIY 映射；均无对应表情时返回 null（不渲染）。
     */
    fun resolveMemeKey(rawKey: String): String? {
        if (rawKey.startsWith("collect_") || rawKey.startsWith("diy_")) {
            return rawKey.takeIf { memeEmoticonMap.containsKey(it) }
        }
        return ("collect_$rawKey").takeIf { memeEmoticonMap.containsKey(it) }
            ?: ("diy_$rawKey").takeIf { memeEmoticonMap.containsKey(it) }
    }

    /**
     * 为文本中出现的个人表情（`MemeEmoticon#{key}|{w}|{h}` 注解）构建 InlineContent。
     * 仅构建文本中实际出现且本地有 URL 的表情，避免全量构建。
     *
     * @param stickerSize true 时按贴纸口径展示：按 sharpText 携带的原始宽高比适配进
     * [EmoticonUtil.MEME_STICKER_SIZE_DP] 方框（宽高缺失按 1:1）；false 时退回单行行高（信息流摘要等紧凑场景）
     */
    fun buildMemeInlineContent(
        text: AnnotatedString,
        sizePx: Float,
        stickerSize: Boolean = true,
    ): Map<String, InlineTextContent> {
        val entries = text.getStringAnnotations(EmoticonUtil.INLINE_CONTENT_TAG, 0, text.length)
            .mapNotNull { annotation ->
                annotation.item
                    .takeIf { it.startsWith(EmoticonUtil.MEME_INLINE_PREFIX) }
                    ?.removePrefix(EmoticonUtil.MEME_INLINE_PREFIX)
                    ?.split('|')
                    ?.takeIf { it.size == 3 }
                    ?.let { (key, w, h) ->
                        Triple(key, w.toIntOrNull() ?: 0, h.toIntOrNull() ?: 0)
                    }
            }
            .distinctBy { it.first }
            .filter { (key, _, _) -> memeEmoticonMap.containsKey(key) }
        if (entries.isEmpty()) return emptyMap()
        val stickerMaxPx = EmoticonUtil.MEME_STICKER_SIZE_DP *
            App.INSTANCE.resources.displayMetrics.density
        return entries.associate { (key, width, height) ->
            val (drawWidth, drawHeight) = if (stickerSize) {
                fitMemeBox(width, height, stickerMaxPx)
            } else {
                sizePx to sizePx
            }
            "${EmoticonUtil.MEME_INLINE_PREFIX}$key|$width|$height" to InlineTextContent(
                placeholder = Placeholder(
                    drawWidth.pxToSp().sp,
                    drawHeight.pxToSp().sp,
                    PlaceholderVerticalAlign.TextCenter
                ),
                children = {
                    AsyncImage(
                        imageUri = memeEmoticonMap[key].orEmpty(),
                        contentDescription = stringResource(id = R.string.emoticon, key),
                        modifier = Modifier.size(
                            drawWidth.pxToDp().dp,
                            drawHeight.pxToDp().dp
                        )
                    )
                }
            )
        }
    }

    /** 原始宽高比适配进 maxSide 正方形（宽高缺失按 1:1 处理） */
    private fun fitMemeBox(width: Int, height: Int, maxSide: Float): Pair<Float, Float> =
        if (width <= 0 || height <= 0) {
            maxSide to maxSide
        } else if (width >= height) {
            maxSide to maxSide * height / width
        } else {
            maxSide * width / height to maxSide
        }

    fun getEmoticonInlineContent(
        sizePx: Float
    ): Map<String, InlineTextContent> {
        return emoticonIds.associate { id ->
            "Emoticon#$id" to InlineTextContent(
                placeholder = Placeholder(
                    sizePx.pxToSp().sp,
                    sizePx.pxToSp().sp,
                    PlaceholderVerticalAlign.TextCenter
                ),
                children = {
                    AsyncImage(
                        imageUri = rememberEmoticonUri(id = id),
                        contentDescription = stringResource(
                            id = R.string.emoticon,
                            getEmoticonNameById(id) ?: ""
                        ),
                        modifier = Modifier.size(sizePx.pxToDp().dp)
                    )
                }
            )
        }
    }

    fun init(context: Context) {
        contextRef = WeakReference(context)
        val emoticonCache = getEmoticonDataCache()
        // 默认集合先落地，再用缓存覆盖：缓存里有服务端学到的真实 name/id 对照（含对默认表的修正），
        // 但新版本新增的默认表情也不能因为「缓存非空」而永远缺席
        for (i in 1..50) {
            emoticonIds.add("image_emoticon$i")
        }
        for (i in 61..101) {
            emoticonIds.add("image_emoticon$i")
        }
        for (i in 125..137) {
            emoticonIds.add("image_emoticon$i")
        }
        emoticonIds.addAll(emoticonCache.ids)
        // 每次 init 都会把默认集合再加一遍：缓存文件里的列表必须去重，否则每启动几次就翻倍膨胀
        emoticonIds.distinct().let {
            emoticonIds.clear()
            emoticonIds.addAll(it)
        }
        emoticonMapping.putAll(DEFAULT_EMOTICON_MAPPING)
        // 只信任当前版本写出的映射；旧缓存的对照可能是错误登记（issue #31/#33），重建后自然修正
        if (emoticonCache.version >= MAPPING_VERSION) {
            emoticonMapping.putAll(emoticonCache.mapping)
        }
        updateCache()
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { fetchEmoticons(context) }
                runCatching { MemeRepository.ensureLoaded() }
            }
        }
    }

    private fun updateCache() {
        runCatching {
            val emoticonDataCacheFile = File(getEmoticonCacheDir(), "emoticon_data_cache")
            if (emoticonDataCacheFile.exists() || emoticonDataCacheFile.createNewFile()) {
                FileUtil.writeFile(
                    emoticonDataCacheFile,
                    EmoticonCache(MAPPING_VERSION, emoticonIds, emoticonMapping).toJson(),
                    false
                )
            }
        }
    }

    private fun getContext(): Context {
        return contextRef.get() ?: App.INSTANCE
    }

    private fun getEmoticonDataCache(): EmoticonCache {
        return runCatching {
            File(getEmoticonCacheDir(), "emoticon_data_cache").takeIf { it.exists() }
                ?.fromJson<EmoticonCache>()
        }.getOrNull() ?: EmoticonCache()
    }

    private fun getEmoticonCacheDir(): File {
        return File(getContext().externalCacheDir ?: getContext().cacheDir, "emoticon").apply {
            if (exists() && isFile) {
                delete()
                mkdirs()
            } else if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun getEmoticonFile(id: String): File {
        return File(getEmoticonCacheDir(), "$id.png")
    }

    fun getAllEmoticon(): List<Emoticon> {
        // 以「名字」为准出面板：发送走 #(名字)，只要名字可发就必须可选；
        // 反之按 id 反查名字会丢名字（多个名字指向同一 id 时只留第一个，issue #31）。
        return emoticonMapping.map { (name, id) ->
            Emoticon(id = id, name = name)
        }
    }

    fun getEmoticonIdByName(name: String): String? {
        return emoticonMapping[name]
    }

    private fun getEmoticonNameById(id: String): String? {
        return emoticonMapping.firstNotNullOfOrNull { (name, emoticonId) ->
            if (emoticonId == id) {
                name
            } else {
                null
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getEmoticonResId(context: Context, id: String): Int {
        return context.resources.getIdentifier(id, "drawable", context.packageName)
    }

    fun getEmoticonDrawable(context: Context, id: String?): Drawable? {
        if (id == null) {
            return null
        }
        if (drawableCache.containsKey(id)) {
            return drawableCache[id]
        }
        val resId = getEmoticonResId(context, id)
        if (resId != 0) {
            return AppCompatResources.getDrawable(context, resId).also { drawableCache[id] = it!! }
        }
        val emoticonFile = getEmoticonFile(id)
        if (!emoticonFile.exists()) {
            return null
        }
        return BitmapDrawable(
            getContext().resources,
            emoticonFile.inputStream()
        ).also { drawableCache[id] = it }
    }

    fun getEmoticonUri(context: Context, id: String?): String {
        id ?: return ""
        val resId = getEmoticonResId(context, id)
        if (resId != 0) {
            return newResourceUri(resId)
        }
        val emoticonFile = getEmoticonFile(id)
        if (!emoticonFile.exists()) {
            return ""
        }
        return newFileUri(emoticonFile)
    }

    @Composable
    fun rememberEmoticonPainter(id: String): Painter {
        val context = LocalContext.current
        val drawable = remember(id) { getEmoticonDrawable(context, id) }
        return rememberDrawablePainter(drawable = drawable)
    }

    @Composable
    fun rememberEmoticonUri(id: String): String {
        val context = LocalContext.current
        return remember(id) { getEmoticonUri(context, id) }
    }

    private suspend fun fetchEmoticons(context: Context) {
        emoticonIds.forEach {
            val resId = getEmoticonResId(context, it)
            val emoticonFile = getEmoticonFile(it)
            if (resId == 0 && !emoticonFile.exists()) {
                val loadEmoticonResult = LoadRequest(
                    context,
                    "http://static.tieba.baidu.com/tb/editor/images/client/$it.png"
                ).execute()
                if (loadEmoticonResult is LoadResult.Success) {
                    ImageUtil.bitmapToFile(
                        loadEmoticonResult.bitmap,
                        emoticonFile,
                        Bitmap.CompressFormat.PNG
                    )
                }
            }
        }
    }

    fun registerEmoticon(id: String, name: String) {
        // 没有名字的表情无法通过 #(名字) 发送，登记进面板只会多一个发不出去的格子
        if (name.isBlank()) return
        val realId = if (id == "image_emoticon") "image_emoticon1" else id
        var changed = false
        if (!emoticonIds.contains(realId)) {
            emoticonIds.add(realId)
            changed = true
        }
        val existing = emoticonMapping[name]
        if (existing != realId) {
            // 同名不同 id：以服务端下发的 id 为准。否则本地旧映射会让「面板选中的那张图」
            // 和「发送后服务端按名字还原的那张图」不是同一张（issue #31）。
            // 只有规范的 image_emoticonN 才覆盖，避免脏数据把名字改到兜底 id 上。
            if (CANONICAL_EMOTICON_ID.matches(realId)) {
                emoticonMapping[name] = realId
                changed = true
            }
            if (existing != null) {
                android.util.Log.w(
                    "EmoticonManager",
                    "emoticon id conflict: name=$name local=$existing server=$realId"
                )
            }
        }
        if (changed) {
            coroutineScope.launch {
                updateCache()
            }
        }
    }

    private val CANONICAL_EMOTICON_ID = Regex("image_emoticon\\d+")
}