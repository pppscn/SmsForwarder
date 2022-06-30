package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.ServerchanResult
import com.idormy.sms.forwarder.entity.setting.ServerchanSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER", "unused")
class ServerchanUtils {
    companion object {

        private val TAG: String = ServerchanUtils::class.java.simpleName

        fun sendMsg(
            setting: ServerchanSetting,
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

            val requestUrl: String = String.format("https://sctapi.ftqq.com/%s.send", setting.sendKey) //推送地址
            Log.i(TAG, "requestUrl:$requestUrl")

            val request = XHttp.post(requestUrl)
                .params("title", title)
                .params("desp", content)

            if (!TextUtils.isEmpty(setting.channel)) request.params("channel", setting.channel)
            if (!TextUtils.isEmpty(setting.openid)) request.params("group", setting.openid)

            request.keepJson(true)
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

                        val resp = Gson().fromJson(response, ServerchanResult::class.java)
                        if (resp?.code == 0L) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })

        }

        fun sendMsg(setting: ServerchanSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}