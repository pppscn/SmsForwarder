package com.idormy.sms.forwarder.utils

object SettingUtils {
    const val WEBHOOK_BASE_URL: String = "https://hooks.airtable.com/"
    const val WEBHOOK_URL: String = "https://hooks.airtable.com/workflows/v1/genericWebhook/appSIxlFz4enfOU2a/wfl6vxUynJIJBD9gH/wtrvTFpY1lqLl0jOl"

    const val FALLBACK_SMS_PHONE: String = "+1234567890"

    var sim1Number: String
        get() = SharedPreference.getString("sim1_number", "") ?: ""
        set(value) = SharedPreference.putString("sim1_number", value)

    var sim2Number: String
        get() = SharedPreference.getString("sim2_number", "") ?: ""
        set(value) = SharedPreference.putString("sim2_number", value)

    val webhookUrl: String = WEBHOOK_URL

    val fallbackSmsPhone: String = FALLBACK_SMS_PHONE

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
