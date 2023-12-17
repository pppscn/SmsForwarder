package com.idormy.sms.forwarder.entity.condition

import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class SimSetting(
    var description: String = "", //描述
    var simState: Int = 0, //SIM卡状态：0-未知状态，1-卡被移除，5-卡已准备就绪
) : Serializable {

    constructor(simStateCheckId: Int) : this() {
        simState = when (simStateCheckId) {
            R.id.rb_sim_state_absent -> 1
            R.id.rb_sim_state_ready -> 5
            else -> 0
        }
        description = String.format(
            getString(R.string.sim_state),
            when (simState) {
                1 -> getString(R.string.sim_state_absent)
                5 -> getString(R.string.sim_state_ready)
                else -> getString(R.string.sim_state_unknown)
            }
        )
    }

    fun getSimStateCheckId(): Int {
        return when (simState) {
            1 -> R.id.rb_sim_state_absent
            5 -> R.id.rb_sim_state_ready
            else -> R.id.rb_sim_state_unknown
        }
    }
}
