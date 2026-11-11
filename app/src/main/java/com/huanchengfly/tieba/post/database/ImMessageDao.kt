package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.huanchengfly.tieba.post.models.database.ImMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ImMessageDao {
    @Query("SELECT * FROM im_message WHERE uid = :uid AND peerUid = :peerUid ORDER BY createTime ASC, msgId ASC")
    fun getFlowByPeer(uid: String, peerUid: Long): Flow<List<ImMessage>>

    @Query("SELECT * FROM im_message WHERE uid = :uid AND peerUid = :peerUid ORDER BY createTime DESC, msgId DESC LIMIT 1")
    suspend fun getLatestByPeer(uid: String, peerUid: Long): ImMessage?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<ImMessage>)

    @Query("DELETE FROM im_message WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}
