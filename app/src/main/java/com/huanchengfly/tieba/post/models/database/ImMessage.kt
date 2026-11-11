package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * IM 消息（1v1 私信文本，一期）。msgId 为服务端分配的全局递增 id。
 */
@Immutable
@Entity(
    tableName = "im_message",
    indices = [Index(value = ["uid", "peerUid", "msgId"], unique = true)],
)
data class ImMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 本账号 uid（多账号隔离） */
    val uid: String = "",
    /** 归属会话的对端 uid */
    val peerUid: Long = 0L,
    /** 所属消息组 id（1v1 私信共用 priv 组） */
    val groupId: Long = 0L,
    val msgId: Long = 0L,
    val msgType: Int = 0,
    val content: String = "",
    /** 发送时间（秒级时间戳） */
    val createTime: Long = 0L,
    val fromUid: Long = 0L,
    val isMine: Boolean = false,
)
