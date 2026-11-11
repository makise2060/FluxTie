package com.huanchengfly.tieba.post.ui.page.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.im.ImManager
import com.huanchengfly.tieba.post.im.ImStatus
import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

sealed interface ChatUiIntent : UiIntent {
    /** 进入聊天页：订阅消息流 + 增量拉取 + 标记已读 */
    data class Load(val peerUid: Long) : ChatUiIntent

    data class Send(val peerUid: Long, val content: String) : ChatUiIntent
}

sealed interface ChatPartialChange : PartialChange<ChatUiState> {
    data class MessagesLoaded(val messages: List<ImMessage>) : ChatPartialChange {
        override fun reduce(oldState: ChatUiState): ChatUiState =
            oldState.copy(messages = messages.toImmutableList())
    }

    data class SessionLoaded(val session: ImSession?) : ChatPartialChange {
        override fun reduce(oldState: ChatUiState): ChatUiState = oldState.copy(session = session)
    }

    data class Sending(val sending: Boolean) : ChatPartialChange {
        override fun reduce(oldState: ChatUiState): ChatUiState = oldState.copy(sending = sending)
    }

    data class SendFailed(val error: Exception) : ChatPartialChange {
        override fun reduce(oldState: ChatUiState): ChatUiState =
            oldState.copy(sending = false)
    }
}

sealed interface ChatUiEvent : UiEvent {
    data class SendFailed(val message: String) : ChatUiEvent
}

@Immutable
data class ChatUiState(
    val messages: ImmutableList<ImMessage> = persistentListOf(),
    val session: ImSession? = null,
    val sending: Boolean = false,
) : UiState

@Stable
@HiltViewModel
class ChatViewModel @Inject constructor() :
    BaseViewModel<ChatUiIntent, ChatPartialChange, ChatUiState, ChatUiEvent>() {

    override fun createInitialState(): ChatUiState = ChatUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ChatUiIntent, ChatPartialChange, ChatUiState> =
        ChatPartialChangeProducer

    override fun dispatchEvent(partialChange: ChatPartialChange): UiEvent? =
        when (partialChange) {
            is ChatPartialChange.SendFailed -> ChatUiEvent.SendFailed(
                partialChange.error.message ?: partialChange.error.toString()
            )

            else -> null
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private object ChatPartialChangeProducer :
    PartialChangeProducer<ChatUiIntent, ChatPartialChange, ChatUiState> {

    override fun toPartialChangeFlow(intentFlow: Flow<ChatUiIntent>): Flow<ChatPartialChange> =
        merge(
            // Load 内的 Room 流常驻收集，Send 不会打断实时刷新
            intentFlow.filterIsInstance<ChatUiIntent.Load>().flatMapLatest { produceLoadPartialChange(it.peerUid) },
            intentFlow.filterIsInstance<ChatUiIntent.Send>().flatMapConcat { produceSendPartialChange(it.peerUid, it.content) },
        )

    /**
     * Load：订阅 Room 消息流（实时刷新），进入时增量拉取一次并标记已读。
     */
    private fun produceLoadPartialChange(peerUid: Long): Flow<ChatPartialChange> = flow {
        val uid = AccountUtil.getLoginInfo()?.uid.orEmpty()
        DatabaseUtil.getImSessionFlow(uid).first().firstOrNull { it.peerUid == peerUid }?.let {
            emit(ChatPartialChange.SessionLoaded(it))
        }
        DatabaseUtil.getImMessageFlow(uid, peerUid).collect { messages ->
            emit(ChatPartialChange.MessagesLoaded(messages))
            runCatching { ImManager.pullMessages() }
            messages.lastOrNull()?.let { last -> ImManager.markRead(peerUid, last.msgId) }
        }
    }

    private fun produceSendPartialChange(peerUid: Long, content: String): Flow<ChatPartialChange> = flow {
        emit(ChatPartialChange.Sending(true))
        try {
            if (ImManager.imStatus.value != ImStatus.Ready) {
                throw IllegalStateException("私信服务未连接")
            }
            ImManager.sendMessage(peerUid, content)
        } catch (e: Exception) {
            emit(ChatPartialChange.SendFailed(e))
        } finally {
            emit(ChatPartialChange.Sending(false))
        }
    }
}
