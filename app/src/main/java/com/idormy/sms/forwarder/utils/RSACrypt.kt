package com.idormy.sms.forwarder.utils

import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * 非对称加密RSA加密和解密
 */
object RSACrypt {

    private const val TRANSFORMATION = "RSA"
    private const val ENCRYPT_MAX_SIZE = 245
    private const val DECRYPT_MAX_SIZE = 256

    /**
     * 私钥加密
     * @param input 原文
     * @param privateKey 私钥
     */
    fun encryptByPrivateKey(input: String, privateKey: PrivateKey): String {

        //创建cipher对象
        val cipher = Cipher.getInstance(TRANSFORMATION)
        //初始化cipher
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)

        //****非对称加密****
        val byteArray = input.toByteArray()

        //分段加密
        var temp: ByteArray?
        var offset = 0 //当前偏移的位置

        val outputStream = ByteArrayOutputStream()

        //拆分input
        while (byteArray.size - offset > 0) {
            //每次最大加密245个字节
            if (byteArray.size - offset >= ENCRYPT_MAX_SIZE) {
                //剩余部分大于245
                //加密完整245
                temp = cipher.doFinal(byteArray, offset, ENCRYPT_MAX_SIZE)
                //重新计算偏移位置
                offset += ENCRYPT_MAX_SIZE
            } else {
                //加密最后一块
                temp = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                //重新计算偏移位置
                offset = byteArray.size
            }
            //存储到临时的缓冲区
            outputStream.write(temp)
        }
        outputStream.close()

        return Base64.encode(outputStream.toByteArray())

    }

    /**
     * 公钥加密
     * @param input 原文
     * @param publicKey 公钥
     */
    fun encryptByPublicKey(input: String, publicKey: PublicKey): String {

        //创建cipher对象
        val cipher = Cipher.getInstance(TRANSFORMATION)
        //初始化cipher
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        //****非对称加密****
        val byteArray = input.toByteArray()

        var temp: ByteArray?
        var offset = 0 //当前偏移的位置

        val outputStream = ByteArrayOutputStream()

        //拆分input
        while (byteArray.size - offset > 0) {
            //每次最大加密117个字节
            if (byteArray.size - offset >= ENCRYPT_MAX_SIZE) {
                //剩余部分大于117
                //加密完整117
                temp = cipher.doFinal(byteArray, offset, ENCRYPT_MAX_SIZE)
                //重新计算偏移位置
                offset += ENCRYPT_MAX_SIZE
            } else {
                //加密最后一块
                temp = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                //重新计算偏移位置
                offset = byteArray.size
            }
            //存储到临时的缓冲区
            outputStream.write(temp)
        }
        outputStream.close()

        return Base64.encode(outputStream.toByteArray())

    }

    /**
     * 私钥解密
     * @param input 秘文
     * @param privateKey 私钥
     */
    fun decryptByPrivateKey(input: String, privateKey: PrivateKey): String {

        //创建cipher对象
        val cipher = Cipher.getInstance(TRANSFORMATION)
        //初始化cipher
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        //****非对称加密****
        val byteArray = Base64.decode(input)

        //分段解密
        var temp: ByteArray?
        var offset = 0 //当前偏移的位置

        val outputStream = ByteArrayOutputStream()

        //拆分input
        while (byteArray.size - offset > 0) {
            //每次最大解密256个字节
            if (byteArray.size - offset >= DECRYPT_MAX_SIZE) {

                temp = cipher.doFinal(byteArray, offset, DECRYPT_MAX_SIZE)
                //重新计算偏移位置
                offset += DECRYPT_MAX_SIZE
            } else {
                //加密最后一块
                temp = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                //重新计算偏移位置
                offset = byteArray.size
            }
            //存储到临时的缓冲区
            outputStream.write(temp)
        }
        outputStream.close()

        return String(outputStream.toByteArray())

    }

    /**
     * 公钥解密
     * @param input 秘文
     * @param publicKey 公钥
     */
    fun decryptByPublicKey(input: String, publicKey: PublicKey): String {

        //创建cipher对象
        val cipher = Cipher.getInstance(TRANSFORMATION)
        //初始化cipher
        cipher.init(Cipher.DECRYPT_MODE, publicKey)

        //****非对称加密****
        val byteArray = Base64.decode(input)

        //分段解密
        var temp: ByteArray?
        var offset = 0 //当前偏移的位置

        val outputStream = ByteArrayOutputStream()

        //拆分input
        while (byteArray.size - offset > 0) {
            //每次最大解密256个字节
            if (byteArray.size - offset >= DECRYPT_MAX_SIZE) {

                temp = cipher.doFinal(byteArray, offset, DECRYPT_MAX_SIZE)
                //重新计算偏移位置
                offset += DECRYPT_MAX_SIZE
            } else {
                //加密最后一块
                temp = cipher.doFinal(byteArray, offset, byteArray.size - offset)
                //重新计算偏移位置
                offset = byteArray.size
            }
            //存储到临时的缓冲区
            outputStream.write(temp)
        }
        outputStream.close()

        return String(outputStream.toByteArray())

    }

    fun getPrivateKey(privateKeyStr: String): PrivateKey {
        //字符串转成秘钥对对象
        val generator = KeyFactory.getInstance("RSA")
        return generator.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKeyStr)))
    }

    fun getPublicKey(publicKeyStr: String): PublicKey {
        //字符串转成秘钥对对象
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(X509EncodedKeySpec(Base64.decode(publicKeyStr)))
    }

}