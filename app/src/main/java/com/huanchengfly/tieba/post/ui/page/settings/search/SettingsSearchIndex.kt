package com.huanchengfly.tieba.post.ui.page.settings.search

import android.content.Context
import android.os.Build
import android.icu.text.Transliterator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MemeEmoticonPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SettingsPageDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 设置搜索结果的跳转目标：携带锚点（分组 key）到目标页定位。 */
internal enum class SettingsDestination(val navigate: (DestinationsNavigator, String) -> Unit) {
    SETTINGS({ navigator, anchor -> navigator.navigate(SettingsPageDestination(anchor = anchor)) }),
    CUSTOM({ navigator, anchor -> navigator.navigate(CustomSettingsPageDestination(anchor = anchor)) }),
    HABIT({ navigator, anchor -> navigator.navigate(HabitSettingsPageDestination(anchor = anchor)) }),
    MEME({ navigator, _ -> navigator.navigate(MemeEmoticonPageDestination) }),
    BLOCK({ navigator, _ -> navigator.navigate(BlockSettingsPageDestination) }),
    OKSIGN({ navigator, _ -> navigator.navigate(OKSignSettingsPageDestination) }),
    ABOUT({ navigator, _ -> navigator.navigate(AboutPageDestination) }),
    ACCOUNT({ navigator, _ -> navigator.navigate(AccountManagePageDestination) }),
}

/**
 * 搜索条目：标题/摘要引用既有字符串资源（避免双份文案），keywords 为额外检索词（含俗名），
 * anchor 为目标页内分组 itemKey，空串表示仅跳页。
 */
internal data class SettingsSearchEntry(
    val titleRes: Int,
    val destination: SettingsDestination,
    val summaryRes: Int? = null,
    val keywords: List<String> = emptyList(),
    val anchor: String = "",
)

internal data class SettingsSearchIndexedEntry(
    val entry: SettingsSearchEntry,
    val title: String,
    val summary: String,
    val pinyin: String,
    val initials: String,
)

internal object SettingsSearchIndex {

    // Han-Latin 转写仅 API 24+ 可用；低版本退化为原文匹配
    private val transliterator: Transliterator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower") }.getOrNull()
        } else null

    val entries: List<SettingsSearchEntry> = listOf(
        // ── 设置主页
        SettingsSearchEntry(R.string.title_account_manage, SettingsDestination.ACCOUNT, keywords = listOf("账号", "登录", "bduss")),
        SettingsSearchEntry(R.string.title_settings_custom, SettingsDestination.CUSTOM, summaryRes = R.string.summary_settings_custom, keywords = listOf("主题", "外观", "颜色", "深色", "浅色")),
        SettingsSearchEntry(R.string.title_settings_read_habit, SettingsDestination.HABIT, summaryRes = R.string.summary_settings_habit, keywords = listOf("浏览", "习惯")),
        SettingsSearchEntry(R.string.title_meme_emoticon, SettingsDestination.MEME, summaryRes = R.string.summary_meme_emoticon, keywords = listOf("表情", "斗图")),
        SettingsSearchEntry(R.string.title_block_settings, SettingsDestination.BLOCK, summaryRes = R.string.summary_block_settings, keywords = listOf("屏蔽", "黑名单", "关键词")),
        SettingsSearchEntry(R.string.title_oksign, SettingsDestination.OKSIGN, summaryRes = R.string.summary_settings_oksign, keywords = listOf("签到", "自动签到")),
        SettingsSearchEntry(R.string.title_use_webview, SettingsDestination.SETTINGS, keywords = listOf("浏览器", "链接"), anchor = "general"),
        SettingsSearchEntry(R.string.title_use_custom_tabs, SettingsDestination.SETTINGS, keywords = listOf("chrome", "浏览器"), anchor = "general"),
        SettingsSearchEntry(R.string.title_open_by_default, SettingsDestination.SETTINGS, keywords = listOf("链接", "跳转"), anchor = "general"),
        SettingsSearchEntry(R.string.title_auto_check_update, SettingsDestination.SETTINGS, keywords = listOf("更新", "版本"), anchor = "general"),
        SettingsSearchEntry(R.string.title_auto_check_interval, SettingsDestination.SETTINGS, keywords = listOf("更新", "频率"), anchor = "general"),
        SettingsSearchEntry(R.string.title_auto_download_update, SettingsDestination.SETTINGS, keywords = listOf("更新", "后台下载"), anchor = "general"),
        SettingsSearchEntry(R.string.title_clear_picture_cache, SettingsDestination.SETTINGS, keywords = listOf("缓存", "空间"), anchor = "general"),
        SettingsSearchEntry(R.string.title_clear_cache_on_exit, SettingsDestination.SETTINGS, keywords = listOf("缓存", "退出"), anchor = "general"),
        SettingsSearchEntry(R.string.title_about, SettingsDestination.ABOUT, keywords = listOf("版本", "开源")),

        // ── 外观设置
        SettingsSearchEntry(R.string.title_theme_mode, SettingsDestination.CUSTOM, keywords = listOf("深色", "浅色", "夜间", "自动"), anchor = "theme_mode"),
        SettingsSearchEntry(R.string.title_dark_style, SettingsDestination.CUSTOM, keywords = listOf("纯黑", "amoled", "深邃灰"), anchor = "dark_style"),
        SettingsSearchEntry(R.string.title_theme_color, SettingsDestination.CUSTOM, keywords = listOf("主题色", "种子色", "自定义颜色"), anchor = "theme_color"),
        SettingsSearchEntry(R.string.title_scheme_style, SettingsDestination.CUSTOM, keywords = listOf("柔和", "中性", "鲜明", "monet"), anchor = "palette_style"),
        SettingsSearchEntry(R.string.settings_app_icon, SettingsDestination.CUSTOM, keywords = listOf("图标", "桌面图标"), anchor = "app_icon"),
        SettingsSearchEntry(R.string.title_settings_status_bar_darker, SettingsDestination.CUSTOM, keywords = listOf("状态栏"), anchor = "more"),
        SettingsSearchEntry(R.string.tip_toolbar_primary_color, SettingsDestination.CUSTOM, keywords = listOf("顶栏", "工具栏颜色"), anchor = "more"),
        SettingsSearchEntry(R.string.settings_forum_single, SettingsDestination.CUSTOM, keywords = listOf("单列", "双列"), anchor = "more"),
        SettingsSearchEntry(R.string.title_hide_explore, SettingsDestination.CUSTOM, keywords = listOf("动态", "首页"), anchor = "more"),
        SettingsSearchEntry(R.string.title_floating_bottom_nav, SettingsDestination.CUSTOM, keywords = listOf("底栏", "导航栏"), anchor = "more"),
        SettingsSearchEntry(R.string.title_splash_animation, SettingsDestination.CUSTOM, keywords = listOf("启动屏", "开屏"), anchor = "more"),

        // ── 使用习惯
        SettingsSearchEntry(R.string.title_auto_hide_bottom_bar, SettingsDestination.HABIT, summaryRes = R.string.tip_auto_hide_bottom_bar_summary, keywords = listOf("底栏", "滚动"), anchor = "browse"),
        SettingsSearchEntry(R.string.settings_home_page_show_history_forum, SettingsDestination.HABIT, keywords = listOf("经过贴吧", "最近逛的吧"), anchor = "browse"),
    )

    private fun indexedEntry(context: Context, entry: SettingsSearchEntry): SettingsSearchIndexedEntry {
        val title = context.getString(entry.titleRes)
        val summary = entry.summaryRes?.let { context.getString(it) }.orEmpty()
        val py = transliterator?.transliterate(title)
            ?.replace(Regex("[^a-z0-9\\s]"), "")
            ?.trim()
            .orEmpty()
        val initials = py.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
        return SettingsSearchIndexedEntry(entry, title, summary, py.replace(" ", ""), initials)
    }

    fun build(context: Context): List<SettingsSearchIndexedEntry> =
        entries.map { indexedEntry(context, it) }

    /** 多词检索打分：值越小越靠前，null 表示不命中。 */
    fun search(query: String, index: List<SettingsSearchIndexedEntry>): List<SettingsSearchIndexedEntry> {
        val query = query.trim().lowercase()
        if (query.isEmpty()) return emptyList()
        val terms = query.split(Regex("\\s+")).filter { it.isNotBlank() }
        return index.mapNotNull { item ->
            var total = 0
            for (term in terms) {
                val score = scoreOf(term, item) ?: return@mapNotNull null
                total += score
            }
            total to item
        }
            .sortedWith(compareBy({ it.first }, { it.second.title }))
            .map { it.second }
    }

    private fun scoreOf(term: String, item: SettingsSearchIndexedEntry): Int? = when {
        item.title.contains(term) -> 0
        item.summary.contains(term) -> 1
        item.entry.keywords.any { it.contains(term) || term.contains(it) } -> 2
        item.pinyin.contains(term) -> 3
        item.initials.startsWith(term) -> 4
        isSubsequence(term, item.pinyin) -> 6
        else -> null
    }

    private fun isSubsequence(query: String, target: String): Boolean {
        if (query.isEmpty() || target.isEmpty()) return false
        var i = 0
        for (c in target) {
            if (c == query[i]) {
                i++
                if (i == query.length) return true
            }
        }
        return false
    }
}
