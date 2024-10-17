package com.idormy.sms.forwarder.utils.sender

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.interceptor.BasicAuthInterceptor
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.idormy.sms.forwarder.utils.interceptor.NoContentInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.resource.ResUtils
import okhttp3.Credentials
import okhttp3.Response
import okhttp3.Route
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class WebhookUtils {
    companion object {

        private val TAG: String = WebhookUtils::class.java.simpleName

        fun sendMsg(
            setting: WebhookSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val from: String = msgInfo.from
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            var requestUrl: String = setting.webServer //推送地址
            Log.i(TAG, "requestUrl:$requestUrl")

            val timestamp = System.currentTimeMillis()
            val orgContent: String = msgInfo.content
            val deviceMark: String = SettingUtils.extraDeviceMark
            val appVersion: String = AppUtils.getAppVersionName()
            val simInfo: String = msgInfo.simInfo
            val receiveTimeTag = Regex("\\[receive_time(:(.*?))?]")

            var sign = ""
            if (!TextUtils.isEmpty(setting.secret)) {
                val stringToSign = "$timestamp\n" + setting.secret
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(
                    SecretKeySpec(
                        setting.secret.toByteArray(StandardCharsets.UTF_8),
                        "HmacSHA256"
                    )
                )
                val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
                sign = URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
            }

            var webParams = setting.webParams.trim()

            //支持HTTP基本认证(Basic Authentication)
            val regex = "^(https?://)([^:]+):([^@]+)@(.+)"
            val matches = Regex(regex, RegexOption.IGNORE_CASE).findAll(requestUrl).toList()
                .flatMap(MatchResult::groupValues)
            Log.i(TAG, "matches = $matches")
            if (matches.isNotEmpty()) {
                requestUrl = matches[1] + matches[4]
                Log.i(TAG, "requestUrl:$requestUrl")
            }

            //通过`Content-Type=applicaton/json`指定请求体为`json`格式
            var isJson = false
            //通过`Content-Type=text/plain、text/html、text/css、text/javascript、text/xml`指定请求体为`文本`格式
            var isText = false
            var mediaType = "text/plain"
            for ((key, value) in setting.headers.entries) {
                if (key.equals("Content-Type", ignoreCase = true)) {
                    if (value.contains("application/json")) {
                        isJson = true
                        break
                    } else if (value.startsWith("text/")) {
                        isText = true
                        mediaType = value
                        break
                    }
                }
            }

            val request = if (setting.method == "GET" && TextUtils.isEmpty(webParams)) {
                setting.webServer += (if (setting.webServer.contains("?")) "&" else "?") + "from=" + URLEncoder.encode(
                    from,
                    "UTF-8"
                )
                requestUrl += "&content=" + URLEncoder.encode(content, "UTF-8")
                if (!TextUtils.isEmpty(sign)) {
                    requestUrl += "&timestamp=$timestamp"
                    requestUrl += "&sign=$sign"
                }
                Log.d(TAG, "method = GET, Url = $requestUrl")
                XHttp.get(requestUrl).keepJson(true)
            } else if (setting.method == "GET" && !TextUtils.isEmpty(webParams)) {
                webParams = msgInfo.replaceTemplate(webParams, "", "URLEncoder")
                webParams = webParams.replace("[from]", URLEncoder.encode(from, "UTF-8"))
                    .replace("[content]", URLEncoder.encode(content, "UTF-8"))
                    .replace("[msg]", URLEncoder.encode(content, "UTF-8"))
                    .replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8"))
                    .replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8"))
                    .replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8"))
                    .replace("[title]", URLEncoder.encode(simInfo, "UTF-8"))
                    .replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8"))
                    .replace(receiveTimeTag) {
                        val format = it.groups[2]?.value
                        URLEncoder.encode(formatDateTime(msgInfo.date, format), "UTF-8")
                    }
                    .replace("\n", "%0A")
                if (!TextUtils.isEmpty(setting.secret)) {
                    webParams = webParams.replace("[timestamp]", timestamp.toString())
                        .replace("[sign]", URLEncoder.encode(sign, "UTF-8"))
                }
                requestUrl += if (webParams.startsWith("/")) {
                    webParams
                } else {
                    (if (requestUrl.contains("?")) "&" else "?") + webParams
                }
                Log.d(TAG, "method = GET, Url = $requestUrl")
                XHttp.get(requestUrl).keepJson(true)
            } else if (webParams.isNotEmpty() && (isJson || isText || webParams.startsWith("{"))) {
                webParams = msgInfo.replaceTemplate(webParams, "", "Gson")
                val bodyMsg = webParams.replace("[from]", from)
                    .replace("[content]", escapeJson(content))
                    .replace("[msg]", escapeJson(content))
                    .replace("[org_content]", escapeJson(orgContent))
                    .replace("[device_mark]", escapeJson(deviceMark))
                    .replace("[app_version]", appVersion)
                    .replace("[title]", escapeJson(simInfo))
                    .replace("[card_slot]", escapeJson(simInfo))
                    .replace(receiveTimeTag) {
                        val format = it.groups[2]?.value
                        formatDateTime(msgInfo.date, format)
                    }
                    .replace("[timestamp]", timestamp.toString())
                    .replace("[sign]", sign)
                Log.d(TAG, "method = ${setting.method}, Url = $requestUrl, bodyMsg = $bodyMsg")
                if (isText) {
                    when (setting.method) {
                        "PUT" -> XHttp.put(requestUrl).keepJson(true).upString(bodyMsg, mediaType)
                        "PATCH" -> XHttp.patch(requestUrl).keepJson(true).upString(bodyMsg, mediaType)
                        else -> XHttp.post(requestUrl).keepJson(true).upString(bodyMsg, mediaType)
                    }
                } else {
                    when (setting.method) {
                        "PUT" -> XHttp.put(requestUrl).keepJson(true).upJson(bodyMsg)
                        "PATCH" -> XHttp.patch(requestUrl).keepJson(true).upJson(bodyMsg)
                        else -> XHttp.post(requestUrl).keepJson(true).upJson(bodyMsg)
                    }
                }
            } else {
                if (webParams.isEmpty()) {
                    webParams = "from=[from]&content=[content]&timestamp=[timestamp]"
                    if (!TextUtils.isEmpty(sign)) webParams += "&sign=[sign]"
                }
                Log.d(TAG, "method = ${setting.method}, Url = $requestUrl")
                val postRequest = when (setting.method) {
                    "PUT" -> XHttp.put(requestUrl).keepJson(true)
                    "PATCH" -> XHttp.patch(requestUrl).keepJson(true)
                    else -> XHttp.post(requestUrl).keepJson(true)
                }
                webParams = msgInfo.replaceTemplate(webParams)
                webParams.trim('&').split("&").forEach {
                    val sepIndex = it.indexOf("=")
                    if (sepIndex != -1) {
                        val key = it.substring(0, sepIndex).trim()
                        val value = it.substring(sepIndex + 1).trim()
                        postRequest.params(key, value.replace("[from]", from)
                            .replace("[content]", content)
                            .replace("[msg]", content)
                            .replace("[org_content]", orgContent)
                            .replace("[device_mark]", deviceMark)
                            .replace("[app_version]", appVersion)
                            .replace("[title]", simInfo)
                            .replace("[card_slot]", simInfo)
                            .replace(receiveTimeTag) { t ->
                                val format = t.groups[2]?.value
                                formatDateTime(msgInfo.date, format)
                            }
                            .replace("[timestamp]", timestamp.toString())
                            .replace("[sign]", sign)
                        )
                    }
                }
                postRequest
            }

            //添加headers
            for ((key, value) in setting.headers.entries) {
                request.headers(key, value)
            }

            //支持HTTP基本认证(Basic Authentication)
            if (matches.isNotEmpty()) {
                request.addInterceptor(BasicAuthInterceptor(matches[2], matches[3]))
            }

            //设置代理
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS)
                && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)
            ) {
                //代理服务器的IP和端口号
                Log.d(TAG, "proxyHost = ${setting.proxyHost}, proxyPort = ${setting.proxyPort}")
                val proxyHost = if (NetworkUtils.isIP(setting.proxyHost)) setting.proxyHost else NetworkUtils.getDomainAddress(setting.proxyHost)
                if (!NetworkUtils.isIP(proxyHost)) {
                    throw Exception(String.format(ResUtils.getString(R.string.invalid_proxy_host), proxyHost))
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

            request.ignoreHttpsCert() //忽略https证书
                .retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
                .retryDelay(SettingUtils.requestDelayTime * 1000) //超时重试的延迟时间
                .retryIncreaseDelay(SettingUtils.requestDelayTime * 1000) //超时重试叠加延时
                .timeStamp(true) //url自动追加时间戳，避免缓存
                .addInterceptor(LoggingInterceptor(logId)) //增加一个log拦截器, 记录请求日志
                .addInterceptor(NoContentInterceptor(logId)) //拦截 HTTP 204 响应
                .execute(object : SimpleCallBack<String>() {

                    override fun onError(e: ApiException) {
                        //e.printStackTrace()
                        Log.e(TAG, e.detailMessage)
                        val status = if (setting.response.isNotEmpty() && e.detailMessage.contains(setting.response)) 2 else 0
                        SendUtils.updateLogs(logId, status, e.displayMessage)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)
                        val status = if (setting.response.isNotEmpty() && !response.contains(setting.response)) 0 else 2
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })

        }

        //JSON需要转义的字符
        private fun escapeJson(str: String?): String {
            if (str == null) return "null"
            val jsonStr: String = Gson().toJson(str)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

        @SuppressLint("SimpleDateFormat")
        fun formatDateTime(currentTime: Date, format: String?): String {
            val actualFormat = format?.removePrefix(":") ?: "yyyy-MM-dd HH:mm:ss"
            val dateFormat = SimpleDateFormat(actualFormat)
            return dateFormat.format(currentTime)
        }

    }
}