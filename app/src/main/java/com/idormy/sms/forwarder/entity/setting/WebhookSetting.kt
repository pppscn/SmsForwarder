package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable
import java.net.Proxy

data class WebhookSetting(
    val method: String = "POST",
    var webServer: String = "",
    val secret: String = "",
    val response: String = "",
    val webParams: String = "",
    val headers: Map<String, String> = mapOf(),
    val proxyType: Proxy.Type = Proxy.Type.DIRECT,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyAuthenticator: Boolean = false,
    val proxyUsername: String = "",
    val proxyPassword: String = "",
) : Serializable {
    fun getMethodCheckId(): Int {
        return when (method) {
            "POST" -> R.id.rb_method_post
            "PUT" -> R.id.rb_method_put
            "PATCH" -> R.id.rb_method_patch
            else -> R.id.rb_method_get
        }
    }

    fun getProxyTypeCheckId(): Int {
        return when (proxyType) {
            Proxy.Type.HTTP -> R.id.rb_proxyHttp
            Proxy.Type.SOCKS -> R.id.rb_proxySocks
            else -> R.id.rb_proxyNone
        }
    }
}