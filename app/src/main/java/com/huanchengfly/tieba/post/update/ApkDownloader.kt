package com.huanchengfly.tieba.post.update

import android.content.Context
import com.huanchengfly.tieba.post.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * APK 流式下载：写 external-files/updates/，`.part` 临时文件 + rename，支持取消。
 */
@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val updatesDir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "updates")

    /**
     * @param onProgress (已读字节, 总字节；总字节 <= 0 表示未知)
     * @return 下载完成的文件
     */
    suspend fun download(url: String, fileName: String, onProgress: (Long, Long) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = updatesDir.apply { mkdirs() }
            // 下载前清理旧包与残留
            dir.listFiles()?.forEach { f ->
                if (f.name.endsWith(".apk") || f.name.endsWith(".part")) {
                    f.delete()
                }
            }
            val target = File(dir, fileName)
            val part = File(dir, "$fileName.part")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FluxTie/${BuildConfig.VERSION_NAME}")
                .build()
            val call = client.newCall(request)
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    val body = response.body
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var readTotal = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                readTotal += read
                                onProgress(readTotal, total)
                            }
                            output.flush()
                        }
                    }
                }
                if (!part.renameTo(target)) {
                    throw IOException("Failed to rename ${part.name}")
                }
                target
            } catch (t: Throwable) {
                call.cancel()
                part.delete()
                throw t
            }
        }

    /**
     * 启动清理：删除中断残留的 `.part`（进程被杀后不会自动清理）。
     */
    fun cleanupStale() {
        runCatching {
            updatesDir.listFiles()?.forEach { f ->
                if (f.name.endsWith(".part")) {
                    f.delete()
                }
            }
        }
    }
}
