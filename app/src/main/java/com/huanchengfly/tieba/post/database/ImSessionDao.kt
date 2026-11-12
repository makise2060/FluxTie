package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.huanchengfly.tieba.post.models.database.ImSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ImSessionDao {
    @Query("SELECT * FROM im_session WHERE uid = :uid ORDER BY lastMsgTime DESC")
    fun getFlowByUid(uid: String): Flow<List<ImSession>>

    @Query("SELECT * FROM im_session WHERE uid = :uid AND peerUid = :peerUid LIMIT 1")
    suspend fun getByPeerUid(uid: String, peerUid: Long): ImSession?

    @Query("SELECT SUM(unreadCount) FROM im_session WHERE uid = :uid")
    fun getUnreadCountFlow(uid: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: ImSession): Long

    @Update
    suspend fun update(session: ImSession)

    @Query("UPDATE im_session SET unreadCount = 0 WHERE uid = :uid AND peerUid = :peerUid")
    suspend fun clearUnread(uid: String, peerUid: Long)

    @Query("DELETE FROM im_session WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    /** 会话不存在则插入，存在则按非零字段刷新（peer 信息与最新消息游标） */
    @Transaction
    suspend fun upsert(session: ImSession) {
        val existing = getByPeerUid(session.uid, session.peerUid) ?: run {
            insert(session)
            return
        }
        update(
            existing.copy(
                peerName = session.peerName.ifEmpty { existing.peerName },
                peerPortrait = session.peerPortrait.ifEmpty { existing.peerPortrait },
                lastMsgId = maxOf(existing.lastMsgId, session.lastMsgId),
                lastMsgTime = maxOf(existing.lastMsgTime, session.lastMsgTime),
                lastMsgPreview = session.lastMsgPreview.ifEmpty { existing.lastMsgPreview },
                lastMsgFromMe = session.lastMsgFromMe,
                peerReadMsgId = maxOf(existing.peerReadMsgId, session.peerReadMsgId),
            )
        )
    }
}
