package com.huanchengfly.tieba.post.update

import android.content.Context
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.utils.appPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.swiftzer.semver.SemVer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 更新检查：GitHub Release 查询 + TTL 缓存 + semver 比较 + 资产匹配。
 *
 * 使用独立 OkHttpClient（不挂贴吧拦截器）；请求头带 UA（GitHub API 强制）。
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param force 手动检查传 true：绕过 TTL，必须拿最新
     * @param prereleaseChannel 预发布通道（实验开关）：查 /releases 列表（含预发布），否则 /releases/latest
     */
    suspend fun check(force: Boolean, prereleaseChannel: Boolean): CheckResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cached = readCache()
            if (!force && cached != null && cached.prerelease == prereleaseChannel &&
                now - cached.fetchedAt < CACHE_TTL_MS
            ) {
                return@withContext evaluate(cached.release)
            }
            return@withContext runCatching { fetch(prereleaseChannel) }.fold(
                onSuccess = { release ->
                    writeCache(
                        UpdateCache(
                            fetchedAt = now,
                            prerelease = prereleaseChannel,
                            release = release
                        )
                    )
                    evaluate(release)
                },
                onFailure = { e ->
                    // 仅限流（403/429）降级读缓存（同通道，允许过期）；无网等其他失败返回错误
                    if (e is RateLimitException && cached != null && cached.prerelease == prereleaseChannel) {
                        evaluate(cached.release)
                    } else {
                        CheckResult.Error
                    }
                }
            )
        }

    private fun fetch(prereleaseChannel: Boolean): GitHubRelease {
        val url = if (prereleaseChannel) {
            "$API_BASE/releases?per_page=10"
        } else {
            "$API_BASE/releases/latest"
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FluxTie/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 403 || response.code == 429) {
                throw RateLimitException("GitHub API rate limited (HTTP ${response.code})")
            }
            if (!response.isSuccessful) {
                throw IOException("GitHub API HTTP ${response.code}")
            }
            val body = response.body.string()
            return if (prereleaseChannel) {
                json.decodeFromString<List<GitHubRelease>>(body)
                    .firstOrNull { !it.draft }
                    ?: throw IOException("No release found")
            } else {
                json.decodeFromString<GitHubRelease>(body)
            }
        }
    }

    private fun evaluate(release: GitHubRelease?): CheckResult {
        if (release == null) return CheckResult.Error
        val remote = SemVer.parseOrNull(release.tagName.removePrefix("v")) ?: return CheckResult.Error
        val current = SemVer.parseOrNull(BuildConfig.VERSION_NAME) ?: return CheckResult.Error
        return if (remote > current) {
            CheckResult.Found(release.toReleaseInfo())
        } else {
            CheckResult.UpToDate
        }
    }

    private fun GitHubRelease.toReleaseInfo(): ReleaseInfo {
        val version = tagName.removePrefix("v")
        return ReleaseInfo(
            version = version,
            tagName = tagName,
            changelog = body?.takeIf { it.isNotBlank() },
            prerelease = prerelease,
            htmlUrl = htmlUrl,
            apk = findApk(version)?.let { ReleaseInfo.ApkAsset(it.name, it.downloadUrl, it.size) },
        )
    }

    /**
     * 资产匹配容错：CI 构建产物名为 `FluxTie-v{版本}+{sha7}.apk`（本地构建无 +sha）。
     */
    private fun GitHubRelease.findApk(version: String): GitHubAsset? {
        val exact = "FluxTie-v$version.apk"
        return assets.firstOrNull { it.name == exact }
            ?: assets.firstOrNull {
                it.name.startsWith("FluxTie-v$version", ignoreCase = true) &&
                    it.name.endsWith(".apk", ignoreCase = true)
            }
            ?: assets.firstOrNull {
                it.name.startsWith("FluxTie-v", ignoreCase = true) &&
                    it.name.endsWith(".apk", ignoreCase = true)
            }
    }

    private fun readCache(): UpdateCache? {
        val raw = context.appPreferences.updateCheckCache ?: return null
        return runCatching { json.decodeFromString<UpdateCache>(raw) }.getOrNull()
    }

    private fun writeCache(cache: UpdateCache) {
        runCatching { context.appPreferences.updateCheckCache = json.encodeToString(cache) }
    }

    companion object {
        private const val API_BASE = "https://api.github.com/repos/makise2060/FluxTie"
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1h
    }
}

/** GitHub API 限流（403/429）——唯一允许降级读缓存的失败类型。 */
private class RateLimitException(message: String) : IOException(message)
