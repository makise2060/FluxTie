package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.api.models.protos.im.CommitPersonalMsgReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.CommitPersonalMsgResIdl
import com.huanchengfly.tieba.post.api.models.protos.im.CommitReceivedPmsgReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.CommitReceivedPmsgResIdl
import com.huanchengfly.tieba.post.api.models.protos.im.GetGroupMsgReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.GetGroupMsgResIdl
import com.huanchengfly.tieba.post.api.models.protos.im.ImError
import com.huanchengfly.tieba.post.api.models.protos.im.ImUserInfo
import com.huanchengfly.tieba.post.api.models.protos.im.PushNotifyResIdl
import com.huanchengfly.tieba.post.api.models.protos.im.UpdateClientInfoReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.UpdateClientInfoResIdl
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.IOException

/** IM 业务异常（携带服务端 errorno/errmsg 或 blockInfo 屏蔽提示） */
class ImException(
    val code: Long,
    message: String,
) : IOException("IM 错误[$code]: $message")

/** 消息组会话信息 */
data class ImGroupInfo(
    val groupId: Long,
    val groupType: Int,
    val lastMsgId: Long,
)

/** 拉取到的一条消息 */
data class ImPulledMessage(
    val msgId: Long,
    val msgType: Int,
    val content: String,
    val createTime: Long,
    val fromUid: Long,
    val fromName: String,
    val fromPortrait: String,
    /** 收件人 uid；官方 proto 中该字段未见于公开资料，按字段位推断（见联调验证点） */
    val toUid: Long,
)

/** 一次拉取结果：组 id → 新消息列表 */
data class ImPullResult(
    val groupId: Long,
    val messages: List<ImPulledMessage>,
)

/**
 * IM 指令层：把 CMD 请求打包成 protobuf 字节、解析响应并检查 errorno。
 * 与 [ImConnection] 配合使用：`connection.request(CMD, ImApi.buildXxx(...))`。
 */
object ImApi {
    // ── CMD 1001 上线鉴权 ─────────────────────────────────────────

    fun buildUpdateClientInfo(
        bduss: String,
        stoken: String,
        cuid: String,
        cuidGalaxy2: String,
        secKey: ByteArray,
    ): ByteArray {
        val device = JSONObject().apply {
            put("cuid", cuid)
            put("_client_version", ImConstants.CLIENT_VERSION)
            put("_msg_status", "1")
            put("cuid_galaxy2", cuidGalaxy2)
            put("_client_type", "2")
            put("timestamp", System.currentTimeMillis().toString())
        }
        val req = UpdateClientInfoReqIdl(
            cuid = "$cuid|${ImConstants.CUID_APP_ID}${ImConstants.CLIENT_VERSION}",
            data_ = UpdateClientInfoReqIdl.DataReq(
                bduss = bduss,
                device = device.toString(),
                secretKey = ImCrypto.wrapSecKey(secKey).toByteString(),
                stoken = stoken,
            ),
        )
        return req.encode()
    }

    /** 解析鉴权响应，返回消息组列表（错误时抛 [ImException]） */
    fun parseUpdateClientInfo(body: ByteArray): List<ImGroupInfo> {
        val res = UpdateClientInfoResIdl.ADAPTER.decode(body)
        checkError(res.error)
        return res.data_?.groupInfo.orEmpty().map {
            ImGroupInfo(groupId = it.groupId, groupType = it.groupType, lastMsgId = it.lastMsgId)
        }
    }

    // ── CMD 202003 拉取消息 ───────────────────────────────────────

    fun buildGetGroupMsg(cuid: String, groupMids: List<Pair<Long, Long>>): ByteArray {
        val req = GetGroupMsgReqIdl(
            cuid = "$cuid|${ImConstants.CUID_APP_ID}${ImConstants.CLIENT_VERSION}",
            data_ = GetGroupMsgReqIdl.DataReq(
                groupMids = groupMids.map { (gid, lastId) ->
                    GetGroupMsgReqIdl.GroupMid(groupId = gid, lastMsgId = lastId)
                },
                gettype = ImConstants.GET_TYPE_DEFAULT.toString(),
            ),
        )
        return req.encode()
    }

    fun parseGetGroupMsg(body: ByteArray): List<ImPullResult> {
        val res = GetGroupMsgResIdl.ADAPTER.decode(body)
        checkError(res.error)
        return res.data_?.groupInfo.orEmpty().map { group ->
            ImPullResult(
                groupId = group.groupInfo?.groupId ?: 0L,
                messages = group.msgList.orEmpty().map { msg -> msg.toPulledMessage() },
            )
        }
    }

    // ── CMD 202006 推送 ───────────────────────────────────────────

    /** 解析服务端推送，返回 (groupId, msgId, groupType) 列表 */
    fun parsePushNotify(body: ByteArray): List<Triple<Long, Long, Int>> =
        PushNotifyResIdl.ADAPTER.decode(body).multiMsg.orEmpty().mapNotNull { push ->
            val data = push.data_ ?: return@mapNotNull null
            Triple(data.groupId, data.msgId, data.groupType)
        }

    // ── CMD 205001 发送私信 ───────────────────────────────────────

    fun buildCommitPersonalMsg(toUid: Long, content: String, recordId: Long): ByteArray {
        val req = CommitPersonalMsgReqIdl(
            data_ = CommitPersonalMsgReqIdl.DataReq(
                toUid = toUid,
                msgType = ImConstants.MSG_TYPE_TEXT,
                content = content,
                recordId = recordId,
            ),
        )
        return req.encode()
    }

    /** 解析发送响应，返回服务端分配的 msgId（被屏蔽时抛 [ImException]） */
    fun parseCommitPersonalMsg(body: ByteArray): Long {
        val res = CommitPersonalMsgResIdl.ADAPTER.decode(body)
        checkError(res.error)
        val blockInfo = res.data_?.blockInfo
        if (blockInfo != null && blockInfo.blockErrno != 0) {
            throw ImException(blockInfo.blockErrno.toLong(), blockInfo.blockErrmsg.ifEmpty { "消息发送被屏蔽" })
        }
        return res.data_?.msgId ?: 0L
    }

    // ── CMD 205006 标记已读 ───────────────────────────────────────

    fun buildCommitReceivedPmsg(groupId: Long, toUid: Long, msgId: Long): ByteArray {
        val req = CommitReceivedPmsgReqIdl(
            data_ = CommitReceivedPmsgReqIdl.DataReq(
                groupId = groupId,
                toUid = toUid,
                msgType = ImConstants.MSG_TYPE_READED,
                msgId = msgId,
            ),
        )
        return req.encode()
    }

    fun parseCommitReceivedPmsg(body: ByteArray) {
        checkError(CommitReceivedPmsgResIdl.ADAPTER.decode(body).error)
    }

    // ── 工具 ─────────────────────────────────────────────────────

    private fun checkError(error: ImError?) {
        val err = error ?: return
        if (err.errorno != 0L) throw ImException(err.errorno, err.errmsg)
    }

    private fun GetGroupMsgResIdl.MsgInfo.toPulledMessage(): ImPulledMessage {
        val user: ImUserInfo = userInfo ?: ImUserInfo()
        return ImPulledMessage(
            msgId = msgId,
            msgType = msgType,
            content = content,
            createTime = createTime.toLong(),
            fromUid = user.userId,
            fromName = user.userName,
            fromPortrait = user.portrait,
            toUid = toUid,
        )
    }
}
