package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.CommonUiEvent.ScrollToTop.bindScrollToTopEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.loadMoreIndicator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Whatshot
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.RankBadge
import com.huanchengfly.tieba.post.ui.widgets.compose.TopicTagChip
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.huanchengfly.tieba.post.api.models.RelateForumBean
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumInfoChip
import com.huanchengfly.tieba.post.ui.widgets.compose.feedCardListSpacing


@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun TopicDetailPage(
    topicId: Long,
    topicName: String,
    navigator: DestinationsNavigator,
    rank: Int = 0,
    tag: Int = 0,
    viewModel: TopicDetailViewModel = pageViewModel()
) {
    val pageSize = 10
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(TopicDetailUiIntent.Refresh(topicId, topicName, pageSize))
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::isRefreshing,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::isError,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::isLoadingMore,
        initial = false
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::hasMore,
        initial = true
    )
    val topicInfo by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::topicInfo,
        initial = null
    )
    val relateForum by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::relateForum,
        initial = persistentListOf()
    )
    val relateThread by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::relateThread,
        initial = persistentListOf()
    )
    val scaffoldState = rememberScaffoldState()
    val lazyListState = rememberLazyListState()
    viewModel.bindScrollToTopEvent(lazyListState = lazyListState)
    val density = LocalDensity.current
    var heightOffset by rememberSaveable { mutableFloatStateOf(0f) }
    var headerHeight by rememberSaveable {
        mutableFloatStateOf(
            with(density) {
                300.dp.toPx()
            }
        )
    }
    val isShowTopBarArea by remember {
        derivedStateOf {
            heightOffset.absoluteValue < headerHeight
        }
    }

    val loadMoreEnd by remember {
        derivedStateOf {
            !hasMore
        }
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = topicInfo == null,
            isLoading = isRefreshing,
            isError = isError,
            onReload = {
                viewModel.send(
                    TopicDetailUiIntent.Refresh(
                        topicId,
                        topicName,
                        pageSize
                    )
                )
            }
        ) {
            MyScaffold(
                scaffoldState = scaffoldState,
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopicToolbar(
                        topicInfo = topicInfo,
                        topicName = topicName,
                        fallbackRank = rank,
                        showTitle = !isShowTopBarArea,
                        topicId = topicId
                    )
                }
            ) { contentPadding ->
                var isFakeLoading by remember { mutableStateOf(false) }
                LaunchedEffect(isFakeLoading) {
                    if (isFakeLoading) {
                        delay(1000)
                        isFakeLoading = false
                    }
                }

                PullToRefreshLayout(
                    refreshing = isFakeLoading,
                    onRefresh = {
                        viewModel.send(
                            TopicDetailUiIntent.Refresh(
                                topicId,
                                topicName,
                                pageSize
                            )
                        )
                        isFakeLoading = true
                    }
                ) {
                    val headerNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y < 0) {
                                    val prevHeightOffset = heightOffset
                                    heightOffset = max(heightOffset + available.y, -headerHeight)
                                    if (prevHeightOffset != heightOffset) {
                                        return available.copy(x = 0f)
                                    }
                                }

                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y > 0f) {
                                    // Adjust the height offset in case the consumed delta Y is less than what was
                                    // recorded as available delta Y in the pre-scroll.
                                    val prevHeightOffset = heightOffset
                                    heightOffset = min(heightOffset + available.y, 0f)
                                    if (prevHeightOffset != heightOffset) {
                                        return available.copy(x = 0f)
                                    }
                                }

                                return Offset.Zero
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .nestedScroll(headerNestedScrollConnection)
                    ) {
                        Column {
                            val containerHeight by remember {
                                derivedStateOf {
                                    with(density) {
                                        (headerHeight + heightOffset).toDp()
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(containerHeight)
                                    .clipToBounds()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentHeight(
                                            align = Alignment.Bottom,
                                            unbounded = true
                                        )
                                        .onSizeChanged {
                                            headerHeight = it.height.toFloat()
                                        }
                                ) {
                                    topicInfo?.let {
                                        TopicHeader(
                                            topicInfo = it,
                                            relateForums = relateForum,
                                            fallbackRank = rank,
                                            tag = tag,
                                            onForumClick = { name ->
                                                navigator.navigate(ForumPageDestination(name))
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            LoadMoreLayout(
                                isLoading = isLoadingMore,
                                onLoadMore = {
                                    if (hasMore) viewModel.send(
                                        TopicDetailUiIntent.LoadMore(
                                            topicId,
                                            topicName,
                                            currentPage + 1,
                                            pageSize,
                                            relateThread.last().feedId
                                        )
                                    )
                                },
                                indicator = { isLoading, loadMoreEnd, willLoad ->
                                    TopicLoadMoreIndicator(
                                        isLoading,
                                        !hasMore,
                                        willLoad,
                                        hasMore
                                    )
                                },
                                lazyListState = lazyListState,
                                loadEnd = loadMoreEnd,
                            ) {
                                MyLazyColumn(
                                    state = lazyListState,
                                    verticalArrangement = Arrangement.spacedBy(feedCardListSpacing()),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    itemsIndexed(
                                        items = relateThread,
                                        key = { _, item -> "${item.feedId}" },
                                    ) { _, item ->
                                        Container {
                                            FeedCard(
                                                    item = wrapImmutable(item),
                                                    onClick = {
                                                        navigator.navigate(
                                                            ThreadPageDestination(
                                                                item.threadInfo.threadId,
                                                                item.threadInfo.forumId
                                                            )
                                                        )
                                                    },
                                                    onClickReply = {
                                                        navigator.navigate(
                                                            ThreadPageDestination(
                                                                item.threadInfo.threadId,
                                                                item.threadInfo.forumId,
                                                                scrollToReply = true
                                                            )
                                                        )
                                                    },
                                                    onAgree = {
                                                        viewModel.send(
                                                            TopicDetailUiIntent.Agree(
                                                                item.threadInfo.threadId,
                                                                item.threadInfo.forumId,
                                                                item.threadInfo.userAgree
                                                            )
                                                        )
                                                    },
                                                    onClickForum = {
                                                        navigator.navigate(
                                                            ForumPageDestination(item.threadInfo.forumName)
                                                        )
                                                    },
                                                    onClickUser = {
                                                        navigator.navigate(
                                                            UserProfilePageDestination(item.threadInfo.userId)
                                                        )
                                                    },
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicToolbar(
    topicInfo: TopicInfoBean?,
    topicName: String,
    fallbackRank: Int,
    showTitle: Boolean,
    topicId: Long? = null,
) {
    val navigator = LocalNavigator.current
    Toolbar(
        title = {
            if (showTitle) {
                // 折叠后的粘性条：话题名 + 名次（热度留给 Hero，避免顶栏拥挤）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 详情接口的 idx_num 对非榜首可能为 0：回退用列表传入的名次
                    val displayRank = topicInfo?.idxNum?.takeIf { it > 0 } ?: fallbackRank
                    if (displayRank > 0) {
                        RankBadge(rank = displayRank)
                    }
                    Text(
                        text = stringResource(
                            id = R.string.title_topic,
                            topicName
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            BackNavigationIcon(onBackPressed = {
                val navigateUp =
                    navigator.navigateUp()
            })
        }
    )
}

@Composable
private fun TopicLoadMoreIndicator(
    isLoading: Boolean,
    loadMoreEnd: Boolean,
    willLoad: Boolean,
    hasMore: Boolean,
) {
    Surface(
        elevation = 8.dp,
        shape = RoundedCornerShape(100),
        color = ExtendedTheme.colors.loadMoreIndicator,
        contentColor = ExtendedTheme.colors.text
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(10.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2.copy(fontSize = 13.sp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = ExtendedTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.text_loading),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    loadMoreEnd -> {
                        Text(
                            text = stringResource(id = R.string.no_more),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    hasMore -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load) else stringResource(
                                id = R.string.pull_to_load
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load_latest_posts) else stringResource(
                                id = R.string.pull_to_load_latest_posts
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicHeader(
    topicInfo: TopicInfoBean,
    relateForums: List<RelateForumBean>,
    fallbackRank: Int,
    tag: Int,
    onForumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExtendedTheme.colors
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(
                width = 0.5.dp,
                color = colors.divider.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // Hero：话题图 16:9 打底（无图降级为容器色渐变），底部渐变遮罩上叠名次/名称/热度
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            if (topicInfo.topicImage.isNotBlank()) {
                NetworkImage(
                    imageUri = topicInfo.topicImage,
                    contentDescription = topicInfo.topicName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    colors.primaryContainer.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified } ?: colors.chip,
                                    colors.secondaryContainer.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified } ?: colors.chip
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                            startY = 90f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val displayRank = topicInfo.idxNum.takeIf { it > 0 } ?: fallbackRank
                    if (displayRank > 0) {
                        RankBadge(rank = displayRank)
                    }
                    TopicTagChip(tag = tag)
                }
                // 话题全称不截断：榜内话题名往往较长，截断后无法辨认具体话题
                Text(
                    text = topicInfo.topicName,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(
                            id = R.string.hot_num,
                            topicInfo.discussNum.getShortNumString()
                        ),
                        style = MaterialTheme.typography.caption,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
        // 图下：完整简介 + 相关吧入口
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (topicInfo.topicDesc.isNotBlank()) {
                Text(
                    text = topicInfo.topicDesc,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary,
                )
            }
            if (relateForums.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    relateForums.forEach { forum ->
                        ForumInfoChip(
                            imageUriProvider = { forum.avatar },
                            nameProvider = { forum.forumName },
                            onClick = { onForumClick(forum.forumName) }
                        )
                    }
                }
            }
        }
    }
}
