package cn.ppps.forwarder.entity.setting

import cn.ppps.forwarder.R
import java.io.Serializable

data class FeishuSetting(
    var webhook: String = "",
    val secret: String = "",
    val msgType: String = "interactive",
    val titleTemplate: String = "",
    val messageCard: String = "", //自定义消息卡片
    val atAll: Boolean = false,   //@所有人
    val atOpenIds: String = "",   //@指定人，逗号分隔的 open_id 或 user_id
) : Serializable {

    fun getMsgTypeCheckId(): Int {
        return if (msgType == "interactive") {
            R.id.rb_msg_type_interactive
        } else {
            R.id.rb_msg_type_text
        }
    }
}