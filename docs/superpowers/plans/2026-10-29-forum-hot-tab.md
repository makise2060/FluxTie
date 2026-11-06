# 吧页「热门」标签 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 FluxTie 吧页（`ForumPage`）最前面插入「热门」标签页，仅在服务端下发热门分区（`tabType == 13`）的吧显示，作为默认首屏，复用现有 `frs/page` 接口的 `sort_type = 3`，只加载第 1 页。

**Architecture:** 把 `ForumPage.kt` 中硬编码的「最新=0、精华=1、通用 tab 从 2 开始」页序改成由 `hasHot` 推导的索引常量（`hotIndex` / `latestIndex` / `goodIndex` / `generalStart` / `pageCount`）。`ForumThreadListType` 枚举增加 `Hot`，`ForumThreadListPage` 的 `isGood: Boolean` 参数改为 `type: ForumThreadListType`，全局事件同步携带 `type` 而非 `isGood`。新增 `HotThreadListViewModel`，与 `LatestThreadListViewModel` / `GoodThreadListViewModel` 并列，三者共用 `ForumThreadListPartialChangeProducer`，由 `type` 决定 `sortType`。`Hot` 类型在成功回调里强制 `hasMore = false`。

**Tech Stack:** Kotlin 2.3、Jetpack Compose（Material2）、Hilt、Wire protobuf、手写 MVI（`BaseViewModel` + `PartialChangeProducer`）、JUnit 4.13.2（`app/src/test`，仅 JVM 单测）。

**Spec:** `docs/superpowers/specs/2026-10-29-forum-hot-tab-design.md`

## Global Constraints

- 不新增接口、不新增 proto 字段、不改 `FrsPageRepository`、不改 `api/*`。
- 热门页 `sortType` 固定为 `3`，与用户设置的「吧默认排序」无关。
- 热门页 `hasMore` 恒为 `false`：`pn=3` 返回内容与 `pn=2` 完全重复，不能无限下拉。
- 落地页判据用 `hasHot`（`navTabInfo?.tab?.any { it.tabType == 13 }`），**不用** `frs_tab_default`（实测所有吧恒为 1，无区分度）。
- 通用 tab 过滤条件保持 `isGeneralTab == 1 && tabType == 15` 不变。
- 字符串资源名 `tab_forum_hot`，文案「热门」，插入位置紧邻 `tab_forum_latest` / `tab_forum_good`。
- 新增代码沿用现有 MVI 模式（`UiIntent` / `PartialChange` / `UiState` / `UiEvent` 四件套 + `@HiltViewModel`）。
- 提交信息用 Conventional Commits，中文描述，与仓库现有风格一致。
- git 提交需要显式传身份：`git -c user.name=xzy1 -c user.email=xzy1@local commit ...`。

## Review Focus

- **无热门 tab 的吧（李毅吧、原神吧）**：页数不能多出一页空白，TabRow 不能出现「热门」，首屏必须是「最新」。
- **热门分页到底**：上拉不能发出第 2 页请求，`LoadMoreLayout` 必须显示"没有更多"而非无限转圈。
- **三页事件串台**：热门页的 FAB 刷新/回到顶部不能触发「最新」或「精华」页刷新，反之亦然。
- **`navTabInfo` 异步到达**：首帧 `hasHot` 为 false、随后变 true 时，落地页要落到热门且不能把用户已手动切到的页抢回来。
- **精华页回归**：`type` 重构后「精华」的分类 chip 切换、`goodClassifyId = 0` 初始请求必须与改造前完全一致。
- **`frs_tab_default` 误用**：任何地方都不得以该字段决定落地页。

---

## File Structure

| 文件 | 职责 |
| --- | --- |
| `app/src/main/res/values/strings.xml` | 新增 `tab_forum_hot` 字符串 |
| `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt` | `ForumThreadListType` 加 `Hot`；`sortType` 解析集中到 `resolveSortType`；`Hot` 强制 `hasMore = false`；新增 `HotThreadListViewModel`；事件 `isGood` → `type` |
| `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListPage.kt` | 参数 `isGood` → `type`；三个 intent 工厂按 `type` 分支；事件过滤按 `type` |
| `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumPage.kt` | 页序索引推导；`hotListState`；`currentListState` 三态；TabRow 加热门 Tab；pager 三态分发；落地页校正；骨架屏 |
| `app/src/test/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListTypeTest.kt` | 新增：`resolveSortType` 与 `hasMore` 覆盖逻辑的 JVM 单测 |

---

### Task 1: 抽出可测的 `sortType` / `hasMore` 纯函数

**Files:**
- Modify: `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt:62-64`（枚举）、`:100`、`:126`、`:170`（三处 sortType 解析）、`:115`、`:139`、`:181`（三处 hasMore）
- Test: `app/src/test/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListTypeTest.kt`

**Interfaces:**
- Produces:
  - `enum class ForumThreadListType { Hot, Latest, Good }`
  - `internal fun ForumThreadListType.resolveSortType(userSortType: Int): Int`
  - `internal fun ForumThreadListType.resolveHasMore(serverHasMore: Boolean): Boolean`
  - `const val HOT_SORT_TYPE = 3`
- Consumes: 无（本任务自包含）

- [ ] **Step 1: 写失败测试**

创建 `app/src/test/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListTypeTest.kt`：

```kotlin
package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumThreadListTypeTest {
    @Test
    fun hot_alwaysUsesSortType3_regardlessOfUserSetting() {
        assertEquals(3, ForumThreadListType.Hot.resolveSortType(0))
        assertEquals(3, ForumThreadListType.Hot.resolveSortType(1))
        assertEquals(3, ForumThreadListType.Hot.resolveSortType(6))
    }

    @Test
    fun latest_passesUserSortTypeThrough() {
        assertEquals(0, ForumThreadListType.Latest.resolveSortType(0))
        assertEquals(1, ForumThreadListType.Latest.resolveSortType(1))
    }

    @Test
    fun good_alwaysUsesMinusOne() {
        assertEquals(-1, ForumThreadListType.Good.resolveSortType(0))
        assertEquals(-1, ForumThreadListType.Good.resolveSortType(1))
    }

    @Test
    fun hot_neverHasMore_evenWhenServerSaysYes() {
        assertFalse(ForumThreadListType.Hot.resolveHasMore(true))
        assertFalse(ForumThreadListType.Hot.resolveHasMore(false))
    }

    @Test
    fun latestAndGood_respectServerHasMore() {
        assertTrue(ForumThreadListType.Latest.resolveHasMore(true))
        assertFalse(ForumThreadListType.Latest.resolveHasMore(false))
        assertTrue(ForumThreadListType.Good.resolveHasMore(true))
        assertFalse(ForumThreadListType.Good.resolveHasMore(false))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd "$WT" && "$GRADLE" :app:testDebugUnitTest --tests "*ForumThreadListTypeTest*" 2>&1 | tail -30
```

Expected: FAIL —— 编译错误 `Unresolved reference: resolveSortType`（`Hot` 枚举项也不存在）。

- [ ] **Step 3: 实现枚举与两个函数**

`ForumThreadListViewModel.kt:62-64` 的枚举改为：

```kotlin
const val HOT_SORT_TYPE = 3

enum class ForumThreadListType {
    Hot, Latest, Good
}

internal fun ForumThreadListType.resolveSortType(userSortType: Int): Int = when (this) {
    ForumThreadListType.Hot -> HOT_SORT_TYPE
    ForumThreadListType.Latest -> userSortType
    ForumThreadListType.Good -> -1
}

internal fun ForumThreadListType.resolveHasMore(serverHasMore: Boolean): Boolean = when (this) {
    ForumThreadListType.Hot -> false
    ForumThreadListType.Latest, ForumThreadListType.Good -> serverHasMore
}
```

然后把三处 `sortType.takeIf { type == ForumThreadListType.Latest } ?: -1`（`:100`、`:126`、`:170`）替换为 `type.resolveSortType(sortType)`，把三处 `response.data_.page.has_more == 1`（`:115`、`:139`、`:181`）替换为 `type.resolveHasMore(response.data_.page.has_more == 1)`。

- [ ] **Step 4: 跑测试确认通过**

```bash
cd "$WT" && "$GRADLE" :app:testDebugUnitTest --tests "*ForumThreadListTypeTest*" 2>&1 | tail -20
```

Expected: PASS，`BUILD SUCCESSFUL`，6 个测试全绿。

- [ ] **Step 5: 提交**

```bash
cd "F:/a个人项目/第三方贴吧/FluxTie-main" && git add app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt app/src/test/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListTypeTest.kt && git -c user.name=xzy1 -c user.email=xzy1@local commit -m "feat(forum): 抽出热门排序与分页的纯函数"
```

---

### Task 2: 新增 `HotThreadListViewModel`

**Files:**
- Modify: `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt:66-78`

**Interfaces:**
- Consumes: Task 1 的 `ForumThreadListType.Hot`、`resolveSortType`、`resolveHasMore`
- Produces: `class HotThreadListViewModel @Inject constructor() : ForumThreadListViewModel()`

- [ ] **Step 1: 加 ViewModel**

在 `GoodThreadListViewModel`（`:73-78`）之后并列新增：

```kotlin
@Stable
@HiltViewModel
class HotThreadListViewModel @Inject constructor() : ForumThreadListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<ForumThreadListUiIntent, ForumThreadListPartialChange, ForumThreadListUiState> =
        ForumThreadListPartialChangeProducer(ForumThreadListType.Hot)
}
```

- [ ] **Step 2: 编译确认**

```bash
cd "$WT" && "$GRADLE" :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 提交**

```bash
cd "F:/a个人项目/第三方贴吧/FluxTie-main" && git add app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt && git -c user.name=xzy1 -c user.email=xzy1@local commit -m "feat(forum): 新增热门列表 ViewModel"
```

---

### Task 3: 事件模型 `isGood` → `type`

**Files:**
- Modify: `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt:423-430`
- Modify: `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListPage.kt:70-106`（三个 intent 工厂）、`:257-281`（入口参数与事件过滤）、`:340`、`:348`、`:375`

**Interfaces:**
- Consumes: Task 1 的 `ForumThreadListType`
- Produces:
  - `data class ForumThreadListUiEvent.Refresh(val type: ForumThreadListType, val sortType: Int)`
  - `data class ForumThreadListUiEvent.BackToTop(val type: ForumThreadListType)`
  - `fun ForumThreadListPage(forumId: Long, forumName: String, type: ForumThreadListType = ForumThreadListType.Latest, viewModel: ForumThreadListViewModel = ..., lazyListState: LazyListState = rememberLazyListState())`

- [ ] **Step 1: 改事件定义**

`ForumThreadListViewModel.kt:423-430` 改为：

```kotlin
    data class Refresh(
        val type: ForumThreadListType,
        val sortType: Int
    ) : ForumThreadListUiEvent

    data class BackToTop(
        val type: ForumThreadListType
    ) : ForumThreadListUiEvent
```

- [ ] **Step 2: 改三个 intent 工厂**

`ForumThreadListPage.kt:70-106` 三个私有函数改为按 `type` 分支（`isGood` 一律替换为 `type == ForumThreadListType.Good`，`sortType` 参数仅在 Latest 生效）：

```kotlin
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
        ForumThreadListType.Good -> ForumThreadListUiIntent.LoadMore(forumId, forumName, page, threadListIds, -1)
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
```

注意：原 Good 分支的 `getFirstLoadIntent` 用 `Refresh(forumName, -1, 0)`、`getLoadMoreIntent` 用 `LoadMore(..., 0)`，**必须逐字保留**，否则精华页行为回归。

- [ ] **Step 3: 改入口参数与事件过滤**

`ForumThreadListPage`（`:257-263`）：

```kotlin
fun ForumThreadListPage(
    forumId: Long,
    forumName: String,
    type: ForumThreadListType = ForumThreadListType.Latest,
    viewModel: ForumThreadListViewModel = when (type) {
        ForumThreadListType.Good -> pageViewModel<GoodThreadListViewModel>()
        ForumThreadListType.Hot -> pageViewModel<HotThreadListViewModel>()
        ForumThreadListType.Latest -> pageViewModel<LatestThreadListViewModel>()
    },
    lazyListState: LazyListState = rememberLazyListState()
) {
```

函数体内派生 `val isGood = type == ForumThreadListType.Good`，并把 `:269`、`:275`、`:340`、`:375` 的 `isGood` 实参全部换成 `type`，把 `:273`、`:278` 的 `filter = { it.isGood == isGood }` 换成 `filter = { it.type == type }`。`:348` 的 `if (isGood)` 保持不变（它读的是派生值）。

- [ ] **Step 4: 编译确认（预期 ForumPage 报错）**

```bash
cd "$WT" && "$GRADLE" :app:compileDebugKotlin 2>&1 | grep -E "^e:|error:" | head -20
```

Expected: FAIL，`ForumPage.kt` 中 `isGood = pageIndex == 1` 与 `ForumThreadListUiEvent.Refresh(...)` / `BackToTop(...)` 的实参类型不匹配。这是预期的跨文件断裂，Task 4 修复。

- [ ] **Step 5: 提交（连同 Task 4 一起，见下）**

---

### Task 4: `ForumPage` 页序重构

**Files:**
- Modify: `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumPage.kt:452-496`、`:541-563`、`:670-704`、`:739-753`、`:890-951`、`:952-1014`、`:1017-1045`、`:1091-1116`

**Interfaces:**
- Consumes: Task 1 的 `ForumThreadListType`；Task 3 的 `ForumThreadListPage(type = ...)` 与 `ForumThreadListUiEvent.Refresh(type, sortType)` / `BackToTop(type)`
- Produces: 无（叶子任务）

- [ ] **Step 1: 加索引推导与 `hotListState`**

在 `:452-461` 的 `generalTabs` 之后、`rememberPagerState` 之前插入：

```kotlin
    val hasHotTab by remember {
        derivedStateOf { navTabInfo?.tab?.any { it.tabType == 13 } ?: false }
    }
    val hotIndex = 0
    val latestIndex = if (hasHotTab) 1 else 0
    val goodIndex = latestIndex + 1
    val generalStart = goodIndex + 1
    val totalPageCount = generalStart + generalTabs.size
```

`rememberPagerState`（`:459`）改为：

```kotlin
    val pagerState = rememberPagerState(
        initialPage = if (hasHotTab) hotIndex else latestIndex,
        pageCount = { totalPageCount }
    )
    val hotListState = rememberLazyListState()
```

命名用 `totalPageCount` 而非 `pageCount`，避免与 `rememberPagerState` 的具名参数 `pageCount` 在 lambda 内同名遮蔽。

- [ ] **Step 2: 改 `currentListState` 与 `enablePullToRefresh`**

`:469` 改为：

```kotlin
    val currentListState = when (currentPage) {
        hotIndex -> hotListState
        latestIndex -> latestListState
        goodIndex -> goodListState
        else -> latestListState
    }
```

`:495` 改为：

```kotlin
        derivedStateOf { currentPage < generalStart && isListAtTop && isHeaderExpanded }
```

- [ ] **Step 3: 加落地页校正**

前提事实：`forumInfo` 与 `navTabInfo` 来自同一个 `ForumPartialChange.Load.Success(forum, tbs, nav_tab_info)`，所以 pager 首次组合时 `hasHotTab` 通常已经正确，`initialPage` 直接生效。下面这段是兜底，只在两者被拆开到达时才起作用。

在 `:496` 之后插入：

```kotlin
    var landingCorrected by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasHotTab) {
        if (hasHotTab && !landingCorrected) {
            landingCorrected = true
            if (pagerState.currentPage == 0) pagerState.scrollToPage(hotIndex)
        }
    }
```

**不要**用 `snapshotFlow { pagerState.currentPage }` 去判断"用户是否手动切页"：`scrollToPage` 自身也会改 `currentPage`，会形成自触发。上面的 `landingCorrected` 一次性标志足够。

- [ ] **Step 4: 改 `AddThreadSuccess` 分支**

`:541-563` 改为：

```kotlin
    onGlobalEvent<GlobalEvent.AddThreadSuccess>() {
        coroutineScope.launch {
            if (currentPage >= generalStart) {
                emitGlobalEvent(GeneralTabListUiEvent.BackToTop)
                emitGlobalEvent(GeneralTabListUiEvent.Refresh())
            } else {
                val type = when (currentPage) {
                    hotIndex -> ForumThreadListType.Hot
                    goodIndex -> ForumThreadListType.Good
                    else -> ForumThreadListType.Latest
                }
                emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(type))
                emitGlobalEventSuspend(
                    ForumThreadListUiEvent.Refresh(type, getSortType(context, forumName))
                )
            }
        }
    }
```

- [ ] **Step 5: 改 FAB 两个分支**

`:670-704` 中两处 `currentPage >= 2` 改为 `currentPage >= generalStart`，`currentPage == 1` 改为 `currentPage == goodIndex`，并套用与 Step 4 相同的 `type` 推导（`BackToTop(type)`、`Refresh(type, getSortType(context, forumName))`）。

- [ ] **Step 6: 改下拉刷新事件**

`:744-750` 的 `ForumThreadListUiEvent.Refresh(currentPage == 1, getSortType(context, forumName))` 改为：

```kotlin
                            ForumThreadListUiEvent.Refresh(
                                when (currentPage) {
                                    hotIndex -> ForumThreadListType.Hot
                                    goodIndex -> ForumThreadListType.Good
                                    else -> ForumThreadListType.Latest
                                },
                                getSortType(context, forumName)
                            )
```

- [ ] **Step 7: TabRow 加热门 Tab**

`:889` 之前插入热门 Tab（仅 `hasHotTab` 时）：

```kotlin
                                if (hasHotTab) {
                                    Tab(
                                        selected = currentPage == hotIndex,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(hotIndex)
                                            }
                                        },
                                        selectedContentColor = ExtendedTheme.colors.primary,
                                        unselectedContentColor = ExtendedTheme.colors.textSecondary
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.tab_forum_hot),
                                                style = tabTextStyle
                                            )
                                        }
                                    }
                                }
```

然后把「最新」Tab 的 `selected = currentPage == 0` 改为 `selected = currentPage == latestIndex`、`animateScrollToPage(0)` 改为 `animateScrollToPage(latestIndex)`；「精华」Tab 的 `selected = currentPage == 1` 改为 `selected = currentPage == goodIndex`、`animateScrollToPage(1)` 改为 `animateScrollToPage(goodIndex)`；排序菜单里的 `ForumThreadListUiEvent.Refresh(currentPage == 1, value)` 改为 `Refresh(ForumThreadListType.Latest, value)`。

- [ ] **Step 8: 改通用 tab 索引基准**

`:953` 的 `val tabIndex = 2 + generalTabs.indexOf(tab)` 改为 `val tabIndex = generalStart + generalTabs.indexOf(tab)`。

- [ ] **Step 9: 改 pager 三态分发**

`:1024-1044` 改为：

```kotlin
                                ) { pageIndex ->
                                    when (pageIndex) {
                                        hotIndex -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = ForumThreadListType.Hot,
                                            lazyListState = hotListState
                                        )
                                        latestIndex -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = ForumThreadListType.Latest,
                                            lazyListState = latestListState
                                        )
                                        goodIndex -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = ForumThreadListType.Good,
                                            lazyListState = goodListState
                                        )
                                        else -> {
                                            val tabIndex = pageIndex - generalStart
                                            if (tabIndex in generalTabs.indices) {
                                                GeneralTabListPage(
                                                    forumId = forumInfo!!.get { id },
                                                    forumName = forumInfo!!.get { name },
                                                    navTabInfo = generalTabs[tabIndex],
                                                    viewModel = pageViewModel(key = "general_tab_$tabIndex"),
                                                )
                                            }
                                        }
                                    }
                                }
```

注意：`hasHotTab == false` 时 `hotIndex == latestIndex == 0`，`when` 的分支会命中 `hotIndex` 先声明的那个。**必须把 `hotIndex` 分支写成 `hasHotTab && pageIndex == hotIndex`**，否则无热门的吧会错误渲染热门页。修正后：

```kotlin
                                    when {
                                        hasHotTab && pageIndex == hotIndex -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = ForumThreadListType.Hot,
                                            lazyListState = hotListState
                                        )
                                        pageIndex == latestIndex -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = ForumThreadListType.Latest,
                                            lazyListState = latestListState
                                        )
                                        pageIndex == goodIndex -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = ForumThreadListType.Good,
                                            lazyListState = goodListState
                                        )
                                        else -> {
                                            val tabIndex = pageIndex - generalStart
                                            if (tabIndex in generalTabs.indices) {
                                                GeneralTabListPage(
                                                    forumId = forumInfo!!.get { id },
                                                    forumName = forumInfo!!.get { name },
                                                    navTabInfo = generalTabs[tabIndex],
                                                    viewModel = pageViewModel(key = "general_tab_$tabIndex"),
                                                )
                                            }
                                        }
                                    }
```

- [ ] **Step 10: 改骨架屏**

`LoadingPlaceholder`（`:1055`）增加参数 `hasHotTab: Boolean = false`，`:1092-1095` 的列表改为：

```kotlin
                (if (hasHotTab) persistentListOf(
                    stringResource(id = R.string.tab_forum_hot),
                    stringResource(id = R.string.tab_forum_latest),
                    stringResource(id = R.string.tab_forum_good),
                ) else persistentListOf(
                    stringResource(id = R.string.tab_forum_latest),
                    stringResource(id = R.string.tab_forum_good),
                )).fastForEach {
```

调用点 `:596` 的 `LoadingPlaceholder(forumName)` 保持默认值不变（骨架屏出现时 `navTabInfo` 通常尚未到达，不显示热门 Tab）。

- [ ] **Step 11: 编译确认**

```bash
cd "$WT" && "$GRADLE" :app:compileDebugKotlin 2>&1 | grep -E "^e:|error:" | head -20
```

Expected: 无输出（编译通过）。

- [ ] **Step 12: 提交**

```bash
cd "F:/a个人项目/第三方贴吧/FluxTie-main" && git add app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumPage.kt app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListPage.kt app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt && git -c user.name=xzy1 -c user.email=xzy1@local commit -m "feat(forum): 吧页新增热门标签页"
```

---

### Task 5: 字符串资源与全量构建

**Files:**
- Modify: `app/src/main/res/values/strings.xml:115-116`

**Interfaces:**
- Consumes: Task 4 引用的 `R.string.tab_forum_hot`
- Produces: 无

- [ ] **Step 1: 加字符串**

`strings.xml` 的 `<string name="tab_forum_good">精华</string>` 之前插入：

```xml
    <string name="tab_forum_hot">热门</string>
```

- [ ] **Step 2: 全量构建**

```bash
cd "$WT" && "$GRADLE" :app:assembleDebug 2>&1 | tail -25
```

Expected: `BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/debug/*.apk`。

- [ ] **Step 3: 跑全部单测**

```bash
cd "$WT" && "$GRADLE" :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`，`ForumThreadListTypeTest` 6 个测试通过。

- [ ] **Step 4: 提交**

```bash
cd "F:/a个人项目/第三方贴吧/FluxTie-main" && git add app/src/main/res/values/strings.xml && git -c user.name=xzy1 -c user.email=xzy1@local commit -m "feat(forum): 新增热门标签文案"
```

---

## 构建环境说明

本机原先没有 Android SDK 与 Gradle 分发，现已备好。**不要用 `./gradlew`**，用已解压的 Gradle 并显式设置四个环境变量。

在 worktree 根目录执行（把 `$WT` 换成实际 worktree 路径）：

```bash
export WT="F:/a个人项目/第三方贴吧/.worktrees/feature-forum-hot-tab"
export JAVA_HOME="C:/Program Files/Java/jdk-17.0.2"
export ANDROID_HOME="F:/a个人项目/第三方贴吧/.toolchain/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="F:/gradle-home"
export GRADLE="F:/a个人项目/第三方贴吧/.toolchain/gradle/gradle-9.7.1/bin/gradle.bat"

cd "$WT" && "$GRADLE" :app:compileDebugKotlin --no-daemon 2>&1 | tail -20
```

`local.properties` 必须存在于 **每个** worktree（它被 `.gitignore:26` 忽略，不会随 worktree 复制）：

```properties
sdk.dir=F\:\a个人项目\第三方贴吧\.toolchain\android-sdk
```

（即 `sdk.dir=F\:个人项目\第三方贴吧\.toolchainndroid-sdk`，Java properties 转义）

项目根需有 `local.properties`（已被 `.gitignore:26` 忽略）：

```properties
sdk.dir=F:\\a个人项目\\第三方贴吧\\.toolchain\\android-sdk
```

### 三个已踩过的坑（勿重复）

1. **`android.overridePathCheck=true` 必须留在 `gradle.properties`**：项目路径含中文，AGP 默认直接拒绝构建。
2. **`GRADLE_USER_HOME` 必须与项目同盘**：项目在 `F:`，缓存默认在 `C:\Users\xzy1\.gradle`，KSP 计算相对路径时跨盘抛
   `java.lang.IllegalArgumentException: this and base files have different roots: C:\...\jetified-okhttp3-integration-5.0.7-api.jar!\...\GlideIndexer_....class and F:\...\app`。
   把缓存搬到 `F:\gradle-home` 后 `:app:compileDebugKotlin` 通过（3m44s）。
   **注意根因不是中文路径**：曾用纯 ASCII 的 junction 路径 `F:\fluxtie-build` 构建，同样报这个错——跨盘才是根因。
3. **`JAVA_HOME` 必须是 `C:\Program Files\Java\jdk-17.0.2`**：`C:/Program Files/Java/jdk-17` 不存在，会报 `ERROR: JAVA_HOME is set to an invalid directory`。

Gradle 用 `F:/a个人项目/第三方贴吧/.toolchain/gradle/gradle-9.7.1/bin/gradle.bat`（已解压，无需下载）。

## 验证限制

`app/src/test` 只有 JVM 单测能力（JUnit 4，无 Robolectric、无 MockK、无 Turbine），因此：

- Task 1 的纯函数（`resolveSortType` / `resolveHasMore`）可被真实单测覆盖。
- Compose UI（TabRow、pager、落地页、下拉刷新）**无法自动测试**，只能靠 `assembleDebug` 通过 + 真机/模拟器手工验收。验收清单见 spec 的「验收标准」六条。
