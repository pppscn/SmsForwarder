package com.idormy.sms.forwarder.entity

import com.google.gson.annotations.SerializedName
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import java.io.Serializable

data class CloneInfo(
    @SerializedName("version_code")
    var versionCode: Int = 0,
    @SerializedName("version_name")
    var versionName: String? = null,
    @SerializedName("enable_sms")
    var enableSms: Boolean = false,
    @SerializedName("enable_phone")
    var enablePhone: Boolean = false,
    @SerializedName("call_type1")
    var callType1: Boolean = false,
    @SerializedName("call_type2")
    var callType2: Boolean = false,
    @SerializedName("call_type3")
    var callType3: Boolean = false,
    @SerializedName("enable_app_notify")
    var enableAppNotify: Boolean = false,
    @SerializedName("cancel_app_notify")
    var cancelAppNotify: Boolean = false,
    @SerializedName("enable_not_user_present")
    var enableNotUserPresent: Boolean = false,
    @SerializedName("duplicate_messages_limits")
    var duplicateMessagesLimits: Int = 0,
    @SerializedName("enable_battery_receiver")
    var enableBatteryReceiver: Boolean = false,
    @SerializedName("battery_level_min")
    var batteryLevelMin: Int = 0,
    @SerializedName("battery_level_max")
    var batteryLevelMax: Int = 0,
    @SerializedName("battery_level_once")
    var batteryLevelOnce: Boolean = false,
    @SerializedName("enable_battery_cron")
    var enableBatteryCron: Boolean = false,
    @SerializedName("battery_cron_start_time")
    var batteryCronStartTime: String? = null,
    @SerializedName("battery_cron_interval")
    var batteryCronInterval: Int = 0,
    @SerializedName("enable_exclude_from_recents")
    var enableExcludeFromRecents: Boolean = false,
    @SerializedName("enable_play_silence_music")
    var enablePlaySilenceMusic: Boolean = false,
    @SerializedName("request_retry_times")
    var requestRetryTimes: Int = 0,
    @SerializedName("request_delay_time")
    var requestDelayTime: Int = 0,
    @SerializedName("request_timeout")
    var requestTimeout: Int = 0,
    @SerializedName("notify_content")
    var notifyContent: String? = null,
    @SerializedName("enable_sms_template")
    var enableSmsTemplate: Boolean = false,
    @SerializedName("sms_template")
    var smsTemplate: String? = null,
    @SerializedName("enable_help_tip")
    var enableHelpTip: Boolean = false,
    @SerializedName("sender_list")
    var senderList: List<Sender>? = null,
    @SerializedName("rule_list")
    var ruleList: List<Rule>? = null,
) : Serializable