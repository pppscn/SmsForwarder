package com.idormy.sms.forwarder.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

//SIM卡信息
data class SimInfo(
    //运营商信息：中国移动 中国联通 中国电信
    @SerializedName("carrier_name")
    var mCarrierName: String? = null,
    //集成电路卡识别码即SIM卡卡号
    @SerializedName("icc_id")
    var mIccId: String? = null,
    //卡槽id：-1=没插入、 0=卡槽1 、1=卡槽2
    @SerializedName("sim_slot_index")
    var mSimSlotIndex: Int = 0,
    //号码
    @SerializedName("number")
    var mNumber: String? = null,
    //国家代码
    @SerializedName("country_iso")
    var mCountryIso: String? = null,
    //SIM的 Subscription Id (SIM插入顺序)
    @SerializedName("subscription_id")
    var mSubscriptionId: Int = 0,
) : Serializable {
    override fun toString(): String {
        return "SimInfo{" +
                "mCarrierName=" + mCarrierName +
                ", mIccId=" + mIccId +
                ", mSimSlotIndex=" + mSimSlotIndex +
                ", mNumber=" + mNumber +
                ", mCountryIso=" + mCountryIso +
                ", mSubscriptionId=" + mSubscriptionId +
                '}'
    }
}
