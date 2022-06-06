package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable
import java.net.Proxy

data class TelegramSetting(
    val method: String? = "POST",
    var apiToken: String = "",
    val chatId: String = "",
    val proxyType: Proxy.Type = Proxy.Type.DIRECT,
    val proxyHost: String? = "",
    val proxyPort: String? = "",
    val proxyAuthenticator: Boolean? = false,
    val proxyUsername: String? = "",
    val proxyPassword: String? = "",
) : Serializable {

    fun getMethodCheckId(): Int {
        return if (method == null || method == "POST") R.id.rb_method_post else R.id.rb_method_get
    }

    fun getProxyTypeCheckId(): Int {
        return when (proxyType) {
            Proxy.Type.HTTP -> R.id.rb_proxyHttp
            Proxy.Type.SOCKS -> R.id.rb_proxySocks
            else -> R.id.rb_proxyNone
        }
    }
}