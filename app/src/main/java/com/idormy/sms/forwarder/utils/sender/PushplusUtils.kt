package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.PushplusResult
import com.idormy.sms.forwarder.entity.setting.PushplusSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xui.utils.ResUtils


@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class PushplusUtils private constructor() {
    companion object {

        private val TAG: String = PushplusUtils::class.java.simpleName

        fun sendMsg(
            setting: PushplusSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.titleTemplate.toString(), rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.titleTemplate.toString())
            }
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val requestUrl = "https://" + setting.website + "/send"
            Log.i(TAG, "requestUrl:$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["token"] = setting.token
            msgMap["content"] = content

            if (!TextUtils.isEmpty(title)) msgMap["title"] = title
            if (!TextUtils.isEmpty(setting.template)) msgMap["template"] = setting.template.toString()
            if (!TextUtils.isEmpty(setting.topic)) msgMap["topic"] = setting.topic.toString()

            if (setting.website == ResUtils.getString(R.string.pushplus_plus)) {
                if (!TextUtils.isEmpty(setting.channel)) msgMap["channel"] = setting.channel.toString()
                if (!TextUtils.isEmpty(setting.webhook)) msgMap["webhook"] = setting.webhook.toString()
                if (!TextUtils.isEmpty(setting.callbackUrl)) msgMap["callbackUrl"] = setting.callbackUrl.toString()
                if (!TextUtils.isEmpty(setting.validTime)) {
                    val validTime = setting.validTime?.toInt()
                    if (validTime != null && validTime > 0) {
                        msgMap["timestamp"] = System.currentTimeMillis() + validTime * 1000L
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

                        val resp = Gson().fromJson(response, PushplusResult::class.java)
                        if (resp?.code == 200L) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })

        }

        fun sendMsg(setting: PushplusSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}