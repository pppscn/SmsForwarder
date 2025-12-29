package com.idormy.sms.forwarder.utils.sender

import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

class WebhookUtils {
    companion object {
        private val TAG: String = WebhookUtils::class.java.simpleName

        fun sendMsg(msgInfo: MsgInfo) {
            val requestUrl = SettingUtils.webhookUrl
            if (requestUrl.isEmpty()) return

            XHttp.post(requestUrl)
                .params("sender", msgInfo.from)
                .params("receiver", msgInfo.simInfo)
                .params("content", msgInfo.content)
                .params("time", msgInfo.date.toString())
                .keepJson(true)
                .execute(object : SimpleCallBack<String>() {
                    override fun onError(e: ApiException) {
                        Log.e(TAG, e.detailMessage ?: "Unknown error")
                        SendUtils.senderLogic(0, msgInfo)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)
                        SendUtils.senderLogic(2, msgInfo)
                    }
                })
        }
    }
}
