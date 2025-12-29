package com.idormy.sms.forwarder.utils.sender

import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.google.gson.Gson
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

class WebhookUtils {
    companion object {
        private val TAG: String = WebhookUtils::class.java.simpleName

        fun sendMsg(msgInfo: MsgInfo) {
            val requestUrl = SettingUtils.webhookUrl
            if (requestUrl.isEmpty()) {
                Log.e(TAG, "Webhook URL is empty")
                return
            }

            Log.d(TAG, "Sending message to $requestUrl: $msgInfo")

            // Split the URL into base URL and relative path to avoid "baseUrl == null" issue
            // and ensure correct concatenation.
            var baseUrl = SettingUtils.WEBHOOK_BASE_URL
            var path = requestUrl
            try {
                if (requestUrl.startsWith("http")) {
                    val uri = java.net.URI(requestUrl)
                    val port = if (uri.port != -1) ":${uri.port}" else ""
                    baseUrl = "${uri.scheme}://${uri.host}$port/"
                    
                    // Extract the path and query parts
                    var rawPath = uri.rawPath ?: ""
                    val rawQuery = uri.rawQuery
                    if (rawQuery != null) {
                        rawPath += "?$rawQuery"
                    }
                    
                    // Remove leading slash to avoid // in the final URL
                    path = if (rawPath.startsWith("/")) rawPath.substring(1) else rawPath
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse base URL: " + e.message)
            }

            val msgMap = mapOf(
                "sender" to msgInfo.from,
                "receiver" to msgInfo.simInfo,
                "content" to msgInfo.content,
                "time" to msgInfo.date.toString()
            )
            val jsonMsg = Gson().toJson(msgMap)

            XHttp.post(path)
                .baseUrl(baseUrl)
                .upJson(jsonMsg)
                .execute(object : SimpleCallBack<String>() {
                    override fun onError(e: ApiException) {
                        Log.e(TAG, "Webhook failed: " + (e.detailMessage ?: "Unknown error"))
                        SendUtils.senderLogic(0, msgInfo)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, "Webhook success: $response")
                        SendUtils.senderLogic(2, msgInfo)
                    }
                })
        }
    }
}
