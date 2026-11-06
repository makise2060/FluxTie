package com.huanchengfly.tieba.post.ui.page.hottopic.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.destinations.TopicDetailPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.TopicCard
import com.huanchengfly.tieba.post.ui.widgets.compose.feedCardListSpacing
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 话题榜：单列 TopicCard 列表，名次独立成列，统一 RankBadge 名次体系。
 * 榜单加载失败/无内容时使用 EmptyState 两态。
 */
@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun HotTopicListPage(
    viewModel: HotTopicListViewModel = pageViewModel<HotTopicListUiIntent, HotTopicListViewModel>(
        listOf(HotTopicListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = HotTopicListUiState::isRefreshing,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = HotTopicListUiState::isError,
        initial = false
    )
    val topicList by viewModel.uiState.collectPartialAsState(
        prop1 = HotTopicListUiState::topicList,
        initial = emptyList()
    )
    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_hot_message_list),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { contentPaddings ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { viewModel.send(HotTopicListUiIntent.Load) }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings)
                .pullRefresh(pullRefreshState)
        ) {
            if (!isRefreshing && topicList.isEmpty()) {
                val reload: () -> Unit = { viewModel.send(HotTopicListUiIntent.Load) }
                EmptyState(
                    title = stringResource(
                        id = if (isError) R.string.title_topic_list_failed else R.string.title_topic_list_empty
                    ),
                    subtitle = if (isError) stringResource(id = R.string.tip_load_failed_retry) else null,
                    illustration = EmptyIllustration.Box,
                    modifier = Modifier.align(Alignment.Center),
                    actions = {
                        Button(onClick = reload) {
                            Text(text = stringResource(id = R.string.button_retry))
                        }
                    }
                )
            } else {
                MyLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(feedCardListSpacing()),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                        items = topicList,
                        key = { _, item -> item.topic_id },
                        contentType = { _, _ -> "TopicCard" }
                    ) { index, item ->
                        TopicCard(
                            rank = index + 1,
                            title = item.topic_name,
                            intro = item.topic_desc,
                            discussNum = item.discuss_num,
                            tag = item.topic_tag,
                            imageUrl = item.topic_image,
                            onClick = {
                                navigator.navigate(
                                    TopicDetailPageDestination(
                                        item.topic_id,
                                        item.topic_name,
                                        rank = index + 1,
                                        tag = item.topic_tag
                                    )
                                )
                            }
                        )
                    }
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
}
