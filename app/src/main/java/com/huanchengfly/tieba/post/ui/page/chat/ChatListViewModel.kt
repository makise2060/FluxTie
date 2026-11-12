package com.huanchengfly.tieba.post.ui.page.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed interface ChatListUiIntent : UiIntent {
    data object Refresh : ChatListUiIntent
}

sealed interface ChatListPartialChange : PartialChange<ChatListUiState> {
    data class Success(val sessions: List<ImSession>) : ChatListPartialChange {
        override fun reduce(oldState: ChatListUiState): ChatListUiState =
            oldState.copy(sessions = sessions.toImmutableList(), refreshing = false)
    }
}

@Immutable
data class ChatListUiState(
    val sessions: ImmutableList<ImSession> = persistentListOf(),
    val refreshing: Boolean = false,
) : UiState

@Stable
@HiltViewModel
class ChatListViewModel @Inject constructor() :
    BaseViewModel<ChatListUiIntent, ChatListPartialChange, ChatListUiState, Nothing>() {

    override fun createInitialState(): ChatListUiState = ChatListUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ChatListUiIntent, ChatListPartialChange, ChatListUiState> =
        ChatListPartialChangeProducer
}

/**
 * 一次 Refresh 订阅即持续收集 Room 流：新消息落库后列表实时刷新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
private object ChatListPartialChangeProducer :
    PartialChangeProducer<ChatListUiIntent, ChatListPartialChange, ChatListUiState> {

    override fun toPartialChangeFlow(intentFlow: Flow<ChatListUiIntent>): Flow<ChatListPartialChange> =
        intentFlow.flatMapLatest { intent ->
            when (intent) {
                ChatListUiIntent.Refresh -> produceRefreshPartialChange()
            }
        }

    private fun produceRefreshPartialChange(): Flow<ChatListPartialChange> = flow {
        val uid = AccountUtil.getLoginInfo()?.uid.orEmpty()
        DatabaseUtil.getImSessionFlow(uid).collect { sessions ->
            emit(ChatListPartialChange.Success(sessions))
        }
    }
}
