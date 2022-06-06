package com.idormy.sms.forwarder.entity.result

data class GotifyResult(
    //失败返回
    var errorCode: Long?,
    var error: String?,
    var errorDescription: String?,
    //成功返回
    var id: Long?,
    var appid: Long?,
    var title: String?,
    var message: String?,
    var priority: Long?,
    var date: String?,
)