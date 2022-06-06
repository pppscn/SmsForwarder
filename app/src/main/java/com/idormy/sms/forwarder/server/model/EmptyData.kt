package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EmptyData(
    @SerializedName("version_code")
    var versionCode: Long = 100038L,
) : Serializable
