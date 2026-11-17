package com.huanchengfly.tieba.post.ui.page.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.TimePickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

/**
 * 设置页列表统一的 contentPadding：M2 Scaffold 在本项目的 edge-to-edge 配置下
 * 不下发系统栏 insets（issue #41），底部需自行拼上导航栏 inset，
 * 避免三键导航设备上末行与虚拟按键重合。
 */
@Composable
internal fun settingsListContentPadding(): PaddingValues = PaddingValues(
    top = 8.dp,
    bottom = 8.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
)

/**
 * 卡片式设置行组件补充：
 * 单选 [ListSettingRow]、文本输入 [TextInputSettingRow]、时间选择 [TimePickerSettingRow]、
 * 提示块 [TipCard]，与 [SettingRow]/[SwitchSettingRow] 同一视觉体系。
 */

/**
 * 单选行（替代旧 PrefsScreen 的 ListPref）：点击弹单选对话框。
 * [key] 传 null 时不落盘（如切换账号），选中值经 [onValueChange] 回调处理。
 */
@Composable
internal fun ListSettingRow(
    icon: ImageVector,
    title: String,
    entries: Map<String, String>,   // value -> 显示文本
    defaultValue: String,
    key: String? = null,
    summary: String? = null,
    enabled: Boolean = true,
    dialogTitle: String = title,
    onValueChange: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    // 只读收集当前值（key 为 null 时不收集，如切换账号场景）
    val current: String? = if (key != null) {
        val prefs by remember {
            context.dataStore.data.map { it[stringPreferencesKey(key)] ?: defaultValue }
        }.collectAsState(initial = defaultValue)
        prefs
    } else {
        null
    }

    SettingRow(
        icon = icon,
        title = title,
        summary = summary ?: entries[current] ?: stringResource(id = R.string.summary_not_set),
        enabled = enabled,
        onClick = { dialogState.show() },
    )

    Dialog(
        dialogState = dialogState,
        title = { Text(text = dialogTitle) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        ListSinglePicker(
            itemTitles = entries.values.toImmutableList(),
            itemValues = entries.keys.toImmutableList(),
            selectedPosition = entries.keys.indexOf(current),
            onItemSelected = { _, _, value, _ ->
                key?.let { k ->
                    context.dataStore.putString(k, value)
                }
                onValueChange?.invoke(value)
                dialogState.show = false
            },
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

/** 文本输入行（替代旧 EditTextPref）：点击弹输入对话框，确认后写回；[emptySummary] 为值为空时的占位摘要 */
@Composable
internal fun TextInputSettingRow(
    icon: ImageVector,
    title: String,
    key: String,
    defaultValue: String = "",
    summary: String? = null,
    @StringRes emptySummary: Int? = null,
    dialogTitle: String? = null,
    enabled: Boolean = true,
    onValueSaved: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    val current by context.dataStore.collectPreferenceAsState(
        key = stringPreferencesKey(key),
        defaultValue = defaultValue
    )

    SettingRow(
        icon = icon,
        title = title,
        summary = summary ?: current.ifBlank { emptySummary?.let { context.getString(it) } },
        enabled = enabled,
        onClick = { dialogState.show() },
    )

    PromptDialog(
        dialogState = dialogState,
        title = { Text(text = dialogTitle ?: title) },
        initialValue = current,
        onConfirm = { value ->
            context.dataStore.putString(key, value)
            onValueSaved?.invoke(value)
        },
    )
}

/** 时间选择行（替代旧 TimePickerPerf）：点击弹时间选择对话框 */
@Composable
internal fun TimePickerSettingRow(
    icon: ImageVector,
    title: String,
    key: String,
    defaultValue: String = "09:00",
    summary: (String) -> String,
    dialogTitle: String? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    val current by context.dataStore.collectPreferenceAsState(
        key = stringPreferencesKey(key),
        defaultValue = defaultValue
    )

    SettingRow(
        icon = icon,
        title = title,
        summary = summary(current),
        enabled = enabled,
        onClick = { dialogState.show() },
    )

    TimePickerDialog(
        dialogState = dialogState,
        title = { Text(text = dialogTitle ?: title) },
        currentTime = current,
        onConfirm = { time ->
            context.dataStore.putString(key, time)
        },
    )
}

/** 卡片外提示块（chip 样式，替代各设置页的提示 Text） */
@Composable
internal fun TipCard(text: AnnotatedString, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = ExtendedTheme.colors.onChip,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ExtendedTheme.colors.chip)
            .padding(12.dp)
    )
}
