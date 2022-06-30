package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.DingtalkResult
import com.idormy.sms.forwarder.entity.result.WeworkAgentResult
import com.idormy.sms.forwarder.entity.setting.WeworkAgentSetting
import com.idormy.sms.forwarder.utils.MMKVUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xui.utils.ResUtils.getString

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class WeworkAgentUtils private constructor() {
    companion object {

        private val TAG: String = WeworkAgentUtils::class.java.simpleName

        fun sendMsg(
            setting: WeworkAgentSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {

            val accessToken: String? = MMKVUtils.getString("access_token_" + setting.agentID, "")
            val expiresIn: Long = MMKVUtils.getLong("expires_in_" + setting.agentID, 0L)
            if (!TextUtils.isEmpty(accessToken) && expiresIn > System.currentTimeMillis()) {
                return sendTextMsg(setting, msgInfo, rule, logId)
            }

            var getTokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?"
            getTokenUrl += "corpid=" + setting.corpID
            getTokenUrl += "&corpsecret=" + setting.secret
            Log.d(TAG, "getTokenUrl：$getTokenUrl")

            XHttp.get(getTokenUrl)
                .keepJson(true)
                .timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
                .cacheMode(CacheMode.NO_CACHE)
                .timeStamp(true)
                .execute(object : SimpleCallBack<String>() {

                    override fun onError(e: ApiException) {
                        Log.e(TAG, e.detailMessage)
                        SendUtils.updateLogs(logId, 0, e.displayMessage)
                    }

                    override fun onSuccess(response: String) {
                        Log.i(TAG, response)

                        val resp = Gson().fromJson(response, WeworkAgentResult::class.java)
                        if (resp.errcode == 0L) {
                            MMKVUtils.put("access_token_" + setting.agentID, resp.access_token)
                            MMKVUtils.put("expires_in_" + setting.agentID, System.currentTimeMillis() + ((resp.expires_in ?: 7200) - 120) * 1000L) //提前2分钟过期
                            sendTextMsg(setting, msgInfo, rule, logId)
                        } else {
                            SendUtils.updateLogs(logId, 0, String.format(getString(R.string.request_failed_tips), response))
                        }
                    }

                })

        }

        //发送文本消息
        private fun sendTextMsg(
            setting: WeworkAgentSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val textMsgMap: MutableMap<String, Any> = mutableMapOf()
            textMsgMap["touser"] = setting.toUser.toString()
            textMsgMap["msgtype"] = "text"
            textMsgMap["agentid"] = setting.agentID
            val textText: MutableMap<String, Any> = mutableMapOf()
            textText["content"] = content
            textMsgMap["text"] = textText
            val requestUrl = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + MMKVUtils.getString("access_token_" + setting.agentID, "")
            Log.i(TAG, "requestUrl:$requestUrl")
            val requestMsg: String = Gson().toJson(textMsgMap)
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

        fun sendMsg(setting: WeworkAgentSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }

    }
}