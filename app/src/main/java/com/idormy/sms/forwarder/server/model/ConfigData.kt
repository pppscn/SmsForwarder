package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ConfigData(
    @SerializedName("enable_api_clone")
    var enableApiClone: Boolean = false,
    @SerializedName("enable_api_sms_send")
    var enableApiSmsSend: Boolean = false,
    @SerializedName("enable_api_sms_query")
    var enableApiSmsQuery: Boolean = false,
    @SerializedName("enable_api_call_query")
    var enableApiCallQuery: Boolean = false,
    @SerializedName("enable_api_contact_query")
    var enableApiContactQuery: Boolean = false,
    @SerializedName("enable_api_battery_query")
    var enableApiBatteryQuery: Boolean = false,
) : Serializable