package com.idormy.sms.forwarder.entity.result

data class SendResponse(
    var logId: Long,
    var status: Int = 0,
    var response: String = "",
)