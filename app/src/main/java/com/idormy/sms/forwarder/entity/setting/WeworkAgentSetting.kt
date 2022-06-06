package com.idormy.sms.forwarder.entity.setting

import java.io.Serializable

data class WeworkAgentSetting(
    var corpID: String = "",
    val agentID: String = "",
    val secret: String = "",
    val atAll: Boolean? = false,
    val toUser: String? = "@all",
) : Serializable