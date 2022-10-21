package com.idormy.sms.forwarder.utils

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SM4分组密码算法是我国自主设计的分组对称密码算法
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object SM4Crypt {

    const val SM4_CBC_NOPADDING = "SM4/CBC/NoPadding"
    const val SM4_CBC_PKCS5 = "SM4/CBC/PKCS5Padding"
    const val SM4_CBC_PKCS7 = "SM4/CBC/PKCS7Padding"
    const val SM4_ECB_NOPADDING = "SM4/ECB/NoPadding"
    const val SM4_ECB_PKCS5 = "SM4/ECB/PKCS5Padding"
    const val SM4_ECB_PKCS7 = "SM4/ECB/PKCS7Padding"
    private val BC_PROVIDER = BouncyCastleProvider()
    private val SM4_CBC_IV = byteArrayOf(3, 5, 6, 9, 6, 9, 5, 9, 3, 5, 6, 9, 6, 9, 5, 9)

    /**
     * 获取随机密钥
     */
    fun createSM4Key(): ByteArray {
        val seed = ByteArray(16)
        val random = SecureRandom()
        random.nextBytes(seed)
        return seed
    }

    @JvmOverloads
    fun encrypt(source: ByteArray, key: ByteArray, mode: String = SM4_CBC_PKCS7, iv: ByteArray? = SM4_CBC_IV): ByteArray {
        return doSM4(true, source, key, mode, iv)
    }

    @JvmOverloads
    fun decrypt(source: ByteArray, key: ByteArray, mode: String = SM4_CBC_PKCS7, iv: ByteArray? = SM4_CBC_IV): ByteArray {
        return doSM4(false, source, key, mode, iv)
    }

    private fun doSM4(forEncryption: Boolean, source: ByteArray, key: ByteArray, mode: String, iv: ByteArray?): ByteArray {
        return try {
            val cryptMode = if (forEncryption) 1 else 2
            val sm4Key = SecretKeySpec(key, "SM4")
            val cipher = Cipher.getInstance(mode, BC_PROVIDER)
            if (iv == null) {
                cipher.init(cryptMode, sm4Key)
            } else {
                val ivParameterSpec = IvParameterSpec(iv)
                cipher.init(cryptMode, sm4Key, ivParameterSpec)
            }
            cipher.doFinal(source)
        } catch (var9: Exception) {
            var9.printStackTrace()
            ByteArray(0)
        }
    }

}