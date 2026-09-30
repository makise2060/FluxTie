package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.pm.ActivityInfo
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material.shimmer
import com.eygraber.compose.placeholder.material.placeholder
import com.stoyanvuchev.systemuibarstweaker.rememberSystemUIBarsTweaker
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadBean
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.video.DefaultVideoPlayerController
import com.huanchengfly.tieba.post.ui.widgets.compose.video.OnFullScreenModeChangedListener
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerSource
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberVideoPlayerController
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val ImmutableHolder<Media>.url: String
    get() = ImageUtil.getUrl(
        App.INSTANCE,
        true,
        get { originPic },
        get { bigPic },
        get { dynamicPic },
        get { srcPic }
    )

@Composable
private fun UserHeader(
    userProvider: () -> ImmutableHolder<User>,
    timeProvider: () -> Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val user = remember(userProvider) { userProvider() }
    val time = remember(timeProvider) { timeProvider() }
    UserHeader(
        avatar = {
            Avatar(
                data = user.get { StringUtil.getAvatarUrl(portrait) },
                size = Sizes.Small,
                contentDescription = stringResource(id = R.string.user_portrait)
            )
        },
        name = {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    context = LocalContext.current,
                    username = user.get { name },
                    nickname = user.get { nameShow },
                    color = LocalContentColor.current
                ),
                color = ExtendedTheme.colors.text
            )
        },
        onClick = onClick,
        desc = {
            Text(
                text = DateTimeUtils.getRelativeTimeString(
                    context,
                    time.toString()
                )
            )
        },
        content = content,
        modifier = modifier
    )
}

@Composable
fun UserHeader(
    nameProvider: () -> String,
    nameShowProvider: () -> String,
    portraitProvider: () -> String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    timeProvider: (() -> Int)? = null,
    content: @Composable RowScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val name = remember(nameProvider) { nameProvider() }
    val nameShow = remember(nameShowProvider) { nameShowProvider() }
    val portrait = remember(portraitProvider) { portraitProvider() }
    val time = remember(timeProvider) { timeProvider?.invoke() }
    UserHeader(
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(portrait),
                size = Sizes.Small,
                contentDescription = stringResource(id = R.string.user_portrait)
            )
        },
        name = {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    context = LocalContext.current,
                    username = name,
                    nickname = nameShow,
                    color = LocalContentColor.current
                ),
                color = ExtendedTheme.colors.text
            )
        },
        onClick = onClick,
        desc = (@Composable {
            Text(
                text = DateTimeUtils.getRelativeTimeString(
                    context,
                    time.toString()
                )
            )
        }).takeIf { time != null },
        content = content,
        modifier = modifier
    )
}


@Composable
fun Card(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
    action: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    val cardModifier =
        if (onClick != null) Modifier.debounceClickable(onClick = onClick) else Modifier

    val paddingModifier = if (action != null) Modifier.padding(top = 16.dp)
    else Modifier.padding(vertical = 16.dp)

    Column(
        modifier = cardModifier
            .then(modifier)
            // 卡片统一左右边距:上下由列表 spacedBy、左右由此处统一提供
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ExtendedTheme.colors.card)
            // hairline 描边:与背景拉开卡片边界(M3 tonal 语言,无阴影)
            .border(
                width = 0.5.dp,
                color = ExtendedTheme.colors.divider.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .then(paddingModifier)
            .padding(contentPadding)
    ) {
        header()
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            content()
        }
        action?.invoke(this)
    }
}

@Composable
fun Badge(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(100),
    backgroundColor: Color = Color.Black.copy(0.5f),
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(text = text, fontSize = 12.sp, color = contentColor)
    }
}

@Composable
fun ThreadContent(
    modifier: Modifier = Modifier,
    title: String = "",
    abstractText: String = "",
    tabName: String = "",
    showTitle: Boolean = true,
    showAbstract: Boolean = true,
    isGood: Boolean = false,
    maxLines: Int = 5,
    highlightKeywords: ImmutableList<String> = persistentListOf(),
) {
    val content = buildAnnotatedString {
        if (showTitle) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                if (isGood) {
                    withStyle(style = SpanStyle(color = ExtendedTheme.colors.accent)) {
                        append(stringResource(id = R.string.tip_good))
                    }
                    append(" ")
                }

                if (tabName.isNotBlank()) {
                    append(tabName)
                    append(" | ")
                }

                append(title)
            }
        }
        if (showTitle && showAbstract) {
            append('\n')
        }
        if (showAbstract) {
            append(abstractText.emoticonString)
        }
    }

    HighlightText(
        text = content,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        fontSize = 15.sp,
        lineSpacing = 0.8.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        style = MaterialTheme.typography.body1,
        highlightKeywords = highlightKeywords
    )
}

@Composable
fun FeedCardPlaceholder() {
    Card(
        header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
        content = {
            Text(
                text = "TitlePlaceholder",
                style = MaterialTheme.typography.subtitle1,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .placeholder(
                        visible = true,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                        highlight = PlaceholderHighlight.shimmer(),
                    )
            )

            Text(
                text = "Text",
                style = MaterialTheme.typography.body1,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(
                        visible = true,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                        highlight = PlaceholderHighlight.shimmer(),
                    )
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    ActionBtnPlaceholder(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
fun ForumInfoChip(
    imageUriProvider: () -> String?,
    nameProvider: () -> String,
    onClick: () -> Unit,
) {
    val imageUri = imageUriProvider()
    val name = nameProvider()
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(4.dp))
            .background(color = ExtendedTheme.colors.chip)
            .debounceClickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUri?.let {
            Avatar(
                data = imageUri,
                contentDescription = stringResource(id = R.string.forum_portrait),
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(4.dp)
            )
        }
        Text(
            text = stringResource(id = R.string.title_forum_name, name),
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.onChip,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MediaPlaceholder(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ExtendedTheme.colors.chip)
            .clickable(
                enabled = onClick != null
            ) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProvideContentColor(color = ExtendedTheme.colors.onChip) {
            Box(
                modifier = Modifier.size(16.dp),
            ) {
                icon()
            }
            ProvideTextStyle(
                value = MaterialTheme.typography.subtitle2,
                content = text
            )
        }
    }
}

@Composable
private fun ThreadMedia(
    forumId: Long,
    forumName: String,
    threadId: Long,
    modifier: Modifier = Modifier,
    medias: ImmutableList<ImmutableHolder<Media>> = persistentListOf(),
    videoInfo: ImmutableHolder<VideoInfo>? = null,
) {
    val context = LocalContext.current

    val mediaCount = remember(medias) {
        medias.size
    }
    val hasPhoto = remember(mediaCount) { mediaCount > 0 }
    val isSinglePhoto = remember(mediaCount) { mediaCount == 1 }

    val hideMedia = context.appPreferences.hideMedia

    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val singleMediaFraction = remember(windowWidthSizeClass) {
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact)
            1f
        else 0.5f
    }

    val hasMedia = remember(hasPhoto, videoInfo) {
        hasPhoto || videoInfo != null
    }

    if (hasMedia) {
        Box(modifier = modifier) {
            if (videoInfo != null) {
                if (hideMedia) {
                    MediaPlaceholder(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.OndemandVideo,
                                contentDescription = stringResource(id = R.string.desc_video)
                            )
                        },
                        text = {
                            Text(text = stringResource(id = R.string.desc_video))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val aspectRatio = remember(videoInfo) {
                        max(
                            videoInfo
                                .get { thumbnailWidth }
                                .toFloat() / videoInfo.get { thumbnailHeight },
                            16f / 9
                        )
                    }
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                    ) {
                        VideoPlayer(
                            videoUrl = videoInfo.get { videoUrl },
                            thumbnailUrl = videoInfo.get { thumbnailUrl },
                            modifier = Modifier
                                .fillMaxWidth(singleMediaFraction)
                                .aspectRatio(aspectRatio)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            } else if (hasPhoto) {
                val mediaWidthFraction = remember(isSinglePhoto, singleMediaFraction) {
                    if (isSinglePhoto) singleMediaFraction else 1f
                }
                val mediaAspectRatio = remember(isSinglePhoto) {
                    if (isSinglePhoto) 2f else 3f
                }
                if (hideMedia) {
                    val photoViewData = remember(
                        medias, forumId, forumName, threadId
                    ) {
                        getPhotoViewData(
                            medias = medias.map { it.get() },
                            forumId = forumId,
                            forumName = forumName,
                            threadId = threadId,
                            index = 0
                        )
                    }
                    MediaPlaceholder(
                        icon = {
                            Icon(
                                imageVector = if (isSinglePhoto) Icons.Rounded.Photo else Icons.Rounded.PhotoLibrary,
                                contentDescription = stringResource(id = R.string.desc_photo)
                            )
                        },
                        text = {
                            Text(text = stringResource(id = R.string.btn_open_photos, mediaCount))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            context.goToActivity<PhotoViewActivity> {
                                putExtra(
                                    PhotoViewActivity.EXTRA_PHOTO_VIEW_DATA,
                                    photoViewData
                                )
                            }
                        }
                    )
                } else {
                    val showMediaCount = remember(medias) { min(medias.size, 3) }
                    val hasMoreMedia = remember(medias) { medias.size > 3 }
                    val showMedias = remember(medias) { medias.subList(0, showMediaCount) }
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(mediaWidthFraction)
                                .aspectRatio(mediaAspectRatio)
                                .clip(RoundedCornerShape(8.dp)),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            showMedias.fastForEachIndexed { index, media ->
                                val photoViewData = remember(
                                    index, medias, forumId, forumName, threadId
                                ) {
                                    getPhotoViewData(
                                        medias = medias.map { it.get() },
                                        forumId = forumId,
                                        forumName = forumName,
                                        threadId = threadId,
                                        index = index
                                    )
                                }
                                NetworkImage(
                                    imageUri = remember(media) { media.url },
                                    contentDescription = stringResource(id = R.string.desc_photo),
                                    modifier = Modifier
                                        .focusable()
                                        .fillMaxHeight()
                                        .weight(1f),
                                    photoViewData = photoViewData,
                                    contentScale = ContentScale.Crop,
                                    enablePreview = true
                                )
                            }
                        }
                        if (hasMoreMedia) {
                            Badge(
                                icon = Icons.Rounded.PhotoSizeSelectActual,
                                text = "${medias.size}",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadMedia(
    item: ImmutableHolder<ThreadInfo>,
    modifier: Modifier = Modifier,
) {
    ThreadMedia(
        forumId = item.get { forumId },
        forumName = item.get { forumName },
        threadId = item.get { threadId },
        medias = item.getImmutableList { media },
        videoInfo = item.get { videoInfo }?.wrapImmutable(),
        modifier = modifier,
    )
}

@Composable
fun OriginThreadCard(
    originThreadInfo: ImmutableHolder<OriginThreadInfo>,
    modifier: Modifier = Modifier,
) {
    val contentRenders = remember(originThreadInfo) { originThreadInfo.get { content.renders } }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            contentRenders.fastForEach {
                it.Render()
            }
        }
        ThreadMedia(
            forumId = originThreadInfo.get { fid },
            forumName = originThreadInfo.get { fname },
            threadId = originThreadInfo.get { tid.toLong() },
            medias = originThreadInfo.getImmutableList { media },
            videoInfo = originThreadInfo.get { video_info }?.wrapImmutable()
        )
    }
}

@Composable
private fun ThreadForumInfo(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (SimpleForum) -> Unit,
) {
    val hasForumInfo = remember(item) { item.isNotNull { forumInfo } }
    if (hasForumInfo) {
        val forumInfo = remember(item) { item.getImmutable { forumInfo!! } }
        ThreadForumInfo(
            forumName = forumInfo.get { name },
            forumAvatar = forumInfo.get { avatar },
            onClick = { onClick(forumInfo.get()) }
        )
    }
}

@Composable
private fun ThreadForumInfo(
    forumName: String,
    forumAvatar: String?,
    onClick: () -> Unit,
) {
    val hasForum = remember(forumName) { forumName.isNotBlank() }
    if (hasForum) {
        ForumInfoChip(
            imageUriProvider = { forumAvatar },
            nameProvider = { forumName },
            onClick = onClick
        )
    }
}

@Composable
fun ThreadReplyBtn(
    replyNum: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionBtn(
        icon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
                contentDescription = stringResource(id = R.string.desc_comment),
            )
        },
        text = {
            Text(
                text = if (replyNum == "0" || replyNum.isEmpty())
                    stringResource(id = R.string.title_reply)
                else replyNum.toLongOrNull()?.getShortNumString() ?: replyNum
            )
        },
        modifier = modifier,
        onClick = onClick,
        color = ExtendedTheme.colors.textSecondary,
    )
}

/**
 * 可复用的动画心形:交叉淡化空心/实心 + 按压缩放 + 点赞瞬间"心跳式"双脉冲过冲 + 旋转摆动 + 光环扩散 + 粒子爆裂。
 * interactionSource 用于感知按压(与外层 clickable 共用);不传则无按压效果。
 */
@Composable
fun AgreeHeart(
    hasAgree: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    interactionSource: MutableInteractionSource? = null,
) {
    val pressed = if (interactionSource != null) {
        interactionSource.collectIsPressedAsState()
    } else {
        remember { mutableStateOf(false) }
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed.value) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "agreePressScale"
    )
    var burstKey by remember { mutableIntStateOf(0) }
    val wasAgree = remember { mutableStateOf(hasAgree) }
    LaunchedEffect(hasAgree) {
        if (hasAgree && !wasAgree.value) burstKey++
        wasAgree.value = hasAgree
    }
    // 心跳式双脉冲:撑大 → 收缩 → 回弹落定,比单次过冲更有"怦然"感
    val likeScale = remember { Animatable(1f) }
    val likeRotation = remember { Animatable(0f) }
    LaunchedEffect(burstKey) {
        if (burstKey > 0) {
            likeScale.snapTo(1f)
            likeRotation.snapTo(0f)
            likeScale.animateTo(1.25f, tween(durationMillis = 90, easing = EaseOutCubic))
            likeRotation.animateTo(-12f, tween(durationMillis = 90, easing = EaseOutCubic))
            likeScale.animateTo(0.85f, tween(durationMillis = 90, easing = EaseOutCubic))
            likeRotation.animateTo(9f, spring(dampingRatio = 0.55f, stiffness = 700f))
            likeScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 700f))
            likeRotation.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 700f))
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(iconSize * 1.2f)
            .graphicsLayer {
                scaleX = pressScale * likeScale.value
                scaleY = pressScale * likeScale.value
                rotationZ = likeRotation.value
            }
    ) {
        Crossfade(
            targetState = hasAgree,
            animationSpec = tween(durationMillis = 150),
            label = "agreeIcon"
        ) { agreed ->
            Icon(
                imageVector = if (agreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.desc_like),
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
        AgreeBurst(
            triggerKey = burstKey,
            modifier = Modifier.size(iconSize * 3.2f)
        )
    }
}

/**
 * 计数数字上滚/下滚:数值增大时旧值上移淡出、新值自下而上进入;减小时方向镜像。
 */
@Composable
fun RollingCount(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.caption,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val increasing = targetState.toLongOrNull()?.let { new ->
                initialState.toLongOrNull()?.let { new > it }
            } != false
            val slide = slideInVertically(tween(180, easing = EaseOutCubic)) {
                if (increasing) it else -it
            } + fadeIn(tween(180))
            val outSlide = slideOutVertically(tween(180, easing = EaseOutCubic)) {
                if (increasing) -it else it
            } + fadeOut(tween(180))
            slide togetherWith outSlide
        },
        label = "rollingCount",
        modifier = modifier
    ) { value ->
        Text(text = value, style = style, color = color)
    }
}

@Composable
fun ThreadAgreeBtn(
    hasAgree: Boolean,
    agreeNum: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val contentColor =
        if (hasAgree) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary
    val animatedColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "agreeBtnContentColor"
    )
    val interactionSource = remember { MutableInteractionSource() }

    val numText = if (agreeNum == "0" || agreeNum.isEmpty())
        stringResource(id = R.string.title_agree)
    else agreeNum.toLongOrNull()?.getShortNumString() ?: agreeNum

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .debounceClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (!hasAgree) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
            .padding(vertical = 16.dp, horizontal = 4.dp)
    ) {
        AgreeHeart(
            hasAgree = hasAgree,
            tint = animatedColor,
            interactionSource = interactionSource
        )
        Spacer(modifier = Modifier.width(8.dp))
        RollingCount(text = numText, color = animatedColor)
    }
}

/**
 * 点赞爆发:中心闪光 + 光环扩散 + 12 粒变速粒子放射(420ms),仅在 triggerKey 递增时播放一次。
 * 粒子按索引确定性抖动(角度/距离/大小各异),视觉上更"炸"而非均匀圆点。
 */
@Composable
private fun AgreeBurst(
    triggerKey: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 420, easing = FastOutLinearInEasing))
        }
    }
    if (progress.value >= 1f) return
    val primaryColor = ExtendedTheme.colors.primary
    val colors = listOf(
        primaryColor,
        ExtendedTheme.colors.accent,
        MaterialTheme.colors.secondary,
        primaryColor,
        primaryColor,
        MaterialTheme.colors.secondary,
        ExtendedTheme.colors.accent,
        primaryColor,
        ExtendedTheme.colors.accent,
        primaryColor,
        MaterialTheme.colors.secondary,
        primaryColor,
    )
    Canvas(modifier = modifier) {
        val p = progress.value
        val maxRadius = size.minDimension / 2f

        // 中心闪光:前 40% 进度内一个实心光斑快速放大淡出,撑出"爆点"
        if (p < 0.4f) {
            val flashP = p / 0.4f
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f * (1f - flashP)),
                radius = maxRadius * (0.25f + 0.35f * flashP),
                center = center
            )
        }

        // 光环扩散:细描边圆环向外扩张并淡出
        drawCircle(
            color = primaryColor.copy(alpha = 0.55f * (1f - p)),
            radius = maxRadius * (0.3f + 0.7f * p),
            center = center,
            style = Stroke(width = 2.dp.toPx() * (1f - p))
        )

        // 12 粒粒子:索引驱动确定性抖动,双速档(偶数快奇数慢)拉开层次
        repeat(12) { i ->
            val angle = (i / 12f * 2f * PI.toFloat()) - (PI / 2f).toFloat() +
                    0.22f * (if (i % 2 == 0) 1f else -1f)
            val speedFactor = if (i % 2 == 0) 1f else 0.72f
            val radius = maxRadius * (0.3f + 0.7f * p) * speedFactor
            val dotRadius = (if (i % 3 == 0) 2.6f else 1.8f).dp.toPx() * (1f - 0.45f * p)
            drawCircle(
                color = colors[i].copy(alpha = (1f - p).coerceIn(0f, 1f)),
                radius = dotRadius,
                center = Offset(
                    center.x + cos(angle) * radius,
                    center.y + sin(angle) * radius
                )
            )
        }
    }
}

@Composable
fun ThreadShareBtn(
    shareNum: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionBtn(
        icon = {
            Icon(
                imageVector = Icons.Rounded.SwapCalls,
                contentDescription = stringResource(id = R.string.desc_share),
            )
        },
        text = {
            Text(
                text = if (shareNum == "0" || shareNum.isEmpty())
                    stringResource(id = R.string.title_share)
                else shareNum.toLongOrNull()?.getShortNumString() ?: shareNum
            )
        },
        modifier = modifier,
        onClick = onClick,
        color = ExtendedTheme.colors.textSecondary,
    )
}

@Composable
@JvmName("FeedCardForThreadInfo")
fun FeedCard(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadInfo) -> Unit = {},
    onClickUser: (User) -> Unit = {},
    onClickForum: (SimpleForum) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
    dislikeAction: @Composable () -> Unit = {},
) {
    Card(
        header = {
            val author = remember(item) { item.getNullableImmutable { author } }
            author?.let {
                UserHeader(
                    userProvider = { it },
                    timeProvider = { item.get { lastTimeInt } },
                    onClick = {
                        onClickUser(it.get())
                    },
                ) { dislikeAction() }
            }
        },
        content = {
            ThreadContent(
                title = item.get { title },
                abstractText = item.get { abstractText },
                tabName = item.get { tabName },
                showTitle = item.get { isNoTitle != 1 && title.isNotBlank() },
                showAbstract = item.get { abstractText.isNotBlank() },
                isGood = item.get { isGood == 1 },
            )

            ThreadMedia(
                item = item,
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { item.get { is_share_thread } == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .debounceClickable(onClick = {
                                onClickOriginThread(it.get())
                            })
                            .padding(16.dp)
                    )
                }

            ThreadForumInfo(item = item, onClick = onClickForum)
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { shareNum }.toString(),
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { replyNum }.toString(),
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { agree?.hasAgree == 1 },
                    agreeNum = item.get { agreeNum }.toString(),
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
        modifier = modifier,
    )
}


@Composable
@JvmName("FeedCardForThreadBean")
fun FeedCard(
    item: ImmutableHolder<ThreadBean>,
    onClick: (ThreadBean) -> Unit,
    onAgree: (ThreadBean) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadBean) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
) {
    Card(
        header = {
            UserHeader(
                nameProvider = { item.get { threadInfo.author.name }.orEmpty() },
                nameShowProvider = { item.get { threadInfo.author.showNickName } },
                portraitProvider = { item.get { threadInfo.author.portrait } },
                timeProvider = { item.get { threadInfo.createTime.toInt() } },
                onClick = {
                    onClickUser(item.get { threadInfo.userId })
                },
            )
        },
        content = {
            ThreadContent(
                title = item.get { threadInfo.title!! },
                abstractText = item.get { threadInfo.abstractText },
                showTitle = item.get { threadInfo.title?.isNotBlank() == true },
                showAbstract = item.get { threadInfo.abstractText.isNotBlank() },
            )

            ThreadMedia(
                forumId = item.get { threadInfo.forumId },
                forumName = item.get { threadInfo.forumName },
                threadId = item.get { threadInfo.threadId },
            )
            ThreadForumInfo(
                forumName = item.get { threadInfo.forumName },
                forumAvatar = item.get { threadInfo.avatar },
                onClick = { onClickForum(item.get { threadInfo.forumName }) }
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { threadInfo.shareNum }.toString(),
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { threadInfo.replyNum }.toString(),
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { threadInfo.agree.hasAgree == 1 },
                    agreeNum = item.get { threadInfo.agreeNum }.toString(),
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
        modifier = modifier,
    )
}

@Composable
fun FeedCard(
    item: ImmutableHolder<PostInfoList>,
    onClick: (PostInfoList) -> Unit,
    onAgree: (PostInfoList) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
) {
    Card(
        header = {
            UserHeader(
                nameProvider = { item.get { user_name } },
                nameShowProvider = { item.get { name_show } },
                portraitProvider = { item.get { user_portrait } },
                timeProvider = { item.get { create_time } },
                onClick = {
                    onClickUser(item.get { user_id })
                },
            )
        },
        content = {
            ThreadContent(
                title = item.get { title },
                abstractText = item.get { abstractText },
                showTitle = item.get { is_ntitle != 1 && title.isNotBlank() },
                showAbstract = item.get { abstractText.isNotBlank() },
            )

            ThreadMedia(
                forumId = item.get { forum_id },
                forumName = item.get { forum_name },
                threadId = item.get { thread_id },
                medias = item.getImmutableList { media },
                videoInfo = item.getNullableImmutable { video_info }
            )

            item.getNullableImmutable { origin_thread_info }
                .takeIf { item.get { is_share_thread } == 1 }?.let {
                    OriginThreadCard(
                        originThreadInfo = it,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .debounceClickable(onClick = {
                                onClickOriginThread(it.get())
                            })
                            .padding(16.dp)
                    )
                }

            ThreadForumInfo(
                forumName = item.get { forum_name },
                forumAvatar = null,
                onClick = { onClickForum(item.get { forum_name }) }
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = item.get { share_num }.toString(),
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadReplyBtn(
                    replyNum = item.get { reply_num }.toString(),
                    onClick = { onClickReply(item.get()) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = item.get { agree?.hasAgree == 1 },
                    agreeNum = item.get { agree_num }.toString(),
                    onClick = { onAgree(item.get()) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item.get()) },
        modifier = modifier,
    )
}

@Composable
private fun ActionBtnPlaceholder(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Button",
            style = MaterialTheme.typography.caption,
            modifier = Modifier
                .placeholder(
                    visible = true,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                    highlight = PlaceholderHighlight.shimmer(),
                ),
        )
    }
}

@Composable
private fun ActionBtn(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier =
        if (onClick != null) Modifier.debounceClickable(onClick = onClick) else Modifier
    Row(
        modifier = clickableModifier
            .padding(vertical = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ProvideContentColor(color = color) {
            Box(modifier = Modifier.size(18.dp)) {
                icon()
            }
            Spacer(modifier = Modifier.width(8.dp))
            ProvideTextStyle(value = MaterialTheme.typography.caption) {
                text()
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    thumbnailUrl: String,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    val context = LocalContext.current
    val systemUIBarsTweaker = rememberSystemUIBarsTweaker()
    val videoPlayerController = rememberVideoPlayerController(
        source = VideoPlayerSource.Network(videoUrl),
        thumbnailUrl = thumbnailUrl,
        fullScreenModeChangedListener = object : OnFullScreenModeChangedListener {
            override fun onFullScreenModeChanged(isFullScreen: Boolean) {
                Log.i("VideoPlayer", "onFullScreenModeChanged $isFullScreen")
                systemUIBarsTweaker.tweakStatusBarVisibility(!isFullScreen)
                systemUIBarsTweaker.tweakNavigationBarVisibility(!isFullScreen)
                if (isFullScreen) {
                    context.findActivity()?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    context.findActivity()?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    )
    val fullScreen by (videoPlayerController as DefaultVideoPlayerController).collect { isFullScreen }
    val videoPlayerContent =
        movableContentOf { isFullScreen: Boolean, playerModifier: Modifier ->
            com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayer(
                videoPlayerController = videoPlayerController,
                modifier = playerModifier,
                backgroundColor = if (isFullScreen) Color.Black else Color.Transparent
            )
        }

    if (fullScreen) {
        Spacer(
            modifier = modifier
        )
        FullScreen {
            DisposableEffect(
                videoPlayerContent(
                    true,
                    Modifier.fillMaxSize()
                )
            ) {
                onDispose {
                    videoPlayerController.release()
                }
            }
        }
    } else {
        DisposableEffect(
            videoPlayerContent(
                false,
                modifier
            )
        ) {
            onDispose {
                videoPlayerController.release()
            }
        }
    }
}

@Preview("FeedCardPreview")
@Composable
fun FeedCardPreview() {
    FeedCard(
        item = wrapImmutable(
            ThreadInfo(
                title = "预览",
                author = User(),
                lastTimeInt = (System.currentTimeMillis() / 1000).toInt()
            )
        ),
        onClick = {},
        onAgree = {},
        modifier = Modifier.background(ExtendedTheme.colors.card)
    )
}