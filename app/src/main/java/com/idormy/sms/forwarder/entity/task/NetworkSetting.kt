package com.idormy.sms.forwarder.entity.task

import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class NetworkSetting(
    var description: String = "", //描述
    var networkState: Int = 0, //网络状态：0-没有网络，1-移动网络，2-WiFi，3-以太网, 4-未知
) : Serializable {

    constructor(networkStateCheckId: Int) : this() {
        networkState = when (networkStateCheckId) {
            R.id.rb_no_network -> 0
            R.id.rb_net_mobile -> 1
            R.id.rb_net_wifi -> 2
            R.id.rb_net_ethernet -> 3
            else -> 4
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
}
