package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.fetch.newResourceUri
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.components.dialogs.CustomThemeDialog
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.dynamicTonalPalette
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.extension.toHexString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val SwatchShape = RoundedCornerShape(14.dp)

private data class Swatch(
    val id: String,
    val name: String,
    val fill: Color,
    val dotColor: Color,
    val stripeColor: Color,
    val selected: Boolean,
    val isDynamic: Boolean = false,
    val isCustom: Boolean = false,
    val isNightTheme: Boolean = false,
    val onClick: () -> Unit,
)

@Destination
@Composable
fun AppThemePage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val themeValues = stringArrayResource(id = R.array.theme_values)
    val themeNames = stringArrayResource(id = R.array.themeNames)
    val darkThemeValues = stringArrayResource(id = R.array.dark_theme_values)
    val darkThemeNames = stringArrayResource(id = R.array.dark_theme_names)
    val currentTheme by remember { ThemeUtil.themeState }
    val isDynamicTheme by rememberPreferenceAsState(
        key = booleanPreferencesKey("useDynamicColorTheme"),
        defaultValue = false
    )
    var followSystemNight by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("follow_system_night"),
        defaultValue = true
    )
    var darkTheme by rememberPreferenceAsMutableState(
        key = stringPreferencesKey(ThemeUtil.KEY_DARK_THEME),
        defaultValue = ThemeUtil.THEME_AMOLED_DARK
    )
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
    var customToolbarPrimaryColor by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(
            ThemeUtil.KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR
        ),
        defaultValue = false
    )
    var customStatusBarFontDark by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(
            ThemeUtil.KEY_CUSTOM_STATUS_BAR_FONT_DARK
        ),
        defaultValue = false
    )

    val currentIsNight = ThemeUtil.isNightMode()
    val oldTheme by rememberPreferenceAsState(
        key = stringPreferencesKey(ThemeUtil.KEY_OLD_THEME),
        defaultValue = ThemeUtil.THEME_DEFAULT
    )
    val lightPreviewTheme = if (currentIsNight) oldTheme else currentTheme

    // 自定义主色弹窗（保持原有数据流）
    Dialog(
        dialogState = customPrimaryColorDialogState,
        title = { Text(text = stringResource(id = R.string.title_custom_theme)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_finish),
                onClick = {
                    customStatusBarFontDark = customStatusBarFontDark || !customToolbarPrimaryColor
                    context.appPreferences.customPrimaryColor =
                        CustomThemeDialog.toString(customPrimaryColor.toArgb())
                    context.appPreferences.toolbarPrimaryColor = customToolbarPrimaryColor
                    context.appPreferences.customStatusBarFontDark = customStatusBarFontDark
                    ThemeUtil.setUseDynamicTheme(false)
                    ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.button_cancel),
                onClick = {
                    customPrimaryColor = Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorPrimary,
                            ThemeUtil.THEME_CUSTOM
                        )
                    )
                }
            )
        }
    ) {
        var useInput by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = useInput,
                label = "",
                modifier = Modifier
                    .wrapContentHeight()
                    .animateContentSize()
            ) { input ->
                if (input) {
                    var inputHexColor by remember { mutableStateOf(customPrimaryColor.toHexString()) }
                    val lastValidColor by produceState(
                        initialValue = customPrimaryColor,
                        inputHexColor
                    ) {
                        if ("^#([0-9a-fA-F]{6})$".toRegex().matches(inputHexColor)) {
                            value = Color(inputHexColor.toColorInt())
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(lastValidColor)
                        )
                        OutlinedTextField(
                            value = inputHexColor,
                            onValueChange = {
                                if ("^#([0-9a-fA-F]{0,6})$".toRegex().matches(it)) {
                                    inputHexColor = it
                                }
                            },
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = ExtendedTheme.colors.primary,
                                focusedBorderColor = ExtendedTheme.colors.primary,
                                focusedLabelColor = ExtendedTheme.colors.primary
                            )
                        )
                        IconButton(
                            onClick = {
                                customPrimaryColor = Color(inputHexColor.toColorInt())
                                useInput = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(id = R.string.button_sure_default)
                            )
                        }
                    }
                } else {
                    Box {
                        HarmonyColorPicker(
                            harmonyMode = ColorHarmonyMode.ANALOGOUS,
                            color = HsvColor.from(customPrimaryColor),
                            onColorChanged = {
                                customPrimaryColor = it.toColor()
                            },
                            modifier = Modifier.sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                        )

                        IconButton(
                            onClick = { useInput = true },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BorderColor,
                                contentDescription = stringResource(id = R.string.desc_input_color)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            customToolbarPrimaryColor =
                                !customToolbarPrimaryColor
                        }
                    )
            ) {
                Checkbox(
                    checked = customToolbarPrimaryColor,
                    onCheckedChange = {
                        customToolbarPrimaryColor = it
                    },
                )
                Text(text = stringResource(id = R.string.tip_toolbar_primary_color))
            }

            if (customToolbarPrimaryColor) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                customStatusBarFontDark =
                                    !customStatusBarFontDark
                            }
                        )
                ) {
                    Checkbox(
                        checked = customStatusBarFontDark,
                        onCheckedChange = {
                            customStatusBarFontDark = it
                        },
                    )
                    Text(text = stringResource(id = R.string.tip_status_bar_font))
                }
            }
        }
    }

    // 色卡数据：动态取色 + 固定主题 + 自定义
    val dynamicLabel = stringResource(id = R.string.title_dynamic_theme)
    val customLabel = stringResource(id = R.string.title_theme_custom)
    val extColors = ExtendedTheme.colors

    val swatches = remember(
        themeValues, themeNames, isDynamicTheme, currentTheme, customPrimaryColor
    ) {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val tonalPalette = dynamicTonalPalette(context)
                add(
                    Swatch(
                        id = "dynamic",
                        name = dynamicLabel,
                        fill = Color.Transparent,
                        dotColor = tonalPalette.primary50,
                        stripeColor = Color.Transparent,
                        selected = isDynamicTheme,
                        isDynamic = true,
                        onClick = { ThemeUtil.setUseDynamicTheme(true) }
                    )
                )
            }
            themeValues.forEachIndexed { index, item ->
                val name = themeNames[index]
                val night = ThemeUtil.isNightMode(item)
                val fill = Color(
                    App.ThemeDelegate.getColorByAttr(
                        context,
                        if (night) R.attr.colorBackground else R.attr.colorNewPrimary,
                        item
                    )
                )
                val dot = Color(
                    App.ThemeDelegate.getColorByAttr(context, R.attr.colorAccent, item)
                )
                val stripe = Color(
                    App.ThemeDelegate.getColorByAttr(context, R.attr.colorBackground, item)
                )
                add(
                    Swatch(
                        id = item,
                        name = name,
                        fill = fill,
                        dotColor = dot,
                        stripeColor = stripe,
                        selected = !isDynamicTheme && currentTheme == item,
                        isNightTheme = night,
                        onClick = {
                            ThemeUtil.switchTheme(item)
                            ThemeUtil.setUseDynamicTheme(false)
                        }
                    )
                )
            }
                add(
                    Swatch(
                        id = "custom",
                        name = customLabel,
                    fill = customPrimaryColor,
                    dotColor = extColors.accent,
                    stripeColor = extColors.background,
                    selected = !isDynamicTheme && currentTheme == ThemeUtil.THEME_CUSTOM,
                    isCustom = true,
                    onClick = { customPrimaryColorDialogState.show() }
                )
            )
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_theme),
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
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── 主题模式：三卡实时预览
            item {
                GroupHeader(
                    icon = { Icon(imageVector = Icons.Rounded.BrightnessAuto, contentDescription = null) },
                    title = stringResource(id = R.string.title_theme_mode)
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    ModeCard(
                        label = stringResource(id = R.string.title_theme_mode_auto),
                        selected = followSystemNight,
                        onClick = { followSystemNight = true },
                        modifier = Modifier.weight(1f),
                        preview = {
                            Row(Modifier.fillMaxSize()) {
                                MiniScreenPreview(
                                    theme = lightPreviewTheme,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniScreenPreview(
                                    theme = darkTheme,
                                    modifier = Modifier.weight(1f)
                                )
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
            item { Spacer(modifier = Modifier.size(20.dp)) }

            // ── 深色样式：三色小卡（夜间下点选立即生效）
            item {
                GroupHeader(
                    icon = { Icon(imageVector = Icons.Rounded.DarkMode, contentDescription = null) },
                    title = stringResource(id = R.string.title_dark_style)
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    darkThemeValues.forEachIndexed { index, value ->
                        DarkStyleCard(
                            name = darkThemeNames[index],
                            theme = value,
                            selected = darkTheme == value,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                darkTheme = value
                                if (ThemeUtil.isNightMode()) {
                                    ThemeUtil.switchTheme(value, recordOldTheme = false)
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.size(20.dp)) }

            // ── 主题色彩：色卡网格
            item {
                GroupHeader(
                    icon = { Icon(imageVector = Icons.Rounded.Palette, contentDescription = null) },
                    title = stringResource(id = R.string.title_theme_color)
                )
            }
            val rows = swatches.chunked(4)
            items(rows.size) { rowIndex ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                ) {
                    val row = rows[rowIndex]
                    row.forEach { swatch ->
                        ColorSwatchCard(swatch = swatch, modifier = Modifier.weight(1f))
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item { Spacer(modifier = Modifier.size(20.dp)) }

            // ── 更多
            item {
                GroupHeader(
                    icon = { Icon(imageVector = Icons.Rounded.Tune, contentDescription = null) },
                    title = stringResource(id = R.string.title_theme_more)
                )
            }
            item {
                ActionRow(
                    text = stringResource(id = R.string.title_theme_translucent),
                    icon = { Icon(imageVector = Icons.Rounded.PhotoSizeSelectActual, contentDescription = null) },
                    selected = ThemeUtil.isTranslucentTheme(currentTheme),
                    onClick = { context.goToActivity<TranslucentThemeActivity>() }
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(
    icon: @Composable () -> Unit,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.size(18.dp)) {
            icon()
        }
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium
        )
    }
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
    swatch: Swatch,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (swatch.selected) ExtendedTheme.colors.primary else ExtendedTheme.colors.divider,
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
                .background(
                    if (swatch.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val tonalPalette = dynamicTonalPalette(LocalContext.current)
                        Brush.sweepGradient(
                            colors = listOf(
                                tonalPalette.primary50,
                                tonalPalette.secondary50,
                                tonalPalette.tertiary50,
                                tonalPalette.primary50,
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(swatch.fill, swatch.fill))
                    }
                )
                .border(
                    width = if (swatch.selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = SwatchShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        swatch.onClick()
                    }
                )
        ) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(swatch.stripeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Spacer(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(swatch.dotColor)
                        )
                        Spacer(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(swatch.fill)
                                .border(1.dp, swatch.dotColor.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                }
            }
            if (swatch.isNightTheme) {
                Icon(
                    imageVector = Icons.Rounded.NightsStay,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(20.dp)
                )
            }
            if (swatch.isDynamic) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(2.dp)
                )
            }
            if (swatch.selected) {
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

@Composable
private fun ActionRow(
    text: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = ExtendedTheme.colors.card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(modifier = Modifier.size(22.dp)) {
            icon()
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.desc_checked),
                tint = ExtendedTheme.colors.primary
            )
        }
    }
}
