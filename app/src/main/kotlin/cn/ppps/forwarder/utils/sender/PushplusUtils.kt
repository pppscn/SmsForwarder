package cn.ppps.forwarder.utils.sender

import android.text.TextUtils
import com.google.gson.Gson
import cn.ppps.forwarder.R
import cn.ppps.forwarder.database.entity.Rule
import cn.ppps.forwarder.entity.MsgInfo
import cn.ppps.forwarder.entity.result.PushplusResult
import cn.ppps.forwarder.entity.setting.PushplusSetting
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SendUtils
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.interceptor.LoggingInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xutil.resource.ResUtils.getString

class PushplusUtils private constructor() {
    companion object {

        private val TAG: String = PushplusUtils::class.java.simpleName

        fun sendMsg(
            setting: PushplusSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.titleTemplate, rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.titleTemplate)
            }
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            val requestUrl = "https://" + setting.website + "/send"
            Log.i(TAG, "requestUrl:$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["token"] = setting.token
            msgMap["content"] = content

            if (!TextUtils.isEmpty(title)) msgMap["title"] = title
            if (!TextUtils.isEmpty(setting.template)) msgMap["template"] = setting.template
            if (!TextUtils.isEmpty(setting.topic)) msgMap["topic"] = setting.topic

            if (setting.website == getString(R.string.pushplus_plus)) {
                if (!TextUtils.isEmpty(setting.channel)) msgMap["channel"] = setting.channel
                if (!TextUtils.isEmpty(setting.webhook)) msgMap["webhook"] = setting.webhook
                if (!TextUtils.isEmpty(setting.callbackUrl)) msgMap["callbackUrl"] = setting.callbackUrl
                if (!TextUtils.isEmpty(setting.validTime)) {
                    val validTime = setting.validTime.toInt()
                    if (validTime > 0) {
                        msgMap["timestamp"] = System.currentTimeMillis() + validTime * 1000L
                    }
                }
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

                        val resp = Gson().fromJson(response, PushplusResult::class.java)
                        val status = if (resp?.code == 200L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })

        }

    }
}