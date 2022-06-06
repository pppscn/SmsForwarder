package com.idormy.sms.forwarder.server.model

data class BaseResponse<T>(
    var code: Int = 200,
    var msg: String = "",
    var data: T?,
    var timestamp: Long = 0L,
    var sign: String? = "",
)