package com.idormy.sms.forwarder.entity

import java.io.Serializable

data class SimInfo(
    var mSubscriptionId: Int,
    var mSimSlotIndex: Int,
    var mCarrierName: String?,
    var mNumber: String?
) : Serializable
