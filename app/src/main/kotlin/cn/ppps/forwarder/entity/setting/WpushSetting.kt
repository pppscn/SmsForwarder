package cn.ppps.forwarder.entity.setting

import java.io.Serializable

data class WpushSetting(
    var apiKey: String = "",
    var channel: String = "wechat",
    var topicCode: String = "",
    var titleTemplate: String = "",
) : Serializable
