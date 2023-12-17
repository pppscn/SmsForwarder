package com.idormy.sms.forwarder.entity.condition

import android.os.BatteryManager
import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
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

    fun getMsg(statusNew: Int, levelNew: Int, levelOld: Int, batteryInfo: String): String {

        when (statusNew) {
            BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> { //充电中
                if (status != BatteryManager.BATTERY_STATUS_CHARGING) return ""
                if (keepReminding && levelOld < levelNew && levelNew >= levelMax) {
                    return String.format(getString(R.string.over_level_max), batteryInfo)
                } else if (!keepReminding && levelOld < levelNew && levelNew == levelMax) {
                    return String.format(getString(R.string.reach_level_max), batteryInfo)
                }
            }

            BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> { //放电中
                if (status != BatteryManager.BATTERY_STATUS_DISCHARGING) return ""
                if (keepReminding && levelOld > levelNew && levelNew <= levelMin) {
                    return String.format(getString(R.string.below_level_min), batteryInfo)
                } else if (!keepReminding && levelOld > levelNew && levelNew == levelMin) {
                    return String.format(getString(R.string.reach_level_min), batteryInfo)
                }
            }
        }

        return ""

    }
}
