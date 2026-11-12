package com.huanchengfly.tieba.post.im

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IM 加密原语的回归测试。
 *
 * PBKDF2 参考向量由 Python `hashlib.pbkdf2_hmac('sha1', secKey, salt, 5, 32)` 生成，
 * 与服务端（Python `cryptography` PBKDF2HMAC）派生口径一致；一旦偏离，全部加密帧都会被服务端丢弃。
 */
class ImCryptoTest {
    /** 与 Python 逐位对齐的参考向量 */
    @Test
    fun pbkdf2HmacSha1_matchesReferenceVector() {
        val secKey = ByteArray(31) { (it + 1).toByte() }
        val expected = hexToBytes("7bd188be85e842344d0b5058f35d14403edea9c457be913cce331c8508337e09")

        val actual = ImCrypto.pbkdf2HmacSha1(
            password = secKey,
            salt = ImConstants.PBKDF2_SALT,
            iterations = ImConstants.PBKDF2_ITERATIONS,
            keyLength = ImConstants.AES_KEY_LENGTH,
        )

        assertArrayEquals(expected, actual)
    }

    @Test
    fun deriveAesKey_usesOfficialSaltAndIterations() {
        val secKey = ByteArray(31) { 0x5A }
        val key = ImCrypto.deriveAesKey(secKey)

        assertEquals(32, key.size)
        assertArrayEquals(
            ImCrypto.pbkdf2HmacSha1(secKey, ImConstants.PBKDF2_SALT, 5, 32),
            key,
        )
    }

    @Test
    fun aesEcb_roundTrip() {
        val key = ByteArray(32) { (it * 7).toByte() }
        val plain = "贴吧私信 · IM 加密往返".toByteArray()

        val cipher = ImCrypto.aesEcbEncrypt(plain, key)
        // PKCS5 填充后有额外块，且不是明文
        assertEquals(0, cipher.size % 16)
        assertArrayEquals(plain, ImCrypto.aesEcbDecrypt(cipher, key))
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
}
