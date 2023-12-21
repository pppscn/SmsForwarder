package com.idormy.sms.forwarder.utils.sender

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Base64
import com.idormy.sms.forwarder.utils.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
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
                        setting.secret?.toByteArray(StandardCharsets.UTF_8),
                        "HmacSHA256"
                    )
                )
                val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
                sign = URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
            }

            var webParams = setting.webParams?.trim()

            //支持HTTP基本认证(Basic Authentication)
            val regex = "^(https?://)([^:]+):([^@]+)@(.+)"
            val matches = Regex(regex, RegexOption.IGNORE_CASE).findAll(requestUrl).toList()
                .flatMap(MatchResult::groupValues)
            Log.i(TAG, "matches = $matches")
            if (matches.isNotEmpty()) {
                requestUrl = matches[1] + matches[4]
                Log.i(TAG, "requestUrl:$requestUrl")
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
                webParams = webParams.toString().replace("[from]", URLEncoder.encode(from, "UTF-8"))
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
            } else if (!webParams.isNullOrEmpty() && webParams.startsWith("{")) {
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
                when (setting.method) {
                    "PUT" -> XHttp.put(requestUrl).keepJson(true).upJson(bodyMsg)
                    "PATCH" -> XHttp.patch(requestUrl).keepJson(true).upJson(bodyMsg)
                    else -> XHttp.post(requestUrl).keepJson(true).upJson(bodyMsg)
                }
            } else {
                if (webParams.isNullOrEmpty()) {
                    webParams = "from=[from]&content=[content]&timestamp=[timestamp]"
                    if (!TextUtils.isEmpty(sign)) webParams += "&sign=[sign]"
                }
                Log.d(TAG, "method = ${setting.method}, Url = $requestUrl")
                val postRequest = when (setting.method) {
                    "PUT" -> XHttp.put(requestUrl).keepJson(true)
                    "PATCH" -> XHttp.patch(requestUrl).keepJson(true)
                    else -> XHttp.post(requestUrl).keepJson(true)
                }
                webParams.trim('&').split("&").forEach {
                    val param = it.split("=")
                    if (param.size == 2) {
                        postRequest.params(
                            param[0], param[1].replace("[from]", from)
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
            for ((key, value) in setting.headers?.entries!!) {
                request.headers(key, value)
            }

            //支持HTTP基本认证(Basic Authentication)
            if (matches.isNotEmpty()) {
                request.addInterceptor(BasicAuthInterceptor(matches[2], matches[3]))
            }

            request.ignoreHttpsCert() //忽略https证书
                .timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
                .cacheMode(CacheMode.NO_CACHE)
                .retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
                .retryDelay(SettingUtils.requestDelayTime) //超时重试的延迟时间
                .retryIncreaseDelay(SettingUtils.requestDelayTime) //超时重试叠加延时
                .timeStamp(true)
                .execute(object : SimpleCallBack<String>() {

                    override fun onError(e: ApiException) {
                        Log.e(TAG, e.detailMessage)
                        val status = 0
                        SendUtils.updateLogs(logId, status, e.displayMessage)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)
                        val status = if (!setting.response.isNullOrEmpty() && !response.contains(setting.response)) 0 else 2
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
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return dateFormat.format(currentTime)
        }

    }
}