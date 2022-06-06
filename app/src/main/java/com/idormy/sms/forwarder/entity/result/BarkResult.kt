package com.idormy.sms.forwarder.entity.result

data class BarkResult(
    var code: Long,
    var message: String,
    var timestamp: Long?,
)