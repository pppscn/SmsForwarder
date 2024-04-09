package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class DingtalkGroupRobotSetting(
    var token: String = "",
    var secret: String = "",
    var atAll: Boolean = false,
    var atMobiles: String = "",
    var atDingtalkIds: String = "",
    var msgtype: String = "text",
    val titleTemplate: String = "",
) : Serializable {

    fun getMsgTypeCheckId(): Int {
        return if (msgtype == "markdown") {
            R.id.rb_msg_type_markdown
        } else {
            R.id.rb_msg_type_text
        }
    }
}