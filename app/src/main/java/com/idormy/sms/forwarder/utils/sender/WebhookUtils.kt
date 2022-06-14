package com.idormy.sms.forwarder.utils.sender

import android.annotation.SuppressLint
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.utils.CertUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xutil.app.AppUtils
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER", "unused")
class WebhookUtils {
    companion object {

        private val TAG: String = WebhookUtils::class.java.simpleName

        fun sendMsg(
            setting: WebhookSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val from: String = msgInfo.from
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            var webServer: String = setting.webServer //推送地址
            Log.i(TAG, "requestUrl:$webServer")

            val timestamp = System.currentTimeMillis()
            val orgContent: String = msgInfo.content
            val deviceMark: String = SettingUtils.extraDeviceMark ?: ""
            val appVersion: String = AppUtils.getAppVersionName()
            val simInfo: String = msgInfo.simInfo
            @SuppressLint("SimpleDateFormat") val receiveTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()) //smsVo.getDate()

            var sign = ""
            if (!TextUtils.isEmpty(setting.secret)) {
                val stringToSign = "$timestamp\n" + setting.secret
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(setting.secret?.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
                val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
                sign = URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
            }

            var webParams = setting.webParams?.trim()
            val requestBuilder: Request.Builder
            if (setting.method == "GET" && TextUtils.isEmpty(webParams)) {
                setting.webServer += (if (setting.webServer.contains("?")) "&" else "?") + "from=" + URLEncoder.encode(from, "UTF-8")
                webServer += "&content=" + URLEncoder.encode(content, "UTF-8")
                if (!TextUtils.isEmpty(setting.secret)) {
                    webServer += "&timestamp=$timestamp"
                    webServer += "&sign=$sign"
                }
                Log.d(TAG, "method = GET, Url = $webServer")
                requestBuilder = Request.Builder().url(webServer).get()
            } else if (setting.method == "GET" && !TextUtils.isEmpty(setting.webParams)) {
                webParams = webParams.toString().replace("[from]", URLEncoder.encode(from, "UTF-8"))
                    .replace("[content]", URLEncoder.encode(content, "UTF-8"))
                    .replace("[msg]", URLEncoder.encode(content, "UTF-8"))
                    .replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8"))
                    .replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8"))
                    .replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8"))
                    .replace("[title]", URLEncoder.encode(simInfo, "UTF-8"))
                    .replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8"))
                    .replace("[receive_time]", URLEncoder.encode(receiveTime, "UTF-8"))
                    .replace("\n", "%0A")
                if (!TextUtils.isEmpty(setting.secret)) {
                    webParams = webParams.replace("[timestamp]", timestamp.toString())
                        .replace("[sign]", URLEncoder.encode(sign, "UTF-8"))
                }
                webServer += (if (webServer.contains("?")) "&" else "?") + webParams
                Log.d(TAG, "method = GET, Url = $webServer")
                requestBuilder = Request.Builder().url(webServer).get()
            } else if (webParams != null && webParams.contains("[msg]")) {
                val bodyMsg: String
                var contentType = "application/x-www-form-urlencoded"
                if (webParams.startsWith("{")) {
                    contentType = "application/json;charset=utf-8"
                    bodyMsg = webParams.replace("[from]", from)
                        .replace("[content]", escapeJson(content))
                        .replace("[msg]", escapeJson(content))
                        .replace("[org_content]", escapeJson(orgContent))
                        .replace("[device_mark]", escapeJson(deviceMark))
                        .replace("[app_version]", appVersion)
                        .replace("[title]", escapeJson(simInfo))
                        .replace("[card_slot]", escapeJson(simInfo))
                        .replace("[receive_time]", receiveTime)
                } else {
                    bodyMsg = webParams.replace("[from]", URLEncoder.encode(from, "UTF-8"))
                        .replace("[content]", URLEncoder.encode(content, "UTF-8"))
                        .replace("[msg]", URLEncoder.encode(content, "UTF-8"))
                        .replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8"))
                        .replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8"))
                        .replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8"))
                        .replace("[title]", URLEncoder.encode(simInfo, "UTF-8"))
                        .replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8"))
                        .replace("[receive_time]", URLEncoder.encode(receiveTime, "UTF-8"))
                }
                val body = RequestBody.create(MediaType.parse(contentType), bodyMsg)
                requestBuilder = Request.Builder()
                    .url(webServer)
                    .addHeader("Content-Type", contentType)
                    .method("POST", body)
                Log.d(TAG, "method = POST webParams, Body = $bodyMsg")
            } else {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("from", from)
                    .addFormDataPart("content", content)
                if (!TextUtils.isEmpty(setting.secret)) {
                    builder.addFormDataPart("timestamp", timestamp.toString())
                    builder.addFormDataPart("sign", sign)
                }
                val body: RequestBody = builder.build()
                Log.d(TAG, "method = POST, Body = $body")
                requestBuilder = Request.Builder().url(webServer).method("POST", body)
            }

            for ((key, value) in setting.headers?.entries!!) {
                requestBuilder.addHeader(key, value)
            }

            val clientBuilder = OkHttpClient.Builder()

            //设置重试拦截器
            val retryTimes: Int = SettingUtils.requestRetryTimes
            if (retryTimes > 0) {
                val delayTime: Long = SettingUtils.requestDelayTime.toLong()
                val retryInterceptor: RetryInterceptor = RetryInterceptor.Builder().executionCount(retryTimes).retryInterval(delayTime).logId(0).build()
                clientBuilder.addInterceptor(retryInterceptor)
            }

            //忽略https证书
            CertUtils.x509TrustManager?.let { clientBuilder.sslSocketFactory(CertUtils.sSLSocketFactory, it).hostnameVerifier(CertUtils.hostnameVerifier) }

            //设置读取超时时间
            val client = clientBuilder
                .readTimeout(SettingUtils.requestTimeout.toLong(), TimeUnit.SECONDS)
                .writeTimeout(SettingUtils.requestTimeout.toLong(), TimeUnit.SECONDS)
                .connectTimeout(SettingUtils.requestTimeout.toLong(), TimeUnit.SECONDS)
                .build()

            client.newCall(requestBuilder.build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    //解决在子线程中调用Toast的异常情况处理
                    Looper.prepare()
                    e.printStackTrace()
                    SendUtils.updateLogs(logId, 0, e.message.toString())
                    //XToastUtils.error(ResUtils.getString(R.string.request_failed) + e.message)
                    Looper.loop()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val responseStr = response.body().toString()
                    Log.d(TAG, "Response：" + response.code() + "，" + responseStr)

                    //返回http状态200即为成功
                    if (200 == response.code()) {
                        Looper.prepare()
                        SendUtils.updateLogs(logId, 2, responseStr)
                        //XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                        Looper.loop()
                    } else {
                        Looper.prepare()
                        SendUtils.updateLogs(logId, 0, responseStr)
                        //XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
                        Looper.loop()
                    }
                }
            })

        }

        //JSON需要转义的字符
        private fun escapeJson(str: String?): String {
            if (str == null) return "null"
            val jsonStr: String = Gson().toJson(str)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

        fun sendMsg(setting: WebhookSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}