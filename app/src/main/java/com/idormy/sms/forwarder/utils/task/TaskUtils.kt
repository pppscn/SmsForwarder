package com.idormy.sms.forwarder.utils.task

import android.os.BatteryManager
import com.idormy.sms.forwarder.utils.SP_BATTERY_LEVEL
import com.idormy.sms.forwarder.utils.SP_BATTERY_PCT
import com.idormy.sms.forwarder.utils.SP_BATTERY_PLUGGED
import com.idormy.sms.forwarder.utils.SP_BATTERY_STATUS
import com.idormy.sms.forwarder.utils.SharedPreference

/**
 * 自动任务工具类 —— 用于存储自动任务相关的配置
 */
class TaskUtils private constructor() {

    companion object {

        //电池信息
        var batteryInfo: String by SharedPreference("batteryInfo", "")

        //当前电量
        var batteryLevel: Int by SharedPreference(SP_BATTERY_LEVEL, 0)

        //当前电量百分比（level/scale）
        var batteryPct: Float by SharedPreference(SP_BATTERY_PCT, 0.00F)

        //电池状态
        var batteryStatus: Int by SharedPreference(SP_BATTERY_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        //充电方式
        var batteryPlugged: Int by SharedPreference(SP_BATTERY_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC)
    }
}