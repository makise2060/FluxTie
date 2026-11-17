package com.huanchengfly.tieba.post.ui.page.settings.account

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SupervisedUserCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.page.settings.components.settingsListContentPadding
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.components.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.components.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.components.ListSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.TextInputSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.TipCard
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.AllAccounts
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.launchUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 账号管理：卡片式分组（账号操作 / 个性化） */
@Destination
@Composable
fun AccountManagePage(
    navigator: DestinationsNavigator,
) {
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_account_manage),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        val account = LocalAccount.current
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = settingsListContentPadding()
        ) {
            // ── 账号操作
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_account_group_manage),
                    icon = Icons.Outlined.AccountCircle
                )
            }
            item {
                AppearanceCard {
                    if (account != null) {
                        ListSettingRow(
                            icon = Icons.Outlined.AccountCircle,
                            title = stringResource(id = R.string.title_switch_account),
                            entries = AllAccounts.current.associate {
                                it.id.toString() to (it.nameShow ?: it.name)
                            },
                            defaultValue = account.id.toString(),
                            key = null,
                            summary = stringResource(
                                id = R.string.summary_now_account,
                                account.nameShow ?: account.name
                            ),
                            onValueChange = { AccountUtil.switchAccount(context, it.toInt()) }
                        )
                    } else {
                        SettingRow(
                            icon = Icons.Outlined.AccountCircle,
                            title = stringResource(id = R.string.title_switch_account),
                            enabled = false
                        )
                    }
                    CardDivider()
                    SettingRow(
                        icon = Icons.Outlined.AddCircleOutline,
                        title = stringResource(id = R.string.title_new_account),
                        onClick = { navigator.navigate(LoginPageDestination) }
                    )
                    CardDivider()
                    SettingRow(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_outlined_close_circle_24),
                        title = stringResource(id = R.string.title_exit_account),
                        onClick = { AccountUtil.exit(context) }
                    )
                    CardDivider()
                    SettingRow(
                        icon = Icons.Outlined.SupervisedUserCircle,
                        title = stringResource(id = R.string.title_modify_username),
                        enabled = account != null,
                        onClick = {
                            launchUrl(
                                context,
                                navigator,
                                "https://wappass.baidu.com/static/manage-chunk/change-username.html#/showUsername"
                            )
                        }
                    )
                    CardDivider()
                    SettingRow(
                        icon = Icons.Outlined.ContentCopy,
                        title = stringResource(id = R.string.title_copy_bduss),
                        summary = stringResource(id = R.string.summary_copy_bduss),
                        enabled = account != null,
                        onClick = { TiebaUtil.copyText(context, account?.bduss, isSensitive = true) }
                    )
                }
            }

            // ── 个性化
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_account_group_personal),
                    icon = Icons.Outlined.Edit
                )
            }
            item {
                AppearanceCard {
                    TextInputSettingRow(
                        icon = Icons.Outlined.Edit,
                        title = stringResource(id = R.string.title_my_tail),
                        key = "little_tail",
                        emptySummary = R.string.tip_no_little_tail,
                        dialogTitle = stringResource(id = R.string.title_dialog_modify_little_tail),
                    )
                }
            }

            item {
                TipCard(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(id = R.string.tip_start))
                        }
                        append(stringResource(id = R.string.tip_account_error))
                    }
                )
            }
        }
    }
}
