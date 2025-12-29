package com.idormy.sms.forwarder.entity

import java.io.Serializable
import java.util.Date

data class MsgInfo(
    var type: String = "sms",
    var from: String,
    var content: String,
    var date: Date,
    var simInfo: String,
    var simSlot: Int = -1,
    var subId: Int = 0
) : Serializable
