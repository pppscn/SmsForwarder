package com.idormy.sms.forwarder.entity.condition

import java.io.Serializable

data class CronSetting(
    var description: String = "", //描述
    var expression: String = "", //表达式
) : Serializable
