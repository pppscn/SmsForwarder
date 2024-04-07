package com.idormy.sms.forwarder.utils.task

import android.bluetooth.BluetoothAdapter
import android.os.BatteryManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.utils.SP_BATTERY_INFO
import com.idormy.sms.forwarder.utils.SP_BATTERY_LEVEL
import com.idormy.sms.forwarder.utils.SP_BATTERY_PCT
import com.idormy.sms.forwarder.utils.SP_BATTERY_PLUGGED
import com.idormy.sms.forwarder.utils.SP_BATTERY_STATUS
import com.idormy.sms.forwarder.utils.SP_BLUETOOTH_STATE
import com.idormy.sms.forwarder.utils.SP_CONNECTED_DEVICE
import com.idormy.sms.forwarder.utils.SP_DATA_SIM_SLOT
import com.idormy.sms.forwarder.utils.SP_DISCOVERED_DEVICES
import com.idormy.sms.forwarder.utils.SP_IPV4
import com.idormy.sms.forwarder.utils.SP_IPV6
import com.idormy.sms.forwarder.utils.SP_IP_LIST
import com.idormy.sms.forwarder.utils.SP_LOCATION_INFO_NEW
import com.idormy.sms.forwarder.utils.SP_LOCATION_INFO_OLD
import com.idormy.sms.forwarder.utils.SP_LOCK_SCREEN_ACTION
import com.idormy.sms.forwarder.utils.SP_NETWORK_STATE
import com.idormy.sms.forwarder.utils.SP_SIM_STATE
import com.idormy.sms.forwarder.utils.SP_WIFI_SSID
import com.idormy.sms.forwarder.utils.SharedPreference
import com.idormy.sms.forwarder.utils.TASK_ACTION_ALARM
import com.idormy.sms.forwarder.utils.TASK_ACTION_CLEANER
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.TASK_ACTION_HTTPSERVER
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_RESEND
import com.idormy.sms.forwarder.utils.TASK_ACTION_RULE
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDER
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TASK_ACTION_SETTINGS
import com.idormy.sms.forwarder.utils.TASK_ACTION_TASK
import com.idormy.sms.forwarder.utils.TASK_CONDITION_APP
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BATTERY
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BLUETOOTH
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CALL
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CHARGE
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CRON
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LEAVE_ADDRESS
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LOCK_SCREEN
import com.idormy.sms.forwarder.utils.TASK_CONDITION_NETWORK
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SIM
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SMS
import com.idormy.sms.forwarder.utils.TASK_CONDITION_TO_ADDRESS

/**
 * 自动任务工具类 —— 用于存储自动任务相关的配置
 */
class TaskUtils private constructor() {

    companion object {

        //获取任务类型图标
        fun getTypeImageId(type: Int): Int {
            return when (type) {
                TASK_CONDITION_CRON -> R.drawable.auto_task_icon_custom_time
                TASK_CONDITION_TO_ADDRESS -> R.drawable.auto_task_icon_to_address
                TASK_CONDITION_LEAVE_ADDRESS -> R.drawable.auto_task_icon_leave_address
                TASK_CONDITION_NETWORK -> R.drawable.auto_task_icon_network
                TASK_CONDITION_SIM -> R.drawable.auto_task_icon_sim
                TASK_CONDITION_BATTERY -> R.drawable.auto_task_icon_battery
                TASK_CONDITION_CHARGE -> R.drawable.auto_task_icon_charge
                TASK_CONDITION_LOCK_SCREEN -> R.drawable.auto_task_icon_lock_screen
                TASK_CONDITION_SMS -> R.drawable.auto_task_icon_sms
                TASK_CONDITION_CALL -> R.drawable.auto_task_icon_incall
                TASK_CONDITION_APP -> R.drawable.auto_task_icon_start_activity
                TASK_CONDITION_BLUETOOTH -> R.drawable.auto_task_icon_bluetooth
                TASK_ACTION_SENDSMS -> R.drawable.auto_task_icon_sms
                TASK_ACTION_NOTIFICATION -> R.drawable.auto_task_icon_notification
                TASK_ACTION_CLEANER -> R.drawable.auto_task_icon_cleaner
                TASK_ACTION_SETTINGS -> R.drawable.auto_task_icon_settings
                TASK_ACTION_FRPC -> R.drawable.auto_task_icon_frpc
                TASK_ACTION_HTTPSERVER -> R.drawable.auto_task_icon_http_server
                TASK_ACTION_RULE -> R.drawable.auto_task_icon_rule
                TASK_ACTION_SENDER -> R.drawable.auto_task_icon_sender
                TASK_ACTION_ALARM -> R.drawable.auto_task_icon_alarm
                TASK_ACTION_RESEND -> R.drawable.auto_task_icon_resend
                TASK_ACTION_TASK -> R.drawable.auto_task_icon_task
                else -> R.drawable.auto_task_icon_custom_time
            }
        }

        //获取任务类型图标（灰色）
        fun getTypeGreyImageId(type: Int): Int {
            return when (type) {
                TASK_CONDITION_CRON -> R.drawable.auto_task_icon_custom_time_grey
                TASK_CONDITION_TO_ADDRESS -> R.drawable.auto_task_icon_to_address_grey
                TASK_CONDITION_LEAVE_ADDRESS -> R.drawable.auto_task_icon_leave_address_grey
                TASK_CONDITION_NETWORK -> R.drawable.auto_task_icon_network_grey
                TASK_CONDITION_SIM -> R.drawable.auto_task_icon_sim_grey
                TASK_CONDITION_BATTERY -> R.drawable.auto_task_icon_battery_grey
                TASK_CONDITION_CHARGE -> R.drawable.auto_task_icon_charge_grey
                TASK_CONDITION_LOCK_SCREEN -> R.drawable.auto_task_icon_lock_screen_grey
                TASK_CONDITION_SMS -> R.drawable.auto_task_icon_sms_grey
                TASK_CONDITION_CALL -> R.drawable.auto_task_icon_incall_grey
                TASK_CONDITION_APP -> R.drawable.auto_task_icon_start_activity_grey
                TASK_CONDITION_BLUETOOTH -> R.drawable.auto_task_icon_bluetooth_grey
                TASK_ACTION_SENDSMS -> R.drawable.auto_task_icon_sms_grey
                TASK_ACTION_NOTIFICATION -> R.drawable.auto_task_icon_notification_grey
                TASK_ACTION_CLEANER -> R.drawable.auto_task_icon_cleaner_grey
                TASK_ACTION_SETTINGS -> R.drawable.auto_task_icon_settings_grey
                TASK_ACTION_FRPC -> R.drawable.auto_task_icon_frpc_grey
                TASK_ACTION_HTTPSERVER -> R.drawable.auto_task_icon_http_server_grey
                TASK_ACTION_RULE -> R.drawable.auto_task_icon_rule_grey
                TASK_ACTION_SENDER -> R.drawable.auto_task_icon_sender_grey
                TASK_ACTION_ALARM -> R.drawable.auto_task_icon_alarm_grey
                TASK_ACTION_RESEND -> R.drawable.auto_task_icon_resend_grey
                TASK_ACTION_TASK -> R.drawable.auto_task_icon_task_grey
                else -> R.drawable.auto_task_icon_custom_time_grey
            }
        }

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

        //IP地址列表
        var ipList: String by SharedPreference(SP_IP_LIST, "")

        //SIM卡状态：0-未知状态，1-卡被移除，5-卡已准备就绪
        var simState: Int by SharedPreference(SP_SIM_STATE, 0)

        //上次定位信息
        var locationInfoOld: LocationInfo by SharedPreference(SP_LOCATION_INFO_OLD, LocationInfo())

        //当前定位信息
        var locationInfoNew: LocationInfo by SharedPreference(SP_LOCATION_INFO_NEW, LocationInfo())

        //上次锁屏广播
        var lockScreenAction: String by SharedPreference(SP_LOCK_SCREEN_ACTION, "")

        //已发现的蓝牙设备
        var discoveredDevices: MutableMap<String, String> by SharedPreference(SP_DISCOVERED_DEVICES, mutableMapOf())

        //已连接的蓝牙设备
        var connectedDevices: MutableMap<String, String> by SharedPreference(SP_CONNECTED_DEVICE, mutableMapOf())

        //蓝牙状态
        var bluetoothState: Int by SharedPreference(SP_BLUETOOTH_STATE, BluetoothAdapter.STATE_ON)

    }
}