package com.huanchengfly.tieba.post.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release API 响应模型（仅取用到的字段）。
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("body") val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L,
)

/**
 * 检查结果本地缓存（TTL 1h，403/429 降级读取）。
 */
@Serializable
data class UpdateCache(
    val fetchedAt: Long,
    val prerelease: Boolean,
    val release: GitHubRelease? = null,
)

/**
 * 领域模型：可供 UI / 下载 / 安装使用的发布信息。
 */
data class ReleaseInfo(
    val version: String,        // 剥 "v" 前缀的 semver 版本号，如 "1.0.3"
    val tagName: String,        // 原 tag，如 "v1.0.3"（展示用）
    val changelog: String?,     // Release 正文（markdown 原文）
    val prerelease: Boolean,
    val htmlUrl: String,
    val apk: ApkAsset?,
) {
    data class ApkAsset(val name: String, val url: String, val size: Long)
}

sealed interface CheckResult {
    data class Found(val release: ReleaseInfo) : CheckResult
    data object UpToDate : CheckResult
    data object Error : CheckResult
}
