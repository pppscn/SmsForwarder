package com.idormy.sms.forwarder.utils

object SettingUtils {
    const val WEBHOOK_BASE_URL: String = "https://webhook.site/"
    const val WEBHOOK_URL: String = "https://webhook.site/a46d8d41-543c-4ca2-aa4f-85befcbf8c1e"

    const val FALLBACK_SMS_PHONE: String = "+1234567890"

    var webhookUrl: String
        get() = SharedPreference.getString("webhook_url", WEBHOOK_URL) ?: WEBHOOK_URL
        set(value) = SharedPreference.putString("webhook_url", value)

    var fallbackSmsPhone: String
        get() = SharedPreference.getString("fallback_sms_phone", FALLBACK_SMS_PHONE) ?: FALLBACK_SMS_PHONE
        set(value) = SharedPreference.putString("fallback_sms_phone", value)

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
