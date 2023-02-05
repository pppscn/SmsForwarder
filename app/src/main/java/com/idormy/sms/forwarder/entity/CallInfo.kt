package com.idormy.sms.forwarder.entity

import com.google.gson.annotations.SerializedName
import com.idormy.sms.forwarder.R
import java.io.Serializable

data class CallInfo(
    //姓名
    var name: String = "",
    //号码
    var number: String = "",
    //获取通话日期
    var dateLong: Long = 0L,
    //获取通话时长，值为多少秒
    var duration: Int = 0,
    //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
    var type: Int = 1,
    //被呼号码
    @SerializedName("via_number")
    var viaNumber: String = "",
    //卡槽ID： 0=Sim1, 1=Sim2, -1=获取失败
    @SerializedName("sim_id")
    var simId: Int = -1,
    //卡槽主键
    @SerializedName("sub_id")
    var subId: Int = 0,
) : Serializable {

    val typeImageId: Int
        get() {
            return when (type) {
                1 -> R.drawable.ic_phone_in
                2 -> R.drawable.ic_phone_out
                else -> R.drawable.ic_phone_missed
            }
        }

    val simImageId: Int
        get() {
            return when (simId) {
                0 -> R.drawable.ic_sim1
                1 -> R.drawable.ic_sim2
                else -> R.drawable.ic_sim
            }
        }

    override fun toString(): String {
        return "CallInfo{" +
                "name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", dateLong=" + dateLong +
                ", duration=" + duration +
                ", type=" + type +
                ", viaNumber=" + viaNumber +
                ", simId=" + simId +
                '}'
    }
}