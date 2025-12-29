package com.idormy.sms.forwarder.utils.sender

import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.google.gson.Gson
import com.xuexiang.xhttp2.XHttp
import okhttp3.*
import java.io.IOException

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

            val msgMap = mutableMapOf(
                "sender" to msgInfo.from,
                "receiver" to msgInfo.simInfo,
                "content" to msgInfo.content,
                "time" to msgInfo.date.toString()
            )
            if (!msgInfo.otp.isNullOrEmpty()) {
                msgMap["otp"] = msgInfo.otp!!
            }
            val jsonMsg = Gson().toJson(msgMap)

            val requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonMsg
            )

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            XHttp.getOkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Webhook failed: " + (e.message ?: "Unknown error"))
                    SendUtils.senderLogic(0, msgInfo)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (response.isSuccessful) {
                            try {
                                val responseStr = response.body()?.string() ?: ""
                                Log.i(TAG, "Webhook success: $responseStr")
                                SendUtils.senderLogic(2, msgInfo)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to read response: " + e.message)
                                SendUtils.senderLogic(2, msgInfo) // Still treat as success if we got a response
                            }
                        } else {
                            Log.e(TAG, "Webhook failed: Code: ${response.code()}, Message: ${response.message()}")
                            SendUtils.senderLogic(0, msgInfo)
                        }
                    }
                }
            })
        }
    }
}
