package com.huanchengfly.tieba.post.im

import android.util.Base64
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * IM 长连接的加密原语。
 *
 * 会话加密：31 字节随机数经 PBKDF2-HMAC-SHA1（官方内置盐，5 轮）派生 AES-256 密钥，
 * 以 AES-ECB/PKCS7 加解密载荷；随机数本体用官方内置 RSA 公钥（PKCS1v15）加密后随鉴权包上报。
 *
 * PBKDF2 手写实现：javax 的 PBKDF2WithHmacSHA1 对 password 字节的解释存在歧义
 * （PBEKeySpec 走 char[]，与 Python 侧原始字节派生不保证一致），为保证与服务端派生结果逐位一致，直接按 RFC 2898 实现。
 */
object ImCrypto {
    /** 官方 IM 内置 RSA 公钥（DER/PKCS#1 X.509 包装，Base64） */
    private const val RSA_PUBLIC_KEY_BASE64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwQpwBZxXJV/JVRF/uNfy" +
            "MSdu7YWwRNLM8+2xbniGp2iIQHOikPpTYQjlQgMi1uvq1kZpJ32rHo3hkwjy2l0l" +
            "Fwr3u4Hk2Wk7vnsqYQjAlYlK0TCzjpmiI+OiPOUNVtbWHQiLiVqFtzvpvi4AU7C1" +
            "iKGvc/4IS45WjHxeScHhnZZ7njS4S1UgNP/GflRIbzgbBhyZ9kEW5/OO5YfG1fy6" +
            "r4KSlDJw4o/mw5XhftyIpL+5ZBVBC6E1EIiP/dd9AbK62VV1PByfPMHMixpxI3GM" +
            "2qwcmFsXcCcgvUXJBa9k6zP8dDQ3csCM2QNT+CQAOxthjtp/TFWaD7MzOdsIYb3T" +
            "HwIDAQAB"

    private val rsaCipher: Cipher by lazy {
        val der = Base64.decode(RSA_PUBLIC_KEY_BASE64, Base64.NO_WRAP)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
        Cipher.getInstance("RSA/ECB/PKCS1Padding").apply { init(Cipher.ENCRYPT_MODE, publicKey) }
    }

    private val secureRandom = SecureRandom()

    /** 生成 31 字节会话随机密钥（派生源，同时是 RSA 上报体） */
    fun generateSecKey(): ByteArray = ByteArray(ImConstants.SEC_KEY_LENGTH).also { secureRandom.nextBytes(it) }

    /** PBKDF2-HMAC-SHA1（RFC 2898），password 为原始字节 */
    fun pbkdf2HmacSha1(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(password, "HmacSHA1"))
        val hLen = mac.macLength
        val blocks = (keyLength + hLen - 1) / hLen
        val result = ByteArray(keyLength)
        var offset = 0
        for (block in 1..blocks) {
            val blockIndex = byteArrayOf(
                (block ushr 24).toByte(),
                (block ushr 16).toByte(),
                (block ushr 8).toByte(),
                block.toByte(),
            )
            // U1 = HMAC(P, salt || INT_32_BE(i))
            mac.update(salt)
            var u = mac.doFinal(blockIndex)
            val t = u.copyOf()
            // T = U1 ^ U2 ^ ... ^ Uc
            for (round in 1 until iterations) {
                u = mac.doFinal(u)
                for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
            }
            val n = minOf(hLen, keyLength - offset)
            System.arraycopy(t, 0, result, offset, n)
            offset += n
        }
        return result
    }

    /** 由 31 字节随机数派生 AES-256-ECB 密钥 */
    fun deriveAesKey(secKey: ByteArray): ByteArray = pbkdf2HmacSha1(
        password = secKey,
        salt = ImConstants.PBKDF2_SALT,
        iterations = ImConstants.PBKDF2_ITERATIONS,
        keyLength = ImConstants.AES_KEY_LENGTH,
    )

    fun aesEcbEncrypt(data: ByteArray, aesKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
        return cipher.doFinal(data)
    }

    fun aesEcbDecrypt(data: ByteArray, aesKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"))
        return cipher.doFinal(data)
    }

    /** RSA/PKCS1v15 加密会话随机密钥（鉴权包 secretKey 字段） */
    fun wrapSecKey(secKey: ByteArray): ByteArray = rsaCipher.doFinal(secKey)
}
