package com.huanchengfly.tieba.post.utils

import android.util.Log
import com.huanchengfly.tieba.post.BuildConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * 轻量启动埋点：只记录耗时，不采集账号或业务内容。
 *
 * 后续优化启动时以同一组标记做前后对照，避免只凭体感判断。
 */
object StartupMetrics {
    private val processStart = System.currentTimeMillis()
    private val marks = ConcurrentHashMap<String, Long>()

    fun mark(name: String) {
        val elapsed = System.currentTimeMillis() - processStart
        marks[name] = elapsed
        if (BuildConfig.DEBUG) Log.i("StartupMetrics", "$name=${elapsed}ms")
    }

    fun snapshot(): Map<String, Long> = marks.toMap()
}
