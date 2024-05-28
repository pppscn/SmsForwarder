package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.DingtalkResult
import com.idormy.sms.forwarder.entity.setting.DingtalkGroupRobotSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

//钉钉群自定义机器人
class DingtalkGroupRobotUtils private constructor() {
    companion object {

        private val TAG: String = DingtalkGroupRobotUtils::class.java.simpleName

        fun sendMsg(
            setting: DingtalkGroupRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            var content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            var requestUrl = if (setting.token.startsWith("http")) setting.token else "https://oapi.dingtalk.com/robot/send?access_token=" + setting.token

            if (!TextUtils.isEmpty(setting.secret)) {
                val timestamp = System.currentTimeMillis()
                val stringToSign = "$timestamp\n" + setting.secret
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(setting.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
                val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
                val sign = URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
                requestUrl += "&timestamp=$timestamp&sign=$sign"
            }

            Log.i(TAG, "requestUrl:$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["msgtype"] = setting.msgtype

            val atMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["at"] = atMap
            if (setting.atAll) {
                atMap["isAtAll"] = true
            } else {
                atMap["isAtAll"] = false
                if (!TextUtils.isEmpty(setting.atMobiles)) {
                    val atMobilesArray: Array<String> = setting.atMobiles.replace("[,，;；]".toRegex(), ",").trim(',').split(',').toTypedArray()
                    if (atMobilesArray.isNotEmpty()) {
                        atMap["atMobiles"] = atMobilesArray
                        for (atMobile in atMobilesArray) {
                            if (!content.contains("@${atMobile}")) {
                                content += " @${atMobile}"
                            }
                        }
                    }
                }
                if (!TextUtils.isEmpty(setting.atDingtalkIds)) {
                    val atDingtalkIdsArray: Array<String> = setting.atDingtalkIds.replace("[,，;；]".toRegex(), ",").trim(',').split(',').toTypedArray()
                    if (atDingtalkIdsArray.isNotEmpty()) {
                        atMap["atDingtalkIds"] = atDingtalkIdsArray
                        for (atDingtalkId in atDingtalkIdsArray) {
                            if (!content.contains("@${atDingtalkId}")) {
                                content += " @${atDingtalkId}"
                            }
                        }
                    }
                }
            }

            if ("markdown" == msgMap["msgtype"]) {
                val titleTemplate = setting.titleTemplate
                val title = rule?.let { msgInfo.getTitleForSend(titleTemplate, it.regexReplace) } ?: msgInfo.getTitleForSend(titleTemplate)
                msgMap["markdown"] = mutableMapOf<String, Any>("title" to title, "text" to content)
            } else {
                msgMap["text"] = mutableMapOf<String, Any>("content" to content)
            }

            val requestMsg: String = Gson().toJson(msgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            XHttp.post(requestUrl)
                .upJson(requestMsg)
                .keepJson(true)
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

                        val resp = Gson().fromJson(response, DingtalkResult::class.java)
                        val status = if (resp?.errcode == 0L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })

        }

    }
}