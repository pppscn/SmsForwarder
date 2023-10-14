package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class WebhookSetting(
    val method: String? = "POST",
    var webServer: String = "",
    val secret: String? = "",
    val response: String? = "",
    val webParams: String? = "",
    val headers: Map<String, String>?,
) : Serializable {
    fun getMethodCheckId(): Int {
        return when (method) {
            null, "POST" -> R.id.rb_method_post
            "PUT" -> R.id.rb_method_put
            "PATCH" -> R.id.rb_method_patch
            else -> R.id.rb_method_get
        }
    }
}