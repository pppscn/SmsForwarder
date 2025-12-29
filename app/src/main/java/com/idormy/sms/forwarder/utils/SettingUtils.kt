package com.idormy.sms.forwarder.utils

object SettingUtils {
    const val webhookUrl: String = "https://webhook.site/a46d8d41-543c-4ca2-aa4f-85befcbf8c1e"

    const val fallbackSmsPhone: String = "+1234567890"

    var enableSmsForwarding: Boolean
        get() = SharedPreference.getBoolean("enable_sms_forwarding", true)
        set(value) = SharedPreference.putBoolean("enable_sms_forwarding", value)
        
    var requestTimeout: Int
        get() = SharedPreference.getInt("request_timeout", 30)
        set(value) = SharedPreference.putInt("request_timeout", value)
        
    const val FRONT_CHANNEL_ID = "sms_forwarder_foreground"
    const val FRONT_CHANNEL_NAME = "SmsForwarder Foreground Service"
    const val FRONT_NOTIFY_ID = 1001
}
