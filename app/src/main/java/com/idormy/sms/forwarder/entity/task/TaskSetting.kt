package com.idormy.sms.forwarder.entity.task

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.TYPE_BARK
import com.idormy.sms.forwarder.utils.TYPE_DINGTALK_GROUP_ROBOT
import com.idormy.sms.forwarder.utils.TYPE_EMAIL
import java.io.Serializable

data class TaskSetting(
    val type: Int,
    val title: String,
    val description: String,
    var setting: String = "",
    var position: Int = -1
) : Serializable {

    val iconId: Int
        get() = when (type) {
            TYPE_DINGTALK_GROUP_ROBOT -> R.drawable.icon_dingtalk
            TYPE_EMAIL -> R.drawable.icon_email
            TYPE_BARK -> R.drawable.icon_bark
            else -> R.drawable.icon_sms
        }
}
