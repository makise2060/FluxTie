package com.huanchengfly.tieba.post.ui.page.settings.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.AddModerator
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.outlined.SpeakerNotesOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.settings.components.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.components.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.components.ListSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.isPhotoPickerAvailable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 使用习惯设置：卡片式分组（图片与媒体 / 浏览与导航 / 收藏 / 隐私与互动） */
@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun HabitSettingsPage(
    navigator: DestinationsNavigator
) {
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_read_habit),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ── 图片与媒体
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_habit_group_image),
                    icon = Icons.Outlined.PhotoSizeSelectActual
                )
            }
            item {
                AppearanceCard {
                    ListSettingRow(
                        icon = Icons.Outlined.PhotoSizeSelectActual,
                        title = stringResource(id = R.string.title_settings_image_load_type),
                        entries = mapOf(
                            "0" to stringResource(id = R.string.title_image_load_type_smart_origin),
                            "1" to stringResource(id = R.string.title_image_load_type_smart_load),
                            "2" to stringResource(id = R.string.title_image_load_type_all_origin),
                            "3" to stringResource(id = R.string.title_image_load_type_all_no)
                        ),
                        defaultValue = "0",
                        key = "image_load_type",
                    )
                    CardDivider()
                    ListSettingRow(
                        icon = Icons.AutoMirrored.Outlined.BrandingWatermark,
                        title = stringResource(id = R.string.title_settings_image_watermark),
                        entries = mapOf(
                            "0" to stringResource(id = R.string.title_image_watermark_none),
                            "1" to stringResource(id = R.string.title_image_watermark_user_name),
                            "2" to stringResource(id = R.string.title_image_watermark_forum_name)
                        ),
                        defaultValue = "2",
                        key = "pic_watermark_type",
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.NightsStay,
                        title = stringResource(id = R.string.settings_image_darken_when_night_mode),
                        key = "imageDarkenWhenNightMode",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = ImageVector.vectorResource(R.drawable.ic_outline_collapse_all),
                        title = stringResource(id = R.string.title_hide_media),
                        key = "hideMedia",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.ImageSearch,
                        title = stringResource(id = R.string.title_do_not_use_photo_picker),
                        key = "doNotUsePhotoPicker",
                        defaultValue = false,
                        enabled = isPhotoPickerAvailable(),
                        summaryProvider = { checked ->
                            if (!isPhotoPickerAvailable()) {
                                context.getString(R.string.summary_photo_picker_not_supported)
                            } else if (checked) {
                                context.getString(R.string.summary_do_not_use_photo_picker)
                            } else {
                                context.getString(R.string.summary_use_photo_picker)
                            }
                        },
                    )
                }
            }

            // ── 浏览与导航
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_habit_group_browse),
                    icon = Icons.Outlined.Explore
                )
            }
            item {
                AppearanceCard {
                    StartPageSelector()
                    CardDivider()
                    ListSettingRow(
                        icon = Icons.Outlined.CalendarViewDay,
                        title = stringResource(id = R.string.title_settings_default_sort_type),
                        entries = mapOf(
                            "0" to stringResource(id = R.string.title_sort_by_reply),
                            "1" to stringResource(id = R.string.title_sort_by_send),
                        ),
                        defaultValue = "0",
                        key = "default_sort_type",
                    )
                    CardDivider()
                    ListSettingRow(
                        icon = Icons.AutoMirrored.Outlined.ExitToApp,
                        title = stringResource(id = R.string.settings_forum_fab_function),
                        entries = mapOf(
                            "post" to stringResource(id = R.string.btn_post),
                            "refresh" to stringResource(id = R.string.btn_refresh),
                            "back_to_top" to stringResource(id = R.string.btn_back_to_top),
                            "hide" to stringResource(id = R.string.btn_hide)
                        ),
                        defaultValue = "post",
                        key = "forumFabFunction",
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.AutoMirrored.Rounded.Reply,
                        title = stringResource(id = R.string.title_predictive_back),
                        summaryProvider = { context.getString(R.string.summary_predictive_back) },
                        key = "predictive_back",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        title = stringResource(id = R.string.title_auto_hide_bottom_bar),
                        summary = stringResource(id = R.string.tip_auto_hide_bottom_bar_summary),
                        key = "auto_hide_bottom_bar",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.WatchLater,
                        title = stringResource(id = R.string.settings_home_page_show_history_forum),
                        key = "homePageShowHistoryForum",
                        defaultValue = true,
                    )
                }
            }

            // ── 收藏
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_habit_group_collect),
                    icon = Icons.Outlined.StarOutline
                )
            }
            item {
                AppearanceCard {
                    SwitchSettingRow(
                        icon = Icons.Outlined.StarOutline,
                        title = stringResource(id = R.string.settings_collect_thread_see_lz),
                        summaryOn = stringResource(id = R.string.tip_collect_thread_see_lz_on),
                        summaryOff = stringResource(id = R.string.tip_collect_thread_see_lz),
                        key = "collect_thread_see_lz",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        title = stringResource(id = R.string.settings_collect_thread_desc_sort),
                        summaryOn = stringResource(id = R.string.tip_collect_thread_desc_sort_on),
                        summaryOff = stringResource(id = R.string.tip_collect_thread_desc_sort),
                        key = "collect_thread_desc_sort",
                        defaultValue = false,
                    )
                }
            }

            // ── 隐私与互动
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_habit_group_privacy),
                    icon = Icons.Outlined.Verified
                )
            }
            item {
                AppearanceCard {
                    SwitchSettingRow(
                        icon = Icons.Outlined.AddModerator,
                        title = stringResource(id = R.string.settings_incognito_mode),
                        key = "incognitoMode",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.SpeakerNotesOff,
                        title = stringResource(id = R.string.title_hide_reply),
                        key = "hideReply",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.Verified,
                        title = stringResource(id = R.string.title_show_both_username_and_nickname),
                        key = "show_both_username_and_nickname",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Outlined.SecurityUpdateWarning,
                        title = stringResource(id = R.string.title_post_or_reply_warning),
                        key = "postOrReplyWarning",
                        defaultValue = true,
                    )
                }
            }
        }
    }
}

/**
 * 启动首选页：标题行与其他设置项一致（icon + 标题），下方保留迷你骨架预览选择卡。
 * 点选写入 defaultStart（0=首页，1=动态页）。
 */
@Composable
private fun StartPageSelector() {
    val hapticFeedback = LocalHapticFeedback.current
    var startPage by rememberPreferenceAsMutableState(
        key = intPreferencesKey("defaultStart"),
        defaultValue = 0
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // 与 SettingRow 同款标题行（icon + 标题）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                tint = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(id = R.string.settings_default_start),
                style = MaterialTheme.typography.subtitle1
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StartPageOption(
                title = stringResource(id = R.string.title_main),
                selected = startPage == 0,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    startPage = 0
                },
                modifier = Modifier.weight(1f)
            ) {
                MiniHomePreview()
            }
            StartPageOption(
                title = stringResource(id = R.string.title_explore),
                selected = startPage == 1,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    startPage = 1
                },
                modifier = Modifier.weight(1f)
            ) {
                MiniExplorePreview()
            }
        }
    }
}

@Composable
private fun StartPageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ExtendedTheme.colors.background)
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) ExtendedTheme.colors.primary
                else ExtendedTheme.colors.divider.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colors.background)
        ) {
            preview()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
                color = if (selected) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun MiniSkeletonBar(
    height: Dp,
    modifier: Modifier = Modifier,
    widthFraction: Float? = null,
    width: Dp? = null,
    color: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
) {
    val base = if (widthFraction != null) modifier.fillMaxWidth(widthFraction) else modifier
    Box(
        modifier = base
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color)
    )
}

/** 首页缩小版骨架:工具栏 + 搜索条 + chips 行 + 两列吧列表 */
@Composable
private fun MiniHomePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            MiniSkeletonBar(width = 26.dp, height = 5.dp)
        }
        MiniSkeletonBar(
            height = 9.dp,
            modifier = Modifier.fillMaxWidth(),
            color = ExtendedTheme.colors.chip
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                MiniSkeletonBar(width = 18.dp, height = 6.dp)
            }
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(3) { MiniForumRow() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                repeat(3) { MiniForumRow() }
            }
        }
    }
}

@Composable
private fun MiniForumRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniSkeletonBar(height = 4.dp, widthFraction = 0.7f)
            MiniSkeletonBar(height = 3.dp, widthFraction = 0.45f)
        }
    }
}

/** 动态页缩小版骨架:顶栏 + 三 Tab 行(首 Tab 主题色)+ 两张 Feed 卡 */
@Composable
private fun MiniExplorePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniSkeletonBar(width = 24.dp, height = 5.dp)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MiniSkeletonBar(width = 14.dp, height = 4.dp, color = ExtendedTheme.colors.primary)
            MiniSkeletonBar(width = 14.dp, height = 4.dp)
            MiniSkeletonBar(width = 14.dp, height = 4.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { MiniFeedCard() }
        }
    }
}

@Composable
private fun MiniFeedCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.04f))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            )
            MiniSkeletonBar(width = 24.dp, height = 3.dp)
        }
        MiniSkeletonBar(height = 3.dp, widthFraction = 0.85f)
        MiniSkeletonBar(height = 22.dp, modifier = Modifier.fillMaxWidth())
    }
}
