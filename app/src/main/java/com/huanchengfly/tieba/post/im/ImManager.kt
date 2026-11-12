package com.huanchengfly.tieba.post.im

import android.util.Log
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.GlobalEventFlow
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** IM 整体可用状态（连接 + 鉴权） */
sealed interface ImStatus {
    data object Idle : ImStatus
    data object Connecting : ImStatus
    data object Ready : ImStatus
    data class Error(val throwable: Throwable) : ImStatus
}

/**
 * 私信（IM 长连接）业务编排：连接生命周期、鉴权、消息收发、已读、推送驱动的增量拉取与本地持久化。
 *
 * 一期范围：前台收发文本私信；App 启动即建连，无独立进程后台保活（二期）。
 * 协议要点（详见 docs 设计/私信功能调研）：
 * - 1v1 私信共用一个 priv 消息组（groupType=6），会话按对端 uid 划分视图；
 * - recordId = priv 组 lastMsgId * 100 + 1；
 * - 推送（CMD 202006）只带 groupId/msgId，需再按游标增量拉取（CMD 202003）。
 */
object ImManager {
    private const val TAG = "ImManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connection = ImConnection(scope)

    private val _imStatus = MutableStateFlow<ImStatus>(ImStatus.Idle)
    val imStatus: StateFlow<ImStatus> = _imStatus.asStateFlow()

    /** 1v1 私信所属的 priv 消息组 id（鉴权响应中 groupType=6 的组） */
    @Volatile
    private var privGid: Long = 0L

    /** 消息组本地拉取游标：groupId → 已拉取的最大 msgId */
    private val gid2LastMsgId = ConcurrentHashMap<Long, Long>()

    /** 本账号数字 uid */
    @Volatile
    private var myUid: Long = 0L

    /** 本账号 uid 字符串（Room 隔离键） */
    @Volatile
    private var accountUid: String = ""

    private var monitorJob: Job? = null

    private var accountEventJob: Job? = null

    /** App 启动钩子：有登录态则启动（幂等），并跟随切号/退出重启或停止连接 */
    fun onAppStart() {
        AccountUtil.getLoginInfo()?.let { start(it) }
        if (accountEventJob?.isActive != true) {
            accountEventJob = scope.launch {
                GlobalEventFlow.filterIsInstance<GlobalEvent.AccountSwitched>().collect {
                    val account = AccountUtil.getLoginInfo()
                    if (account != null) start(account) else stop()
                }
            }
        }
    }

    /** 登录/切号后调用；同账号重复调用幂等 */
    fun start(account: Account) {
        if (accountUid == account.uid && monitorJob?.isActive == true) return
        if (account.sToken.isEmpty()) {
            Log.w(TAG, "账号缺少 STOKEN，私信长连接不可用")
            _imStatus.value = ImStatus.Error(IllegalStateException("账号缺少 STOKEN"))
            return
        }
        stop()
        accountUid = account.uid
        myUid = account.uid.toLongOrNull() ?: 0L
        monitorJob = scope.launch { monitorLoop(account) }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        connection.close()
        privGid = 0L
        gid2LastMsgId.clear()
        _imStatus.value = ImStatus.Idle
    }

    /**
     * 连接保活循环：connect → 鉴权 → 收推送，断开后指数退避重连。
     */
    private suspend fun monitorLoop(account: Account) {
        var backoff = ImConstants.RECONNECT_BASE_DELAY_MS
        while (scope.isActive) {
            try {
                _imStatus.value = ImStatus.Connecting
                connection.connect()
                auth(account)
                _imStatus.value = ImStatus.Ready
                backoff = ImConstants.RECONNECT_BASE_DELAY_MS
                // 推送与「会话结束」并行等待：SharedFlow 不会自行结束，断线必须靠 sessionEnd 感知
                coroutineScope {
                    launch { connection.pushFlow.collect { frame -> onPush(frame) } }
                    connection.awaitSessionEnd()
                }
                Log.i(TAG, "IM 连接已断开，准备重连")
                _imStatus.value = ImStatus.Error(IllegalStateException("IM 连接已断开"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "IM 连接异常：${e.message}")
                _imStatus.value = ImStatus.Error(e)
            }
            connection.close()
            delay(backoff)
            backoff = minOf(backoff * 2, ImConstants.RECONNECT_MAX_DELAY_MS)
        }
    }

    /** CMD 1001 上线鉴权：上报密钥换取消息组列表，初始化游标与会话 */
    private suspend fun auth(account: Account) {
        val secKey = connection.newSessionKey()
        val body = connection.request(
            ImConstants.CMD_UPDATE_CLIENT_INFO,
            ImApi.buildUpdateClientInfo(
                bduss = account.bduss,
                stoken = account.sToken,
                // IM 的 cuid 用官方形态 baidutiebaapp{uuid}；session/device 里再带 cuid_galaxy2
                cuid = UIDUtil.newCUID,
                cuidGalaxy2 = CuidUtils.getNewCuid(),
                secKey = secKey,
            ),
            encrypt = false,
        )
        val groups = ImApi.parseUpdateClientInfo(body)
        privGid = groups.firstOrNull { it.groupType == ImConstants.GROUP_TYPE_PRIVATE_MSG }?.groupId ?: 0L
        groups.forEach { gid2LastMsgId[it.groupId] = it.lastMsgId }
        Log.i(TAG, "IM 鉴权成功：privGid=$privGid, groups=${groups.size}")
    }

    /** 服务端推送（CMD 202006）：按 groupType 过滤私信推送并触发增量拉取 */
    private suspend fun onPush(frame: ImFrame.Frame) {
        val notifies = try {
            ImApi.parsePushNotify(frame.payload)
        } catch (e: Exception) {
            Log.w(TAG, "推送解析失败", e)
            return
        }
        val gids = notifies
            .filter { it.third == ImConstants.GROUP_TYPE_PRIVATE_MSG }
            .map { it.first }
            .distinct()
        if (gids.isEmpty()) return
        runCatching { pullMessages(gids) }
            .onFailure { Log.w(TAG, "推送拉取失败", it) }
    }

    /**
     * 按游标增量拉取并持久化（推送驱动 / 聊天页主动刷新共用）。
     */
    suspend fun pullMessages(groupIds: List<Long> = listOfNotNull(privGid.takeIf { it > 0L })) {
        if (accountUid.isEmpty() || groupIds.isEmpty()) return
        val pairs = groupIds.map { it to (gid2LastMsgId[it] ?: 0L) }
        val body = connection.request(ImConstants.CMD_GET_GROUP_MSG, ImApi.buildGetGroupMsg(UIDUtil.newCUID, pairs))
        persistPull(ImApi.parseGetGroupMsg(body))
    }

    private suspend fun persistPull(results: List<ImPullResult>) {
        for (result in results) {
            // 服务端未回组信息时按 1v1 私信默认组归属，避免落到 gid=0
            val gid = result.groupId.takeIf { it > 0L } ?: privGid
            var maxMsgId = gid2LastMsgId[gid] ?: 0L
            for (msg in result.messages) {
                maxMsgId = maxOf(maxMsgId, msg.msgId)
                val isMine = msg.fromUid == myUid
                when {
                    msg.msgType == ImConstants.MSG_TYPE_READED -> {
                        // 对端已读回执：更新我方消息的已读游标，不入消息表
                        if (!isMine) {
                            val peer = msg.fromUid
                            DatabaseUtil.getImSession(accountUid, peer)?.let { session ->
                                if (msg.msgId > session.peerReadMsgId) {
                                    DatabaseUtil.upsertImSession(session.copy(peerReadMsgId = msg.msgId))
                                }
                            }
                        }
                    }

                    msg.msgType == ImConstants.MSG_TYPE_TEXT -> {
                        val peerUid = if (isMine) msg.toUid else msg.fromUid
                        if (peerUid <= 0L) {
                            Log.w(TAG, "消息缺少归属会话（toUid 推断失败），msgId=${msg.msgId}")
                            continue
                        }
                        // 重复投递（IGNORE 命中）不算新消息，避免未读重复累加
                        val inserted = DatabaseUtil.insertImMessages(
                            listOf(
                                ImMessage(
                                    uid = accountUid,
                                    peerUid = peerUid,
                                    groupId = gid,
                                    msgId = msg.msgId,
                                    msgType = msg.msgType,
                                    content = msg.content,
                                    createTime = msg.createTime,
                                    fromUid = msg.fromUid,
                                    isMine = isMine,
                                )
                            )
                        )
                        val isNew = inserted.any { it != -1L }
                        DatabaseUtil.getImSession(accountUid, peerUid)?.let { session ->
                            DatabaseUtil.upsertImSession(
                                session.copy(
                                    peerName = msg.fromName.ifEmpty { session.peerName },
                                    peerPortrait = msg.fromPortrait.ifEmpty { session.peerPortrait },
                                    lastMsgId = maxOf(session.lastMsgId, msg.msgId),
                                    lastMsgTime = maxOf(session.lastMsgTime, msg.createTime),
                                    lastMsgPreview = msg.content,
                                    lastMsgFromMe = isMine,
                                    unreadCount = if (isMine || !isNew) session.unreadCount else session.unreadCount + 1,
                                )
                            )
                        } ?: DatabaseUtil.upsertImSession(
                            ImSession(
                                uid = accountUid,
                                peerUid = peerUid,
                                peerName = msg.fromName,
                                peerPortrait = msg.fromPortrait,
                                lastMsgId = msg.msgId,
                                lastMsgTime = msg.createTime,
                                lastMsgPreview = msg.content,
                                lastMsgFromMe = isMine,
                                unreadCount = if (isMine || !isNew) 0 else 1,
                            )
                        )
                    }

                    else -> {
                        // 一期仅收发文本，其他类型忽略（不更新游标以外状态）
                    }
                }
            }
            gid2LastMsgId[gid] = maxMsgId
        }
    }

    /**
     * 发送文本私信。返回服务端分配的 msgId；被屏蔽时抛 [ImException]。
     */
    suspend fun sendMessage(toUid: Long, content: String): Long {
        check(_imStatus.value == ImStatus.Ready) { "私信服务未连接" }
        check(privGid > 0L) { "私信会话未初始化" }
        val recordId = (gid2LastMsgId[privGid] ?: 0L) * 100 + 1
        val body = connection.request(
            ImConstants.CMD_COMMIT_PERSONAL_MSG,
            ImApi.buildCommitPersonalMsg(toUid, content, recordId),
        )
        val msgId = ImApi.parseCommitPersonalMsg(body)
        val createTime = System.currentTimeMillis() / 1000
        if (msgId <= 0L) {
            // 服务端未回消息 id：不写本地（避免 msgId=0 撞唯一索引或留下幽灵气泡），下次拉取会带回
            Log.w(TAG, "发送成功但未返回 msgId，跳过本地落库")
            return msgId
        }
        gid2LastMsgId[privGid] = maxOf(gid2LastMsgId[privGid] ?: 0L, msgId)
        // 本地落库（乐观）：响应成功即视为已发出
        DatabaseUtil.insertImMessages(
            listOf(
                ImMessage(
                    uid = accountUid,
                    peerUid = toUid,
                    groupId = privGid,
                    msgId = msgId,
                    msgType = ImConstants.MSG_TYPE_TEXT,
                    content = content,
                    createTime = createTime,
                    fromUid = myUid,
                    isMine = true,
                )
            )
        )
        DatabaseUtil.getImSession(accountUid, toUid)?.let { session ->
            DatabaseUtil.upsertImSession(
                session.copy(
                    lastMsgId = maxOf(session.lastMsgId, msgId),
                    lastMsgTime = maxOf(session.lastMsgTime, createTime),
                    lastMsgPreview = content,
                    lastMsgFromMe = true,
                )
            )
        } ?: DatabaseUtil.upsertImSession(
            ImSession(
                uid = accountUid,
                peerUid = toUid,
                lastMsgId = msgId,
                lastMsgTime = createTime,
                lastMsgPreview = content,
                lastMsgFromMe = true,
            )
        )
        return msgId
    }

    /**
     * 标记某会话已读（对端为 peerUid，读到 msgId），并清零本地未读。
     */
    suspend fun markRead(peerUid: Long, msgId: Long) {
        if (_imStatus.value != ImStatus.Ready || privGid <= 0L || msgId <= 0L) return
        runCatching {
            val body = connection.request(
                ImConstants.CMD_COMMIT_RECEIVED_PMSG,
                ImApi.buildCommitReceivedPmsg(privGid, peerUid, msgId),
            )
            ImApi.parseCommitReceivedPmsg(body)
            DatabaseUtil.clearImUnread(accountUid, peerUid)
        }.onFailure { Log.w(TAG, "标记已读失败", it) }
    }
}
