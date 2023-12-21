package com.idormy.sms.forwarder.utils.sender

import com.idormy.sms.forwarder.utils.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.WeworkRobotResult
import com.idormy.sms.forwarder.entity.setting.WeworkRobotSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException

class WeworkRobotUtils private constructor() {
    companion object {

        private val TAG: String = WeworkRobotUtils::class.java.simpleName

        fun sendMsg(
            setting: WeworkRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            val requestUrl = setting.webHook
            Log.i(TAG, "requestUrl:$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["msgtype"] = setting.msgType

            val contextMap = mutableMapOf<String, Any>()
            contextMap["content"] = content

            if (setting.msgType == "markdown") {
                msgMap["markdown"] = contextMap
            } else {
                if (setting.atAll == true) {
                    contextMap["mentioned_list"] = arrayListOf("@all")
                } else {
                    if (!setting.atUserIds.isNullOrEmpty()) {
                        contextMap["mentioned_list"] = setting.atUserIds!!.split(",")
                    }
                    if (!setting.atMobiles.isNullOrEmpty()) {
                        contextMap["mentioned_mobile_list"] = setting.atMobiles!!.split(",")
                    }
                }
                msgMap["text"] = contextMap
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
                        SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)

                        val resp = Gson().fromJson(response, WeworkRobotResult::class.java)
                        val status = if (resp?.errcode == 0L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })

        }

    }
}