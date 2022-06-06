package com.idormy.sms.forwarder.entity.setting

import java.io.Serializable

data class DingtalkSetting(
    var token: String = "",
    var secret: String? = "",
    var atAll: Boolean? = false,
    var atMobiles: String? = "",
) : Serializable