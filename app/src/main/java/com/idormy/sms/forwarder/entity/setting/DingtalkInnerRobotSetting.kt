package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable
import java.net.Proxy

data class DingtalkInnerRobotSetting(
    val agentID: String = "",
    val appKey: String = "",
    val appSecret: String = "",
    val userIds: String = "",
    val msgKey: String = "sampleText",
    val titleTemplate: String = "",
    val proxyType: Proxy.Type = Proxy.Type.DIRECT,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyAuthenticator: Boolean = false,
    val proxyUsername: String = "",
    val proxyPassword: String = "",
) : Serializable {

    fun getProxyTypeCheckId(): Int {
        return when (proxyType) {
            Proxy.Type.HTTP -> R.id.rb_proxyHttp
            Proxy.Type.SOCKS -> R.id.rb_proxySocks
            else -> R.id.rb_proxyNone
        }
    }

    fun getMsgTypeCheckId(): Int {
        return if (msgKey == "sampleMarkdown") {
            R.id.rb_msg_type_markdown
        } else {
            R.id.rb_msg_type_text
        }
    }
}