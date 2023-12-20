package com.idormy.sms.forwarder.entity

import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class LocationInfo(
    var longitude: Double = 0.0,
    var latitude: Double = 0.0,
    var address: String = "",
    var time: String = "",
    var provider: String = ""
) : Serializable {

    override fun toString(): String {
        var msg = ""
        msg += "\n" + String.format(getString(R.string.location_longitude), longitude)
        msg += "\n" + String.format(getString(R.string.location_latitude), latitude)
        if (address != "") msg += "\n" + String.format(getString(R.string.location_address), address)
        if (time != "") msg += "\n" + String.format(getString(R.string.location_time), time)
        if (provider != "") msg += "\n" + String.format(getString(R.string.location_provider), provider)
        return msg + "\n"
    }

}
