package com.idormy.sms.forwarder.utils.sender

import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.EmailSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.mail.Mail
import com.idormy.sms.forwarder.utils.mail.MailSender
import com.xuexiang.xutil.resource.ResUtils.getString

class EmailUtils {
    companion object {

        //private val TAG: String = EmailUtils::class.java.simpleName

        fun sendMsg(
            setting: EmailSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.title.toString(), rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.title.toString())
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
                    setting.port = "587"
                    setting.ssl = true
                    setting.startTls = true
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

                else -> {}
            }

            //收件地址
            val toAddressList = setting.toEmail.toString().replace("[,，;；]".toRegex(), ",").trim(',').split(',')

            //创建邮箱
            val mail = Mail().apply {
                mailServerHost = setting.host.toString()
                mailServerPort = setting.port.toString()
                fromAddress = setting.fromEmail.toString()
                fromNickname = msgInfo.getTitleForSend(setting.nickname.toString())
                password = setting.pwd.toString()
                toAddress = toAddressList
                subject = title
                content = message.replace("\n", "<br>")
                openSSL = setting.ssl == true
                startTls = setting.startTls == true
            }

            MailSender.getInstance().sendMail(mail, object : MailSender.OnMailSendListener {
                override fun onError(e: Throwable) {
                    Log.e("MailSender", e.message.toString())
                    val status = 0
                    SendUtils.updateLogs(logId, status, e.message.toString())
                    SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                }

                override fun onSuccess() {
                    SendUtils.updateLogs(logId, 2, getString(R.string.request_succeeded))
                    SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
                }
            })

        }

    }
}