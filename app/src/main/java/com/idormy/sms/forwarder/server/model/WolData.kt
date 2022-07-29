package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WolData(
    @SerializedName("mac")
    var mac: String,
    @SerializedName("ip")
    var ip: String = "",
    @SerializedName("port")
    var port: Int = 9,
) : Serializable