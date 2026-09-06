package cn.ppps.forwarder.utils.sender

import android.text.TextUtils
import cn.ppps.forwarder.database.entity.Rule
import cn.ppps.forwarder.entity.MsgInfo
import cn.ppps.forwarder.entity.result.WpushResult
import cn.ppps.forwarder.entity.setting.WpushSetting
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SendUtils
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.interceptor.LoggingInterceptor
import com.google.gson.Gson
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

class WpushUtils private constructor() {
    companion object {

        private val TAG: String = WpushUtils::class.java.simpleName
        private const val REQUEST_URL = "https://api.wpush.cn/api/v1/send"

        fun sendMsg(
            setting: WpushSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.titleTemplate, rule.regexReplace, rule.title)
            } else {
                msgInfo.getTitleForSend(setting.titleTemplate)
            }
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace, rule.title)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            // WPUSH requires title; fall back to content when empty
            val resolvedTitle = if (!TextUtils.isEmpty(title)) title else content
            val channel = if (!TextUtils.isEmpty(setting.channel)) setting.channel else "wechat"

            Log.i(TAG, "requestUrl:$REQUEST_URL")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["apikey"] = setting.apiKey
            msgMap["title"] = resolvedTitle
            msgMap["content"] = content
            msgMap["channel"] = channel
            if (!TextUtils.isEmpty(setting.topicCode)) {
                msgMap["topic_code"] = setting.topicCode
            }

            val requestMsg: String = Gson().toJson(msgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            XHttp.post(REQUEST_URL)
                .upJson(requestMsg)
                .keepJson(true)
                .retryCount(SettingUtils.requestRetryTimes)
                .retryDelay(SettingUtils.requestDelayTime * 1000)
                .retryIncreaseDelay(SettingUtils.requestDelayTime * 1000)
                .timeStamp(true)
                .addInterceptor(LoggingInterceptor(logId))
                .execute(object : SimpleCallBack<String>() {

                    override fun onError(e: ApiException) {
                        Log.e(TAG, e.detailMessage)
                        val status = 0
                        SendUtils.updateLogs(logId, status, e.displayMessage)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)
                        val resp = Gson().fromJson(response, WpushResult::class.java)
                        // WPUSH success is code === 0 (unlike PushPlus which uses 200)
                        val status = if (resp?.code == 0L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }
                })
        }
    }
}
