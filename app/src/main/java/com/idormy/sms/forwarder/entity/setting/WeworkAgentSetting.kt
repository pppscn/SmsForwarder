package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable
import java.net.Proxy

data class WeworkAgentSetting(
    var corpID: String = "",
    val agentID: String = "",
    val secret: String = "",
    val atAll: Boolean = false,
    val toUser: String = "@all",
    val toParty: String = "",
    val toTag: String = "",
    val proxyType: Proxy.Type = Proxy.Type.DIRECT,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyAuthenticator: Boolean = false,
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    val customizeAPI: String = "https://qyapi.weixin.qq.com",
) : Serializable {

    fun getProxyTypeCheckId(): Int {
        return when (proxyType) {
            Proxy.Type.HTTP -> R.id.rb_proxyHttp
            Proxy.Type.SOCKS -> R.id.rb_proxySocks
            else -> R.id.rb_proxyNone
        }
    }
}