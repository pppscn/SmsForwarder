package com.idormy.sms.forwarder.entity.action

import java.io.Serializable

data class ResendSetting(
    var description: String = "", //描述
    var hours: Int = 1, //自动重发N小时以来的转发记录，0=不限制
    var statusList: List<Int> = listOf(0), //状态列表，默认只重发失败的
) : Serializable
