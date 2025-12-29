package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.sender.SmsUtils
import com.idormy.sms.forwarder.utils.sender.WebhookUtils
import com.xuexiang.xutil.net.NetworkUtils

object SendUtils {
    private const val TAG = "SendUtils"

    fun sendMsg(msgInfo: MsgInfo) {
        Log.d(TAG, "sendMsg: $msgInfo")
        if (!SettingUtils.enableSmsForwarding) {
            Log.d(TAG, "SMS forwarding is disabled")
            return
        }

        if (NetworkUtils.isHaveInternet()) {
            Log.d(TAG, "Internet available, sending to webhook")
            WebhookUtils.sendMsg(msgInfo)
        } else {
            Log.d(TAG, "No internet, skipping SMS fallback")
        }
    }

    fun senderLogic(status: Int, msgInfo: MsgInfo) {
        if (status == 0 && NetworkUtils.isHaveInternet()) {
            Log.d(TAG, "Webhook failed, skipping SMS fallback")
        }
    }
}
