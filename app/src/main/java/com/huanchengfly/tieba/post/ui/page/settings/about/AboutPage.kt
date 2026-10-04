package com.huanchengfly.tieba.post.ui.page.settings.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.destinations.LogPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfettiBurst
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.launchUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REPO_URL = "https://github.com/makise2060/FluxTie"
private const val RELEASES_URL = "https://github.com/makise2060/FluxTie/releases"

@Destination
@Composable
fun AboutPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var clickCount by remember { mutableIntStateOf(0) }

    // ── 彩蛋:连点渐强 + 庆祝爆发 ──
    var burstKey by remember { mutableIntStateOf(0) }
    var bubbleVisible by remember { mutableStateOf(false) }
    var bubbleResId by remember { mutableIntStateOf(0) }
    val tapScale = remember { Animatable(1f) }
    val wiggleRotation = remember { Animatable(0f) }
    val celebrateScale = remember { Animatable(1f) }
    val celebrateRotation = remember { Animatable(0f) }

    fun onLogoTap() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) {
            clickCount++
        } else {
            clickCount = 1
        }
        lastClickTime = currentTime
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // 每次点击:轻微弹跳;第 5/6 次:轻晃暗示
        coroutineScope.launch {
            tapScale.snapTo(0.95f)
            tapScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 800f))
        }
        if (clickCount == 5 || clickCount == 6) {
            coroutineScope.launch {
                wiggleRotation.snapTo(-3f)
                wiggleRotation.animateTo(3f, tween(durationMillis = 80))
                wiggleRotation.animateTo(0f, tween(durationMillis = 80))
            }
        }
        if (clickCount >= 7) {
            clickCount = 0
            val enabled = !context.appPreferences.showExperimentalFeatures
            context.appPreferences.showExperimentalFeatures = enabled
            bubbleResId = if (enabled) R.string.easter_egg_on else R.string.easter_egg_off
            bubbleVisible = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            burstKey++
            coroutineScope.launch {
                celebrateScale.snapTo(1f)
                celebrateScale.animateTo(1.2f, tween(durationMillis = 120, easing = EaseOutCubic))
                celebrateScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 800f))
            }
            coroutineScope.launch {
                celebrateRotation.snapTo(0f)
                celebrateRotation.animateTo(360f, tween(durationMillis = 500, easing = EaseOutCubic))
            }
        }
    }

    LaunchedEffect(bubbleVisible) {
        if (bubbleVisible) {
            delay(2500)
            bubbleVisible = false
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_about),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 头部：logo + 名称 + 版本（连点 7 次触发彩蛋庆祝：纸屑 + logo 弹跳旋转 + 气泡）
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ConfettiBurst(
                        triggerKey = burstKey,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onLogoTap() }
                            )
                            .padding(vertical = 32.dp)
                    ) {
                        Image(
                            painter = rememberDrawablePainter(
                                drawable = context.getDrawable(R.mipmap.ic_launcher_new_round)
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .graphicsLayer {
                                    scaleX = tapScale.value * celebrateScale.value
                                    scaleY = tapScale.value * celebrateScale.value
                                    rotationZ = wiggleRotation.value + celebrateRotation.value
                                }
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "FluxTie",
                            style = MaterialTheme.typography.h4,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                id = R.string.summary_about_version,
                                BuildConfig.VERSION_NAME
                            ),
                            fontSize = 15.sp,
                            color = ExtendedTheme.colors.textSecondary
                        )
                    }
                    // 彩蛋气泡：叠加层，不参与 Column 测量，出现/消失不挤动下方内容
                    // （气泡高度小于 Column 底部内边距，底对齐即落在版本号下方空隙）
                    AnimatedVisibility(
                        visible = bubbleVisible,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { -it / 2 },
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(100))
                                .clip(RoundedCornerShape(100))
                                .background(color = ExtendedTheme.colors.primary)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(id = bubbleResId),
                                color = ExtendedTheme.colors.onAccent,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ── 信息
            item {
                AboutSectionLabel(text = stringResource(id = R.string.title_about_info))
            }
            item {
                AboutCard {
                    AboutRow(
                        icon = Icons.Rounded.History,
                        title = stringResource(id = R.string.title_check_update),
                        onClick = { launchUrl(context, navigator, RELEASES_URL) }
                    )
                    AboutDivider()
                    AboutRow(
                        icon = Icons.Rounded.Description,
                        title = stringResource(id = R.string.title_open_source_license),
                        onClick = { launchUrl(context, navigator, "$REPO_URL/blob/main/LICENSE") }
                    )
                }
            }

            // ── 开发
            item {
                AboutSectionLabel(text = stringResource(id = R.string.title_about_develop))
            }
            item {
                AboutCard {
                    AboutRow(
                        icon = Icons.Rounded.Code,
                        title = stringResource(id = R.string.title_project_source),
                        summary = "GitHub",
                        onClick = { launchUrl(context, navigator, REPO_URL) }
                    )
                    AboutDivider()
                    AboutRow(
                        icon = Icons.Rounded.BugReport,
                        title = stringResource(id = R.string.title_app_logs),
                        onClick = { navigator.navigate(LogPageDestination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = ExtendedTheme.colors.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ExtendedTheme.colors.primary
        )
    }
}

@Composable
private fun AboutCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color = ExtendedTheme.colors.card),
        content = content
    )
}

@Composable
private fun AboutDivider() {
    Divider(
        thickness = 1.dp,
        color = ExtendedTheme.colors.divider.copy(
            alpha = if (ExtendedTheme.colors.isNightMode) 0.45f else 0.6f
        ),
        modifier = Modifier.padding(start = 56.dp, end = 16.dp)
    )
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
