package com.idormy.sms.forwarder.entity.task

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BATTERY
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CHARGE
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CRON
import com.idormy.sms.forwarder.utils.TASK_CONDITION_WLAN
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
            TASK_CONDITION_CRON -> R.drawable.auto_task_icon_cron
            TASK_CONDITION_BATTERY -> R.drawable.auto_task_icon_battery
            TASK_CONDITION_CHARGE -> R.drawable.auto_task_icon_charge
            TASK_CONDITION_WLAN -> R.drawable.auto_task_icon_wlan
            else -> R.drawable.auto_task_icon_sim
        }
}
