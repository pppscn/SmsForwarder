package com.idormy.sms.forwarder.entity.action

import java.io.Serializable

data class HttpServerSetting(
    var description: String = "", //描述
    var action: String = "start", //动作: start=启动, stop=停止
    var startUid: String = "", //手机号码
    var stopUid: String = "", //短信内容
) : Serializable
