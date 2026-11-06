# 吧页「热门」标签设计

日期：2026-10-29
状态：待实现

## 背景

FluxTie 的吧页（`ForumPage`）只有「最新」「精华」两栏，加上服务端下发的通用 tab（`tab_type == 15`）。原版贴吧在部分吧（如抗压背锅吧）的顶部还有一栏「热门」，本项目缺失。

## 根因

`app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumPage.kt:454` 的 tab 过滤条件：

```kotlin
navTabInfo?.tab?.filter { it.isGeneralTab == 1 }?.filter { it.tabType == 15 } ?: emptyList()
```

服务端下发的「热门」是 `tabType = 13`、`isGeneralTab = 0`，被这个条件整排过滤掉，因此从未被渲染。

## 验证结论（实测数据）

通过匿名 JSON 接口（`/c/f/frs/page`，客户端版本 `12.52.1.0`，MD5 签名）对多个吧实测：

| 吧 | 有热门 tab | 首个 tab |
| --- | --- | --- |
| 抗压背锅吧 | 是 | 热门 (tab_type=13, tab_id=1) |
| 孙笑川吧 | 是 | 热门 |
| 足球吧 | 是 | 热门 |
| 王者荣耀吧 | 是 | 热门 |
| 李毅吧 | 否 | 最新 (tab_type=14, tab_id=1) |
| 原神吧 | 否 | 最新 |
| 崩坏星穹铁道吧 | 否 | 最新 |

关键事实：

1. 热门 tab 在 `nav_tab_info.tab` 中，特征为 `tabType == 13`、`tabName == "热门"`、`netTabType == 2`、`tabId == 1`。
2. 热门数据不需要新接口，复用 `/c/f/frs/page`，参数 `sort_type = 3`。
3. 排序确实不同：`sort_type=3` 的帖子中位回复数约 219，`sort_type=0`（最新）约 158。
4. `frs_tab_default` 与 `page_data_tab_id` 在所有吧恒为 1（包括没有热门 tab 的吧），**不能作为"该落热门"的判据**。
5. 热门分页只有约 2 页：`pn=1` 返回 15 条、`pn=2` 返回 30 条、`pn=3` 与 `pn=2` **完全重复**。不能沿用无限下拉。

## 决策

| 决策点 | 结论 |
| --- | --- |
| Tab 位置 | 最前面，作为首屏 |
| 分页策略 | 只加载第 1 页 + 下拉刷新，禁用上拉加载 |
| 默认落地页 | 有热门 tab 的吧落热门页；没有的落最新页（判据用 `hasHot`，不用恒真的 `frs_tab_default`） |

## 实现方案

### 1. 数据来源

不新增接口、不新增 proto、不改 `FrsPageRepository`。

```kotlin
val hotTab = navTabInfo?.tab?.firstOrNull { it.tabType == 13 }
val hasHot = hotTab != null
```

数据复用现有 `FrsPageRepository.frsPage(forumName, page, loadType, sortType = 3, goodClassifyId = null)`。

### 2. 页序重构

现有代码硬编码 `2 + generalTabs.size`，插入热门页后改为偏移量推导：

```kotlin
val hotIndex = 0
val latestIndex = if (hasHot) 1 else 0
val goodIndex = latestIndex + 1
val generalStart = goodIndex + 1
val pageCount = generalStart + generalTabs.size
```

需要同步替换的硬编码位置（`ForumPage.kt`）：

| 行号 | 现有代码 | 改为 |
| --- | --- | --- |
| 459 | `rememberPagerState(pageCount = { 2 + generalTabs.size })` | `pageCount` |
| 465 | `if (currentPage == 0) latestListState else goodListState` | 按索引映射三态 |
| 495 | `currentPage < 2 && ...` | `currentPage < generalStart && ...` |
| 541-560 | `if (currentPage >= 2)` | `if (currentPage >= generalStart)` |
| 638-660 | FAB refresh / back_to_top 的 `currentPage >= 2` | 同上 |
| 900-1020 | TabRow 三个 `selected = currentPage == N` | 用索引常量 |
| 1024-1045 | `when (pageIndex) { 0, 1 -> ...; else -> tabIndex = pageIndex - 2 }` | 三态 + `pageIndex - generalStart` |

`currentListState` 需要新增 `hotListState`（`rememberLazyListState()`）。

### 3. ViewModel 扩展

`ForumThreadListViewModel.kt`：

```kotlin
enum class ForumThreadListType { Hot, Latest, Good }
```

三处 `sortType` 解析（`FirstLoad` / `Refresh` / `LoadMore`）从：

```kotlin
sortType.takeIf { type == ForumThreadListType.Latest } ?: -1
```

改为：

```kotlin
when (type) {
    ForumThreadListType.Latest -> sortType
    ForumThreadListType.Good -> -1
    ForumThreadListType.Hot -> 3
}
```

新增 `HotThreadListViewModel`（`@HiltViewModel`），与现有两个并列。

`Hot` 类型强制 `hasMore = false`：在 `FirstLoad.Success` / `Refresh.Success` 里，`type == Hot` 时覆盖服务端返回的 `page.has_more`。

### 4. 事件模型

`ForumThreadListUiEvent` 的 `Refresh` / `BackToTop` 把 `isGood: Boolean` 改为 `type: ForumThreadListType`，让三个页面各自过滤：

```kotlin
data class Refresh(val type: ForumThreadListType, val sortType: Int)
data class BackToTop(val type: ForumThreadListType)
```

`ForumThreadListPage` 入口参数从 `isGood: Boolean = false` 改为 `type: ForumThreadListType = ForumThreadListType.Latest`；内部 `isGood` 变为 `type == Good` 的派生值，精华分类、`goodClassifyId` 等逻辑保持不变。

`getFirstLoadIntent` / `getRefreshIntent` / `getLoadMoreIntent` 三个私有函数同步改为按 `type` 分支。

### 5. 默认落地页

```kotlin
val initialPage = if (hasHot) hotIndex else latestIndex
```

在 `pagerState` 初始化时使用（`rememberPagerState(initialPage = initialPage)`）。注意 `hasHot` 依赖 `navTabInfo` 异步到达，需在 `navTabInfo` 首次非空时校正一次落地页，且只在用户尚未手动切页时校正。

### 6. 字符串与骨架屏

- `app/src/main/res/values/strings.xml` 增加 `<string name="tab_forum_hot">热门</string>`，位置紧邻 `tab_forum_latest` / `tab_forum_good`。
- `ForumPage.kt` 的 `LoadingPlaceholder` 骨架屏当前写死两个 tab（`tab_forum_latest` / `tab_forum_good`），有热门时显示三个。

## 影响面

| 文件 | 改动性质 |
| --- | --- |
| `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumPage.kt` | 页序索引重构、TabRow、pager、骨架屏 |
| `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListViewModel.kt` | 枚举加 Hot、sortType 解析、hasMore、新 ViewModel |
| `app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/threadlist/ForumThreadListPage.kt` | 参数 `isGood` → `type` |
| `app/src/main/res/values/strings.xml` | 一条字符串 |

不改动：`FrsPageRepository`、`api/*`、`app/src/main/protos/*`。

## 风险与对策

| 风险 | 对策 |
| --- | --- |
| 热门分页重复（`pn=3` 返回 `pn=2` 内容） | `Hot` 类型强制 `hasMore = false`，只加载第 1 页 |
| `navTabInfo` 异步到达导致落地页抖动 | 仅在 `navTabInfo` 首次非空且用户未手动切页时校正一次 |
| 无热门的吧出现空页 | `hasHot` 为 false 时不注册热门页，页数计算自动退化 |
| 事件过滤失效导致三页串台 | 事件携带 `type` 而非 `isGood`，精确匹配 |

## 验收标准

1. 进入抗压背锅吧，首屏为「热门」页，TabRow 显示「热门 / 最新 / 精华 / ...」。
2. 进入李毅吧（无热门 tab），首屏为「最新」页，TabRow 不出现「热门」。
3. 热门页下拉可刷新；上拉不触发加载更多（`hasMore` 为 false）。
4. 热门页点赞、进帖、返回、FAB 刷新、回到顶部均正常，且不影响「最新」「精华」页。
5. 「精华」页的分类 chip 切换仍正常。
6. `./gradlew assembleDebug` 构建通过。
