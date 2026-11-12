package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * IM 会话（1v1 私信按对端用户划分视图，底层共用同一个 priv 消息组）。
 */
@Immutable
@Entity(
    tableName = "im_session",
    indices = [Index(value = ["uid", "peerUid"], unique = true)],
)
data class ImSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 本账号 uid（多账号隔离） */
    val uid: String = "",
    /** 对端用户 uid */
    val peerUid: Long = 0L,
    val peerName: String = "",
    val peerPortrait: String = "",
    /** 该会话最新消息 id（本地游标） */
    val lastMsgId: Long = 0L,
    val lastMsgTime: Long = 0L,
    val lastMsgPreview: String = "",
    val lastMsgFromMe: Boolean = false,
    val unreadCount: Int = 0,
    /** 对端已读回执指向的 msgId */
    val peerReadMsgId: Long = 0L,
)
