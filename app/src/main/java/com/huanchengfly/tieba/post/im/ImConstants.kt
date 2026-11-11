package com.huanchengfly.tieba.post.im

/**
 * 贴吧 IM 长连接（私信）协议常量。
 *
 * 协议为反向工程所得（参考 aiotieba v4.8.1 的公开实现行为），无官方文档：
 * - 通道：ws://im.tieba.baidu.com:8000（明文 ws，握手头 im_version=2.3）
 * - 帧：1B flag + 4B cmd(大端) + 4B reqId(大端) + 载荷
 * - 载荷：protobuf，默认 AES-256-ECB/PKCS7 加密
 */
object ImConstants {
    const val WS_URL = "ws://im.tieba.baidu.com:8000"

    /** 握手头，仿官方 IM SDK 版本 */
    const val HEADER_IM_VERSION = "im_version=2.3"

    /** 鉴权包与拉包使用的伪装客户端版本（aiotieba 实测值） */
    const val CLIENT_VERSION = "22.12.1.0"

    /** cuid 外层拼接格式："{cuid}|com.baidu.tieba{version}" */
    const val CUID_APP_ID = "com.baidu.tieba"

    const val CMD_UPDATE_CLIENT_INFO = 1001
    const val CMD_GET_GROUP_MSG = 202003
    const val CMD_PUSH_NOTIFY = 202006
    const val CMD_COMMIT_PERSONAL_MSG = 205001
    const val CMD_COMMIT_RECEIVED_PMSG = 205006

    const val FLAG_BASE = 0x08
    const val FLAG_COMPRESS = 0x40
    const val FLAG_ENCRYPT = 0x80

    /** 消息内容类型：文本（一期仅支持收发文本） */
    const val MSG_TYPE_TEXT = 1

    /** 消息内容类型：已读回执 */
    const val MSG_TYPE_READED = 22

    /** 消息组类型：1v1 私信（全部 1v1 会话共用一个 priv 组） */
    const val GROUP_TYPE_PRIVATE_MSG = 6

    /** 增量拉取类型（aiotieba 默认值） */
    const val GET_TYPE_DEFAULT = 1

    /** PBKDF2-HMAC-SHA1 盐（官方内置常量） */
    val PBKDF2_SALT = byteArrayOf(
        0xA4.toByte(), 0x0B, 0xC8.toByte(), 0x34, 0xD6.toByte(), 0x95.toByte(), 0xF3.toByte(), 0x13,
    )

    /** PBKDF2 迭代次数与派生长度（AES-256） */
    const val PBKDF2_ITERATIONS = 5
    const val AES_KEY_LENGTH = 32

    /** 随机密钥长度（31 字节，RSA-2048 单块可容纳 245 字节明文，充裕） */
    const val SEC_KEY_LENGTH = 31

    /** 发送超时（秒），仿 aiotieba ws_send=3s */
    const val SEND_TIMEOUT_SECONDS = 3L

    /** 响应等待超时（毫秒），仿 aiotieba ws_read=8s */
    const val READ_TIMEOUT_MS = 8_000L

    /** OkHttp 层心跳间隔（秒），仿 aiotieba ws_heartbeat=180s */
    const val PING_INTERVAL_SECONDS = 180L

    /** 断线重连退避 */
    const val RECONNECT_BASE_DELAY_MS = 5_000L
    const val RECONNECT_MAX_DELAY_MS = 300_000L
}
