package com.huanchengfly.tieba.post.ui.page.settings.block

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.page.settings.components.settingsListContentPadding
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.destinations.BlockListPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.components.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.components.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.components.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 屏蔽设置：卡片式分组（屏蔽列表入口 + 内容过滤开关） */
@Destination
@Composable
fun BlockSettingsPage(
    navigator: DestinationsNavigator
) {
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_settings),
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
            contentPadding = settingsListContentPadding()
        ) {
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_block_settings),
                    icon = Icons.Outlined.Block
                )
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Outlined.Block,
                        title = stringResource(id = R.string.title_block_list),
                        onClick = { navigator.navigate(BlockListPageDestination) }
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.HideSource,
                        title = stringResource(id = R.string.settings_hide_blocked_content),
                        key = "hideBlockedContent",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.VideocamOff,
                        title = stringResource(id = R.string.settings_block_video),
                        summary = stringResource(id = R.string.settings_block_video_summary),
                        key = "blockVideo",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.Block,
                        title = stringResource(id = R.string.settings_show_followed_only),
                        summary = stringResource(id = R.string.settings_show_followed_only_summary),
                        key = "showFollowedOnly",
                        defaultValue = false,
                    )
                }
            }
        }
    }
}
