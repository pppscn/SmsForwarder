package cn.ppps.forwarder.entity.condition

import android.os.BatteryManager
import cn.ppps.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class ChargeSetting(
    var description: String = "",
    var statusList: List<Int> = emptyList(),  // 多选状态列表
    var pluggedList: List<Int> = emptyList(),  // 多选充电方式列表，空=不限
    // 旧版字段，保留以兼容已存储的历史数据
    var status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    var plugged: Int = BatteryManager.BATTERY_PLUGGED_AC,
) : Serializable {

    constructor(statusCheckIds: List<Int>, pluggedCheckIds: List<Int>) : this() {
        statusList = statusCheckIds.mapNotNull { id ->
            when (id) {
                R.id.cb_battery_charging -> BatteryManager.BATTERY_STATUS_CHARGING
                R.id.cb_battery_discharging -> BatteryManager.BATTERY_STATUS_DISCHARGING
                R.id.cb_battery_not_charging -> BatteryManager.BATTERY_STATUS_NOT_CHARGING
                R.id.cb_battery_full -> BatteryManager.BATTERY_STATUS_FULL
                R.id.cb_battery_unknown -> BatteryManager.BATTERY_STATUS_UNKNOWN
                else -> null
            }
        }
        pluggedList = if (pluggedCheckIds.contains(R.id.cb_plugged_unlimited)) {
            emptyList() // 不限 = 任意充电方式
        } else {
            pluggedCheckIds.mapNotNull { id ->
                when (id) {
                    R.id.cb_plugged_ac -> BatteryManager.BATTERY_PLUGGED_AC
                    R.id.cb_plugged_usb -> BatteryManager.BATTERY_PLUGGED_USB
                    R.id.cb_plugged_wireless -> BatteryManager.BATTERY_PLUGGED_WIRELESS
                    else -> null
                }
            }
        }
        description = buildDescription()
    }

    // 兼容旧数据：statusList 为空时回退到旧字段 status
    private fun getEffectiveStatusList(): List<Int> =
        statusList.ifEmpty { listOf(status) }

    // 兼容旧数据：statusList 为空时代表旧格式，回退到旧字段 plugged
    private fun getEffectivePluggedList(): List<Int> {
        if (statusList.isEmpty()) return if (plugged != 0) listOf(plugged) else emptyList()
        return pluggedList // 新格式：空列表 = 任意充电方式
    }

    private fun buildDescription(): String {
        val statusStr = getEffectiveStatusList().joinToString("/") { getStatusStr(it) }
        val effectivePlugged = getEffectivePluggedList()
        val pluggedStr = if (effectivePlugged.isEmpty()) getString(R.string.battery_unlimited)
                         else effectivePlugged.joinToString("/") { getPluggedStr(it) }
        return String.format(getString(R.string.battery_status), statusStr) + ", " +
               String.format(getString(R.string.battery_plugged), pluggedStr)
    }

    private fun getStatusStr(s: Int): String = when (s) {
        BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.battery_charging)
        BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.battery_discharging)
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> getString(R.string.battery_not_charging)
        BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.battery_full)
        else -> getString(R.string.battery_unknown)
    }

    private fun getPluggedStr(p: Int): String = when (p) {
        BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.battery_ac)
        BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.battery_usb)
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.battery_wireless)
        else -> getString(R.string.battery_unlimited)
    }

    fun getStatusCheckIds(): List<Int> = getEffectiveStatusList().mapNotNull { s ->
        when (s) {
            BatteryManager.BATTERY_STATUS_CHARGING -> R.id.cb_battery_charging
            BatteryManager.BATTERY_STATUS_DISCHARGING -> R.id.cb_battery_discharging
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.id.cb_battery_not_charging
            BatteryManager.BATTERY_STATUS_FULL -> R.id.cb_battery_full
            BatteryManager.BATTERY_STATUS_UNKNOWN -> R.id.cb_battery_unknown
            else -> null
        }
    }

    fun getPluggedCheckIds(): List<Int> {
        val effective = getEffectivePluggedList()
        if (effective.isEmpty()) return listOf(R.id.cb_plugged_unlimited)
        return effective.mapNotNull { p ->
            when (p) {
                BatteryManager.BATTERY_PLUGGED_AC -> R.id.cb_plugged_ac
                BatteryManager.BATTERY_PLUGGED_USB -> R.id.cb_plugged_usb
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.id.cb_plugged_wireless
                else -> null
            }
        }
    }

    fun getMsg(statusNew: Int, statusOld: Int, pluggedNew: Int, pluggedOld: Int, batteryInfo: String): String {
        val effectiveStatusList = getEffectiveStatusList()
        val effectivePluggedList = getEffectivePluggedList()
        if (effectiveStatusList.isNotEmpty() && statusNew !in effectiveStatusList) return ""
        if (effectivePluggedList.isNotEmpty() && pluggedNew !in effectivePluggedList) return ""
        return getString(R.string.battery_status_changed) + getStatusStr(statusOld) + "(" + getPluggedStr(pluggedOld) + ") → " + getStatusStr(statusNew) + "(" + getPluggedStr(pluggedNew) + ")" + batteryInfo
    }
}
