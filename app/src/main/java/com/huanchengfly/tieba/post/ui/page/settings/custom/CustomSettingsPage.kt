package com.huanchengfly.tieba.post.ui.page.settings.custom

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.LightMode
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
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.App
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
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
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val SwatchShape = RoundedCornerShape(14.dp)

private fun parseSeedColor(hex: String): Color = runCatching {
    Color(
        if (hex.startsWith("0x", ignoreCase = true)) {
            hex.substring(2).toLong(16).toInt()
        } else {
            hex.toColorInt()
        }
    )
}.getOrDefault(Color(0xFF2C7BF2))

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
    var schemeVariant by rememberPreferenceAsMutableState(
        key = stringPreferencesKey("theme_scheme_variant"),
        defaultValue = "TONAL_SPOT"
    )
    var useSeedTheme by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("use_seed_theme"),
        defaultValue = false
    )
    var seedColorPref by rememberPreferenceAsMutableState(
        key = stringPreferencesKey("custom_primary_color"),
        defaultValue = "#FF2C7BF2"
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
    val hapticFeedback = LocalHapticFeedback.current
    val currentIsNightForPreview = ThemeUtil.isNightMode()
    val customLabel = stringResource(id = R.string.title_theme_custom)
    val schemeStyleLabel = stringResource(id = R.string.title_scheme_style)
    val variantTonalSpot = stringResource(id = R.string.scheme_tonal_spot)
    val variantNeutral = stringResource(id = R.string.scheme_neutral)
    val variantVibrant = stringResource(id = R.string.scheme_vibrant)
    val variantExpressive = stringResource(id = R.string.scheme_expressive)
    val variantFidelity = stringResource(id = R.string.scheme_fidelity)
    val variantContent = stringResource(id = R.string.scheme_content)
    val variantMonochrome = stringResource(id = R.string.scheme_monochrome)
    val variantRainbow = stringResource(id = R.string.scheme_rainbow)
    val variantFruitSalad = stringResource(id = R.string.scheme_fruit_salad)
    val seedBlue = stringResource(id = R.string.seed_blue)
    val seedTeal = stringResource(id = R.string.seed_teal)
    val seedSakura = stringResource(id = R.string.seed_sakura)
    val seedSpring = stringResource(id = R.string.seed_spring)
    val seedAutumn = stringResource(id = R.string.seed_autumn)
    val seedPurple = stringResource(id = R.string.seed_purple)
    val seedOrange = stringResource(id = R.string.seed_orange)
    val seedPink = stringResource(id = R.string.seed_pink)
    val seedRed = stringResource(id = R.string.seed_red)
    val seedIndigo = stringResource(id = R.string.seed_indigo)

    val schemeVariants = listOf(
        "TONAL_SPOT", "NEUTRAL", "VIBRANT", "EXPRESSIVE", "FIDELITY",
        "CONTENT", "MONOCHROME", "RAINBOW", "FRUIT_SALAD",
    )
    val variantLabels = mapOf(
        "TONAL_SPOT" to variantTonalSpot,
        "NEUTRAL" to variantNeutral,
        "VIBRANT" to variantVibrant,
        "EXPRESSIVE" to variantExpressive,
        "FIDELITY" to variantFidelity,
        "CONTENT" to variantContent,
        "MONOCHROME" to variantMonochrome,
        "RAINBOW" to variantRainbow,
        "FRUIT_SALAD" to variantFruitSalad,
    )
    val seedPresets = listOf(
        ThemeSwatch("0xFF2C7BF2", seedBlue, Color(0xFF2C7BF2), Color(0xFF2C7BF2), Color.White),
        ThemeSwatch("0xFF00897B", seedTeal, Color(0xFF00897B), Color(0xFF00897B), Color.White),
        ThemeSwatch("0xFF8E4955", seedSakura, Color(0xFF8E4955), Color(0xFF8E4955), Color.White),
        ThemeSwatch("0xFF4C662B", seedSpring, Color(0xFF4C662B), Color(0xFF4C662B), Color.White),
        ThemeSwatch("0xFF735C0C", seedAutumn, Color(0xFF735C0C), Color(0xFF735C0C), Color.White),
        ThemeSwatch("0xFF6750A4", seedPurple, Color(0xFF6750A4), Color(0xFF6750A4), Color.White),
        ThemeSwatch("0xFFEF6C00", seedOrange, Color(0xFFEF6C00), Color(0xFFEF6C00), Color.White),
        ThemeSwatch("0xFFD81B60", seedPink, Color(0xFFD81B60), Color(0xFFD81B60), Color.White),
        ThemeSwatch("0xFFC62828", seedRed, Color(0xFFC62828), Color(0xFFC62828), Color.White),
        ThemeSwatch("0xFF3F51B5", seedIndigo, Color(0xFF3F51B5), Color(0xFF3F51B5), Color.White),
    )

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
                useSeedTheme = true
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
                SectionLabel(text = stringResource(id = R.string.title_theme_mode), icon = Icons.Rounded.BrightnessAuto)
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
                        icon = Icons.Rounded.BrightnessAuto,
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
                        icon = Icons.Rounded.LightMode,
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
                        icon = Icons.Rounded.DarkMode,
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
                SectionLabel(text = stringResource(id = R.string.title_dark_style), icon = Icons.Rounded.DarkMode)
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

            // ── 配色风格（MD3 DynamicSchemeVariant，色卡即预览）
            item {
                SectionLabel(text = schemeStyleLabel)
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    schemeVariants.forEach { variantId ->
                        val style = runCatching { PaletteStyle.valueOf(variantId) }
                            .getOrDefault(PaletteStyle.TonalSpot)
                        val variantScheme = remember(seedColorPref, variantId, currentIsNight) {
                            dynamicColorScheme(
                                seedColor = parseSeedColor(seedColorPref),
                                isDark = currentIsNight,
                                style = style,
                            )
                        }
                        VariantCard(
                            label = variantLabels[variantId] ?: variantId,
                            primary = variantScheme.primary,
                            secondary = variantScheme.secondary,
                            tertiary = variantScheme.tertiary,
                            selected = schemeVariant == variantId,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                schemeVariant = variantId
                                if (useSeedTheme && currentTheme != ThemeUtil.THEME_CUSTOM) {
                                    ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                                }
                            }
                        )
                    }
                }
            }

            // ── 主题色彩（MD3 种子色）
            item {
                SectionLabel(text = stringResource(id = R.string.title_theme_color), icon = Icons.Rounded.Palette)
            }
            val seedRows = (seedPresets + ThemeSwatch(
                "custom",
                customLabel,
                Color(runCatching { seedColorPref.toColorInt() }.getOrDefault(0xFF2C7BF2.toInt())),
                Color.Transparent,
                Color.Transparent
            )).chunked(4)
            items(seedRows.size) { rowIndex ->
                val row = seedRows[rowIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    row.forEach { swatch ->
                        val selected = useSeedTheme && swatch.id != "custom" &&
                                seedColorPref == swatch.id
                        ColorSwatchCard(
                            swatch = swatch,
                            selected = selected,
                            schemeVariant = schemeVariant,
                            isNight = currentIsNight,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (swatch.id == "custom") {
                                    customPrimaryColorDialogState.show()
                                } else {
                                    seedColorPref = swatch.id
                                    useSeedTheme = true
                                    ThemeUtil.setUseDynamicTheme(false)
                                    ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                                }
                            }
                        )
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── 通用
            item {
                SectionLabel(text = stringResource(id = R.string.title_theme_more))
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
internal fun SectionLabel(text: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ExtendedTheme.colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ExtendedTheme.colors.primary
        )
    }
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
    icon: ImageVector,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = if (selected) {
                    ExtendedTheme.colors.primary.copy(alpha = 0.10f)
                } else {
                    ExtendedTheme.colors.card
                }
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            preview()
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.desc_checked),
                    tint = ExtendedTheme.colors.background,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ExtendedTheme.colors.primary)
                        .padding(3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.text
            )
        }
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
private fun VariantCard(
    label: String,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.divider,
        animationSpec = tween(durationMillis = 200),
        label = "variantBorder"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = ExtendedTheme.colors.card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
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
                .fillMaxWidth()
                .height(72.dp)
                .background(primary)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(primary, secondary, tertiary).forEach { color ->
                Spacer(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.text,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun ColorSwatchCard(
    swatch: ThemeSwatch,
    selected: Boolean,
    schemeVariant: String,
    isNight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    // 三色点 = 该种子在当前配色风格下生成的 primary/secondary/tertiary（色卡即预览）
    val scheme = remember(swatch.fill, schemeVariant, isNight) {
        dynamicColorScheme(
            seedColor = swatch.fill,
            isDark = isNight,
            style = runCatching { PaletteStyle.valueOf(schemeVariant) }
                .getOrDefault(PaletteStyle.TonalSpot),
        )
    }
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
                .aspectRatio(0.9f)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ExtendedTheme.colors.background)
                    .padding(vertical = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(scheme.primary, scheme.secondary, scheme.tertiary).forEach { color ->
                        Spacer(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.desc_checked),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(6.dp)
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
