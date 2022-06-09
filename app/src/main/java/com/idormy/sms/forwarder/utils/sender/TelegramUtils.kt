package com.idormy.sms.forwarder.utils.sender

import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.TelegramResult
import com.idormy.sms.forwarder.entity.setting.TelegramSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xui.utils.ResUtils
import okhttp3.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER", "unused")
class TelegramUtils {
    companion object {

        private val TAG: String = TelegramUtils::class.java.simpleName

        fun sendMsg(
            setting: TelegramSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val requestUrl = if (setting.apiToken.startsWith("http")) {
                setting.apiToken
            } else {
                "https://api.telegram.org/bot" + setting.apiToken + "/sendMessage"
            }
            Log.i(TAG, "requestUrl:$requestUrl")

            val clientBuilder = OkHttpClient.Builder()
            //设置代理
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS) && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)) {
                //代理服务器的IP和端口号
                clientBuilder.proxy(Proxy(setting.proxyType, setting.proxyPort?.let { InetSocketAddress(setting.proxyHost, it.toInt()) }))

                //代理的鉴权账号密码
                if (setting.proxyAuthenticator == true && (!TextUtils.isEmpty(setting.proxyUsername) || !TextUtils.isEmpty(setting.proxyPassword))) {
                    clientBuilder.proxyAuthenticator { _: Route?, response: Response ->
                        //设置代理服务器账号密码
                        val credential = Credentials.basic(setting.proxyUsername.toString(), setting.proxyPassword.toString())
                        response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build()
                    }
                }
            }

            //设置重试拦截器
            val retryTimes: Int = SettingUtils.requestRetryTimes
            if (retryTimes > 0) {
                val delayTime: Long = SettingUtils.requestDelayTime.toLong()
                val retryInterceptor: RetryInterceptor = RetryInterceptor.Builder().executionCount(retryTimes).retryInterval(delayTime).logId(0).build()
                clientBuilder.addInterceptor(retryInterceptor)
            }


            //设置读取超时时间
            val client = clientBuilder
                .readTimeout(SettingUtils.requestTimeout.toLong(), TimeUnit.SECONDS)
                .writeTimeout(SettingUtils.requestTimeout.toLong(), TimeUnit.SECONDS)
                .connectTimeout(SettingUtils.requestTimeout.toLong(), TimeUnit.SECONDS)
                .build()

            val request: Request
            if (setting.method != null && setting.method == "GET") {
                request = Request.Builder()
                    .url(requestUrl + "?chat_id=" + setting.chatId + "&text=" + URLEncoder.encode(content, "UTF-8"))
                    .build()
            } else {
                val bodyMap: MutableMap<String, Any> = mutableMapOf()
                bodyMap["chat_id"] = setting.chatId
                bodyMap["text"] = content
                bodyMap["parse_mode"] = "HTML"
                bodyMap["disable_web_page_preview"] = "true"
                val requestMsg: String = Gson().toJson(bodyMap)
                Log.i(TAG, "requestMsg:$requestMsg")
                val requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestMsg)
                request = Request.Builder()
                    .url(requestUrl)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .post(requestBody)
                    .build()
            }

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    SendUtils.updateLogs(logId, 0, e.message.toString())
                    e.printStackTrace()
                    Looper.prepare()
                    XToastUtils.error("发送失败：" + e.message)
                    Looper.loop()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val responseStr = response.body()?.string()
                    Log.d(TAG, "Response：" + response.code() + "，" + responseStr)

                    val resp = Gson().fromJson(responseStr, TelegramResult::class.java)
                    if (resp.ok == true) {
                        SendUtils.updateLogs(logId, 2, responseStr.toString())
                        Looper.prepare()
                        XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                        Looper.loop()
                    } else {
                        SendUtils.updateLogs(logId, 0, responseStr.toString())
                        Looper.prepare()
                        XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
                        Looper.loop()
                    }
                }
            })

        }

        fun sendMsg(setting: TelegramSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}