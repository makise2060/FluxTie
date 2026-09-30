package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.compose.calcStatusBarColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


@Immutable
data class ExplorePageItem(
    val id: String,
    val icon: ImageVector,
    val name: @Composable (selected: Boolean) -> Unit,
    val content: @Composable () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.ExplorePageTab(
    pagerState: PagerState,
    pages: ImmutableList<ExplorePageItem>,
    collapseFraction: Float,
) {
    val coroutineScope = rememberCoroutineScope()
    // 收起后压缩为“左图标右文字”紧凑形态，行高与宽度随收起进度平滑过渡
    val compact = collapseFraction > 0.5f
    val rowHeight by animateDpAsState(
        targetValue = if (compact) 48.dp else 72.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "exploreTabRowHeight"
    )
    val tabWidth by animateDpAsState(
        targetValue = if (compact) 96.dp else 76.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "exploreTabWidth"
    )

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            PagerTabIndicator(
                pagerState = pagerState,
                tabPositions = tabPositions
            )
        },
        divider = {
            // 收起时浮现发丝线，强化“紧凑工具条”语义
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        color = ExtendedTheme.colors.onTopBar.copy(
                            alpha = 0.08f * collapseFraction
                        )
                    )
            )
        },
        backgroundColor = Color.Transparent,
        contentColor = ExtendedTheme.colors.onTopBar,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .width(tabWidth * pages.size),
    ) {
        pages.fastForEachIndexed { index, item ->
            val selected = pagerState.currentPage == index
            Tab(
                selected = selected,
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage == index) {
                            emitGlobalEvent(GlobalEvent.Refresh(item.id))
                        } else {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
                modifier = Modifier.height(rowHeight),
                text = {
                    AnimatedContent(
                        targetState = compact,
                        transitionSpec = {
                            (fadeIn(tween(150, easing = LinearOutSlowInEasing)) +
                                    scaleIn(
                                        initialScale = 0.9f,
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    )) togetherWith
                                    fadeOut(tween(100, easing = FastOutLinearInEasing))
                        },
                        label = "exploreTabLayout"
                    ) { isCompact ->
                        if (isCompact) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                item.name(selected)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                item.name(selected)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TabText(
    text: String,
    selected: Boolean
) {
    val style = MaterialTheme.typography.button.copy(
        letterSpacing = 0.75.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
    Text(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorePage() {
    val account = LocalAccount.current
    val navigator = LocalNavigator.current
    val density = LocalDensity.current

    val loggedIn = remember(account) { account != null }

    val pages = remember {
        listOfNotNull(
            if (loggedIn) ExplorePageItem(
                "concern",
                icon = Icons.Rounded.Favorite,
                { TabText(text = stringResource(id = R.string.title_concern), selected = it) },
                { ConcernPage() }
            ) else null,
            ExplorePageItem(
                "personalized",
                icon = Icons.Rounded.AutoAwesome,
                { TabText(text = stringResource(id = R.string.title_personalized), selected = it) },
                { PersonalizedPage() }
            ),
            ExplorePageItem(
                "hot",
                icon = Icons.Rounded.LocalFireDepartment,
                { TabText(text = stringResource(id = R.string.title_hot), selected = it) },
                { HotPage() }
            ),
        ).toImmutableList()
    }
    val pagerState = rememberPagerState(initialPage = if (account != null) 1 else 0) { pages.size }
    val coroutineScope = rememberCoroutineScope()

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "explore" }
    ) {
        coroutineScope.emitGlobalEvent(GlobalEvent.Refresh(pages[pagerState.currentPage].id))
    }

    var heightOffset by rememberSaveable { mutableFloatStateOf(0f) }
    var titleBarHeight by rememberSaveable {
        mutableFloatStateOf(with(density) { 56.dp.toPx() })
    }
    // 标题栏收起进度 0(展开)→1(完全收起)，驱动 Tab 行压缩形态
    val collapseFraction = if (titleBarHeight > 0f) {
        (-heightOffset / titleBarHeight).coerceIn(0f, 1f)
    } else 0f

    val headerNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y < 0) {
                    val prevHeightOffset = heightOffset
                    heightOffset = max(heightOffset + available.y, -titleBarHeight)
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

    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Column {
                Spacer(
                    modifier = Modifier
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .fillMaxWidth()
                        .background(color = ExtendedTheme.colors.topBar.calcStatusBarColor())
                )
                Box(
                    modifier = Modifier
                        .height(with(density) { (titleBarHeight + heightOffset).toDp() })
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentHeight(
                                align = Alignment.Bottom,
                                unbounded = true
                            )
                            .onSizeChanged { titleBarHeight = it.height.toFloat() }
                    ) {
                        Toolbar(
                            title = stringResource(id = R.string.title_explore),
                            insets = false,
                            navigationIcon = accountNavIconIfCompact(),
                            actions = {
                                ActionItem(
                                    icon = Icons.Rounded.Search,
                                    contentDescription = stringResource(id = R.string.title_search)
                                ) {
                                    navigator.navigate(SearchPageDestination)
                                }
                            },
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = ExtendedTheme.colors.topBar),
                ) {
                    ExplorePageTab(
                        pagerState = pagerState,
                        pages = pages,
                        collapseFraction = collapseFraction
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            // 呼吸位在各子页列表的 contentPadding 上(Pager 的 contentPadding 会裁剪页面视口)
            contentPadding = paddingValues,
            state = pagerState,
            key = { pages[it].id },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(headerNestedScrollConnection),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            pages[it].content()
        }
    }
}