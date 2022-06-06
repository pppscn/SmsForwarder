package com.idormy.sms.forwarder.entity.setting

import java.io.Serializable

data class EmailSetting(
    var mailType: String? = "",
    var fromEmail: String? = "",
    var pwd: String? = "",
    var nickname: String? = "",
    var host: String? = "",
    var port: String? = "",
    var ssl: Boolean? = false,
    var startTls: Boolean? = false,
    var toEmail: String? = "",
    var title: String? = "",
) : Serializable