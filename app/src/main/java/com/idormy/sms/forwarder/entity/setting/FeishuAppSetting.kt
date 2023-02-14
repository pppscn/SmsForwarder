package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

@Suppress("SENSELESS_COMPARISON")
data class FeishuAppSetting(
    var appId: String = "",
    val appSecret: String = "",
    val receiveId: String = "",
    val msgType: String = "interactive",
    val titleTemplate: String = "",
) : Serializable {

    fun getMsgTypeCheckId(): Int {
        return if (msgType == null || msgType == "interactive") {
            R.id.rb_msg_type_interactive
        } else {
            R.id.rb_msg_type_text
        }
    }
}