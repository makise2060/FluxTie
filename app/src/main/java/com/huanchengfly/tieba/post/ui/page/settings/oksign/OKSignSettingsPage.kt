package com.huanchengfly.tieba.post.ui.page.settings.oksign

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.ui.page.settings.components.settingsListContentPadding
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.page.settings.components.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.components.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.components.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.TimePickerSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.TipCard
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.powerManager
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

/** 一键签到设置：卡片式分组（签到行为 / 后台运行） */
@Destination
@Composable
fun OKSignSettingsPage(
    navigator: DestinationsNavigator,
) {
    val coroutineScope = rememberCoroutineScope()
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_oksign),
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current
        // 自动签到开启后时间选择行才可用（替代旧 PrefsScreen 的 depend）
        val autoSignEnabled by context.dataStore.collectPreferenceAsState(
            key = booleanPreferencesKey("auto_sign"),
            defaultValue = false
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = settingsListContentPadding()
        ) {
            // ── 签到行为
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_oksign_group_sign),
                    icon = Icons.Outlined.OfflinePin
                )
            }
            item {
                AppearanceCard {
                    SwitchSettingRow(
                        icon = Icons.Outlined.BrowseGallery,
                        title = stringResource(id = R.string.title_oksign_slow_mode),
                        summaryOn = stringResource(id = R.string.summary_oksign_slow_mode_on),
                        summaryOff = stringResource(id = R.string.summary_oksign_slow_mode),
                        key = "oksign_slow_mode",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.Speed,
                        title = stringResource(id = R.string.title_oksign_use_official_oksign),
                        summary = stringResource(id = R.string.summary_oksign_use_official_oksign),
                        key = "oksign_use_official_oksign",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.Cancel,
                        title = stringResource(id = R.string.title_oksign_fail_auto_stop),
                        summary = stringResource(id = R.string.summary_oksign_fail_auto_stop),
                        key = "oksign_fail_auto_stop",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.OfflinePin,
                        title = stringResource(id = R.string.title_auto_sign),
                        summaryOn = stringResource(id = R.string.summary_auto_sign_on),
                        summaryOff = stringResource(id = R.string.summary_auto_sign),
                        key = "auto_sign",
                        defaultValue = false,
                    )
                    CardDivider()
                    TimePickerSettingRow(
                        icon = Icons.Outlined.WatchLater,
                        title = stringResource(id = R.string.title_auto_sign_time),
                        key = "auto_sign_time",
                        defaultValue = "09:00",
                        summary = { context.getString(R.string.summary_auto_sign_time, it) },
                        dialogTitle = stringResource(id = R.string.title_auto_sign_time),
                        enabled = autoSignEnabled
                    )
                }
            }

            // ── 后台运行
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_oksign_group_background),
                    icon = Icons.Outlined.BatteryAlert
                )
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Outlined.BatteryAlert,
                        title = stringResource(id = R.string.title_ignore_battery_optimization),
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !context.isIgnoringBatteryOptimizations(),
                        summary =
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) stringResource(id = R.string.summary_battery_optimization_old_android_version)
                        else if (context.isIgnoringBatteryOptimizations()) stringResource(id = R.string.summary_battery_optimization_ignored)
                        else stringResource(id = R.string.summary_ignore_battery_optimization),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!context.powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                    context.requestIgnoreBatteryOptimizations()
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.toast_ignore_battery_optimization_already))
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                TipCard(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(id = R.string.tip_start))
                        }
                        append(stringResource(id = R.string.tip_auto_sign))
                    }
                )
            }
        }
    }
}
