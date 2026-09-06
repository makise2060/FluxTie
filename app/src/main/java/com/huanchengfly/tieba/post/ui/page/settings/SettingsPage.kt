package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MoreSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.custom.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.custom.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.custom.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.custom.SettingRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
internal fun LeadingIcon(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides ExtendedTheme.colors.primary) {
        content()
        Spacer(modifier = Modifier.size(56.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowAccountItem(
    account: Account?,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    if (account != null) {
        SettingRow(
            icon = Icons.Rounded.AccountCircle,
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_now_account, account.nameShow ?: account.name),
            onClick = { navigator.navigate(AccountManagePageDestination) },
        )
    } else {
        SettingRow(
            icon = Icons.Rounded.AccountCircle,
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_not_logged_in),
            onClick = { navigator.navigate(LoginPageDestination) },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // ── 账号
                item {
                    SectionLabel(text = stringResource(id = R.string.title_account_manage))
                }
                item {
                    AppearanceCard {
                        AccountRow(account = account)
                    }
                }

                // ── 通用
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings))
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
                            icon = ImageVector.vectorResource(id = R.drawable.ic_dashboard_customize_black_24),
                            title = stringResource(id = R.string.title_settings_read_habit),
                            summary = stringResource(id = R.string.summary_settings_habit),
                            onClick = { navigator.navigate(HabitSettingsPageDestination) }
                        )
                        CardDivider()
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_rocket_launch_black_24),
                            title = stringResource(id = R.string.title_oksign),
                            summary = stringResource(id = R.string.summary_settings_oksign),
                            onClick = { navigator.navigate(OKSignSettingsPageDestination) }
                        )
                    }
                }

                // ── 其他
                item {
                    SectionLabel(text = stringResource(id = R.string.title_settings_more))
                }
                item {
                    AppearanceCard {
                        SettingRow(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_more_horiz_black_24),
                            title = stringResource(id = R.string.title_settings_more),
                            summary = stringResource(id = R.string.summary_settings_more),
                            onClick = { navigator.navigate(MoreSettingsPageDestination) }
                        )
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
                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
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
