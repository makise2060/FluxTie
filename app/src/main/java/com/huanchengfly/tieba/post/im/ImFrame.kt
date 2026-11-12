package com.huanchengfly.tieba.post.im

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * IM 帧编解码：`1B flag + 4B cmd(大端) + 4B reqId(大端) + 载荷`。
 *
 * flag：0x08 恒定；0x40 载荷 gzip 压缩；0x80 载荷 AES-ECB 加密。
 */
object ImFrame {
    class Frame(val cmd: Int, val reqId: Int, val payload: ByteArray)

    fun pack(
        data: ByteArray,
        cmd: Int,
        reqId: Int,
        aesKey: ByteArray?,
        compress: Boolean = false,
        encrypt: Boolean = true,
    ): ByteArray {
        require(!(encrypt && aesKey == null)) { "加密帧必须有会话 AES 密钥" }
        var flag = ImConstants.FLAG_BASE
        var payload = data
        if (compress) {
            flag = flag or ImConstants.FLAG_COMPRESS
            payload = gzip(payload)
        }
        if (encrypt) {
            flag = flag or ImConstants.FLAG_ENCRYPT
            payload = ImCrypto.aesEcbEncrypt(payload, aesKey!!)
        }
        val frame = ByteArray(9 + payload.size)
        frame[0] = flag.toByte()
        writeInt(frame, 1, cmd)
        writeInt(frame, 5, reqId)
        System.arraycopy(payload, 0, frame, 9, payload.size)
        return frame
    }

    fun parse(data: ByteArray, aesKey: ByteArray?): Frame {
        require(data.size >= 9) { "IM 帧过短：${data.size}" }
        val flag = data[0].toInt() and 0xFF
        val cmd = readInt(data, 1)
        val reqId = readInt(data, 5)
        var payload = data.copyOfRange(9, data.size)
        if (flag and ImConstants.FLAG_ENCRYPT != 0) {
            check(aesKey != null) { "收到加密帧但无会话密钥" }
            payload = ImCrypto.aesEcbDecrypt(payload, aesKey)
        }
        if (flag and ImConstants.FLAG_COMPRESS != 0) {
            payload = ungzip(payload)
        }
        return Frame(cmd, reqId, payload)
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    private fun readInt(source: ByteArray, offset: Int): Int =
        // 注意：Kotlin 的 infix（shl/or）同优先级且左结合，必须显式加括号，
        // 否则 `a shl 24 or b shl 16` 会被解析成 `((a shl 24) or b) shl 16`。
        (((source[offset].toInt() and 0xFF) shl 24) or
            ((source[offset + 1].toInt() and 0xFF) shl 16) or
            ((source[offset + 2].toInt() and 0xFF) shl 8) or
            (source[offset + 3].toInt() and 0xFF))

    private fun gzip(data: ByteArray): ByteArray = ByteArrayOutputStream().use { bos ->
        GZIPOutputStream(bos).use { it.write(data) }
        bos.toByteArray()
    }

    private fun ungzip(data: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}
