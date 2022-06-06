package com.idormy.sms.forwarder.entity.result

data class FeishuResult(
    var code: Long,
    var msg: String,
    var data: Any?,
)