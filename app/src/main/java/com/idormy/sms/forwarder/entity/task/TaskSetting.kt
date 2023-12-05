package com.idormy.sms.forwarder.entity.task

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.TASK_ACTION_HTTPSERVER
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BATTERY
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CHARGE
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CRON
import com.idormy.sms.forwarder.utils.TASK_CONDITION_NETWORK
import java.io.Serializable

data class TaskSetting(
    val type: Int, // TASK_CONDITION_FRAGMENT_LIST 索引加上 KEY_BACK_CODE_CONDITION 或者 TASK_ACTION_FRAGMENT_LIST 索引加上 KEY_BACK_CODE_ACTION
    val title: String,
    val description: String,
    var setting: String = "",
    var position: Int = -1
) : Serializable {

    val iconId: Int
        get() = when (type) {
            TASK_CONDITION_CRON -> R.drawable.auto_task_icon_custom_time
            TASK_CONDITION_BATTERY -> R.drawable.auto_task_icon_battery
            TASK_CONDITION_CHARGE -> R.drawable.auto_task_icon_charge
            TASK_CONDITION_NETWORK -> R.drawable.auto_task_icon_wlan
            TASK_ACTION_SENDSMS -> R.drawable.auto_task_icon_sms
            TASK_ACTION_NOTIFICATION -> R.drawable.auto_task_icon_sender
            TASK_ACTION_FRPC -> R.drawable.auto_task_icon_frpc
            TASK_ACTION_HTTPSERVER -> R.drawable.auto_task_icon_http_server
            else -> R.drawable.auto_task_icon_sms
        }
}
