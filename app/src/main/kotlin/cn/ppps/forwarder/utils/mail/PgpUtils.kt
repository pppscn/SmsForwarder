package cn.ppps.forwarder.utils.mail

import jakarta.activation.DataHandler
import jakarta.activation.FileDataSource
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimeUtility
import jakarta.mail.util.ByteArrayDataSource
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
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
import java.security.Security
import java.util.Date
import java.util.Properties

@Suppress("unused", "PrivatePropertyName")
class PgpUtils(
    private val properties: Properties,
    private val session: Session,
    // 邮件参数
    private val from: String, // 发件人邮箱
    private val fromAlias: String, // 发件人邮箱别名(⚠️注意: 如果需要加密/签名，建议使用真实邮箱，避免收件人无法验证签名)
    private val nickname: String, // 发件人昵称
    private val subject: String, // 邮件主题
    private val body: String, // 邮件正文
    private val attachFiles: List<File> = emptyList(), // 附件
    // 收件人参数
    private val toAddress: List<String> = emptyList(), // 收件人邮箱
    private val ccAddress: List<String> = emptyList(), // 抄送者邮箱
    private val bccAddress: List<String> = emptyList(), // 密送者邮箱
    //邮件 PGP 加密和签名
    private val recipientPGPPublicKeyRing: PGPPublicKeyRing? = null, // 收件人公钥（用于加密）
    private val senderPGPSecretKeyRing: PGPSecretKeyRing? = null, // 发件人私钥（用于签名）
    private val senderPGPSecretKeyPassword: String = "" // 发件人私钥密码
) {
    private val TAG: String = PgpUtils::class.java.simpleName

    init {
        // 确保BouncyCastle仅添加一次，避免重复添加异常
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /** 发送明文邮件 */
    fun sendPlainEmail(): Pair<Boolean, String> = try {
        val message = buildOriginalMessage()
        Transport.send(message)
        Pair(true, "Email sent successfully")
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(false, "Failed to send email: ${e.message ?: e.toString()}")
    }

    /** 发送签名邮件 */
    fun sendSignedEmail(): Pair<Boolean, String> = try {
        checkPgpSignParams() // 校验签名必要参数
        val message = buildOriginalMessage()
        signMessage(message) // 直接修改原始message，不复用新建
        Transport.send(message)
        Pair(true, "Signed email sent successfully")
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(false, "Failed to send signed email: ${e.message ?: e.toString()}")
    }

    /** 发送加密邮件 */
    fun sendEncryptedEmail(): Pair<Boolean, String> = try {
        checkPgpEncParams() // 校验加密必要参数
        val message = buildOriginalMessage()
        encryptMessage(message)
        Transport.send(message)
        Pair(true, "Encrypted email sent successfully")
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(false, "Failed to send encrypted email: ${e.message ?: e.toString()}")
    }

    /** 发送签名+加密邮件 */
    fun sendSignedAndEncryptedEmail(): Pair<Boolean, String> = try {
        checkPgpSignParams()
        checkPgpEncParams()
        val message = buildOriginalMessage()
        encryptAndSignMessage(message)
        Transport.send(message)
        Pair(true, "Signed and encrypted email sent successfully")
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(false, "Failed to send signed+encrypted email: ${e.message ?: e.toString()}")
    }

    /** 构建原始 MimeMessage */
    private fun buildOriginalMessage(): MimeMessage {
        val message = MimeMessage(session)
        // 发件人：使用fromAlias（别名），底层关联真实from，解决中文昵称乱码
        message.setFrom(InternetAddress(fromAlias, nickname, "UTF-8"))
        // 收件人：处理空列表，避免空字符串异常
        if (toAddress.isNotEmpty()) {
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress.joinToString(",")))
        }
        if (ccAddress.isNotEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccAddress.joinToString(",")))
        }
        if (bccAddress.isNotEmpty()) {
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccAddress.joinToString(",")))
        }
        // 主题编码，避免中文乱码
        message.subject = MimeUtility.encodeText(subject, "UTF-8", null)
        message.sentDate = Date()

        // 仅传入子类型 "mixed"，符合JavaMail构造器规范
        val multipart = MimeMultipart("mixed")

        val bodyPart = MimeBodyPart()
        bodyPart.setContent(body, "text/html;charset=UTF-8")
        multipart.addBodyPart(bodyPart)

        // 附件处理：优化编码，避免文件名乱码
        attachFiles.forEach { file ->
            val filePart = MimeBodyPart()
            val fileDs = FileDataSource(file)
            filePart.dataHandler = DataHandler(fileDs)
            // 修复：使用MimeUtility.encodeText处理文件名，兼容所有客户端
            filePart.fileName = MimeUtility.encodeText(file.name, "UTF-8", "B")
            multipart.addBodyPart(filePart)
        }

        message.setContent(multipart)
        message.saveChanges() // 保存修改，生成原始字节流
        return message
    }

    /** 签名邮件 */
    private fun signMessage(message: MimeMessage) {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.fromPassword(senderPGPSecretKeyPassword))
        val secretKeyRing = senderPGPSecretKeyRing!!
        // 安全获取第一个用户ID，避免空指针
        val userId = secretKeyRing.secretKey.publicKey.userIDs.asSequence().firstOrNull()
            ?: throw IllegalArgumentException("PGP私钥环中未找到用户ID")

        // 使用BINARY_DOCUMENT适配复合MIME内容（二进制流），不做文本规范化
        val signingOptions = SigningOptions().addInlineSignature(
            protector,
            secretKeyRing,
            userId,
            DocumentSignatureType.BINARY_DOCUMENT
        ).overrideHashAlgorithm(HashAlgorithm.SHA256)

        // 直接对原始message应用PGP签名，替换其内容
        applyPgpToOriginalMessage(message, ProducerOptions.sign(signingOptions))
    }

    /** 加密邮件 */
    private fun encryptMessage(message: MimeMessage) {
        val encryptionOptions = EncryptionOptions.encryptCommunications()
            .addRecipient(recipientPGPPublicKeyRing!!)
        applyPgpToOriginalMessage(message, ProducerOptions.encrypt(encryptionOptions))
    }

    /** 加密+签名邮件 */
    private fun encryptAndSignMessage(message: MimeMessage) {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.fromPassword(senderPGPSecretKeyPassword))
        val secretKeyRing = senderPGPSecretKeyRing!!
        val userId = secretKeyRing.secretKey.publicKey.userIDs.asSequence().firstOrNull()
            ?: throw IllegalArgumentException("PGP私钥环中未找到用户ID")

        val signingOptions = SigningOptions().addInlineSignature(
            protector,
            secretKeyRing,
            userId,
            DocumentSignatureType.BINARY_DOCUMENT
        ).overrideHashAlgorithm(HashAlgorithm.SHA256)

        val encryptionOptions = EncryptionOptions.encryptCommunications()
            .addRecipient(recipientPGPPublicKeyRing!!)

        applyPgpToOriginalMessage(message, ProducerOptions.signAndEncrypt(encryptionOptions, signingOptions))
    }

    /**
     * 应用PGP操作到**原始MimeMessage**，仅替换内容，保留所有原始头信息
     * 解决「签名内容与发送内容不一致」的致命问题
     */
    private fun applyPgpToOriginalMessage(message: MimeMessage, options: ProducerOptions) {
        // 1. 读取原始message的完整字节流（签名/加密的原始内容）
        val originalBaos = ByteArrayOutputStream()
        message.writeTo(originalBaos)
        val originalInput = ByteArrayInputStream(originalBaos.toByteArray())

        // 2. 执行PGP签名/加密操作，生成PGP处理后的字节流
        val pgpBaos = ByteArrayOutputStream()
        val pgpStream = PGPainless.encryptAndOrSign().onOutputStream(pgpBaos).withOptions(options)
        originalInput.copyTo(pgpStream)
        pgpStream.close()

        // 3. 构建PGP/MIME标准的multipart/encrypted结构
        val pgpMultipart = MimeMultipart("encrypted; protocol=\"application/pgp-encrypted\"")

        // 3.1 PGP版本标识部分（PGP/MIME规范要求必须有）
        val versionPart = MimeBodyPart().apply {
            setText("Version: 1")
            setHeader("Content-Type", "application/pgp-encrypted")
            setHeader("Content-Description", "PGP/MIME version identification")
        }

        // 3.2 PGP加密/签名内容部分
        val contentPart = MimeBodyPart().apply {
            val dataSource = ByteArrayDataSource(pgpBaos.toByteArray(), "application/octet-stream")
            dataHandler = DataHandler(dataSource)
            setHeader("Content-Type", "application/octet-stream; name=\"encrypted.asc\"")
            setHeader("Content-Description", "OpenPGP encrypted/signed message")
            setHeader("Content-Disposition", "inline; filename=\"encrypted.asc\"")
        }

        pgpMultipart.addBodyPart(versionPart)
        pgpMultipart.addBodyPart(contentPart)

        // 4. 替换原始message的内容为PGP/MIME结构，保留所有原始头信息
        message.setContent(pgpMultipart)
        message.saveChanges() // 仅保存内容修改，不重建头信息
    }

    // 校验PGP签名必要参数，提前抛异常，避免空指针
    private fun checkPgpSignParams() {
        if (senderPGPSecretKeyRing == null) {
            throw IllegalArgumentException("发送方PGP私钥环（senderPGPSecretKeyRing）不能为空")
        }
        if (senderPGPSecretKeyPassword.isBlank()) {
            throw IllegalArgumentException("发送方PGP私钥密码（senderPGPSecretKeyPassword）不能为空")
        }
    }

    // 校验PGP加密必要参数
    private fun checkPgpEncParams() {
        if (recipientPGPPublicKeyRing == null) {
            throw IllegalArgumentException("接收方PGP公钥环（recipientPGPPublicKeyRing）不能为空")
        }
    }
}