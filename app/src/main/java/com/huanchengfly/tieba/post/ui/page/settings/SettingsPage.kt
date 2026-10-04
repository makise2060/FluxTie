package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.custom.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.custom.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.custom.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.custom.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.custom.SwitchSettingRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LeadingIcon(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides ExtendedTheme.colors.primary) {
        content()
        Spacer(modifier = Modifier.size(56.dp))
    }
}

/**
 * 设置主页：按用途分为「账号 / 通用 / 浏览 / 存储 / 系统」五组。
 * 原先收纳在「更多-其他杂项」子页的条目已按性质归入对应分组，
 * 与「我的」页重复的「关于」入口不再保留。
 */
@Destination
@Composable
fun SettingsPage(
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        val account = LocalAccount.current
        MyScaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_settings),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                        )
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    }
                )
            },
        ) { paddingValues ->
            val context = LocalContext.current
            val snackbarHostState = LocalSnackbarHostState.current
            val coroutineScope = rememberCoroutineScope()

            // 「使用 Custom Tabs」仅在内置浏览器不接管全部链接时才有意义（只读依赖，不回写）
            val useWebView by context.dataStore.collectPreferenceAsState(
                key = booleanPreferencesKey("use_webview"),
                defaultValue = true
            )
            // 图片缓存占用（进入页面时在后台统计，清除后归零）
            var cacheSize by remember { mutableStateOf("0.0B") }
            LaunchedEffect(Unit) {
                cacheSize = withContext(Dispatchers.IO) { ImageCacheUtil.getCacheSize(context) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // ── 账号
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings_group_account))
                }
                item {
                    AppearanceCard {
                        AccountRow(account = account)
                    }
                }

                // ── 通用
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings_group_general))
                }
                item {
                    AppearanceCard {
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_brush_black_24dp),
                            title = stringResource(id = R.string.title_settings_custom),
                            summary = stringResource(id = R.string.summary_settings_custom),
                            onClick = { navigator.navigate(CustomSettingsPageDestination) }
                        )
                        CardDivider()
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_settings_block),
                            title = stringResource(id = R.string.title_block_settings),
                            summary = stringResource(id = R.string.summary_block_settings),
                            onClick = { navigator.navigate(BlockSettingsPageDestination) }
                        )
                        CardDivider()
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_rocket_launch_black_24),
                            title = stringResource(id = R.string.title_oksign),
                            summary = stringResource(id = R.string.summary_settings_oksign),
                            onClick = { navigator.navigate(OKSignSettingsPageDestination) }
                        )
                        CardDivider()
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_dashboard_customize_black_24),
                            title = stringResource(id = R.string.title_settings_read_habit),
                            summary = stringResource(id = R.string.summary_settings_habit),
                            onClick = { navigator.navigate(HabitSettingsPageDestination) }
                        )
                    }
                }

                // ── 浏览
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings_group_browsing))
                }
                item {
                    AppearanceCard {
                        SwitchSettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_chrome),
                            title = stringResource(id = R.string.title_use_webview),
                            summaryOn = stringResource(id = R.string.tip_use_webview_on),
                            summaryOff = stringResource(id = R.string.tip_use_webview),
                            key = "use_webview",
                            defaultValue = true,
                        )
                        CardDivider()
                        SwitchSettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_today),
                            title = stringResource(id = R.string.title_use_custom_tabs),
                            summaryOn = stringResource(id = R.string.tip_use_custom_tab_on),
                            summaryOff = stringResource(id = R.string.tip_use_custom_tab),
                            key = "use_custom_tabs",
                            defaultValue = true,
                            enabled = !useWebView,
                        )
                    }
                }

                // ── 存储
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings_group_storage))
                }
                item {
                    AppearanceCard {
                        SettingRow(
                            icon = Icons.Outlined.OfflineBolt,
                            title = stringResource(id = R.string.title_clear_picture_cache),
                            summary = stringResource(id = R.string.tip_cache, cacheSize),
                            onClick = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        ImageCacheUtil.clearImageAllCache(context)
                                    }
                                    cacheSize = "0.0B"
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.toast_clear_picture_cache_success)
                                    )
                                }
                            }
                        )
                    }
                }

                // ── 系统
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings_group_system))
                }
                item {
                    AppearanceCard {
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_link),
                            title = stringResource(id = R.string.title_open_by_default),
                            summary = stringResource(id = R.string.tip_open_by_default),
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    ).addFlags(
                                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                    )
                                )
                            }
                        )
                        CardDivider()
                        SwitchSettingRow(
                            icon = Icons.Outlined.Update,
                            title = stringResource(id = R.string.title_auto_check_update),
                            summaryOn = stringResource(id = R.string.tip_auto_check_update_on),
                            summaryOff = stringResource(id = R.string.tip_auto_check_update_off),
                            key = "auto_check_update",
                            defaultValue = true,
                        )
                        if (context.appPreferences.showExperimentalFeatures) {
                            CardDivider()
                            SwitchSettingRow(
                                icon = Icons.Outlined.BugReport,
                                title = stringResource(id = R.string.title_check_ci_update),
                                summary = stringResource(id = R.string.tip_check_ci_update),
                                key = "checkCIUpdate",
                                defaultValue = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account?) {
    val navigator = LocalNavigator.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (account != null) {
                        navigator.navigate(AccountManagePageDestination)
                    } else {
                        navigator.navigate(LoginPageDestination)
                    }
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (account != null) {
            Avatar(
                data = StringUtil.getAvatarUrl(account.portrait),
                size = Sizes.Small,
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.title_account_manage),
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = stringResource(
                        id = R.string.summary_now_account,
                        account.nameShow ?: account.name
                    ),
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.title_account_manage),
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = stringResource(id = R.string.summary_not_logged_in),
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
    }
}
