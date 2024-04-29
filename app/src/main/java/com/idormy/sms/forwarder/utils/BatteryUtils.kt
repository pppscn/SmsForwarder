package com.idormy.sms.forwarder.utils

import android.content.Intent
import android.os.BatteryManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.BatteryInfo
import com.xuexiang.xutil.resource.ResUtils.getString

@Suppress("MemberVisibilityCanBePrivate")
object BatteryUtils {
    //private const val TAG = "BatteryUtils"

    fun getBatteryInfo(intent: Intent?): BatteryInfo {
        val batteryInfo = BatteryInfo()
        if (intent == null) return batteryInfo

        val level = intent.getIntExtra("level", 0)
        val scale = intent.getIntExtra("scale", 0)
        val voltage = intent.getIntExtra("voltage", 0)
        val temperature = intent.getIntExtra("temperature", 0)
        val status = intent.getIntExtra("status", 0)
        val health = intent.getIntExtra("health", 0)
        val plugged = intent.getIntExtra("plugged", 0)

        batteryInfo.level = "$level%"
        if (scale > 0) batteryInfo.scale = "$scale%"
        if (voltage > 0) batteryInfo.voltage = "${String.format("%.2f", voltage / 1000f)}V"
        if (temperature > 0) batteryInfo.temperature = "${String.format("%.2f", temperature / 10f)}℃"
        batteryInfo.status = getStatus(status)
        batteryInfo.health = getHealth(health)
        batteryInfo.plugged = getPlugged(plugged)
        //Log.i(TAG, batteryInfo.toString())
        return batteryInfo
    }

    fun getStatus(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.battery_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.battery_discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> getString(R.string.battery_not_charging)
            BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.battery_full)
            BatteryManager.BATTERY_STATUS_UNKNOWN -> getString(R.string.battery_unknown)
            else -> getString(R.string.battery_unknown)
        }
    }

    fun getHealth(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_UNKNOWN -> getString(R.string.battery_unknown)
            BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.battery_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.battery_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.battery_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.battery_over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> getString(R.string.battery_unspecified_failure)
            BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.battery_cold)
            else -> getString(R.string.battery_unknown)
        }
    }

    fun getPlugged(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.battery_ac)
            BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.battery_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.battery_wireless)
            else -> getString(R.string.battery_unknown)
        }
    }

}