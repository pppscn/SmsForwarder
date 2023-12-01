package com.idormy.sms.forwarder.entity.task

import java.io.Serializable

data class CronSetting(
    var expression: String,
    var description: String = "",
) : Serializable
