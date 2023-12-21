package com.idormy.sms.forwarder.utils.mail

import android.text.Html
import android.text.Spanned
import com.idormy.sms.forwarder.utils.Log
import com.xuexiang.xrouter.utils.TextUtils
import java.io.UnsupportedEncodingException
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility

/**
 * desc: 邮件帮助类
 * time: 2019/8/1
 * @author teprinciple
 */
@Suppress("DEPRECATION")
object MailUtil {

    /**
     * 创建邮件
     */
    fun createMailMessage(mail: Mail): MimeMessage {
        Log.e("createMailMessage", mail.toString())
        val properties = Properties()
        properties["mail.debug"] = "true"
        properties["mail.smtp.host"] = mail.mailServerHost
        properties["mail.smtp.port"] = mail.mailServerPort
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.ssl.enable"] = mail.openSSL
        if (mail.startTls) {
            properties["mail.smtp.starttls.enable"] = true
        }
        if (mail.openSSL) {
            properties["mail.smtp.socketFactory.class"] = mail.sslFactory
        }
        val authenticator = MailAuthenticator(mail.fromAddress, mail.password)
        val session = Session.getInstance(properties, authenticator)
        session.debug = true

        Log.e("createMailMessage", session.toString())
        return MimeMessage(session).apply {

            // 设置发件箱
            if (TextUtils.isEmpty(mail.fromNickname)) {
                setFrom(InternetAddress(mail.fromAddress))
            } else {
                var nickname = mail.fromNickname.replace(":", "-").replace("\n", "-")
                try {
                    Log.d("createMailMessage", "nickname = $nickname")
                    nickname = MimeUtility.encodeText(nickname)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                    Log.e("createMailMessage", "UnsupportedEncodingException = ${e.message}")
                }

                Log.d("createMailMessage", "nickname = $nickname")
                setFrom(InternetAddress("$nickname <${mail.fromAddress}>"))
            }

            // 设置直接接收者收件箱
            val toAddress = mail.toAddress.map {
                InternetAddress(it)
            }.toTypedArray()
            setRecipients(Message.RecipientType.TO, toAddress)

            // 设置抄送者收件箱
            val ccAddress = mail.ccAddress.map {
                InternetAddress(it)
            }.toTypedArray()
            setRecipients(Message.RecipientType.CC, ccAddress)

            // 设置密送者收件箱
            val bccAddress = mail.bccAddress.map {
                InternetAddress(it)
            }.toTypedArray()
            setRecipients(Message.RecipientType.BCC, bccAddress)

            // 邮件主题
            subject = mail.subject.replace(":", "-").replace("\n", "-")
            try {
                subject = MimeUtility.encodeText(subject)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Log.e("createMailMessage", "UnsupportedEncodingException = ${e.message}")
            }

            // 邮件内容
            val contentPart = MimeMultipart()

            // 邮件正文
            val textBodyPart = MimeBodyPart()
            if (mail.content is Spanned) {
                textBodyPart.setContent(
                    Html.toHtml(mail.content as Spanned),
                    "text/html;charset=UTF-8"
                )
            } else {
                textBodyPart.setContent(mail.content, "text/html;charset=UTF-8")
            }
            contentPart.addBodyPart(textBodyPart)

            // 邮件附件
            mail.attachFiles.forEach {
                val fileBodyPart = MimeBodyPart()
                val ds = FileDataSource(it)
                val dh = DataHandler(ds)
                fileBodyPart.dataHandler = dh
                fileBodyPart.fileName = MimeUtility.encodeText(dh.name)
                contentPart.addBodyPart(fileBodyPart)
            }
            contentPart.setSubType("mixed")
            setContent(contentPart)
            saveChanges()
        }
    }

    /**
     * 发件箱auth校验
     */
    class MailAuthenticator(username: String?, private var password: String?) : Authenticator() {
        private var userName: String? = username
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(userName, password)
        }
    }
}