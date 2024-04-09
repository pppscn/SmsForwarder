package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class FeishuSetting(
    var webhook: String = "",
    val secret: String = "",
    val msgType: String = "interactive",
    val titleTemplate: String = "",
    val messageCard: String = "", //自定义消息卡片
) : Serializable {

    fun getMsgTypeCheckId(): Int {
        return if (msgType == "interactive") {
            R.id.rb_msg_type_interactive
        } else {
            R.id.rb_msg_type_text
        }
    }
}