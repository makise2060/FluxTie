package com.huanchengfly.tieba.post.im

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** IM 帧编解码：9 字节帧头（flag + cmd + reqId，大端）+ 可选 gzip/AES 载荷。 */
class ImFrameTest {
    private val key = ByteArray(32) { (it * 3 + 1).toByte() }

    @Test
    fun pack_encryptAndParse_roundTrip() {
        val payload = """{"probe":true}""".toByteArray()
        val frame = ImFrame.pack(payload, cmd = 205001, reqId = 12345, aesKey = key)

        assertEquals(9 + 16 * ((payload.size / 16) + 1), frame.size)
        assertEquals(ImConstants.FLAG_BASE or ImConstants.FLAG_ENCRYPT, frame[0].toInt() and 0xFF)

        val parsed = ImFrame.parse(frame, key)
        assertEquals(205001, parsed.cmd)
        assertEquals(12345, parsed.reqId)
        assertArrayEquals(payload, parsed.payload)
    }

    @Test
    fun pack_compressAndEncrypt_roundTrip() {
        val payload = ByteArray(512) { 'a'.code.toByte() }
        val frame = ImFrame.pack(payload, cmd = 202003, reqId = 1, aesKey = key, compress = true)

        val flag = frame[0].toInt() and 0xFF
        assertTrue(flag and ImConstants.FLAG_COMPRESS != 0)
        assertTrue(flag and ImConstants.FLAG_ENCRYPT != 0)
        // 高重复内容压缩后应显著变小（加密填充后仍远小于原文）
        assertTrue(frame.size < payload.size)

        val parsed = ImFrame.parse(frame, key)
        assertEquals(202003, parsed.cmd)
        assertArrayEquals(payload, parsed.payload)
    }

    @Test
    fun pack_plainFrame_matchesBigEndianHeader() {
        val frame = ImFrame.pack(byteArrayOf(1, 2, 3), cmd = 1001, reqId = 0x01020304, aesKey = null, encrypt = false)

        assertEquals(ImConstants.FLAG_BASE, frame[0].toInt() and 0xFF)
        // cmd = 1001 = 0x000003E9
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x03, 0xE9.toByte()), frame.copyOfRange(1, 5))
        // reqId 大端
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), frame.copyOfRange(5, 9))
        assertArrayEquals(byteArrayOf(1, 2, 3), frame.copyOfRange(9, frame.size))
    }
}
