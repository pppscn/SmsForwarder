package com.idormy.sms.forwarder.utils.mail

import java.io.File

/**
 * desc: 邮件实体类
 * time: 2019/8/1
 * @author teprinciple
 */
data class Mail(
    var mailServerHost: String = "", // 发件箱邮箱服务器地址
    var mailServerPort: String = "", // 发件箱邮箱服务器端口
    var fromAddress: String = "", // 发件箱
    var fromNickname: String = "", // 发件人昵称
    var password: String = "", // 发件箱授权码（密码）

    var toAddress: List<String> = ArrayList(), // 直接收件人邮箱
    var ccAddress: ArrayList<String> = ArrayList(), // 抄送者邮箱
    var bccAddress: ArrayList<String> = ArrayList(), // 密送者邮箱

    var subject: String = "",  // 邮件主题
    var content: CharSequence = "", // 邮件内容
    var attachFiles: ArrayList<File> = ArrayList(), // 附件

    var openSSL: Boolean = false, //是否开启ssl验证 默认关闭
    var sslFactory: String = "javax.net.ssl.SSLSocketFactory", //SSL构建类名
    var startTls: Boolean = false, //是否开启starttls加密方式 默认关闭
)
