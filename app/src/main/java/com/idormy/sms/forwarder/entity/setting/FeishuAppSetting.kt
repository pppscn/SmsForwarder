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
    val receiveIdType: String = "user_id",
    val messageCard: String = "", //自定义消息卡片
) : Serializable {

    fun getReceiveIdTypeCheckId(): Int {
        return when (receiveIdType) {
            "open_id" -> R.id.rb_receive_id_type_open_id
            "user_id" -> R.id.rb_receive_id_type_user_id
            "union_id" -> R.id.rb_receive_id_type_union_id
            "email" -> R.id.rb_receive_id_type_email
            "chat_id" -> R.id.rb_receive_id_type_chat_id
            else -> R.id.rb_receive_id_type_user_id
        }
    }

    fun getMsgTypeCheckId(): Int {
        return if (msgType == null || msgType == "interactive") {
            R.id.rb_msg_type_interactive
        } else {
            R.id.rb_msg_type_text
        }
    }
}