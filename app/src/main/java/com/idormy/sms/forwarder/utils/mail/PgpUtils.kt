package com.idormy.sms.forwarder.utils.mail

import com.idormy.sms.forwarder.utils.Log
import jakarta.activation.DataHandler
import jakarta.activation.FileDataSource
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimeUtility
import jakarta.mail.util.ByteArrayDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.DocumentSignatureType
import org.pgpainless.algorithm.HashAlgorithm
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.encryption_signing.SigningOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.Security
import java.util.Date
import java.util.Properties


@Suppress("PrivatePropertyName")
class PgpUtils(
    private val properties: Properties,
    private val authenticator: Authenticator,
    // 邮件参数
    private val from: String, // 发件人邮箱
    private val nickname: String, // 发件人昵称
    private val subject: String, // 邮件主题
    private val body: String, // 邮件正文
    private val attachFiles: MutableList<File> = mutableListOf(), // 附件
    // 收件人参数
    private val toAddress: MutableList<String> = mutableListOf(), // 收件人邮箱
    private val ccAddress: MutableList<String> = mutableListOf(), // 抄送者邮箱
    private val bccAddress: MutableList<String> = mutableListOf(), // 密送者邮箱
    //邮件 PGP 加密和签名
    private var recipientPGPPublicKeyRing: PGPPublicKeyRing? = null, // 收件人公钥（用于加密）
    private var senderPGPSecretKeyRing: PGPSecretKeyRing? = null, // 发件人私钥（用于签名）
    private val senderPGPSecretKeyPassword: String = "", // 发件人私钥密码
) {

    private val TAG: String = PgpUtils::class.java.simpleName

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    // 发送明文邮件
    suspend fun sendPlainEmail(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendPlainEmail")
        try {
            val originalMessage = getOriginalMessage()
            Transport.send(originalMessage)
            Pair(true, "Email sent successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to send email: ${e.message}")
        }
    }

    // 发送签名后的邮件
    suspend fun sendSignedEmail(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendSignedEmail")
        try {
            val originalMessage = getOriginalMessage()
            val secretKeyDecryptor = SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.fromPassword(senderPGPSecretKeyPassword))
            val producerOptions = ProducerOptions.sign(
                SigningOptions()
                    .addInlineSignature(secretKeyDecryptor, senderPGPSecretKeyRing!!, DocumentSignatureType.CANONICAL_TEXT_DOCUMENT)
                    .overrideHashAlgorithm(HashAlgorithm.SHA256)
            ).setAsciiArmor(true)
            val signedMessage = getEncryptedAndOrSignedMessage(originalMessage, producerOptions)
            Transport.send(signedMessage)
            Pair(true, "Email signed and sent successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to sign and send email: ${e.message}")
        }
    }

    // 发送加密邮件
    suspend fun sendEncryptedEmail(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendEncryptedEmail")
        try {
            val originalMessage = getOriginalMessage()
            val producerOptions = ProducerOptions.encrypt(
                EncryptionOptions.encryptCommunications().addRecipient(recipientPGPPublicKeyRing!!)
            ).setAsciiArmor(true)
            val encryptedMessage = getEncryptedAndOrSignedMessage(originalMessage, producerOptions)
            Transport.send(encryptedMessage)
            Pair(true, "Encrypted email sent successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to send encrypted email: ${e.message}")
        }
    }

    // 发送签名加密邮件
    suspend fun sendSignedAndEncryptedEmail(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendSignedAndEncryptedEmail")
        try {
            val originalMessage = getOriginalMessage()
            val secretKeyDecryptor = SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.fromPassword(senderPGPSecretKeyPassword))
            val producerOptions = ProducerOptions.signAndEncrypt(
                EncryptionOptions.encryptCommunications().addRecipient(recipientPGPPublicKeyRing!!),
                SigningOptions()
                    .addInlineSignature(secretKeyDecryptor, senderPGPSecretKeyRing!!, DocumentSignatureType.CANONICAL_TEXT_DOCUMENT)
                    .overrideHashAlgorithm(HashAlgorithm.SHA256)
            ).setAsciiArmor(true)
            val encryptedMessage = getEncryptedAndOrSignedMessage(originalMessage, producerOptions)
            Transport.send(encryptedMessage)
            Pair(true, "Signed and encrypted email sent successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to send signed and encrypted email: ${e.message}")
        }
    }

    // 获取原始邮件
    private fun getOriginalMessage(): MimeMessage {
        val session = Session.getInstance(properties, authenticator)
        session.debug = true
        val message = MimeMessage(session)
        // 设置直接接收者收件箱
        val toAddress = toAddress.map { InternetAddress(it) }.toTypedArray()
        message.setRecipients(Message.RecipientType.TO, toAddress)
        // 设置抄送者收件箱
        val ccAddress = ccAddress.map { InternetAddress(it) }.toTypedArray()
        message.setRecipients(Message.RecipientType.CC, ccAddress)
        // 设置密送者收件箱
        val bccAddress = bccAddress.map { InternetAddress(it) }.toTypedArray()
        message.setRecipients(Message.RecipientType.BCC, bccAddress)
        // 设置发件箱
        when {
            nickname.isEmpty() -> message.setFrom(InternetAddress(from))
            else -> try {
                var name = nickname.replace(":", "-").replace("\n", "-")
                name = MimeUtility.encodeText(name)
                message.setFrom(InternetAddress("$name <$from>"))
            } catch (e: Exception) {
                e.printStackTrace()
                message.setFrom(InternetAddress(from))
            }
        }
        // 邮件主题
        try {
            message.subject = MimeUtility.encodeText(subject.replace(":", "-").replace("\n", "-"))
        } catch (e: Exception) {
            e.printStackTrace()
            message.subject = subject
        }

        // 邮件内容
        val contentPart = MimeMultipart("mixed")

        // 邮件正文
        val textBodyPart = MimeBodyPart()
        textBodyPart.setContent(body, "text/html;charset=UTF-8")
        contentPart.addBodyPart(textBodyPart)

        // 邮件附件
        attachFiles.forEach {
            val fileBodyPart = MimeBodyPart()
            val ds = FileDataSource(it)
            val dh = DataHandler(ds)
            fileBodyPart.dataHandler = dh
            fileBodyPart.fileName = MimeUtility.encodeText(dh.name)
            contentPart.addBodyPart(fileBodyPart)
        }

        message.setContent(contentPart)
        message.sentDate = Date()
        message.saveChanges()
        return message
    }

    // 获取加密或且签名邮件: https://datatracker.ietf.org/doc/html/rfc3156#section-4
    private fun getEncryptedAndOrSignedMessage(originalMessage: MimeMessage, producerOptions: ProducerOptions): MimeMessage {
        // 将原始消息写入InputStream
        val baos = ByteArrayOutputStream()
        originalMessage.writeTo(baos)
        val inputStream: InputStream = ByteArrayInputStream(baos.toByteArray())

        // 加密数据
        val outputStream = ByteArrayOutputStream()
        val encryptionStream = PGPainless.encryptAndOrSign().onOutputStream(outputStream).withOptions(producerOptions)
        Streams.pipeAll(inputStream, encryptionStream)
        encryptionStream.close()
        val result = encryptionStream.result
        Log.d(TAG, result.toString())

        // The first body part contains the control information necessary to
        // decrypt the data in the second body part and is labeled according to
        // the value of the protocol parameter.
        val versionPart = MimeBodyPart().apply {
            setText("Version: 1")
            addHeader("Content-Type", "application/pgp-encrypted")
            addHeader("Content-Description", "PGP/MIME version identification")
            //addHeader("Content-Transfer-Encoding", "base64")
        }

        // The second body part contains the data which was encrypted
        // and is always labeled application/octet-stream.
        val encryptedPart = MimeBodyPart().apply {
            dataHandler = DataHandler(ByteArrayDataSource(outputStream.toByteArray(), "application/octet-stream"))
            fileName = "encrypted.asc"
            addHeader("Content-Type", "application/octet-stream; name=\"encrypted.asc\"")
            addHeader("Content-Description", "OpenPGP encrypted message")
            addHeader("Content-Disposition", "inline; filename=\"encrypted.asc\"")
        }

        val encryptedMultiPart = MimeMultipart("encrypted; protocol=\"application/pgp-encrypted\"")
        encryptedMultiPart.addBodyPart(versionPart, 0)
        encryptedMultiPart.addBodyPart(encryptedPart, 1)

        val encryptedMessage = MimeMessage(originalMessage.session)
        encryptedMessage.setRecipients(Message.RecipientType.TO, originalMessage.getRecipients(Message.RecipientType.TO))
        encryptedMessage.setRecipients(Message.RecipientType.CC, originalMessage.getRecipients(Message.RecipientType.CC))
        encryptedMessage.setRecipients(Message.RecipientType.BCC, originalMessage.getRecipients(Message.RecipientType.BCC))
        encryptedMessage.addFrom(originalMessage.from)
        encryptedMessage.subject = originalMessage.subject
        encryptedMessage.sentDate = originalMessage.sentDate
        encryptedMessage.setContent(encryptedMultiPart)
        encryptedMessage.saveChanges()

        return encryptedMessage
    }

}
