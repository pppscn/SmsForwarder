package com.idormy.sms.forwarder.server.model

data class BaseRequest<T>(
    var data: T,
    var timestamp: Long = 0L,
    var sign: String? = "",
)