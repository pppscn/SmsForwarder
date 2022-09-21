package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.FeishuAppResult
import com.idormy.sms.forwarder.entity.setting.FeishuAppSetting
import com.idormy.sms.forwarder.utils.MMKVUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xui.utils.ResUtils.getString

//飞书企业应用
@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class FeishuAppUtils private constructor() {
    companion object {

        private val TAG: String = FeishuAppUtils::class.java.simpleName

        fun sendMsg(
            setting: FeishuAppSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {

            val accessToken: String? = MMKVUtils.getString("feishu_access_token_" + setting.appId, "")
            val expiresIn: Long = MMKVUtils.getLong("feishu_expires_in_" + setting.appId, 0L)
            if (!TextUtils.isEmpty(accessToken) && expiresIn > System.currentTimeMillis()) {
                return sendTextMsg(setting, msgInfo, rule, logId)
            }

            val requestUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
            Log.d(TAG, "requestUrl：$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["app_id"] = setting.appId
            msgMap["app_secret"] = setting.appSecret
            val requestMsg: String = Gson().toJson(msgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            XHttp.post(requestUrl)
                .upJson(requestMsg)
                .keepJson(true)
                .ignoreHttpsCert()
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

                        val resp = Gson().fromJson(response, FeishuAppResult::class.java)
                        if (!TextUtils.isEmpty(resp?.tenant_access_token)) {
                            MMKVUtils.put("feishu_access_token_" + setting.appId, resp.tenant_access_token)
                            MMKVUtils.put("feishu_expires_in_" + setting.appId, System.currentTimeMillis() + ((resp.expire ?: 7010) - 120) * 1000L) //提前2分钟过期
                            sendTextMsg(setting, msgInfo, rule, logId)
                        } else {
                            SendUtils.updateLogs(logId, 0, String.format(getString(R.string.request_failed_tips), response))
                        }
                    }

                })

        }

        //发送文本消息
        private fun sendTextMsg(
            setting: FeishuAppSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val requestUrl = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=user_id"
            Log.d(TAG, "requestUrl：$requestUrl")

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val msgContent = if ("interactive" == setting.msgType) {
                val title = if (rule != null) {
                    msgInfo.getTitleForSend(setting.titleTemplate, rule.regexReplace)
                } else {
                    msgInfo.getTitleForSend(setting.titleTemplate)
                }
                "{\"elements\":[{\"tag\":\"markdown\",\"content\":\"**[{{MSG_TITLE}}]({{MSG_URL}})**\\n --------------\\n{{MSG_CONTENT}}\"}]}".trimIndent().replace("{{MSG_TITLE}}", jsonInnerStr(title))
                    .replace("{{MSG_URL}}", jsonInnerStr("https://github.com/pppscn/SmsForwarder"))
                    .replace("{{MSG_CONTENT}}", jsonInnerStr(content))
            } else {
                "{\"text\":\"{{MSG_CONTENT}}\"}".trimIndent().replace("{{MSG_CONTENT}}", jsonInnerStr(content))
            }

            val textMsgMap: MutableMap<String, Any> = mutableMapOf()
            textMsgMap["receive_id"] = setting.receiveId
            textMsgMap["msg_type"] = setting.msgType
            textMsgMap["content"] = msgContent

            val requestMsg: String = Gson().toJson(textMsgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            XHttp.post(requestUrl)
                .upJson(requestMsg)
                .headers("Authorization", "Bearer " + MMKVUtils.getString("feishu_access_token_" + setting.appId, ""))
                .keepJson(true)
                //.ignoreHttpsCert()
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
                        //Log.d(TAG, "tlsVersion=" + response.handshake().tlsVersion())
                        //Log.d(TAG, "cipherSuite=" + response.handshake().cipherSuite().toString())

                        val resp = Gson().fromJson(response, FeishuAppResult::class.java)
                        if (resp?.code == 0L) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })
        }

        fun sendMsg(setting: FeishuAppSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }

        private fun jsonInnerStr(string: String?): String {
            if (string == null) return "null"

            val jsonStr: String = Gson().toJson(string)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

    }
}