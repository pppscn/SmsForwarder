package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.FeishuAppResult
import com.idormy.sms.forwarder.entity.setting.FeishuAppSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.SharedPreference
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xutil.resource.ResUtils.getString

//飞书企业应用
class FeishuAppUtils private constructor() {
    companion object {

        private val TAG: String = FeishuAppUtils::class.java.simpleName

        fun sendMsg(
            setting: FeishuAppSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {

            var accessToken: String by SharedPreference("feishu_access_token_" + setting.appId, "")
            var expiresIn: Long by SharedPreference("feishu_expires_in_" + setting.appId, 0L)
            if (!TextUtils.isEmpty(accessToken) && expiresIn > System.currentTimeMillis()) {
                return sendTextMsg(setting, msgInfo, rule, senderIndex, logId, msgId)
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

                        val resp = Gson().fromJson(response, FeishuAppResult::class.java)
                        if (!TextUtils.isEmpty(resp?.tenant_access_token)) {
                            accessToken = resp.tenant_access_token.toString()
                            expiresIn = System.currentTimeMillis() + ((resp.expire ?: 7010) - 120) * 1000L //提前2分钟过期
                            sendTextMsg(setting, msgInfo, rule, senderIndex, logId, msgId)
                        } else {
                            SendUtils.updateLogs(logId, 0, String.format(getString(R.string.request_failed_tips), response))
                            SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
                        }
                    }

                })

        }

        //发送文本消息
        private fun sendTextMsg(
            setting: FeishuAppSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val requestUrl = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=${setting.receiveIdType}"
            Log.d(TAG, "requestUrl：$requestUrl")

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            val msgContent = if ("interactive" == setting.msgType) {
                val title = if (rule != null) {
                    msgInfo.getTitleForSend(setting.titleTemplate, rule.regexReplace)
                } else {
                    msgInfo.getTitleForSend(setting.titleTemplate)
                }
                if (TextUtils.isEmpty(setting.messageCard.trim())) {
                    "{\"elements\":[{\"tag\":\"markdown\",\"content\":\"**[{{MSG_TITLE}}]({{MSG_URL}})**\\n --------------\\n{{MSG_CONTENT}}\"}]}".trimIndent().replace("{{MSG_TITLE}}", jsonInnerStr(title)).replace("{{MSG_URL}}", jsonInnerStr("https://github.com/pppscn/SmsForwarder")).replace("{{MSG_CONTENT}}", jsonInnerStr(content))
                } else {
                    msgInfo.getContentFromJson(
                        setting.messageCard.trimIndent()
                            .replace("{{MSG_TITLE}}", jsonInnerStr(title))
                            .replace("{{MSG_URL}}", jsonInnerStr("https://github.com/pppscn/SmsForwarder"))
                            .replace("{{MSG_CONTENT}}", jsonInnerStr(content))
                    )
                }
            } else {
                "{\"text\":\"{{MSG_CONTENT}}\"}".trimIndent().replace("{{MSG_CONTENT}}", jsonInnerStr(content))
            }

            val textMsgMap: MutableMap<String, Any> = mutableMapOf()
            textMsgMap["receive_id"] = setting.receiveId
            textMsgMap["msg_type"] = setting.msgType
            textMsgMap["content"] = msgContent

            val requestMsg: String = Gson().toJson(textMsgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            val accessToken: String by SharedPreference("feishu_access_token_" + setting.appId, "")
            XHttp.post(requestUrl).upJson(requestMsg).headers("Authorization", "Bearer $accessToken").keepJson(true)
                //.ignoreHttpsCert()
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
                        //Log.d(TAG, "tlsVersion=" + response.handshake().tlsVersion())
                        //Log.d(TAG, "cipherSuite=" + response.handshake().cipherSuite().toString())

                        val resp = Gson().fromJson(response, FeishuAppResult::class.java)
                        val status = if (resp?.code == 0L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })
        }

        private fun jsonInnerStr(string: String?): String {
            if (string == null) return "null"

            val jsonStr: String = Gson().toJson(string)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

    }
}