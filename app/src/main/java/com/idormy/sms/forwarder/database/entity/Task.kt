package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.TASK_ACTION_HTTPSERVER
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BATTERY
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CHARGE
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CRON
import com.idormy.sms.forwarder.utils.TASK_CONDITION_NETWORK
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SIM
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "Task")
data class Task(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "type", defaultValue = "1") var type: Int = 1, // 任务类型：＜1000为任务模板，>=1000为自定义任务
    @ColumnInfo(name = "name", defaultValue = "") val name: String = "", // 任务名称
    @ColumnInfo(name = "description", defaultValue = "") val description: String = "", // 任务描述
    @ColumnInfo(name = "conditions", defaultValue = "") val conditions: String = "", // 触发条件
    @ColumnInfo(name = "actions", defaultValue = "") val actions: String = "", // 执行动作
    @ColumnInfo(name = "status", defaultValue = "1") var status: Int = 1, // 任务状态
    @ColumnInfo(name = "last_exec_time") var lastExecTime: Date = Date(), // 上次执行时间
    @ColumnInfo(name = "next_exec_time") var nextExecTime: Date = Date(), // 下次执行时间
) : Parcelable {

    val imageId: Int
        get() = when (type) {
            TASK_CONDITION_CRON -> R.drawable.auto_task_icon_custom_time
            TASK_CONDITION_BATTERY -> R.drawable.auto_task_icon_battery
            TASK_CONDITION_CHARGE -> R.drawable.auto_task_icon_charge
            TASK_CONDITION_NETWORK -> R.drawable.auto_task_icon_network
            TASK_CONDITION_SIM -> R.drawable.auto_task_icon_sim
            TASK_ACTION_SENDSMS -> R.drawable.auto_task_icon_sms
            TASK_ACTION_NOTIFICATION -> R.drawable.auto_task_icon_sender
            TASK_ACTION_FRPC -> R.drawable.auto_task_icon_frpc
            TASK_ACTION_HTTPSERVER -> R.drawable.auto_task_icon_http_server
            else -> R.drawable.auto_task_icon_custom_time
        }

    val greyImageId: Int
        get() = when (type) {
            TASK_CONDITION_CRON -> R.drawable.auto_task_icon_custom_time_grey
            TASK_CONDITION_BATTERY -> R.drawable.auto_task_icon_battery_grey
            TASK_CONDITION_CHARGE -> R.drawable.auto_task_icon_charge_grey
            TASK_CONDITION_NETWORK -> R.drawable.auto_task_icon_network_grey
            TASK_CONDITION_SIM -> R.drawable.auto_task_icon_sim_grey
            TASK_ACTION_SENDSMS -> R.drawable.auto_task_icon_sms_grey
            TASK_ACTION_NOTIFICATION -> R.drawable.auto_task_icon_sender_grey
            TASK_ACTION_FRPC -> R.drawable.auto_task_icon_frpc_grey
            TASK_ACTION_HTTPSERVER -> R.drawable.auto_task_icon_http_server_grey
            else -> R.drawable.auto_task_icon_custom_time_grey
        }

    val statusImageId: Int
        get() = when (status) {
            STATUS_OFF -> R.drawable.icon_off
            else -> R.drawable.icon_on
        }

}