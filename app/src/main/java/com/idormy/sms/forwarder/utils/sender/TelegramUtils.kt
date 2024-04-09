package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.TelegramResult
import com.idormy.sms.forwarder.entity.setting.TelegramSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import okhttp3.Credentials
import okhttp3.Response
import okhttp3.Route
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URLEncoder

class TelegramUtils private constructor() {
    companion object {

        private val TAG: String = TelegramUtils::class.java.simpleName

        fun sendMsg(
            setting: TelegramSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            if (setting.method == "POST") {
                msgInfo.content = htmlEncode(msgInfo.content)
                msgInfo.simInfo = htmlEncode(msgInfo.simInfo)
            }

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            var requestUrl = if (setting.apiToken.startsWith("http")) {
                setting.apiToken
            } else {
                "https://api.telegram.org/bot" + setting.apiToken + "/sendMessage"
            }
            Log.i(TAG, "requestUrl:$requestUrl")

            val request = if (setting.method == "GET") {
                requestUrl += "?chat_id=" + setting.chatId + "&text=" + URLEncoder.encode(content, "UTF-8")
                Log.i(TAG, "requestUrl:$requestUrl")
                XHttp.get(requestUrl)
            } else {
                val bodyMap: MutableMap<String, Any> = mutableMapOf()
                bodyMap["chat_id"] = setting.chatId
                bodyMap["text"] = content
                bodyMap["parse_mode"] = "HTML"
                bodyMap["disable_web_page_preview"] = "true"
                val requestMsg: String = Gson().toJson(bodyMap)
                Log.i(TAG, "requestMsg:$requestMsg")
                XHttp.post(requestUrl).upJson(requestMsg)
            }

            //设置代理
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS)
                && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)
            ) {
                //代理服务器的IP和端口号
                Log.d(TAG, "proxyHost = ${setting.proxyHost}, proxyPort = ${setting.proxyPort}")
                val proxyHost = if (NetworkUtils.isIP(setting.proxyHost)) setting.proxyHost else NetworkUtils.getDomainAddress(setting.proxyHost)
                if (!NetworkUtils.isIP(proxyHost)) {
                    throw Exception(String.format(getString(R.string.invalid_proxy_host), proxyHost))
                }
                val proxyPort: Int = setting.proxyPort.toInt()

                Log.d(TAG, "proxyHost = $proxyHost, proxyPort = $proxyPort")
                request.okproxy(Proxy(setting.proxyType, InetSocketAddress(proxyHost, proxyPort)))

                //代理的鉴权账号密码
                if (setting.proxyAuthenticator && (!TextUtils.isEmpty(setting.proxyUsername) || !TextUtils.isEmpty(setting.proxyPassword))
                ) {
                    Log.i(TAG, "proxyUsername = ${setting.proxyUsername}, proxyPassword = ${setting.proxyPassword}")

                    if (setting.proxyType == Proxy.Type.HTTP) {
                        request.okproxyAuthenticator { _: Route?, response: Response ->
                            //设置代理服务器账号密码
                            val credential = Credentials.basic(setting.proxyUsername, setting.proxyPassword)
                            response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        }
                    } else {
                        Authenticator.setDefault(object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(setting.proxyUsername, setting.proxyPassword.toCharArray())
                            }
                        })
                    }
                }
            }

            request.keepJson(true)
                //.ignoreHttpsCert()
                .retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
                .retryDelay(SettingUtils.requestDelayTime * 1000) //超时重试的延迟时间
                .retryIncreaseDelay(SettingUtils.requestDelayTime * 1000) //超时重试叠加延时
                .timeStamp(true) //url自动追加时间戳，避免缓存
                .addInterceptor(LoggingInterceptor(logId)) //增加一个log拦截器, 记录请求日志
                .execute(object : SimpleCallBack<String>() {

                    override fun onError(e: ApiException) {
                        Log.e(TAG, e.detailMessage)
                        val status = 0
                        SendUtils.updateLogs(logId, status, e.displayMessage)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)

                        val resp = Gson().fromJson(response, TelegramResult::class.java)
                        val status = if (resp?.ok == true) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })

        }

        private fun htmlEncode(source: String?): String {
            if (source == null) {
                return ""
            }
            val buffer = StringBuffer()
            for (element in source) {
                when (element) {
                    '<' -> buffer.append("&lt;")
                    '>' -> buffer.append("&gt;")
                    '&' -> buffer.append("&amp;")
                    '"' -> buffer.append("&quot;")
                    //10, 13 -> buffer.append("\n")
                    else -> buffer.append(element)
                }
            }
            return buffer.toString()
        }
    }
}