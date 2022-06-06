package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class FeishuSetting(
    var webhook: String = "",
    val secret: String? = "",
    val msgType: String? = "interactive",
    val titleTemplate: String? = "",
) : Serializable {

    fun getMsgTypeCheckId(): Int {
        return if (msgType == null || msgType == "interactive") {
            R.id.rb_msg_type_interactive
        } else {
            R.id.rb_msg_type_text
        }
    }
}