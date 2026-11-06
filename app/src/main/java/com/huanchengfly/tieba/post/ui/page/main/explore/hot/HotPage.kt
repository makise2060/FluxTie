package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eygraber.compose.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomInsets
import com.huanchengfly.tieba.post.ui.common.theme.compose.OrangeA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.RedA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.common.theme.compose.Yellow
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HotTopicListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.TopicDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyState
import com.huanchengfly.tieba.post.ui.widgets.compose.RankBadge
import com.huanchengfly.tieba.post.ui.widgets.compose.ScrollableTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TopicHorizontalCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.feedCardListSpacing
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.items
import com.huanchengfly.tieba.post.ui.widgets.compose.itemsIndexed
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun HotPage(
    viewModel: HotViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(HotUiIntent.Load)
        viewModel.initialized = true
    }
    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "hot" }
    ) {
        viewModel.send(HotUiIntent.Load)
    }
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::isRefreshing,
        initial = false
    )
    val topicList by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::topicList,
        initial = emptyList()
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::threadList,
        initial = emptyList()
    )
    val tabList by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::tabList,
        initial = emptyList()
    )
    val currentTabCode by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::currentTabCode,
        initial = "all"
    )
    val topicImages by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::topicImages,
        initial = emptyMap()
    )
    val isLoadingThreadList by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::isLoadingThreadList,
        initial = false
    )
    // 热榜接口的 topic_pic 为空：对整条话题榜逐个补详情图（只触发一次，结果由 ViewModel 持有并逐条追加）
    LaunchedEffect(topicList) {
        val missing = topicList
            .filter {
                it.get { topicPic }.isBlank() &&
                    topicImages[it.get { topicId }.toString()].isNullOrBlank()
            }
            .associate { it.get { topicId } to it.get { topicName } }
        if (missing.isNotEmpty()) {
            viewModel.send(HotUiIntent.LoadTopicImages(missing))
        }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.send(HotUiIntent.Load) })
    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        if (!isLoading && topicList.isEmpty() && threadList.isEmpty()) {
            // 空榜单/失败兜底：无缓存可展示时给出统一空态，重试走下拉刷新链路
            EmptyState(
                title = stringResource(id = R.string.title_topic_list_failed),
                subtitle = stringResource(id = R.string.tip_load_failed_retry),
                illustration = EmptyIllustration.Box,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalMainBottomInsets.current)
            )
        } else {
            MyLazyColumn(
            verticalArrangement = Arrangement.spacedBy(feedCardListSpacing()),
            contentPadding = PaddingValues(top = 4.dp, bottom = LocalMainBottomInsets.current),
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            if (topicList.isNotEmpty()) {
                item(key = "TopicHeader") {
                    Container {
                        // 标题与「展开话题榜」入口合并为一行（原底部「查看完整榜单」整条入口移除）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .padding(horizontal = 16.dp)
                                .debounceClickable(
                                    onClick = { navigator.navigate(HotTopicListPageDestination) }
                                )
                        ) {
                            ChipHeader(text = stringResource(id = R.string.hot_topic_rank))
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.title_topic_rank_expand),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ExtendedTheme.colors.textSecondary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = ExtendedTheme.colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                item(key = "TopicList") {
                    Container {
                        val topicListState = rememberLazyListState()
                        val colors = ExtendedTheme.colors
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawWithContent {
                                    drawContent()
                                    // 右缘渐隐：暗示榜单还有更多内容（滚到末尾自动消失）
                                    if (topicListState.canScrollForward) {
                                        val fadeWidth = 36.dp.toPx()
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, colors.background),
                                                startX = size.width - fadeWidth,
                                                endX = size.width
                                            ),
                                            topLeft = Offset(size.width - fadeWidth, 0f),
                                            size = Size(fadeWidth, size.height)
                                        )
                                    }
                                }
                        ) {
                            LazyRow(
                                state = topicListState,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = topicList,
                                    key = { _, item -> "TopicCard_${item.get { topicId }}" }
                                ) { index, item ->
                                    TopicHorizontalCard(
                                        rank = index + 1,
                                        title = item.get { topicName },
                                        imageUrl = topicImages[item.get { topicId }.toString()]
                                            .orEmpty()
                                            .ifBlank { item.get { topicPic } },
                                        discussNum = item.get { discussNum },
                                        tag = item.get { tag },
                                        onClick = {
                                            navigator.navigate(
                                                TopicDetailPageDestination(
                                                    item.get { topicId },
                                                    item.get { topicName },
                                                    rank = index + 1,
                                                    tag = item.get { tag }
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                            // 滑动引导：未滑到末尾时在右缘悬浮一枚圆形箭头，滚到底自动隐藏
                            val canScrollForward by remember {
                                derivedStateOf { topicListState.canScrollForward }
                            }
                            AnimatedVisibility(
                                visible = canScrollForward && topicList.size > 3,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(ExtendedTheme.colors.chip),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = ExtendedTheme.colors.onChip,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (threadList.isNotEmpty()) {
                if (tabList.isNotEmpty()) {
                    item(key = "ThreadTabs") {
                        Container {
                            // 分类筛选与全站 TabRow 同源，替换原自绘胶囊行
                            val tabCodes = buildList {
                                add("all")
                                tabList.forEach { add(it.get { tabCode }) }
                            }
                            val selectedTabIndex = tabCodes.indexOf(currentTabCode).coerceAtLeast(0)
                            ScrollableTabRow(
                                selectedTabIndex = selectedTabIndex,
                                backgroundColor = Color.Transparent,
                                contentColor = ExtendedTheme.colors.text,
                                edgePadding = 16.dp,
                                divider = {}
                            ) {
                                androidx.compose.material.Tab(
                                    selected = currentTabCode == "all",
                                    onClick = { viewModel.send(HotUiIntent.RefreshThreadList("all")) },
                                    text = {
                                        Text(
                                            text = stringResource(id = R.string.tab_all_hot_thread),
                                            style = MaterialTheme.typography.body2,
                                            color = if (currentTabCode == "all") MaterialTheme.colors.primary
                                            else ExtendedTheme.colors.textSecondary
                                        )
                                    }
                                )
                                tabList.forEach { tab ->
                                    val selected = currentTabCode == tab.get { tabCode }
                                    androidx.compose.material.Tab(
                                        selected = selected,
                                        onClick = {
                                            viewModel.send(
                                                HotUiIntent.RefreshThreadList(tab.get { tabCode })
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = tab.get { tabName },
                                                style = MaterialTheme.typography.body2,
                                                color = if (selected) MaterialTheme.colors.primary
                                                else ExtendedTheme.colors.textSecondary
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "ThreadListTip") {
                    Container(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.hot_thread_rank_rule),
                            color = ExtendedTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                if (isLoadingThreadList) {
                    items(10) { ThreadListItemPlaceholder() }
                } else {
                    itemsIndexed(
                        items = threadList,
                        key = { _, item -> "Thread_${item.get { threadId }}" }
                    ) { index, item ->
                        Container {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                FeedCard(
                                    item = item,
                                    onClick = {
                                        navigator.navigate(
                                            ThreadPageDestination(
                                                threadId = it.id,
                                                threadInfo = it
                                            )
                                        )
                                    },
                                    onClickReply = {
                                        navigator.navigate(
                                            ThreadPageDestination(
                                                threadId = it.id,
                                                scrollToReply = true
                                            )
                                        )
                                    },
                                    onAgree = {
                                        viewModel.send(
                                            HotUiIntent.Agree(
                                                threadId = it.threadId,
                                                postId = it.firstPostId,
                                                hasAgree = it.hasAgree
                                            )
                                        )
                                    },
                                    onClickForum = { navigator.navigate(ForumPageDestination(it.name)) },
                                    onClickUser = { navigator.navigate(UserProfilePageDestination(it.id)) },
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RankBadge(rank = index + 1)
                                        Text(
                                            text = stringResource(
                                                id = R.string.hot_num,
                                                item.get { hotNum }.getShortNumString()
                                            ),
                                            style = MaterialTheme.typography.caption,
                                            color = ExtendedTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
//                        ThreadListItem(
//                            index = index,
//                            itemHolder = item,
//                            onClick = {
//                                navigator.navigate(
//                                    ThreadPageDestination(
//                                        threadId = it.id,
//                                        threadInfo = it
//                                    )
//                                )
//                            }
//                        )
                    }
                }
            }
        }

        }

        PullRefreshLogoIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.onPrimary,
        )
    }
}

@Composable
private fun ThreadListItemPlaceholder() {
    Row(modifier = Modifier.padding(all = 16.dp)) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "1",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = ExtendedTheme.colors.background,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .wrapContentSize()
                    .placeholder(visible = true, color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                    .padding(vertical = 1.dp, horizontal = 4.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(visible = true, color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                )
                Text(
                    text = stringResource(id = R.string.hot_num, "666"),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                )
            }
        }
    }
}
@Composable
private fun ChipHeader(
    text: String,
    invert: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        color = if (invert) MaterialTheme.colors.onSecondary else ExtendedTheme.colors.onChip,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .then(modifier)
            .background(color = if (invert) MaterialTheme.colors.secondary else ExtendedTheme.colors.chip)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}