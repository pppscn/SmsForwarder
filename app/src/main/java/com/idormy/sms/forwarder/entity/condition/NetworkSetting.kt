package com.idormy.sms.forwarder.entity.condition

import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class NetworkSetting(
    var description: String = "", //描述
    var networkState: Int = 0, //网络状态：0-没有网络，1-移动网络，2-WiFi，3-以太网, 4-未知
    var dataSimSlot: Int = 0, //数据卡槽：0-不限，1-卡1，2-卡2
    var wifiSsid: String = "", //WiFi名称
) : Serializable {

    constructor(networkStateCheckId: Int, dataSimSlotCheckId: Int, ssid: String) : this() {
        wifiSsid = ssid
        networkState = when (networkStateCheckId) {
            R.id.rb_no_network -> 0
            R.id.rb_net_mobile -> 1
            R.id.rb_net_wifi -> 2
            R.id.rb_net_ethernet -> 3
            else -> 4
        }
        dataSimSlot = when (dataSimSlotCheckId) {
            R.id.rb_data_sim_slot_0 -> 0
            R.id.rb_data_sim_slot_1 -> 1
            R.id.rb_data_sim_slot_2 -> 2
            else -> 0
        }
        description = String.format(
            getString(R.string.network_state),
            when (networkState) {
                0 -> getString(R.string.no_network)
                1 -> getString(R.string.net_mobile)
                2 -> getString(R.string.net_wifi)
                3 -> getString(R.string.net_ethernet)
                else -> getString(R.string.net_unknown)
            }
        )
        if (networkState == 1 && dataSimSlot != 0) {
            description += ", " + getString(R.string.data_sim_index) + ": SIM-" + dataSimSlot
        }
        if (networkState == 2 && wifiSsid.isNotEmpty()) {
            description += ", " + getString(R.string.wifi_ssid) + ": " + wifiSsid
        }
    }

    fun getNetworkStateCheckId(): Int {
        return when (networkState) {
            0 -> R.id.rb_no_network
            1 -> R.id.rb_net_mobile
            2 -> R.id.rb_net_wifi
            3 -> R.id.rb_net_ethernet
            else -> R.id.rb_net_unknown
        }
    }

    fun getDataSimSlotCheckId(): Int {
        return when (dataSimSlot) {
            0 -> R.id.rb_data_sim_slot_0
            1 -> R.id.rb_data_sim_slot_1
            2 -> R.id.rb_data_sim_slot_2
            else -> R.id.rb_data_sim_slot_0
        }
    }
}
