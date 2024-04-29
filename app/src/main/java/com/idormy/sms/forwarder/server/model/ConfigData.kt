package com.idormy.sms.forwarder.server.model

import com.google.gson.annotations.SerializedName
import com.idormy.sms.forwarder.entity.SimInfo
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
    @SerializedName("enable_api_contact_add")
    var enableApiContactAdd: Boolean = false,
    @SerializedName("enable_api_battery_query")
    var enableApiBatteryQuery: Boolean = false,
    @SerializedName("enable_api_wol")
    var enableApiWol: Boolean = false,
    @SerializedName("enable_api_location")
    var enableApiLocation: Boolean = false,
    @SerializedName("extra_device_mark")
    var extraDeviceMark: String = "",
    @SerializedName("extra_sim1")
    var extraSim1: String = "",
    @SerializedName("extra_sim2")
    var extraSim2: String = "",
    @SerializedName("sim_info_list")
    var simInfoList: MutableMap<Int, SimInfo> = mutableMapOf(),
    @SerializedName("version_code")
    var versionCode: Int = 0,
    @SerializedName("version_name")
    var versionName: String = "",
) : Serializable