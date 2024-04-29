package com.idormy.sms.forwarder.entity.action

import java.io.Serializable

data class SmsSetting(
    var description: String = "", //描述
    var simSlot: Int = 1, //卡槽
    var phoneNumbers: String = "", //手机号码
    var msgContent: String = "", //短信内容
) : Serializable
