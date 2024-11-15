package com.idormy.sms.forwarder.utils.mail

import android.text.Html
import android.text.Spanned
import com.idormy.sms.forwarder.utils.Log
import com.sun.mail.util.MailSSLSocketFactory
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Properties


@Suppress("PrivatePropertyName", "DEPRECATION")
class EmailSender(
    // SMTP参数
    private val host: String, // SMTP服务器地址
    private val port: String, // SMTP服务器端口
    private val from: String, // 发件人邮箱
    private val password: String, // 发件人邮箱密码/授权码
    // 邮件参数
    private val nickname: String, // 发件人昵称
    private val subject: String, // 邮件主题
    private val body: CharSequence, // 邮件正文
    private val attachFiles: MutableList<File> = mutableListOf(), // 附件
    // 收件人参数
    private val toAddress: MutableList<String> = mutableListOf(), // 收件人邮箱
    private val ccAddress: MutableList<String> = mutableListOf(), // 抄送者邮箱
    private val bccAddress: MutableList<String> = mutableListOf(), // 密送者邮箱
    // 监听器
    private val listener: EmailTaskListener? = null,
    // 安全选项
    private val openSSL: Boolean = false, //是否开启ssl验证 默认关闭
    private val startTls: Boolean = false, //是否开启starttls加密方式 默认关闭
    // 邮件加密方式: S/MIME、OpenPGP、Plain（不传证书）
    private val encryptionProtocol: String = "S/MIME",
    // 邮件 S/MIME 加密和签名
    private val recipientX509Cert: X509Certificate? = null, //收件人公钥（用于加密）
    private val senderPrivateKey: PrivateKey? = null, //发件人私玥（用于签名）
    private val senderX509Cert: X509Certificate? = null, //发件人公玥（用于签名）
    //邮件 PGP 加密和签名
    private var recipientPGPPublicKeyRing: PGPPublicKeyRing? = null, // 收件人公钥（用于加密）
    private var senderPGPSecretKeyRing: PGPSecretKeyRing? = null, // 发件人私钥（用于签名）
    private val senderPGPSecretKeyPassword: String = "", // 发件人私钥密码
) {

    private val TAG: String = EmailSender::class.java.simpleName

    private val properties: Properties = Properties().apply {
        // 设置邮件服务器的主机名
        put("mail.smtp.host", host)
        // 设置邮件服务器的端口号
        put("mail.smtp.port", port)
        // 设置是否需要身份验证
        put("mail.smtp.auth", "true")
        // 设置是否启用 SSL 连接
        if (openSSL) {
            put("mail.smtp.ssl.enable", "true")
            // 使用 TLSv1.2 协议 & 信任所有主机
            val sf = MailSSLSocketFactory("TLSv1.2")
            sf.setTrustedHosts("*")
            put("mail.smtp.ssl.socketFactory", sf)
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }
        // 设置是否启用 TLS 连接
        if (startTls) {
            put("mail.smtp.starttls.enable", "true")
        }
    }

    suspend fun sendEmail() {
        try {
            val authenticator = MailAuthenticator(from, password)
            // 邮件正文
            val html = try {
                if (body is Spanned) Html.toHtml(body) else body.toString()
            } catch (e: Exception) {
                body.toString()
            }

            // 发送 S/MIME 邮件
            when (encryptionProtocol) {
                "S/MIME" -> {
                    val smimeUtils = SmimeUtils(
                        properties,
                        authenticator,
                        from,
                        nickname,
                        subject,
                        html,
                        attachFiles,
                        toAddress,
                        ccAddress,
                        bccAddress,
                        recipientX509Cert,
                        senderPrivateKey,
                        senderX509Cert,
                    )
                    val isEncrypt: Boolean = recipientX509Cert != null
                    val isSign: Boolean = senderX509Cert != null && senderPrivateKey != null
                    Log.d(TAG, "isEncrypt=$isEncrypt, isSign=$isSign")
                    val result = when {
                        isEncrypt && isSign -> smimeUtils.sendSignedAndEncryptedEmail()
                        isEncrypt -> smimeUtils.sendEncryptedEmail()
                        isSign -> smimeUtils.sendSignedEmail()
                        else -> smimeUtils.sendPlainEmail()
                    }
                    listener?.onEmailSent(result.first, result.second)
                }

                "OpenPGP" -> {
                    // 发送 PGP 邮件
                    val pgpEmail = PgpUtils(
                        properties,
                        authenticator,
                        from,
                        nickname,
                        subject,
                        html,
                        attachFiles,
                        toAddress,
                        ccAddress,
                        bccAddress,
                        recipientPGPPublicKeyRing,
                        senderPGPSecretKeyRing,
                        senderPGPSecretKeyPassword,
                    )
                    val isEncrypt: Boolean = recipientPGPPublicKeyRing != null
                    val isSign: Boolean = senderPGPSecretKeyRing != null
                    Log.d(TAG, "isEncrypt=$isEncrypt, isSign=$isSign")
                    val result = when {
                        isEncrypt && isSign -> pgpEmail.sendSignedAndEncryptedEmail()
                        isEncrypt -> pgpEmail.sendEncryptedEmail()
                        isSign -> pgpEmail.sendSignedEmail()
                        else -> pgpEmail.sendPlainEmail()
                    }
                    listener?.onEmailSent(result.first, result.second)
                }

                else -> {
                    // 发送普通邮件
                    val simpleEmail = SmimeUtils(
                        properties,
                        authenticator,
                        from,
                        nickname,
                        subject,
                        html,
                        attachFiles,
                        toAddress,
                        ccAddress,
                        bccAddress,
                    )
                    val result = simpleEmail.sendPlainEmail()
                    listener?.onEmailSent(result.first, result.second)
                }
            }
        } catch (e: Exception) {
            listener?.onEmailSent(false, "Error sending email: ${e.message}")
        }
    }

    interface EmailTaskListener {
        fun onEmailSent(success: Boolean, message: String)
    }

    /**
     * 发件箱auth校验
     */
    private class MailAuthenticator(username: String, private var password: String) : Authenticator() {
        private var userName: String? = username
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(userName, password)
        }
    }
}
