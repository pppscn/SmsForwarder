package com.idormy.sms.forwarder.utils.task

import android.os.BatteryManager
import com.idormy.sms.forwarder.utils.SP_BATTERY_INFO
import com.idormy.sms.forwarder.utils.SP_BATTERY_LEVEL
import com.idormy.sms.forwarder.utils.SP_BATTERY_PCT
import com.idormy.sms.forwarder.utils.SP_BATTERY_PLUGGED
import com.idormy.sms.forwarder.utils.SP_BATTERY_STATUS
import com.idormy.sms.forwarder.utils.SP_DATA_SIM_SLOT
import com.idormy.sms.forwarder.utils.SP_IPV4
import com.idormy.sms.forwarder.utils.SP_IPV6
import com.idormy.sms.forwarder.utils.SP_NETWORK_STATE
import com.idormy.sms.forwarder.utils.SP_WIFI_SSID
import com.idormy.sms.forwarder.utils.SharedPreference

/**
 * 自动任务工具类 —— 用于存储自动任务相关的配置
 */
class TaskUtils private constructor() {

    companion object {

        //电池信息
        var batteryInfo: String by SharedPreference(SP_BATTERY_INFO, "")

        //当前电量
        var batteryLevel: Int by SharedPreference(SP_BATTERY_LEVEL, 0)

        //当前电量百分比（level/scale）
        var batteryPct: Float by SharedPreference(SP_BATTERY_PCT, 0.00F)

        //电池状态
        var batteryStatus: Int by SharedPreference(SP_BATTERY_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        //充电方式
        var batteryPlugged: Int by SharedPreference(SP_BATTERY_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC)

        //网络状态：0-没有网络，1-移动网络，2-WiFi，3-以太网, 4-未知
        var networkState: Int by SharedPreference(SP_NETWORK_STATE, 0)

        //数据卡槽：0-未知，1-卡1，2-卡2
        var dataSimSlot: Int by SharedPreference(SP_DATA_SIM_SLOT, 0)

        //WiFi名称
        var wifiSsid: String by SharedPreference(SP_WIFI_SSID, "")

        //IPv4地址
        var ipv4: String by SharedPreference(SP_IPV4, "")

        //IPv6地址
        var ipv6: String by SharedPreference(SP_IPV6, "")
    }
}