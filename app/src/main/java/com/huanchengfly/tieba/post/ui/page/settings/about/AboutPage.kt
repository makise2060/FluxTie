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
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Report
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
import com.huanchengfly.tieba.post.arch.hiltViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.destinations.LogPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.components.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.components.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.components.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
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

/** 问题反馈邮箱（GitHub 访问不便时的兜底渠道） */
private const val FEEDBACK_EMAIL = "makise2060@gmail.com"

@Destination
@Composable
fun AboutPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val updateViewModel = hiltViewModel<UpdateViewModel>()
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
                SectionLabel(
                    text = stringResource(id = R.string.title_about_info),
                    icon = Icons.Rounded.Info
                )
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Rounded.History,
                        title = stringResource(id = R.string.title_check_update),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { updateViewModel.check() }
                    )
                    CardDivider()
                    SettingRow(
                        icon = Icons.Rounded.Description,
                        title = stringResource(id = R.string.title_open_source_license),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { launchUrl(context, navigator, "$REPO_URL/blob/main/LICENSE") }
                    )
                }
            }

            // ── 开发
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_about_develop),
                    icon = Icons.Rounded.Code
                )
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Rounded.Code,
                        title = stringResource(id = R.string.title_project_source),
                        summary = "GitHub",
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { launchUrl(context, navigator, REPO_URL) }
                    )
                    CardDivider()
                    SettingRow(
                        icon = Icons.Rounded.BugReport,
                        title = stringResource(id = R.string.title_app_logs),
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { navigator.navigate(LogPageDestination) }
                    )
                }
            }

            // ── 反馈：GitHub Issue 直达 + 邮件兜底
            item {
                SectionLabel(
                    text = stringResource(id = R.string.title_about_feedback),
                    icon = Icons.Rounded.RateReview
                )
            }
            item {
                AppearanceCard {
                    SettingRow(
                        icon = Icons.Rounded.Report,
                        title = stringResource(id = R.string.title_feedback_github_issue),
                        summary = "makise2060/FluxTie",
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { launchUrl(context, navigator, "$REPO_URL/issues/new") }
                    )
                    CardDivider()
                    SettingRow(
                        icon = Icons.Rounded.Email,
                        title = stringResource(id = R.string.title_feedback_email),
                        summary = FEEDBACK_EMAIL,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = ExtendedTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$FEEDBACK_EMAIL"))
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
