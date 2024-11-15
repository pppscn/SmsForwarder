package com.idormy.sms.forwarder.utils.sender

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.EmailSetting
import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.mail.EmailSender
import com.xuexiang.xutil.resource.ResUtils.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.key.info.KeyRingInfo
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class EmailUtils {
    companion object {

        private val TAG: String = EmailUtils::class.java.simpleName

        fun sendMsg(
            setting: EmailSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.title, rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.title)
            }
            val message: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            //常用邮箱类型的转换
            when (setting.mailType) {
                "@qq.com", "@foxmail.com" -> {
                    setting.host = "smtp.qq.com"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@exmail.qq.com" -> {
                    setting.host = "smtp.exmail.qq.com"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@msn.com" -> {
                    setting.host = "smtp-mail.outlook.com"
                    setting.port = "587"
                    setting.ssl = false
                    setting.startTls = true
                    setting.fromEmail += setting.mailType
                }

                "@outlook.com", "@office365.com", "@live.com", "@hotmail.com" -> {
                    setting.host = "smtp.office365.com"
                    setting.port = "587"
                    setting.ssl = false
                    setting.startTls = true
                    setting.fromEmail += setting.mailType
                }

                "@gmail.com" -> {
                    setting.host = "smtp.gmail.com"
                    setting.port = "465"
                    setting.ssl = true
                    //setting.startTls = true
                    setting.fromEmail += setting.mailType
                }

                "@yeah.net" -> {
                    setting.host = "smtp.yeah.net"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@163.com" -> {
                    setting.host = "smtp.163.com"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@126.com" -> {
                    setting.host = "smtp.126.com"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@sina.com" -> {
                    setting.host = "smtp.sina.com"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@sina.cn" -> {
                    setting.host = "smtp.sina.cn"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@139.com" -> {
                    setting.host = "smtp.139.com"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@189.cn" -> {
                    setting.host = "smtp.189.cn"
                    setting.port = "465"
                    setting.ssl = true
                    setting.fromEmail += setting.mailType
                }

                "@icloud.com" -> {
                    setting.host = "smtp.mail.me.com"
                    setting.port = "587"
                    setting.ssl = false
                    setting.startTls = true
                    setting.fromEmail += setting.mailType
                }

                else -> {}
            }

            runBlocking {
                val job = launch(Dispatchers.IO) {
                    try {
                        // 设置邮件参数
                        val host = setting.host
                        val port = setting.port
                        val from = setting.fromEmail
                        val password = setting.pwd
                        val nickname = msgInfo.getTitleForSend(setting.nickname)
                        setting.recipients.ifEmpty {
                            //兼容旧的设置
                            val emails = setting.toEmail.replace("[,，;；]".toRegex(), ",").trim(',').split(',')
                            emails.forEach {
                                setting.recipients[it] = Pair("", "")
                            }
                        }
                        val content = message.replace("\n", "<br>")
                        val openSSL = setting.ssl
                        val startTls = setting.startTls

                        //发件人S/MIME私钥（用于签名）
                        var signingPrivateKey: PrivateKey? = null
                        var signingCertificate: X509Certificate? = null
                        //发件人OpenPGP私钥（用于签名）
                        var senderPGPSecretKeyRing: PGPSecretKeyRing? = null
                        var senderPGPSecretKeyPassword = ""

                        if (setting.keystore.isNotEmpty() && setting.password.isNotEmpty()) {
                            try {
                                val keystoreStream = if (setting.keystore.startsWith("/")) {
                                    FileInputStream(setting.keystore)
                                } else {
                                    val decodedBytes = Base64.decode(setting.keystore)
                                    ByteArrayInputStream(decodedBytes)
                                }
                                when (setting.encryptionProtocol) {
                                    "S/MIME" -> {
                                        val keystorePassword = setting.password
                                        val keyStore = KeyStore.getInstance("PKCS12")
                                        keyStore.load(keystoreStream, keystorePassword.toCharArray())
                                        val privateKeyAlias = keyStore.aliases().toList().first { keyStore.isKeyEntry(it) }
                                        signingPrivateKey = keyStore.getKey(privateKeyAlias, keystorePassword.toCharArray()) as PrivateKey
                                        signingCertificate = keyStore.getCertificate(privateKeyAlias) as X509Certificate
                                    }

                                    "OpenPGP" -> {
                                        senderPGPSecretKeyRing = PGPainless.readKeyRing().secretKeyRing(keystoreStream)
                                        senderPGPSecretKeyPassword = setting.password
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.w(TAG, "Failed to load keystore: ${e.message}")
                            }
                        }

                        // 发送结果监听器
                        val listener = object : EmailSender.EmailTaskListener {
                            override fun onEmailSent(success: Boolean, message: String) {
                                if (success) {
                                    SendUtils.updateLogs(logId, 2, getString(R.string.request_succeeded) + ": " + message)
                                    SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
                                } else {
                                    val status = 0
                                    SendUtils.updateLogs(logId, status, message)
                                    SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                                }
                            }
                        }

                        //逐一发送加密邮件
                        val recipientsWithoutCert = mutableListOf<String>()
                        setting.recipients.forEach { (email, cert) ->
                            val keystoreBase64 = cert.first
                            val keystorePassword = cert.second
                            var recipientX509Cert: X509Certificate? = null
                            var recipientPGPPublicKeyRing: PGPPublicKeyRing? = null
                            try {
                                when {
                                    //从私钥证书文件提取公钥
                                    keystoreBase64.isNotEmpty() && keystorePassword.isNotEmpty() -> {
                                        val keystoreStream = if (keystoreBase64.startsWith("/")) {
                                            FileInputStream(keystoreBase64)
                                        } else {
                                            val decodedBytes = Base64.decode(keystoreBase64)
                                            ByteArrayInputStream(decodedBytes)
                                        }

                                        when (setting.encryptionProtocol) {
                                            "S/MIME" -> {
                                                val keyStore = KeyStore.getInstance("PKCS12")
                                                keyStore.load(keystoreStream, keystorePassword.toCharArray())
                                                val alias = keyStore.aliases().nextElement()
                                                recipientX509Cert = keyStore.getCertificate(alias) as X509Certificate
                                            }

                                            "OpenPGP" -> {
                                                val recipientPGPSecretKeyRing = PGPainless.readKeyRing().secretKeyRing(keystoreStream)
                                                recipientPGPPublicKeyRing = recipientPGPSecretKeyRing?.let { PGPainless.extractCertificate(it) }
                                                if (recipientPGPPublicKeyRing != null) {
                                                    val keyInfo = KeyRingInfo(recipientPGPPublicKeyRing)
                                                    Log.d(TAG, "Recipient key info: $keyInfo")
                                                }
                                            }
                                        }
                                    }

                                    //从证书文件提取公钥
                                    keystoreBase64.isNotEmpty() && keystorePassword.isEmpty() -> {
                                        val keystoreStream = if (keystoreBase64.startsWith("/")) {
                                            FileInputStream(keystoreBase64)
                                        } else {
                                            val decodedBytes = Base64.decode(keystoreBase64)
                                            ByteArrayInputStream(decodedBytes)
                                        }

                                        when (setting.encryptionProtocol) {
                                            "S/MIME" -> {
                                                val certFactory = CertificateFactory.getInstance("X.509")
                                                recipientX509Cert = certFactory.generateCertificate(FileInputStream(keystoreBase64)) as X509Certificate
                                            }

                                            "OpenPGP" -> {
                                                recipientPGPPublicKeyRing = PGPainless.readKeyRing().publicKeyRing(keystoreStream)
                                                if (recipientPGPPublicKeyRing != null) {
                                                    val keyInfo = KeyRingInfo(recipientPGPPublicKeyRing)
                                                    Log.d(TAG, "Recipient key info: $keyInfo")
                                                }
                                            }
                                        }
                                    }

                                    else -> {
                                        recipientsWithoutCert.add(email)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.w(TAG, "Failed to load recipient($email) keystore($cert): ${e.message}")
                                //无法加载证书时，发送明文邮件
                                recipientsWithoutCert.add(email)
                            }

                            if (recipientX509Cert != null || recipientPGPPublicKeyRing != null) {
                                val senderWithRecipientCert = EmailSender(
                                    host,
                                    port,
                                    from,
                                    password,
                                    nickname,
                                    title,
                                    content,
                                    toAddress = mutableListOf(email),
                                    listener = listener,
                                    openSSL = openSSL,
                                    startTls = startTls,
                                    encryptionProtocol = setting.encryptionProtocol,
                                    recipientX509Cert = recipientX509Cert,
                                    senderPrivateKey = signingPrivateKey,
                                    senderX509Cert = signingCertificate,
                                    recipientPGPPublicKeyRing = recipientPGPPublicKeyRing,
                                    senderPGPSecretKeyRing = senderPGPSecretKeyRing,
                                    senderPGPSecretKeyPassword = senderPGPSecretKeyPassword,
                                )
                                senderWithRecipientCert.sendEmail()
                            }
                        }

                        //批量发送明文邮件
                        if (recipientsWithoutCert.isNotEmpty()) {
                            val senderWithoutRecipientCert = EmailSender(
                                host,
                                port,
                                from,
                                password,
                                nickname,
                                title,
                                content,
                                toAddress = recipientsWithoutCert,
                                listener = listener,
                                openSSL = openSSL,
                                startTls = startTls,
                                encryptionProtocol = setting.encryptionProtocol,
                                senderPrivateKey = signingPrivateKey,
                                senderX509Cert = signingCertificate,
                                //TODO: OpenPGP 只签名不加密时，提示无效的数字签名，暂未解决
                                senderPGPSecretKeyRing = senderPGPSecretKeyRing,
                                senderPGPSecretKeyPassword = senderPGPSecretKeyPassword,
                            )
                            senderWithoutRecipientCert.sendEmail()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, e.message.toString())
                        val status = 0
                        SendUtils.updateLogs(logId, status, e.message.toString())
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }
                }
                job.join() // 等待协程完成
            }

        }

    }
}
