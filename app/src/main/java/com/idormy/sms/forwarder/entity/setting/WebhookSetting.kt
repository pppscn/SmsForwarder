package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class WebhookSetting(
    val method: String? = "POST",
    var webServer: String = "",
    val secret: String? = "",
    val webParams: String? = "",
    val headers: Map<String, String>?,
) : Serializable {
    fun getMethodCheckId(): Int {
        return if (method == null || method == "POST") {
            R.id.rb_method_post
        } else {
            R.id.rb_method_get
        }
    }
}