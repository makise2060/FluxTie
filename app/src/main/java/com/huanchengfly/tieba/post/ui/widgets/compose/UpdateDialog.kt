package com.huanchengfly.tieba.post.ui.widgets.compose

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.update.ReleaseInfo
import com.huanchengfly.tieba.post.update.UpdateManager
import com.huanchengfly.tieba.post.update.UpdateUiState

/**
 * 更新弹窗唯一宿主（挂载于 MainActivityV2）：三态弹窗 + 下载进度 + 安装入口。
 */
@Composable
fun UpdateDialogHost(manager: UpdateManager) {
    val state by manager.state.collectAsState()
    val visible by manager.dialogVisible.collectAsState()
    val dialogState = rememberDialogState()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(visible) {
        if (visible) {
            dialogState.show()
        } else {
            dialogState.show = false
        }
    }

    Dialog(
        dialogState = dialogState,
        onDismiss = { manager.dismissDialog() },
        title = {
            Text(
                text = when (val s = state) {
                    UpdateUiState.Checking, UpdateUiState.CheckFailed, UpdateUiState.UpToDate ->
                        stringResource(id = R.string.title_check_update)
                    is UpdateUiState.Available ->
                        stringResource(id = R.string.title_dialog_update, s.release.tagName)
                    is UpdateUiState.Downloading ->
                        stringResource(id = R.string.tip_update_downloading)
                    is UpdateUiState.Downloaded ->
                        stringResource(id = R.string.title_update_downloaded)
                    is UpdateUiState.DownloadFailed ->
                        stringResource(id = R.string.title_dialog_update, s.release.tagName)
                    UpdateUiState.Idle -> ""
                }
            )
        },
        buttons = {
            when (val s = state) {
                UpdateUiState.Checking -> Unit

                UpdateUiState.UpToDate -> {
                    DialogPositiveButton(text = stringResource(id = R.string.button_sure_default))
                }

                UpdateUiState.CheckFailed -> {
                    DialogPositiveButton(
                        text = stringResource(id = R.string.button_retry),
                        dismissOnClick = false
                    ) { manager.checkManually() }
                    DialogNegativeButton(text = stringResource(id = R.string.button_update_browser)) {
                        manager.openReleasesPage()
                    }
                }

                is UpdateUiState.Available -> {
                    DialogPositiveButton(
                        text = stringResource(id = R.string.button_update_download),
                        dismissOnClick = false
                    ) { manager.download() }
                    if (!s.manual) {
                        DialogNegativeButton(text = stringResource(id = R.string.button_dont_remind_again)) {
                            manager.ignoreVersion()
                        }
                    }
                    DialogNegativeButton(text = stringResource(id = R.string.button_next_time))
                }

                is UpdateUiState.Downloading -> {
                    DialogNegativeButton(text = stringResource(id = R.string.button_cancel)) {
                        manager.cancelDownload()
                    }
                }

                is UpdateUiState.Downloaded -> {
                    DialogPositiveButton(text = stringResource(id = R.string.button_update_install)) {
                        activity?.let { manager.installOrPrompt(it) }
                    }
                    DialogNegativeButton(text = stringResource(id = R.string.button_update_later))
                }

                is UpdateUiState.DownloadFailed -> {
                    DialogPositiveButton(
                        text = stringResource(id = R.string.button_retry),
                        dismissOnClick = false
                    ) { manager.download() }
                    DialogNegativeButton(text = stringResource(id = R.string.button_update_browser)) {
                        manager.openReleasesPage()
                    }
                }

                UpdateUiState.Idle -> Unit
            }
        }
    ) {
        when (val s = state) {
            UpdateUiState.Checking -> CheckingContent()
            UpdateUiState.UpToDate -> SimpleTextContent(text = stringResource(id = R.string.title_dialog_update_latest))
            UpdateUiState.CheckFailed -> SimpleTextContent(text = stringResource(id = R.string.tip_update_check_failed))
            is UpdateUiState.Available -> AvailableContent(release = s.release)
            is UpdateUiState.Downloading -> DownloadingContent(state = s)
            is UpdateUiState.Downloaded -> DownloadedContent(release = s.release)
            is UpdateUiState.DownloadFailed -> SimpleTextContent(text = stringResource(id = R.string.tip_update_download_failed))
            UpdateUiState.Idle -> Unit
        }
    }
}

@Composable
private fun CheckingContent() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = ExtendedTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.tip_update_checking),
            fontSize = 14.sp,
            color = ExtendedTheme.colors.textSecondary
        )
    }
}

@Composable
private fun SimpleTextContent(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = ExtendedTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}

@Composable
private fun AvailableContent(release: ReleaseInfo) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VersionChip(text = release.tagName)
            if (release.prerelease) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.tip_update_prerelease),
                    color = ExtendedTheme.colors.onAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(ExtendedTheme.colors.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        val changelog = release.changelog
        if (!changelog.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.title_update_changelog),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = remember(changelog) { renderChangelog(changelog) },
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun DownloadingContent(state: UpdateUiState.Downloading) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = if (state.indeterminate) "…" else "${state.percent}%",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (state.indeterminate) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = ExtendedTheme.colors.primary,
                backgroundColor = ExtendedTheme.colors.chip
            )
        } else {
            LinearProgressIndicator(
                progress = state.percent / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = ExtendedTheme.colors.primary,
                backgroundColor = ExtendedTheme.colors.chip
            )
        }
    }
}

@Composable
private fun DownloadedContent(release: ReleaseInfo) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        VersionChip(text = release.tagName)
    }
}

@Composable
private fun VersionChip(text: String) {
    Text(
        text = text,
        color = ExtendedTheme.colors.onChip,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(ExtendedTheme.colors.chip)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/**
 * 轻量 markdown 清洗（不引入渲染库）：去标题/引用/分隔线/强调/链接/行内代码标记，保留文本与列表。
 */
private fun renderChangelog(raw: String): String {
    return raw.lineSequence()
        .map { line ->
            val l = line.trim()
            when {
                l == "---" || l == "***" || l == "___" -> ""
                l.startsWith("> ") -> l.removePrefix("> ")
                l.startsWith("#") -> l.trimStart('#').trimStart()
                l.startsWith("- ") || l.startsWith("* ") -> "· ${l.substring(2)}"
                else -> l
            }
        }
        .map { l ->
            l.replace("**", "")
                .replace("__", "")
                .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "\$1")
                .replace(Regex("`([^`]*)`"), "\$1")
        }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
