package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SmsSendData(
    @SerializedName("sim_slot")
    var simSlot: Int,
    @SerializedName("phone_numbers")
    var phoneNumbers: String,
    @SerializedName("msg_content")
    var msgContent: String,
) : Serializable