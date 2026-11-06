package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.frsPage.Classify
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumRuleDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.forum.getSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.feedCardListSpacing
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private fun getFirstLoadIntent(
    context: Context,
    forumName: String,
    type: ForumThreadListType = ForumThreadListType.Latest,
): ForumThreadListUiIntent {
    return when (type) {
        ForumThreadListType.Good -> ForumThreadListUiIntent.Refresh(forumName, -1, 0)
        ForumThreadListType.Hot -> ForumThreadListUiIntent.FirstLoad(forumName, HOT_SORT_TYPE, null)
        ForumThreadListType.Latest -> ForumThreadListUiIntent.FirstLoad(forumName, getSortType(context, forumName), null)
    }
}

private fun getRefreshIntent(
    context: Context,
    forumName: String,
    type: ForumThreadListType = ForumThreadListType.Latest,
    sortType: Int = getSortType(context, forumName),
    goodClassifyId: Int? = if (type == ForumThreadListType.Good) 0 else null,
): ForumThreadListUiIntent {
    return when (type) {
        ForumThreadListType.Good -> ForumThreadListUiIntent.Refresh(forumName, -1, goodClassifyId)
        ForumThreadListType.Hot -> ForumThreadListUiIntent.Refresh(forumName, HOT_SORT_TYPE, null)
        ForumThreadListType.Latest -> ForumThreadListUiIntent.Refresh(forumName, sortType, null)
    }
}

private fun getLoadMoreIntent(
    context: Context,
    forumId: Long,
    forumName: String,
    page: Int,
    threadListIds: List<Long>,
    type: ForumThreadListType = ForumThreadListType.Latest,
): ForumThreadListUiIntent {
    return when (type) {
        ForumThreadListType.Good -> ForumThreadListUiIntent.LoadMore(forumId, forumName, page, threadListIds, 0)
        ForumThreadListType.Hot -> ForumThreadListUiIntent.LoadMore(forumId, forumName, page, threadListIds, HOT_SORT_TYPE)
        ForumThreadListType.Latest -> ForumThreadListUiIntent.LoadMore(
            forumId,
            forumName,
            page,
            threadListIds,
            getSortType(context, forumName)
        )
    }
}

private enum class ItemType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

@Composable
private fun GoodClassifyTabs(
    goodClassifyHolders: ImmutableList<ImmutableHolder<Classify>>,
    selectedItem: Int?,
    onSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = goodClassifyHolders,
            key = { it.get { "${class_id}_$class_name" } }
        ) { holder ->
            Chip(
                text = holder.get { class_name },
                invertColor = selectedItem == holder.get { class_id },
                onClick = { onSelected(holder.get { class_id }) }
            )
        }
    }
}

@Composable
private fun TopThreadItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: String = stringResource(id = R.string.content_top),
) {
    Row(
        modifier = modifier
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Chip(
            text = type,
            shape = RoundedCornerShape(3.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
    }
}

@Composable
private fun ThreadList(
    state: LazyListState,
    items: ImmutableList<ThreadItemData>,
    forumName: String,
    onItemClicked: (ThreadInfo) -> Unit,
    onItemReplyClicked: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    forumRuleTitle: String? = null,
    onOpenForumRule: (() -> Unit)? = null,
    onOriginThreadClicked: (OriginThreadInfo) -> Unit = {},
    onUserClicked: (User) -> Unit = {},
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val itemFraction = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 0.5f
        else -> 1f
    }
    val ruleTitle = forumRuleTitle?.takeIf { it.isNotEmpty() }
    // 置顶帖由服务端排在最前，按 isTop 拆成独立区块，折叠时整块收起（issue #28）
    val (pinned, normal) = remember(items) {
        items.partition { it.thread.get { isTop } == 1 }
    }
    var pinnedExpanded by rememberSaveable(forumName) { mutableStateOf(true) }
    val hasPinnedSection = pinned.isNotEmpty() || ruleTitle != null

    MyLazyColumn(
        state = state,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(feedCardListSpacing()),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = 4.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        if (hasPinnedSection) {
            item(key = "PinnedSectionHeader") {
                PinnedSectionHeader(
                    expanded = pinnedExpanded,
                    itemCount = pinned.size + if (ruleTitle != null) 1 else 0,
                    modifier = Modifier.fillMaxWidth(itemFraction),
                    onToggle = { pinnedExpanded = !pinnedExpanded }
                )
            }
        }
        if (pinnedExpanded) {
            if (ruleTitle != null) {
                item(key = "ForumRule") {
                    TopThreadItem(
                        title = ruleTitle,
                        onClick = {
                            onOpenForumRule?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth(itemFraction),
                        type = stringResource(id = R.string.desc_forum_rule)
                    )
                }
            }
            itemsIndexed(
                items = pinned,
                key = { index, data -> "pinned_${index}_${data.thread.get { id }}" },
                contentType = { _, data -> data.contentType() }
            ) { _, data ->
                ThreadRow(
                    data = data,
                    itemFraction = itemFraction,
                    onItemClicked = onItemClicked,
                    onItemReplyClicked = onItemReplyClicked,
                    onAgree = onAgree,
                    onOriginThreadClicked = onOriginThreadClicked,
                    onUserClicked = onUserClicked
                )
            }
        }
        itemsIndexed(
            items = normal,
            key = { index, data -> "thread_${index}_${data.thread.get { id }}" },
            contentType = { _, data -> data.contentType() }
        ) { _, data ->
            ThreadRow(
                data = data,
                itemFraction = itemFraction,
                onItemClicked = onItemClicked,
                onItemReplyClicked = onItemReplyClicked,
                onAgree = onAgree,
                onOriginThreadClicked = onOriginThreadClicked,
                onUserClicked = onUserClicked
            )
        }
    }
}

private fun ThreadItemData.contentType(): ItemType {
    val item = thread.get()
    return if (item.isTop == 1) ItemType.Top
    else {
        if (item.media.isNotEmpty())
            if (item.media.size == 1) ItemType.SingleMedia else ItemType.MultiMedia
        else if (item.videoInfo != null)
            ItemType.Video
        else ItemType.PlainText
    }
}

@Composable
private fun ThreadRow(
    data: ThreadItemData,
    itemFraction: Float,
    onItemClicked: (ThreadInfo) -> Unit,
    onItemReplyClicked: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    onOriginThreadClicked: (OriginThreadInfo) -> Unit,
    onUserClicked: (User) -> Unit,
) {
    BlockableContent(
        blocked = data.blocked,
        blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(itemFraction)
        ) {
            val item = data.thread.get()
            if (item.isTop == 1) {
                val title = item.title.takeUnless { it.isBlank() } ?: item.abstractText
                TopThreadItem(
                    title = title,
                    onClick = { onItemClicked(item) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                FeedCard(
                    item = data.thread,
                    onClick = onItemClicked,
                    onClickReply = onItemReplyClicked,
                    onAgree = onAgree,
                    onClickOriginThread = onOriginThreadClicked,
                    onClickUser = onUserClicked
                )
            }
        }
    }
}

/** 折叠「置顶与吧规」区块的一行入口：收起后只占一行，把首屏让给帖子列表。 */
@Composable
private fun PinnedSectionHeader(
    expanded: Boolean,
    itemCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .debounceClickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Chip(
            text = stringResource(id = R.string.content_top),
            shape = RoundedCornerShape(3.dp)
        )
        Text(
            text = stringResource(id = R.string.title_forum_pinned_section),
            style = MaterialTheme.typography.subtitle2,
            fontSize = 15.sp,
            maxLines = 1
        )
        Text(
            text = stringResource(id = R.string.tip_forum_pinned_count, itemCount),
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ForumThreadListPage(
    forumId: Long,
    forumName: String,
    type: ForumThreadListType,
    viewModel: ForumThreadListViewModel = when (type) {
        ForumThreadListType.Good -> pageViewModel<GoodThreadListViewModel>()
        ForumThreadListType.Hot -> pageViewModel<HotThreadListViewModel>()
        ForumThreadListType.Latest -> pageViewModel<LatestThreadListViewModel>()
    },
    lazyListState: LazyListState = rememberLazyListState()
) {
    val isGood = type == ForumThreadListType.Good
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(getFirstLoadIntent(context, forumName, type))
        viewModel.initialized = true
    }
    onGlobalEvent<ForumThreadListUiEvent.Refresh>(
        filter = { it.type == type },
    ) {
        viewModel.send(getRefreshIntent(context, forumName, type, it.sortType))
    }
    onGlobalEvent<ForumThreadListUiEvent.BackToTop>(
        filter = { it.type == type },
    ) {
        lazyListState.animateScrollToItem(0)
    }
    viewModel.onEvent<ForumThreadListUiEvent.AgreeFail> {
        val snackbarResult = snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.snackbar_agree_fail,
                it.errorCode,
                it.errorMsg
            ),
            actionLabel = context.getString(R.string.button_retry)
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            viewModel.send(
                ForumThreadListUiIntent.Agree(
                    it.threadId,
                    it.postId,
                    it.hasAgree
                )
            )
        }
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::hasMore,
        // 热门列表没有下一页，采集初值也必须是 false：produceState 在首个组合帧用的是这个字面量，
        // 而 LoadMoreLayout 的 canLoadMore 只看它。给 true 就等于在首屏第一帧开了一次翻页窗口。
        initial = type != ForumThreadListType.Hot
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::currentPage,
        initial = 1
    )
    val forumRuleTitle by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::forumRuleTitle,
        initial = null
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threadList,
        initial = persistentListOf()
    )
    val threadListIds by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threadListIds,
        initial = persistentListOf()
    )
    val goodClassifyId by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::goodClassifyId,
        initial = null
    )
    val goodClassifies by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::goodClassifies,
        initial = persistentListOf()
    )
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(getRefreshIntent(context, forumName, type)) }
    )
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isGood) {
                GoodClassifyTabs(
                    goodClassifyHolders = goodClassifies,
                    selectedItem = goodClassifyId,
                    onSelected = {
                        viewModel.send(
                            getRefreshIntent(
                                context,
                                forumName,
                                type,
                                goodClassifyId = it
                            )
                        )
                    }
                )
            }

            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = {
                    viewModel.send(
                        getLoadMoreIntent(
                            context,
                            forumId,
                            forumName,
                            currentPage,
                            threadListIds,
                            type
                        )
                    )
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = threadList.isEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                ThreadList(
                    state = lazyListState,
                    items = threadList,
                    forumName = forumName,
                    onItemClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.threadId,
                                forumId = it.forumId,
                                threadInfo = it
                            )
                        )
                    },
                    onItemReplyClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.threadId,
                                forumId = it.forumId,
                                scrollToReply = true
                            )
                        )
                    },
                    onAgree = {
                        viewModel.send(
                            ForumThreadListUiIntent.Agree(
                                it.threadId,
                                it.firstPostId,
                                it.agree?.hasAgree ?: 0
                            )
                        )
                    },
                    forumRuleTitle = forumRuleTitle,
                    onOpenForumRule = {
                        navigator.navigate(ForumRuleDetailPageDestination(forumId))
                    },
                    onOriginThreadClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                threadId = it.tid.toLong(),
                                forumId = it.fid,
                            )
                        )
                    }
                ) { navigator.navigate(UserProfilePageDestination(it.id)) }
            }
        }

        PullRefreshLogoIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.onPrimary,
        )
    }
}