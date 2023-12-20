package com.idormy.sms.forwarder.entity

import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class BatteryInfo(
    var level: String = "",
    var scale: String = "",
    var voltage: String = "",
    var temperature: String = "",
    var status: String = "",
    var health: String = "",
    var plugged: String = "",
) : Serializable {
    override fun toString(): String {
        var msg = ""
        msg += "\n" + String.format(getString(R.string.battery_level), level)
        if (scale != "") msg += "\n" + String.format(getString(R.string.battery_scale), scale)
        if (voltage != "") msg += "\n" + String.format(getString(R.string.battery_voltage), voltage)
        if (temperature != "") msg += "\n" + String.format(getString(R.string.battery_temperature), temperature)
        msg += "\n" + String.format(getString(R.string.battery_status), status)
        msg += "\n" + String.format(getString(R.string.battery_health), health)
        msg += "\n" + String.format(getString(R.string.battery_plugged), plugged)
        return msg
    }
}
