package com.idormy.sms.forwarder.entity.result

@Suppress("PropertyName")
data class FeishuAppResult(
    var code: Long,
    var msg: String,
    //获取access_token返回
    var tenant_access_token: String?,
    var expire: Long?,
    //发送接口返回
    var content: String?,
)