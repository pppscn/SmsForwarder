package com.idormy.sms.forwarder.entity.action

import java.io.Serializable

data class CleanerSetting(
    var description: String = "", //描述
    var days: Int = 0, //自动删除N天前的转发记录
) : Serializable
