package com.huanchengfly.tieba.post.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.SplashState
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.requestPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState                          // 仅手动检查展示
    data object UpToDate : UpdateUiState                          // 仅手动检查展示
    data class Available(val release: ReleaseInfo, val manual: Boolean) : UpdateUiState
    data class Downloading(val release: ReleaseInfo, val percent: Int, val indeterminate: Boolean) : UpdateUiState
    data class Downloaded(val release: ReleaseInfo, val file: File) : UpdateUiState
    data object CheckFailed : UpdateUiState                       // 仅手动检查展示
    data class DownloadFailed(val release: ReleaseInfo) : UpdateUiState
}

/**
 * 更新状态中枢（单例）：检查 / 下载 / 取消 / 忽略 / 安装编排。
 * UI 弹窗由 MainActivityV2 的 UpdateDialogHost 唯一承载。
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checker: UpdateChecker,
    private val downloader: ApkDownloader,
    private val notification: UpdateNotification,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    // ── 检查 ──────────────────────────────────────────────

    /** 手动检查（关于页「检查更新」）；已有下载进度/已下载时仅重新打开弹窗。 */
    fun checkManually() {
        when (_state.value) {
            is UpdateUiState.Downloading, is UpdateUiState.Downloaded -> {
                _dialogVisible.value = true
            }
            is UpdateUiState.Checking -> {
                _dialogVisible.value = true
            }
            else -> startCheck(manual = true)
        }
    }

    /** 启动静默检查（默认开；发现新版本且未被忽略才弹窗）。 */
    fun autoCheckIfNeeded() {
        scope.launch {
            if (downloadJob?.isActive != true) {
                // 清理进程被杀遗留的下载残留与通知
                downloader.cleanupStale()
                notification.cancel()
            }
            if (!context.appPreferences.autoCheckUpdate) return@launch
            // 等启动屏结束，避免弹窗被遮挡
            SplashState.finished.first { it }
            delay(600)
            if (!context.appPreferences.autoCheckUpdate) return@launch
            if (_state.value != UpdateUiState.Idle) return@launch
            startCheck(manual = false)
        }
    }

    private fun startCheck(manual: Boolean) {
        if (manual) {
            checkJob?.cancel()
        } else if (checkJob?.isActive == true) {
            return
        }
        checkJob = scope.launch {
            if (manual) {
                _state.value = UpdateUiState.Checking
                _dialogVisible.value = true
            }
            when (val result = checker.check(
                force = manual,
                prereleaseChannel = context.appPreferences.checkCIUpdate
            )) {
                is CheckResult.Found -> {
                    if (!manual && result.release.version == context.appPreferences.ignoredUpdateVersion) {
                        _state.value = UpdateUiState.Idle
                    } else {
                        _state.value = UpdateUiState.Available(result.release, manual)
                        _dialogVisible.value = true
                    }
                }
                CheckResult.UpToDate -> {
                    if (manual) {
                        _state.value = UpdateUiState.UpToDate
                        _dialogVisible.value = true
                    } else {
                        _state.value = UpdateUiState.Idle
                    }
                }
                CheckResult.Error -> {
                    if (manual) {
                        _state.value = UpdateUiState.CheckFailed
                        _dialogVisible.value = true
                    } else {
                        _state.value = UpdateUiState.Idle
                    }
                }
            }
        }
    }

    // ── 下载 ──────────────────────────────────────────────

    fun download() {
        val release = when (val s = _state.value) {
            is UpdateUiState.Available -> s.release
            is UpdateUiState.DownloadFailed -> s.release
            else -> return
        }
        if (downloadJob?.isActive == true) return
        val asset = release.apk
        if (asset == null) {
            context.toastShort(R.string.toast_update_no_asset)
            openReleasesPage()
            _dialogVisible.value = false
            return
        }
        downloadJob = scope.launch {
            _state.value = UpdateUiState.Downloading(release, 0, asset.size <= 0)
            var lastNotifiedPercent = -1
            var lastNotifyAt = 0L
            runCatching {
                downloader.download(asset.url, "FluxTie-v${release.version}.apk") { read, total ->
                    val indeterminate = total <= 0
                    val percent = if (indeterminate) 0 else (read * 100 / total).toInt().coerceIn(0, 100)
                    _state.value = UpdateUiState.Downloading(release, percent, indeterminate)
                    val now = System.currentTimeMillis()
                    // 系统对每包通知有 enqueue 速率限制（约 5 条/秒，超限直接丢弃）：
                    // 进度通知节流到 2 条/秒，否则高频 notify 会连「下载完成」通知一起被 shed
                    if (percent == 100 || (percent != lastNotifiedPercent && now - lastNotifyAt >= 500)) {
                        lastNotifiedPercent = percent
                        lastNotifyAt = now
                        notification.showProgress(percent, indeterminate)
                    }
                }
            }.onSuccess { file ->
                _state.value = UpdateUiState.Downloaded(release, file)
                notification.showComplete()
            }.onFailure { e ->
                notification.cancel()
                if (e is CancellationException) {
                    _state.value = UpdateUiState.Available(release, manual = true)
                } else {
                    _state.value = UpdateUiState.DownloadFailed(release)
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    // ── 弹窗动作 ───────────────────────────────────────────

    fun dismissDialog() {
        _dialogVisible.value = false
        when (_state.value) {
            is UpdateUiState.UpToDate, is UpdateUiState.CheckFailed -> {
                _state.value = UpdateUiState.Idle
            }
            else -> {}
        }
    }

    /** 「不再提示」：忽略当前版本（出现更高版本仍会提示）。 */
    fun ignoreVersion() {
        val release = (_state.value as? UpdateUiState.Available)?.release ?: return
        context.appPreferences.ignoredUpdateVersion = release.version
        _dialogVisible.value = false
        _state.value = UpdateUiState.Idle
    }

    // ── 安装 ──────────────────────────────────────────────

    /** 安装已下载的更新；无「安装未知应用」授权时走两段式引导，拒绝则兜底跳发布页。 */
    fun installOrPrompt(activity: Activity) {
        val downloaded = _state.value as? UpdateUiState.Downloaded
        if (downloaded == null || !downloaded.file.exists()) {
            if (downloaded != null) {
                // 文件丢失：回到可下载状态
                _state.value = UpdateUiState.Available(downloaded.release, manual = true)
                _dialogVisible.value = true
            } else {
                openReleasesPage()
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            activity.requestPermission {
                permissions = listOf(PermissionUtils.REQUEST_INSTALL_PACKAGES)
                description = activity.getString(R.string.desc_permission_install_update)
                onGranted = { startInstall(activity, downloaded.file) }
                onDenied = { openReleasesPage() }
            }
        } else {
            startInstall(activity, downloaded.file)
        }
    }

    private fun startInstall(context: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.share.FileProvider",
                file
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }.onFailure {
            openReleasesPage()
        }
    }

    /** 兜底：跳转 Releases 发布页（浏览器）。 */
    fun openReleasesPage() {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    companion object {
        const val EXTRA_INSTALL_UPDATE = "update_install"
        private const val RELEASES_URL = "https://github.com/makise2060/FluxTie/releases"
    }
}
