package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.DingtalkResult
import com.idormy.sms.forwarder.entity.setting.DingtalkGroupRobotSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

//钉钉群自定义机器人
@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class DingtalkGroupRobotUtils private constructor() {
    companion object {

        private val TAG: String = DingtalkGroupRobotUtils::class.java.simpleName

        fun sendMsg(
            setting: DingtalkGroupRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            var requestUrl = if (setting.token.startsWith("http")) setting.token else "https://oapi.dingtalk.com/robot/send?access_token=" + setting.token

            if (!TextUtils.isEmpty(setting.secret)) {
                val timestamp = System.currentTimeMillis()
                val stringToSign = "$timestamp\n" + setting.secret
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(setting.secret?.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
                val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
                val sign = URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
                requestUrl += "&timestamp=$timestamp&sign=$sign"
            }

            Log.i(TAG, "requestUrl:$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["msgtype"] = "text"

            val textText: MutableMap<String, Any> = mutableMapOf()
            textText["content"] = content
            msgMap["text"] = textText

            val atMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["at"] = atMap
            if (setting.atAll == true) {
                atMap["isAtAll"] = true
            } else {
                atMap["isAtAll"] = false
                if (!TextUtils.isEmpty(setting.atMobiles)) {
                    val atMobilesArray: Array<String>? = setting.atMobiles?.split(",".toRegex())?.toTypedArray()
                    if (atMobilesArray != null) {
                        val atMobilesList: MutableList<String> = ArrayList()
                        for (atMobile in atMobilesArray) {
                            if (TextUtils.isDigitsOnly(atMobile)) {
                                atMobilesList.add(atMobile)
                            }
                        }
                        if (atMobilesList.isNotEmpty()) {
                            atMap["atMobiles"] = atMobilesList
                        }
                    }
                }
            }

            val requestMsg: String = Gson().toJson(msgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            XHttp.post(requestUrl)
                .upJson(requestMsg)
                .keepJson(true)
                .timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
                .cacheMode(CacheMode.NO_CACHE)
                .retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
                .retryDelay(SettingUtils.requestDelayTime) //超时重试的延迟时间
                .retryIncreaseDelay(SettingUtils.requestDelayTime) //超时重试叠加延时
                .timeStamp(true)
                .execute(object : SimpleCallBack<String>() {

                    override fun onError(e: ApiException) {
                        Log.e(TAG, e.detailMessage)
                        SendUtils.updateLogs(logId, 0, e.displayMessage)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)

                        val resp = Gson().fromJson(response, DingtalkResult::class.java)
                        if (resp.errcode == 0L) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })

        }

        fun sendMsg(setting: DingtalkGroupRobotSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}