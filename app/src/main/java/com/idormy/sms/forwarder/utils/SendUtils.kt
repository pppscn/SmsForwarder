package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.sender.SmsUtils
import com.idormy.sms.forwarder.utils.sender.WebhookUtils
import com.xuexiang.xutil.net.NetworkUtils

object SendUtils {
    private const val TAG = "SendUtils"

    fun sendMsg(msgInfo: MsgInfo) {
        if (!SettingUtils.enableSmsForwarding) return

        if (NetworkUtils.isHaveInternet()) {
            WebhookUtils.sendMsg(msgInfo)
        } else {
            SmsUtils.sendMsg(msgInfo)
        }
    }

    fun senderLogic(status: Int, msgInfo: MsgInfo) {
        if (status == 0 && NetworkUtils.isHaveInternet()) {
            SmsUtils.sendMsg(msgInfo)
        }
    }
}
