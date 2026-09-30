package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.models.protos.addThread.AddThreadResponse
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object AddPostRepository {
    fun addThread(
        content: String,
        forumId: Long,
        forumName: String,
        title: String? = "",
        isHide: Int? = 1,
        isTitle: Int? = 1
    ): Flow<AddThreadResponse> =
        TiebaApi.getInstance()
            .addThreadFlow(
                content,
                forumName,
                forumId.toString(),
                title.orEmpty(),
                requireNotNull(isHide),
                requireNotNull(isTitle)
            ).onEach {
                val data = it.data_ ?: return@onEach
                GlobalScope.launch {
                    val threadId = data.tid.toLongOrNull() ?: return@launch
                    val postId = data.pid.toLongOrNull() ?: return@launch
                    val msg = data.toast?.content
                        ?.joinToString("") { it.text }
                        ?.takeIf { it.isNotEmpty() }
                        ?: data.msg
                    emitGlobalEvent(GlobalEvent.AddThreadSuccess(threadId, postId, msg))
                }
            }

    fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String? = null,
        nameShow: String? = null,
        postId: Long? = null,
        subPostId: Long? = null,
        replyUserId: Long? = null,
    ): Flow<AddPostResponse> =
        TiebaApi.getInstance()
            .addPostFlow(
                content,
                forumId.toString(),
                forumName,
                threadId.toString(),
                tbs,
                nameShow,
                postId?.toString(),
                subPostId?.toString(),
                replyUserId?.toString()
            )
            .onEach {
                val newPostId = checkNotNull(it.data_?.pid?.toLongOrNull())
                GlobalScope.launch {
                    if (postId != null) {
                        emitGlobalEvent(
                            GlobalEvent.ReplySuccess(
                                threadId,
                                postId,
                                postId,
                                subPostId,
                                newPostId
                            )
                        )
                    } else {
                        emitGlobalEvent(GlobalEvent.ReplySuccess(threadId, newPostId))
                    }
                }
            }
}