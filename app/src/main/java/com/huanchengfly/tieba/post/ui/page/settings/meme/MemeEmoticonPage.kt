package com.huanchengfly.tieba.post.ui.page.settings.meme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.ui.page.settings.components.settingsListContentPadding
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.MemeEmoticon
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun MemeEmoticonPage(
    viewModel: MemeEmoticonViewModel = pageViewModel<MemeEmoticonUiIntent, MemeEmoticonViewModel>(
        listOf(MemeEmoticonUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    val isLoggedIn by viewModel.uiState.collectPartialAsState(
        prop1 = MemeEmoticonUiState::isLoggedIn,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = MemeEmoticonUiState::isLoading,
        initial = true
    )
    val isSyncing by viewModel.uiState.collectPartialAsState(
        prop1 = MemeEmoticonUiState::isSyncing,
        initial = false
    )
    val memes by viewModel.uiState.collectPartialAsState(
        prop1 = MemeEmoticonUiState::memes,
        initial = persistentListOf()
    )

    var pendingDelete by remember { mutableStateOf<MemeEmoticon?>(null) }
    val deleteDialogState = rememberDialogState()

    val collectList = remember(memes) {
        memes.filter { it.type == MemeEmoticon.TYPE_COLLECT }
    }
    val diyList = remember(memes) {
        memes.filter { it.type == MemeEmoticon.TYPE_DIY }
    }

    ConfirmDialog(
        dialogState = deleteDialogState,
        onConfirm = {
            pendingDelete?.let { viewModel.send(MemeEmoticonUiIntent.Remove(it.picId, it.type)) }
            pendingDelete = null
        },
        onCancel = { pendingDelete = null },
        onDismiss = { pendingDelete = null },
        title = { Text(text = stringResource(id = R.string.title_delete_meme)) },
    ) {
        Text(text = stringResource(id = R.string.tip_delete_meme))
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_meme_emoticon),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                actions = {
                    if (isLoggedIn) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp,
                                color = ExtendedTheme.colors.primary
                            )
                        } else {
                            IconButton(onClick = { viewModel.send(MemeEmoticonUiIntent.Sync) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(id = R.string.desc_sync)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (!isLoggedIn) {
                StateScreen(
                    isEmpty = true,
                    isError = false,
                    isLoading = false,
                    modifier = Modifier.fillMaxSize(),
                    emptyScreen = {
                        Text(
                            text = stringResource(id = R.string.tip_login_first),
                            style = MaterialTheme.typography.body1,
                            color = ExtendedTheme.colors.textSecondary
                        )
                    }
                ) {}
            } else {
                val coroutineScope = rememberCoroutineScope()
                val pagerState = rememberPagerState { 2 }
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        PagerTabIndicator(pagerState = pagerState, tabPositions = tabPositions)
                    },
                    divider = {},
                    backgroundColor = Color.Transparent,
                    contentColor = ExtendedTheme.colors.primary,
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        selectedContentColor = ExtendedTheme.colors.primary,
                        unselectedContentColor = ExtendedTheme.colors.textSecondary,
                    ) {
                        Box(
                            modifier = Modifier.height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(id = R.string.title_meme_collect),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        selectedContentColor = ExtendedTheme.colors.primary,
                        unselectedContentColor = ExtendedTheme.colors.textSecondary,
                    ) {
                        Box(
                            modifier = Modifier.height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(id = R.string.title_meme_diy),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    key = { it },
                ) { page ->
                    val list = if (page == 0) collectList else diyList
                    StateScreen(
                        isEmpty = list.isEmpty(),
                        isError = false,
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxSize(),
                        onReload = { viewModel.send(MemeEmoticonUiIntent.Sync) },
                        clickToReload = true,
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(72.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = settingsListContentPadding(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(list, key = { it.id }) { item ->
                                MemeEmoticonItem(
                                    item = item,
                                    onLongClick = {
                                        pendingDelete = item
                                        deleteDialogState.show = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemeEmoticonItem(
    item: MemeEmoticon,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            imageUri = item.thumbnail?.takeIf { it.isNotEmpty() } ?: item.picUrl,
            contentDescription = stringResource(id = R.string.emoticon, ""),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        )
    }
}
