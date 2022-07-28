package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WolData(
    @SerializedName("ip")
    var ip: String,
    @SerializedName("mac")
    var mac: String,
) : Serializable