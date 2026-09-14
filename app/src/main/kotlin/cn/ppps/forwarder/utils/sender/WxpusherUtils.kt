package cn.ppps.forwarder.utils.sender

import android.text.TextUtils
import cn.ppps.forwarder.database.entity.Rule
import cn.ppps.forwarder.entity.MsgInfo
import cn.ppps.forwarder.entity.result.WxpusherResult
import cn.ppps.forwarder.entity.setting.WxpusherSetting
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SendUtils
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.interceptor.LoggingInterceptor
import com.google.gson.Gson
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

//WxPusher官方文档：https://wxpusher.zjiecode.com/docs/
class WxpusherUtils private constructor() {
    companion object {

        private val TAG: String = WxpusherUtils::class.java.simpleName

        //WxPusher返回码：1000=成功
        private const val WXPUSHER_CODE_SUCCESS = 1000L

        fun sendMsg(
            setting: WxpusherSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            var summary: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.summaryTemplate, rule.regexReplace, rule.title)
            } else {
                msgInfo.getTitleForSend(setting.summaryTemplate)
            }
            var content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace, rule.title)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            //通道级正则替换：无需配置转发规则即可在WxPusher通道直接提取验证码等，同时作用于摘要和内容
            if (!TextUtils.isEmpty(setting.regexReplace)) {
                summary = msgInfo.applyRegexReplace(summary, setting.regexReplace)
                content = msgInfo.applyRegexReplace(content, setting.regexReplace)
            }

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["content"] = content
            msgMap["contentType"] = setting.contentType
            if (summary.isNotEmpty()) msgMap["summary"] = summary

            //两种发送方式：SPT极简推送(发送者和接收者是同一人) 和 标准推送(appToken+UID/主题ID群发)
            val requestUrl: String = if (setting.isSimplePush) {
                val sptList = setting.spt.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (sptList.isEmpty()) throw IllegalArgumentException("invalid spt")
                if (sptList.size == 1) msgMap["spt"] = sptList[0] else msgMap["sptList"] = sptList
                "https://wxpusher.zjiecode.com/api/send/message/simple-push"
            } else {
                if (setting.appToken.isBlank()) throw IllegalArgumentException("invalid appToken")
                msgMap["appToken"] = setting.appToken
                val uids = setting.uids.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (uids.isNotEmpty()) msgMap["uids"] = uids
                val topicIds = setting.topicIds.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (topicIds.isNotEmpty()) msgMap["topicIds"] = topicIds
                "https://wxpusher.zjiecode.com/api/send/message"
            }

            val requestMsg: String = Gson().toJson(msgMap)
            Log.i(TAG, "requestUrl:$requestUrl")
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
                        val resp = try {
                            Gson().fromJson(response, WxpusherResult::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        val status = if (resp?.code == WXPUSHER_CODE_SUCCESS) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })

        }

    }
}
