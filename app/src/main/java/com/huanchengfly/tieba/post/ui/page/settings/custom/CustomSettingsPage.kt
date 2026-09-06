package com.huanchengfly.tieba.post.ui.page.settings.custom

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.AppFontSizeActivity
import com.huanchengfly.tieba.post.components.dialogs.CustomThemeDialog
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val SwatchShape = RoundedCornerShape(14.dp)

private data class ThemeSwatch(
    val id: String,
    val name: String,
    val fill: Color,
    val dotColor: Color,
    val stripeColor: Color,
)

private data class DarkStyleOption(
    val id: String,
    val name: String,
)

// 深色样式可选项（旧的静谧蓝已随前代主题一并移除）
private val DarkStyleOptions = listOf(
    DarkStyleOption(ThemeUtil.THEME_GREY_DARK, "grey"),
    DarkStyleOption(ThemeUtil.THEME_AMOLED_DARK, "amoled"),
)

@Destination
@Composable
fun CustomSettingsPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val themeValues = arrayOf(
        ThemeUtil.THEME_DEFAULT,
        ThemeUtil.THEME_BLUE,
        ThemeUtil.THEME_PINK,
        ThemeUtil.THEME_RED,
        ThemeUtil.THEME_PURPLE,
    )
    val themeLabels = listOf(
        stringResource(id = R.string.title_theme_color_default),
        stringResource(id = R.string.title_theme_color_blue),
        stringResource(id = R.string.title_theme_color_pink),
        stringResource(id = R.string.title_theme_color_red),
        stringResource(id = R.string.title_theme_color_purple),
    )
    val darkStyleNames = mapOf(
        ThemeUtil.THEME_GREY_DARK to stringResource(id = R.string.title_dark_style_grey),
        ThemeUtil.THEME_AMOLED_DARK to stringResource(id = R.string.title_dark_style_amoled),
    )
    val currentTheme by remember { ThemeUtil.themeState }
    var followSystemNight by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("follow_system_night"),
        defaultValue = true
    )
    var darkTheme by rememberPreferenceAsMutableState(
        key = stringPreferencesKey(ThemeUtil.KEY_DARK_THEME),
        defaultValue = ThemeUtil.THEME_AMOLED_DARK
    )
    var appIcon by rememberPreferenceAsMutableState(
        key = stringPreferencesKey("app_icon"),
        defaultValue = LauncherIcons.NEW_ICON
    )
    val appIconDialogState = rememberDialogState()
    val customPrimaryColorDialogState = rememberDialogState()
    var customPrimaryColor by remember {
        mutableStateOf(
            Color(
                App.ThemeDelegate.getColorByAttr(
                    context,
                    R.attr.colorPrimary,
                    ThemeUtil.THEME_CUSTOM
                )
            )
        )
    }

    val currentIsNight = ThemeUtil.isNightMode()
    val oldTheme by rememberPreferenceAsState(
        key = stringPreferencesKey(ThemeUtil.KEY_OLD_THEME),
        defaultValue = ThemeUtil.THEME_DEFAULT
    )
    val lightPreviewTheme = if (currentIsNight) oldTheme else currentTheme
    val customLabel = stringResource(id = R.string.title_theme_custom)

    val allSwatches = arrayOf(
        ThemeUtil.THEME_DEFAULT,
        ThemeUtil.THEME_BLUE,
        ThemeUtil.THEME_PINK,
        ThemeUtil.THEME_RED,
        ThemeUtil.THEME_PURPLE,
    ).mapIndexed { index, themeId ->
        ThemeSwatch(
            id = themeId,
            name = themeLabels[index],
            fill = Color(
                App.ThemeDelegate.getColorByAttr(context, R.attr.colorNewPrimary, themeId)
            ),
            dotColor = Color(
                App.ThemeDelegate.getColorByAttr(context, R.attr.colorAccent, themeId)
            ),
            stripeColor = Color(
                App.ThemeDelegate.getColorByAttr(context, R.attr.colorBackground, themeId)
            ),
        )
    } + ThemeSwatch(
        "custom", customLabel, customPrimaryColor, ExtendedTheme.colors.accent, ExtendedTheme.colors.background
    )
    val swatchRows = allSwatches.chunked(3)

    // 自定义主色弹窗
    Dialog(
        dialogState = customPrimaryColorDialogState,
        title = { Text(text = stringResource(id = R.string.title_custom_theme)) },
        buttons = {
            androidx.compose.material.TextButton(onClick = {
                customPrimaryColor = Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        R.attr.colorPrimary,
                        ThemeUtil.THEME_CUSTOM
                    )
                )
                customPrimaryColorDialogState.show = false
            }) {
                Text(text = stringResource(id = R.string.button_cancel))
            }
            androidx.compose.material.TextButton(onClick = {
                context.appPreferences.customPrimaryColor =
                    CustomThemeDialog.toString(customPrimaryColor.toArgb())
                ThemeUtil.setUseDynamicTheme(false)
                ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                customPrimaryColorDialogState.show = false
            }) {
                Text(text = stringResource(id = R.string.button_finish))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(customPrimaryColor)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
                    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFF44336),
                ).forEach { preset ->
                    Spacer(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                width = if (customPrimaryColor == preset) 2.dp else 0.dp,
                                color = ExtendedTheme.colors.primary,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { customPrimaryColor = preset }
                            )
                    )
                }
            }
        }
    }

    // 应用图标选择弹窗
    Dialog(
        dialogState = appIconDialogState,
        title = { Text(text = stringResource(id = R.string.settings_app_icon)) },
        buttons = {
            androidx.compose.material.TextButton(onClick = { appIconDialogState.show = false }) {
                Text(text = stringResource(id = R.string.button_cancel))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val iconOptions = listOf(
                Triple(LauncherIcons.NEW_ICON, "新图标", R.drawable.ic_launcher_new_round),
                Triple(LauncherIcons.NEW_ICON_INVERT, "新图标（反色）", R.drawable.ic_launcher_new_invert_round),
                Triple(LauncherIcons.OLD_ICON, "旧图标", R.drawable.ic_launcher_round),
            )
            iconOptions.forEachIndexed { index, (value, name, drawableRes) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                appIcon = value
                                AppIconUtil.setIcon(icon = value)
                                appIconDialogState.show = false
                            }
                        )
                        .padding(12.dp)
                ) {
                    Image(
                        painter = rememberDrawablePainter(
                            drawable = context.getDrawable(drawableRes)
                        ),
                        contentDescription = name,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f)
                    )
                    if (appIcon == value) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(id = R.string.desc_checked),
                            tint = ExtendedTheme.colors.primary
                        )
                    }
                }
                if (index != iconOptions.lastIndex) {
                    CardDivider()
                }
            }
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_custom),
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
            // ── 主题模式
            item {
                SectionLabel(text = stringResource(id = R.string.title_theme_mode))
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    ModeCard(
                        label = stringResource(id = R.string.title_theme_mode_auto),
                        selected = followSystemNight,
                        onClick = { followSystemNight = true },
                        modifier = Modifier.weight(1f),
                        preview = {
                            Row(Modifier.fillMaxSize()) {
                                MiniScreenPreview(theme = lightPreviewTheme, modifier = Modifier.weight(1f))
                                MiniScreenPreview(theme = darkTheme, modifier = Modifier.weight(1f))
                            }
                        }
                    )
                    ModeCard(
                        label = stringResource(id = R.string.title_theme_mode_light),
                        selected = !followSystemNight && !currentIsNight,
                        onClick = {
                            followSystemNight = false
                            if (ThemeUtil.isNightMode()) ThemeUtil.switchNightMode()
                        },
                        modifier = Modifier.weight(1f),
                        preview = { MiniScreenPreview(theme = lightPreviewTheme, modifier = Modifier.fillMaxSize()) }
                    )
                    ModeCard(
                        label = stringResource(id = R.string.title_theme_mode_dark),
                        selected = !followSystemNight && currentIsNight,
                        onClick = {
                            followSystemNight = false
                            if (!ThemeUtil.isNightMode()) ThemeUtil.switchNightMode()
                        },
                        modifier = Modifier.weight(1f),
                        preview = { MiniScreenPreview(theme = darkTheme, modifier = Modifier.fillMaxSize()) }
                    )
                }
            }

            // ── 深色样式
            item {
                SectionLabel(text = stringResource(id = R.string.title_dark_style))
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    DarkStyleOptions.forEach { option ->
                        DarkStyleCard(
                            name = darkStyleNames[option.id] ?: option.id,
                            theme = option.id,
                            selected = darkTheme == option.id,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                darkTheme = option.id
                                if (ThemeUtil.isNightMode()) {
                                    ThemeUtil.switchTheme(option.id, recordOldTheme = false)
                                }
                            }
                        )
                    }
                }
            }

            // ── 主题色彩
            item {
                SectionLabel(text = stringResource(id = R.string.title_theme_color))
            }
            items(swatchRows.size) { rowIndex ->
                val row = swatchRows[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    row.forEach { swatch ->
                        ColorSwatchCard(
                            swatch = swatch,
                            selected = currentTheme == swatch.id,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (swatch.id == "custom") {
                                    customPrimaryColorDialogState.show()
                                } else {
                                    ThemeUtil.switchTheme(swatch.id)
                                    ThemeUtil.setUseDynamicTheme(false)
                                }
                            }
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── 通用
            item {
                SectionLabel(text = stringResource(id = R.string.title_theme_more))
            }
            item {
                AppearanceCard {
                    SwitchSettingRow(
                        icon = Icons.Rounded.PhotoSizeSelectActual,
                        title = stringResource(id = R.string.title_settings_status_bar_darker),
                        summary = stringResource(id = R.string.summary_settings_status_bar_darker),
                        key = "status_bar_darker",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Rounded.FormatColorFill,
                        title = stringResource(id = R.string.tip_toolbar_primary_color),
                        summary = stringResource(id = R.string.tip_toolbar_primary_color_summary),
                        key = "custom_toolbar_primary_color",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Rounded.ViewAgenda,
                        title = stringResource(id = R.string.settings_forum_single),
                        key = "listSingle",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Rounded.Explore,
                        title = stringResource(id = R.string.title_hide_explore),
                        key = "hideExplore",
                        defaultValue = false,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Rounded.Dock,
                        title = stringResource(id = R.string.title_floating_bottom_nav),
                        summary = stringResource(id = R.string.summary_floating_bottom_nav),
                        key = "floatingBottomNav",
                        defaultValue = true,
                    )
                    CardDivider()
                    SwitchSettingRow(
                        icon = Icons.Rounded.Upcoming,
                        title = stringResource(id = R.string.title_lift_up_bottom_bar),
                        summary = stringResource(id = R.string.summary_lift_up_bottom_bar),
                        key = "liftUpBottomBar",
                        defaultValue = true,
                    )
                }
            }

            // ── 应用图标
            item {
                SectionLabel(text = stringResource(id = R.string.settings_app_icon))
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Outlined.Apps,
                        title = stringResource(id = R.string.settings_app_icon),
                        onClick = { appIconDialogState.show() },
                        trailing = {
                            Text(
                                text = when (appIcon) {
                                    LauncherIcons.NEW_ICON -> "新图标"
                                    LauncherIcons.NEW_ICON_INVERT -> "新图标（反色）"
                                    LauncherIcons.OLD_ICON -> "旧图标"
                                    else -> ""
                                },
                                color = ExtendedTheme.colors.textSecondary
                            )
                        }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CardDivider()
                        val currentIcon by rememberPreferenceAsState(
                            key = stringPreferencesKey("app_icon"),
                            defaultValue = LauncherIcons.NEW_ICON
                        )
                        SwitchSettingRow(
                            icon = Icons.Outlined.ColorLens,
                            title = stringResource(id = R.string.title_settings_use_themed_icon),
                            summary = stringResource(id = R.string.tip_settings_use_themed_icon_summary_not_supported)
                                .takeIf { currentIcon != LauncherIcons.NEW_ICON },
                            key = "useThemedIcon",
                            defaultValue = false,
                            enabled = currentIcon == LauncherIcons.NEW_ICON,
                            onCheckedChange = { AppIconUtil.setIcon(isThemed = it) },
                        )
                    }
                }
            }

            // ── 字体
            item {
                SectionLabel(text = stringResource(id = R.string.title_custom_font_size))
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Rounded.FontDownload,
                        title = stringResource(id = R.string.title_custom_font_size),
                        onClick = { context.goToActivity<AppFontSizeActivity>() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = ExtendedTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/** Bettbox 风格分组卡片：圆角 20，组内行间细分隔线 */
@Composable
internal fun AppearanceCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color = ExtendedTheme.colors.card),
        content = content
    )
}

@Composable
internal fun CardDivider() {
    Divider(
        thickness = 1.dp,
        color = ExtendedTheme.colors.divider.copy(alpha = if (ExtendedTheme.colors.isNightMode) 0.45f else 0.6f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
    )
}

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && onClick != null,
                onClick = { onClick?.invoke() }
            )
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    var checked by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(key),
        defaultValue = defaultValue
    )
    SettingRow(
        icon = icon,
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = {
            checked = !checked
            onCheckedChange?.invoke(checked)
        },
        trailing = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    )
}

@Composable
private fun ThemeMiniColors(theme: String): Triple<Color, Color, Color> {
    val context = LocalContext.current
    val background = remember(theme) {
        Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorBackground, theme))
    }
    val primary = remember(theme) {
        Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorNewPrimary, theme))
    }
    val text = remember(theme) {
        Color(App.ThemeDelegate.getColorByAttr(context, R.attr.colorText, theme))
    }
    return Triple(background, primary, text)
}

@Composable
private fun MiniScreenPreview(
    theme: String,
    modifier: Modifier = Modifier,
) {
    val (background, primary, text) = ThemeMiniColors(theme)
    Column(
        modifier = modifier
            .background(background)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(primary)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(4.dp)
                .clip(CircleShape)
                .background(text.copy(alpha = 0.55f))
        )
        Spacer(modifier = Modifier.size(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(3.dp))
                .background(text.copy(alpha = 0.12f))
        )
        Spacer(modifier = Modifier.size(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(4.dp)
                .clip(CircleShape)
                .background(text.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun ModeCard(
    label: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.divider,
        animationSpec = tween(durationMillis = 200),
        label = "modeCardBorder"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        animationSpec = tween(durationMillis = 200),
        label = "modeCardLabelAlpha"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(14.dp))
                .background(color = ExtendedTheme.colors.card)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                )
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                preview()
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.desc_checked),
                    tint = ExtendedTheme.colors.background,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ExtendedTheme.colors.primary)
                        .padding(2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.text,
            modifier = Modifier.alpha(labelAlpha)
        )
    }
}


@Composable
private fun DarkStyleCard(
    name: String,
    theme: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val (background, primary, _) = ThemeMiniColors(theme)
    val borderColor by animateColorAsState(
        targetValue = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.divider,
        animationSpec = tween(durationMillis = 200),
        label = "darkStyleBorder"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.NightsStay,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(18.dp)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.desc_checked),
                    tint = background,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(primary)
                        .padding(2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.text
        )
    }
}

@Composable
private fun ColorSwatchCard(
    swatch: ThemeSwatch,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.divider,
        animationSpec = tween(durationMillis = 200),
        label = "swatchBorder"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SwatchShape)
                .background(swatch.fill)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = SwatchShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(swatch.stripeColor),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(swatch.dotColor)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.desc_checked),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ExtendedTheme.colors.primary)
                        .padding(2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.size(5.dp))
        Text(
            text = swatch.name,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = ExtendedTheme.colors.text
        )
    }
}
