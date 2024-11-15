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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSAlgorithm
import org.bouncycastle.cms.CMSEnvelopedDataGenerator
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.OutputEncryptor
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Properties


@Suppress("PrivatePropertyName")
class SmimeUtils(
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
    // 邮件 S/MIME 加密和签名
    private val recipientX509Cert: X509Certificate? = null, //收件人公钥（用于加密）
    private val senderPrivateKey: PrivateKey? = null, //发件人私玥（用于签名）
    private val senderX509Cert: X509Certificate? = null, //发件人公玥（用于签名）
) {

    private val TAG: String = SmimeUtils::class.java.simpleName

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
            val signedMessage = getSignedMessage(originalMessage)
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
            val encryptedMessage = getEncryptedMessage(originalMessage)
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
            val signedMessage = getSignedMessage(originalMessage)
            val encryptedMessage = getEncryptedMessage(signedMessage)
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

    // 获取签名邮件
    private fun getSignedMessage(originalMessage: MimeMessage): MimeMessage {
        // 创建签名者信息生成器
        val contentSigner = JcaContentSignerBuilder("SHA256withRSA").build(senderPrivateKey)
        val certificateHolder = JcaX509CertificateHolder(senderX509Cert)
        val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
            JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider()).build()
        ).build(contentSigner, certificateHolder)

        // 创建 CMSSignedDataGenerator 并添加签名者信息和证书
        val generator = CMSSignedDataGenerator()
        generator.addSignerInfoGenerator(signerInfoGenerator)
        val certStore = JcaCertStore(listOf(senderX509Cert))
        generator.addCertificates(certStore)

        // 将邮件内容转换为 CMSSignedData
        val outputStream = ByteArrayOutputStream()
        originalMessage.writeTo(outputStream)
        val contentData = CMSProcessableByteArray(outputStream.toByteArray())
        val signedData = generator.generate(contentData, true)

        // 创建 MimeMessage 并设置签名后的内容
        val signedMessage = MimeMessage(originalMessage.session, ByteArrayInputStream(signedData.encoded))
        /*
        //TODO: 为什么不需要再设置这些？
        signedMessage.setRecipients(Message.RecipientType.TO, originalMessage.getRecipients(Message.RecipientType.TO))
        signedMessage.setRecipients(Message.RecipientType.CC, originalMessage.getRecipients(Message.RecipientType.CC))
        signedMessage.setRecipients(Message.RecipientType.BCC, originalMessage.getRecipients(Message.RecipientType.BCC))
        signedMessage.addFrom(originalMessage.from)
        signedMessage.subject = originalMessage.subject
        signedMessage.sentDate = originalMessage.sentDate
        */
        signedMessage.setContent(signedData.encoded, "application/pkcs7-mime; name=smime.p7m; smime-type=signed-data")
        signedMessage.saveChanges()

        return signedMessage
    }

    // 获取加密邮件
    private fun getEncryptedMessage(originalMessage: MimeMessage): MimeMessage {
        // 使用收件人的证书进行加密
        val cmsEnvelopedDataGenerator = CMSEnvelopedDataGenerator()
        val recipientInfoGenerator = JceKeyTransRecipientInfoGenerator(recipientX509Cert)
        cmsEnvelopedDataGenerator.addRecipientInfoGenerator(recipientInfoGenerator)

        // 使用 3DES 加密
        val outputEncryptor: OutputEncryptor = JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).build()
        val originalContent = ByteArrayOutputStream()
        originalMessage.writeTo(originalContent)
        val inputStream = originalContent.toByteArray()
        val cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(
            CMSProcessableByteArray(inputStream),
            outputEncryptor
        )

        // 创建加密邮件
        val encryptedMessage = MimeMessage(originalMessage.session)
        encryptedMessage.setRecipients(Message.RecipientType.TO, originalMessage.getRecipients(Message.RecipientType.TO))
        encryptedMessage.setRecipients(Message.RecipientType.CC, originalMessage.getRecipients(Message.RecipientType.CC))
        encryptedMessage.setRecipients(Message.RecipientType.BCC, originalMessage.getRecipients(Message.RecipientType.BCC))
        encryptedMessage.addFrom(originalMessage.from)
        encryptedMessage.subject = originalMessage.subject
        encryptedMessage.sentDate = originalMessage.sentDate
        encryptedMessage.setContent(cmsEnvelopedData.encoded, "application/pkcs7-mime; name=smime.p7m; smime-type=enveloped-data")
        encryptedMessage.setHeader("Content-Type", "application/pkcs7-mime; name=smime.p7m; smime-type=enveloped-data")
        encryptedMessage.setHeader("Content-Disposition", "attachment; filename=smime.p7m")
        encryptedMessage.setHeader("Content-Description", "S/MIME Encrypted Message")
        encryptedMessage.addHeader("Content-Transfer-Encoding", "base64")
        encryptedMessage.saveChanges()

        return encryptedMessage
    }

}
