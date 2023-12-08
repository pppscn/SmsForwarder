package com.idormy.sms.forwarder.entity.task

import android.os.BatteryManager
import com.idormy.sms.forwarder.R
import java.io.Serializable

data class BatterySetting(
    var description: String = "", //描述
    var status: Int = BatteryManager.BATTERY_STATUS_CHARGING, //状态
    var levelMin: Int = 1, //电量下限
    var levelMax: Int = 100, //电量上限
    var keepReminding: Boolean = false, //持续提醒
) : Serializable {

    fun getStatusCheckId(): Int {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> R.id.rb_battery_charging
            BatteryManager.BATTERY_STATUS_DISCHARGING -> R.id.rb_battery_discharging
            else -> R.id.rb_battery_charging
        }
    }
}
