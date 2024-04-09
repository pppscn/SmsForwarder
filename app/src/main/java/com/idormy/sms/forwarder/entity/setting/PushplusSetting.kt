package com.idormy.sms.forwarder.entity.setting

import java.io.Serializable

data class PushplusSetting(
    var website: String = "www.pushplus.plus",
    var token: String = "",
    val topic: String = "",
    val template: String = "",
    val channel: String = "",
    val webhook: String = "",
    val callbackUrl: String = "",
    val validTime: String = "",
    val titleTemplate: String = "",
) : Serializable