package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CallQueryData(
    // 短信类型: 1=接收, 2=发送
    @SerializedName("type")
    var type: Int = 1,
    @SerializedName("page_num")
    var pageNum: Int = 1,
    @SerializedName("page_size")
    var pageSize: Int = 10,
    @SerializedName("phone_number")
    var phoneNumber: String? = "",
) : Serializable