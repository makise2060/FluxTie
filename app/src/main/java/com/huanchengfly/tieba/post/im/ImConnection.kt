package com.huanchengfly.tieba.post.im

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** 连接状态 */
sealed interface ImConnectionState {
    data object Idle : ImConnectionState
    data object Connecting : ImConnectionState
    data object Connected : ImConnectionState
    data class Failed(val error: Throwable) : ImConnectionState
}

/**
 * IM WebSocket 连接层：帧编解码、reqId 请求-响应配对、服务端推送分发、断线重连。
 *
 * 仅负责字节层与配对，业务编解码见 [ImApi]，生命周期编排见 [ImManager]。
 */
class ImConnection(
    private val externalScope: CoroutineScope,
) {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(ImConstants.PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    private val _state = MutableStateFlow<ImConnectionState>(ImConnectionState.Idle)
    val state: StateFlow<ImConnectionState> = _state.asStateFlow()

    private val _pushFlow = MutableSharedFlow<ImFrame.Frame>(extraBufferCapacity = 64)
    val pushFlow: SharedFlow<ImFrame.Frame> = _pushFlow.asSharedFlow()

    /** reqId → 等待中的响应 */
    private val waiters = ConcurrentHashMap<Int, CompletableDeferred<ByteArray>>()

    private val reqIdCounter = AtomicLong(System.currentTimeMillis() / 1000)

    /** 每次 connect 分配连接代次，旧 WebSocket 的迟到回调不得污染新会话 */
    private val generation = java.util.concurrent.atomic.AtomicInteger(0)

    /** 会话 AES 密钥（鉴权成功后设置） */
    @Volatile
    var aesKey: ByteArray? = null
        private set

    /** 会话随机密钥（RSA 上报体，重连时需复用以保持密钥一致） */
    @Volatile
    var secKey: ByteArray? = null
        private set

    private var webSocket: WebSocket? = null

    private val incoming = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    private var dispatcherJob: kotlinx.coroutines.Job? = null

    /** 每次连接创建一个「会话结束」信号：断开时完成，供上层保活循环感知（SharedFlow 不会自行结束） */
    @Volatile
    private var sessionEnd = CompletableDeferred<Unit>()

    /** 挂起直到当前连接断开 */
    suspend fun awaitSessionEnd() {
        sessionEnd.await()
    }

    /** 新密钥对（重连换新，避免旧密钥残留） */
    fun newSessionKey(): ByteArray {
        val key = ImCrypto.generateSecKey()
        secKey = key
        aesKey = ImCrypto.deriveAesKey(key)
        return key
    }

    /** 建立连接并启动分发循环；已连接/正在连接时直接返回 */
    suspend fun connect(): Unit = withContext(Dispatchers.IO) {
        if (_state.value == ImConnectionState.Connected || _state.value == ImConnectionState.Connecting) {
            return@withContext
        }
        _state.value = ImConnectionState.Connecting
        sessionEnd = CompletableDeferred()
        val request = Request.Builder()
            .url(ImConstants.WS_URL)
            .header("Sec-WebSocket-Extensions", ImConstants.HEADER_IM_VERSION)
            .header("Accept-Encoding", "gzip")
            .build()

        val deferred = CompletableDeferred<Unit>()
        val sessionGeneration = generation.incrementAndGet()
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (generation.get() == sessionGeneration) {
                        deferred.complete(Unit)
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (generation.get() == sessionGeneration) {
                        incoming.trySend(bytes.toByteArray())
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    val error = RuntimeException("连接关闭：$code $reason")
                    if (generation.get() == sessionGeneration) {
                        deferred.completeExceptionally(error)
                        onDisconnected(error, sessionGeneration)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (generation.get() == sessionGeneration) {
                        deferred.completeExceptionally(t)
                        onDisconnected(t, sessionGeneration)
                    }
                }
            },
        )
        // 握手失败会由 onFailure 完成 exceptionally，不会无限等待
        withTimeout(ImConstants.CONNECT_TIMEOUT_MS) { deferred.await() }
        _state.value = ImConnectionState.Connected
        if (dispatcherJob?.isActive != true) {
            dispatcherJob = externalScope.launch { dispatchLoop() }
        }
    }

    private fun onDisconnected(t: Throwable, sessionGeneration: Int) {
        // 代次不匹配说明该事件属于旧连接，不覆盖当前状态
        if (generation.get() != sessionGeneration || _state.value == ImConnectionState.Idle) return
        _state.value = ImConnectionState.Failed(t)
        sessionEnd.complete(Unit)
        // 唤醒所有等待者
        waiters.values.forEach { it.completeExceptionally(t) }
        waiters.clear()
    }

    private suspend fun dispatchLoop() {
        for (raw in incoming) {
            try {
                val frame = ImFrame.parse(raw, aesKey)
                if (frame.cmd == ImConstants.CMD_PUSH_NOTIFY) {
                    _pushFlow.emit(frame)
                } else {
                    waiters.remove(frame.reqId)?.complete(frame.payload)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // 单帧解析失败只影响该帧，不断连接
                android.util.Log.w("ImConnection", "帧处理失败", e)
            }
        }
    }

    /**
     * 发起一次请求并等待响应（按 reqId 配对）。
     *
     * @param encrypt 载荷是否加密（鉴权首包为明文）
     */
    suspend fun request(
        cmd: Int,
        payload: ByteArray,
        encrypt: Boolean = true,
        compress: Boolean = false,
    ): ByteArray = withContext(Dispatchers.IO) {
        val ws = webSocket ?: throw IllegalStateException("连接未建立")
        val reqId = reqIdCounter.incrementAndGet().toInt()
        val deferred = CompletableDeferred<ByteArray>()
        waiters[reqId] = deferred
        val frame = ImFrame.pack(payload, cmd, reqId, aesKey, compress = compress, encrypt = encrypt)
        val sent = ws.send(frame.toByteString())
        if (!sent) {
            waiters.remove(reqId)
            throw IllegalStateException("IM 连接已断开")
        }
        try {
            withTimeout(ImConstants.READ_TIMEOUT_MS) { deferred.await() }
        } finally {
            waiters.remove(reqId)
        }
    }

    fun close() {
        // 先递增代次并置 Idle：旧 listener 的迟到回调不再影响新连接
        generation.incrementAndGet()
        _state.value = ImConnectionState.Idle
        webSocket?.close(1000, null)
        webSocket = null
        sessionEnd.complete(Unit)
        waiters.values.forEach { it.completeExceptionally(RuntimeException("连接已关闭")) }
        waiters.clear()
        dispatcherJob?.cancel()
        dispatcherJob = null
    }
}
