package com.huanchengfly.tieba.post.utils

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.flow.first

/**
 * 帖子续看标记：记录最后停留的楼层，重开同一帖子时提供「续看」入口。
 *
 * 存 DataStore 单键 JSON（`thread_read_marks`），按时间 LRU 截断，不引入 Room 迁移；
 * 跨排序（正/倒序）的记录不互相消费，避免倒序视图跳到错误方向。
 */
data class ThreadReadMark(
    val postId: Long = 0,
    val floor: Int = 0,
    /** 记录时的排序：true = 倒序 */
    val isDesc: Boolean = false,
    val timestamp: Long = 0,
)

object ThreadReadMarks {
    private val KEY = stringPreferencesKey("thread_read_marks")
    private const val MAX_ENTRIES = 300

    private val store get() = App.INSTANCE.dataStore

    suspend fun get(threadId: Long): ThreadReadMark? =
        runCatching { currentMarks()[threadId.toString()] }.getOrNull()

    suspend fun put(threadId: Long, mark: ThreadReadMark) {
        runCatching {
            store.edit { prefs ->
                val marks = currentMarks(prefs)
                marks[threadId.toString()] = mark
                if (marks.size > MAX_ENTRIES) {
                    // LRU 截断：只保留最近访问的条目
                    val kept = marks.entries
                        .sortedByDescending { it.value.timestamp }
                        .take(MAX_ENTRIES)
                        .associate { it.key to it.value }
                    marks.clear()
                    marks.putAll(kept)
                }
                prefs[KEY] = marks.toJson()
            }
        }
    }

    private suspend fun currentMarks(): MutableMap<String, ThreadReadMark> =
        currentMarks(store.data.first())

    private fun currentMarks(prefs: androidx.datastore.preferences.core.Preferences): MutableMap<String, ThreadReadMark> =
        prefs[KEY]?.fromJson<MutableMap<String, ThreadReadMark>>() ?: mutableMapOf()
}
