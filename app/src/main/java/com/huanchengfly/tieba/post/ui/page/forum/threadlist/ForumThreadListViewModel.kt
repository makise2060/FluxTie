package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.protos.frsPage.Classify
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.models.protos.updateAgreeStatus
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.repository.FrsPageRepository
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.models.distinctById
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.math.min

abstract class ForumThreadListViewModel :
    BaseViewModel<ForumThreadListUiIntent, ForumThreadListPartialChange, ForumThreadListUiState, ForumThreadListUiEvent>() {
    override fun createInitialState(): ForumThreadListUiState = ForumThreadListUiState()

    override fun dispatchEvent(partialChange: ForumThreadListPartialChange): UiEvent? =
        when (partialChange) {
            is ForumThreadListPartialChange.FirstLoad.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is ForumThreadListPartialChange.Refresh.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is ForumThreadListPartialChange.LoadMore.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is ForumThreadListPartialChange.Agree.Failure -> {
                ForumThreadListUiEvent.AgreeFail(
                    partialChange.threadId,
                    partialChange.postId,
                    partialChange.hasAgree,
                    partialChange.error.getErrorCode(),
                    partialChange.error.getErrorMessage()
                )
            }

            else -> null
        }
}

const val HOT_SORT_TYPE = 3

enum class ForumThreadListType {
    Hot, Latest, Good
}

internal fun ForumThreadListType.resolveSortType(userSortType: Int): Int = when (this) {
    ForumThreadListType.Hot -> HOT_SORT_TYPE
    ForumThreadListType.Latest -> userSortType
    ForumThreadListType.Good -> -1
}

/**
 * 计算某一页列表的 `hasMore`。
 *
 * `Hot -> false` 是刻意为之，不是漏写，**不要“修正”成 [serverHasMore]**：
 * 热门排序（`sort_type = 3`）在服务端只有约两页有效数据，`pn = 3` 与 `pn = 2` 返回的 30 条完全相同、
 * 新增 id 为 0。若在这里透传服务端的 `has_more == 1`，热门页就会在第二、三页之间原地打转，
 * 无限下拉出重复内容。产品上也已确定「热门只加载第一页 + 下拉刷新」。
 */
internal fun ForumThreadListType.resolveHasMore(serverHasMore: Boolean): Boolean = when (this) {
    ForumThreadListType.Hot -> false
    ForumThreadListType.Latest, ForumThreadListType.Good -> serverHasMore
}

@Stable
@HiltViewModel
class LatestThreadListViewModel @Inject constructor() : ForumThreadListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<ForumThreadListUiIntent, ForumThreadListPartialChange, ForumThreadListUiState> =
        ForumThreadListPartialChangeProducer(ForumThreadListType.Latest)
}

@Stable
@HiltViewModel
class GoodThreadListViewModel @Inject constructor() : ForumThreadListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<ForumThreadListUiIntent, ForumThreadListPartialChange, ForumThreadListUiState> =
        ForumThreadListPartialChangeProducer(ForumThreadListType.Good)
}

@Stable
@HiltViewModel
class HotThreadListViewModel @Inject constructor() : ForumThreadListViewModel() {
    /**
     * 热门列表从**初始状态**起就必须是 `hasMore = false`，不能只靠 [resolveHasMore] 在响应到达时兜底。
     *
     * 基类默认 `hasMore = true`；而首屏加载走的是 `isRefreshing`，`isLoadingMore` 在整个首屏 RTT 内恒为 false。
     * 于是 `LoadMoreLayout` 的 `canLoadMore = enableLoadMore && !loadEnd` 在首屏期间为 true，
     * 用户此时上拉会真的发出 `LoadMore`，把 `pn = 2` 的数据拉进来 —— 那一页在服务端是真实有效的，
     * 结果就是热门列表变长，违反「热门只加载第一页」的约束。
     *
     * 覆写初始状态直接把这个窗口关掉：状态一出生就到底，预加载与上拉两个触发点都被 `!loadEnd` 挡死。
     */
    override fun createInitialState(): ForumThreadListUiState =
        ForumThreadListUiState(hasMore = false)

    override fun createPartialChangeProducer(): PartialChangeProducer<ForumThreadListUiIntent, ForumThreadListPartialChange, ForumThreadListUiState> =
        ForumThreadListPartialChangeProducer(ForumThreadListType.Hot)
}

private class ForumThreadListPartialChangeProducer(val type: ForumThreadListType) :
    PartialChangeProducer<ForumThreadListUiIntent, ForumThreadListPartialChange, ForumThreadListUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<ForumThreadListUiIntent>): Flow<ForumThreadListPartialChange> =
        merge(
            intentFlow.filterIsInstance<ForumThreadListUiIntent.FirstLoad>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<ForumThreadListUiIntent.Refresh>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<ForumThreadListUiIntent.LoadMore>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<ForumThreadListUiIntent.Agree>()
                .flatMapConcat { it.producePartialChange() },
        )

    private fun ForumThreadListUiIntent.FirstLoad.producePartialChange() =
        FrsPageRepository.frsPage(
            forumName,
            1,
            1,
            type.resolveSortType(sortType),
            goodClassifyId.takeIf { type == ForumThreadListType.Good }
        )
            .map<FrsPageResponse, ForumThreadListPartialChange.FirstLoad> { response ->
                if (response.data_?.page == null) throw TiebaUnknownException
                val threadList =
                    response.data_.thread_list.map { ThreadItemData(it.wrapImmutable()) }
                ForumThreadListPartialChange.FirstLoad.Success(
                    response.data_.forum_rule?.title.takeIf {
                        type == ForumThreadListType.Latest && response.data_.forum_rule?.has_forum_rule == 1
                    },
                    threadList,
                    response.data_.thread_id_list,
                    (response.data_.forum?.good_classify ?: emptyList()).wrapImmutable(),
                    goodClassifyId.takeIf { type == ForumThreadListType.Good },
                    type.resolveHasMore(response.data_.page.has_more == 1)
                )
            }
            .onStart { emit(ForumThreadListPartialChange.FirstLoad.Start) }
            .catch { emit(ForumThreadListPartialChange.FirstLoad.Failure(it)) }

    private fun ForumThreadListUiIntent.Refresh.producePartialChange() =
        FrsPageRepository.frsPage(
            forumName,
            1,
            1,
            type.resolveSortType(sortType),
            goodClassifyId.takeIf { type == ForumThreadListType.Good },
            forceNew = true
        )
            .map<FrsPageResponse, ForumThreadListPartialChange.Refresh> { response ->
                if (response.data_?.page == null) throw TiebaUnknownException
                val threadList =
                    response.data_.thread_list.map { ThreadItemData(it.wrapImmutable()) }
                ForumThreadListPartialChange.Refresh.Success(
                    threadList,
                    response.data_.thread_id_list,
                    (response.data_.forum?.good_classify ?: emptyList()).wrapImmutable(),
                    goodClassifyId.takeIf { type == ForumThreadListType.Good },
                    type.resolveHasMore(response.data_.page.has_more == 1)
                )
            }
            .onStart { emit(ForumThreadListPartialChange.Refresh.Start) }
            .catch { emit(ForumThreadListPartialChange.Refresh.Failure(it)) }

    private fun ForumThreadListUiIntent.LoadMore.producePartialChange(): Flow<ForumThreadListPartialChange.LoadMore> {
        val flow = if (threadListIds.isNotEmpty()) {
            val size = min(threadListIds.size, 30)
            FrsPageRepository.threadList(
                forumId,
                forumName,
                currentPage,
                sortType,
                threadListIds.subList(0, size).joinToString(separator = ",") { "$it" }
            ).map { response ->
                if (response.data_ == null) throw TiebaUnknownException
                val threadList =
                    response.data_.thread_list.map { ThreadItemData(it.wrapImmutable()) }
                ForumThreadListPartialChange.LoadMore.Success(
                    threadList = threadList,
                    threadListIds = threadListIds.drop(size),
                    currentPage = currentPage,
                    hasMore = response.data_.thread_list.isNotEmpty()
                )
            }
        } else {
            FrsPageRepository.frsPage(
                forumName,
                currentPage + 1,
                2,
                type.resolveSortType(sortType),
                goodClassifyId.takeIf { type == ForumThreadListType.Good }
            )
                .map<FrsPageResponse, ForumThreadListPartialChange.LoadMore> { response ->
                    if (response.data_?.page == null) throw TiebaUnknownException
                    val threadList =
                        response.data_.thread_list.map { ThreadItemData(it.wrapImmutable()) }
                    ForumThreadListPartialChange.LoadMore.Success(
                        threadList = threadList,
                        threadListIds = response.data_.thread_id_list,
                        currentPage = currentPage + 1,
                        type.resolveHasMore(response.data_.page.has_more == 1)
                    )
                }
        }
        return flow
            .onStart { emit(ForumThreadListPartialChange.LoadMore.Start) }
            .catch { emit(ForumThreadListPartialChange.LoadMore.Failure(it)) }
    }

    private fun ForumThreadListUiIntent.Agree.producePartialChange(): Flow<ForumThreadListPartialChange.Agree> =
        TiebaApi.getInstance().opAgreeFlow(
            threadId.toString(),
            postId.toString(),
            hasAgree,
            objType = 3
        ).map<AgreeBean, ForumThreadListPartialChange.Agree> {
            ForumThreadListPartialChange.Agree.Success(
                threadId,
                hasAgree xor 1
            )
        }
            .catch {
                emit(
                    ForumThreadListPartialChange.Agree.Failure(
                        threadId,
                        postId,
                        hasAgree,
                        it
                    )
                )
            }
            .onStart { emit(ForumThreadListPartialChange.Agree.Start(threadId, hasAgree xor 1)) }
}

sealed interface ForumThreadListUiIntent : UiIntent {
    data class FirstLoad(
        val forumName: String,
        val sortType: Int = -1,
        val goodClassifyId: Int? = null,
    ) : ForumThreadListUiIntent

    data class Refresh(
        val forumName: String,
        val sortType: Int = -1,
        val goodClassifyId: Int? = null,
    ) : ForumThreadListUiIntent

    data class LoadMore(
        val forumId: Long,
        val forumName: String,
        val currentPage: Int,
        val threadListIds: List<Long>,
        val sortType: Int = -1,
        val goodClassifyId: Int? = null,
    ) : ForumThreadListUiIntent

    data class Agree(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Int
    ) : ForumThreadListUiIntent
}

sealed interface ForumThreadListPartialChange : PartialChange<ForumThreadListUiState> {
    sealed class FirstLoad : ForumThreadListPartialChange {
        override fun reduce(oldState: ForumThreadListUiState): ForumThreadListUiState =
            when (this) {
                Start -> oldState
                is Success -> oldState.copy(
                    isRefreshing = false,
                    forumRuleTitle = forumRuleTitle,
                    threadList = threadList.distinctById(),
                    threadListIds = threadListIds.toImmutableList(),
                    goodClassifies = goodClassifies.toImmutableList(),
                    goodClassifyId = goodClassifyId,
                    currentPage = 1,
                    hasMore = hasMore
                )

                is Failure -> oldState.copy(isRefreshing = false)
            }

        data object Start : FirstLoad()

        data class Success(
            val forumRuleTitle: String?,
            val threadList: List<ThreadItemData>,
            val threadListIds: List<Long>,
            val goodClassifies: List<ImmutableHolder<Classify>>,
            val goodClassifyId: Int?,
            val hasMore: Boolean,
        ) : FirstLoad()

        data class Failure(
            val error: Throwable
        ) : FirstLoad()
    }

    sealed class Refresh : ForumThreadListPartialChange {
        override fun reduce(oldState: ForumThreadListUiState): ForumThreadListUiState =
            when (this) {
                Start -> oldState.copy(isRefreshing = true)
                is Success -> oldState.copy(
                    isRefreshing = false,
                    threadList = threadList.distinctById(),
                    threadListIds = threadListIds.toImmutableList(),
                    goodClassifies = goodClassifies.toImmutableList(),
                    goodClassifyId = goodClassifyId,
                    currentPage = 1,
                    hasMore = hasMore
                )

                is Failure -> oldState.copy(isRefreshing = false)
            }

        data object Start : Refresh()

        data class Success(
            val threadList: List<ThreadItemData>,
            val threadListIds: List<Long>,
            val goodClassifies: List<ImmutableHolder<Classify>>,
            val goodClassifyId: Int? = null,
            val hasMore: Boolean,
        ) : Refresh()

        data class Failure(
            val error: Throwable
        ) : Refresh()
    }

    sealed class LoadMore : ForumThreadListPartialChange {
        override fun reduce(oldState: ForumThreadListUiState): ForumThreadListUiState =
            when (this) {
                Start -> oldState.copy(isLoadingMore = true)
                is Success -> oldState.copy(
                    isLoadingMore = false,
                    threadList = (oldState.threadList + threadList).distinctById(),
                    threadListIds = threadListIds.toImmutableList(),
                    currentPage = currentPage,
                    hasMore = hasMore
                )

                is Failure -> oldState.copy(isLoadingMore = false)
            }

        data object Start : LoadMore()

        data class Success(
            val threadList: List<ThreadItemData>,
            val threadListIds: List<Long>,
            val currentPage: Int,
            val hasMore: Boolean,
        ) : LoadMore()

        data class Failure(
            val error: Throwable
        ) : LoadMore()
    }

    sealed class Agree private constructor() : ForumThreadListPartialChange {
        private fun List<ThreadItemData>.updateAgreeStatus(
            threadId: Long,
            hasAgree: Int,
        ): ImmutableList<ThreadItemData> {
            return map { data ->
                val (thread) = data
                if (thread.get { id } == threadId) {
                    ThreadItemData(thread.getImmutable { updateAgreeStatus(hasAgree) })
                } else data
            }.toImmutableList()
        }

        override fun reduce(oldState: ForumThreadListUiState): ForumThreadListUiState =
            when (this) {
                is Start -> {
                    oldState.copy(
                        threadList = oldState.threadList.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }

                is Success -> {
                    oldState.copy(
                        threadList = oldState.threadList.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }

                is Failure -> {
                    oldState.copy(
                        threadList = oldState.threadList.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }
            }

        data class Start(
            val threadId: Long,
            val hasAgree: Int
        ) : Agree()

        data class Success(
            val threadId: Long,
            val hasAgree: Int
        ) : Agree()

        data class Failure(
            val threadId: Long,
            val postId: Long,
            val hasAgree: Int,
            val error: Throwable
        ) : Agree()
    }
}

data class ForumThreadListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val goodClassifyId: Int? = null,
    val forumRuleTitle: String? = null,
    val threadList: ImmutableList<ThreadItemData> = persistentListOf(),
    val threadListIds: ImmutableList<Long> = persistentListOf(),
    val goodClassifies: ImmutableList<ImmutableHolder<Classify>> = persistentListOf(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
) : UiState

sealed interface ForumThreadListUiEvent : UiEvent {
    data class AgreeFail(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Int,
        val errorCode: Int,
        val errorMsg: String
    ) : ForumThreadListUiEvent

    data class Refresh(
        val type: ForumThreadListType,
        val sortType: Int
    ) : ForumThreadListUiEvent

    data class BackToTop(
        val type: ForumThreadListType
    ) : ForumThreadListUiEvent

    data class AddThread(
        val forumName: String,
    ):ForumThreadListUiEvent
}