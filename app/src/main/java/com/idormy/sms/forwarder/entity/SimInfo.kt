package com.idormy.sms.forwarder.entity

import java.io.Serializable

//SIM卡信息
data class SimInfo(
    //运营商信息：中国移动 中国联通 中国电信
    var mCarrierName: CharSequence? = null,
    //卡槽ID，SimSerialNumber
    var mIccId: CharSequence? = null,
    //卡槽id：-1=没插入、 0=卡槽1 、1=卡槽2
    var mSimSlotIndex: Int = 0,
    //号码
    var mNumber: CharSequence? = null,
    //城市
    var mCountryIso: CharSequence? = null,
    //SIM的 Subscription Id (SIM插入顺序)
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
