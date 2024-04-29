package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.WeworkAgentResult
import com.idormy.sms.forwarder.entity.setting.WeworkAgentSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.SharedPreference
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import okhttp3.Credentials
import okhttp3.Response
import okhttp3.Route
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

class WeworkAgentUtils private constructor() {
    companion object {

        private val TAG: String = WeworkAgentUtils::class.java.simpleName

        fun sendMsg(
            setting: WeworkAgentSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {

            var accessToken: String by SharedPreference("access_token_" + setting.agentID, "")
            var expiresIn: Long by SharedPreference("expires_in_" + setting.agentID, 0L)
            if (!TextUtils.isEmpty(accessToken) && expiresIn > System.currentTimeMillis()) {
                return sendTextMsg(setting, msgInfo, rule, senderIndex, logId, msgId)
            }

            val customApi = if (TextUtils.isEmpty(setting.customizeAPI)) "https://qyapi.weixin.qq.com" else setting.customizeAPI
            var getTokenUrl = "$customApi/cgi-bin/gettoken?"
            getTokenUrl += "corpid=" + setting.corpID
            getTokenUrl += "&corpsecret=" + setting.secret
            Log.d(TAG, "getTokenUrl：$getTokenUrl")

            val request = XHttp.get(getTokenUrl)

            //设置代理
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS) && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)) {
                //代理服务器的IP和端口号
                Log.d(TAG, "proxyHost = ${setting.proxyHost}, proxyPort = ${setting.proxyPort}")
                val proxyHost = if (NetworkUtils.isIP(setting.proxyHost)) setting.proxyHost else NetworkUtils.getDomainAddress(setting.proxyHost)
                if (!NetworkUtils.isIP(proxyHost)) {
                    throw Exception(String.format(getString(R.string.invalid_proxy_host), proxyHost))
                }
                val proxyPort: Int = setting.proxyPort.toInt()

                Log.d(TAG, "proxyHost = $proxyHost, proxyPort = $proxyPort")
                request.okproxy(Proxy(setting.proxyType, InetSocketAddress(proxyHost, proxyPort)))

                //代理的鉴权账号密码
                if (setting.proxyAuthenticator && (!TextUtils.isEmpty(setting.proxyUsername) || !TextUtils.isEmpty(setting.proxyPassword))) {
                    Log.i(TAG, "proxyUsername = ${setting.proxyUsername}, proxyPassword = ${setting.proxyPassword}")

                    if (setting.proxyType == Proxy.Type.HTTP) {
                        request.okproxyAuthenticator { _: Route?, response: Response ->
                            //设置代理服务器账号密码
                            val credential = Credentials.basic(setting.proxyUsername, setting.proxyPassword)
                            response.request().newBuilder().header("Proxy-Authorization", credential).build()
                        }
                    } else {
                        Authenticator.setDefault(object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(setting.proxyUsername, setting.proxyPassword.toCharArray())
                            }
                        })
                    }
                }
            }

            request.keepJson(true)
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

                        val resp = Gson().fromJson(response, WeworkAgentResult::class.java)
                        if (resp?.errcode == 0L) {
                            accessToken = resp.access_token.toString()
                            expiresIn = System.currentTimeMillis() + ((resp.expires_in ?: 7200) - 120) * 1000L //提前2分钟过期
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
            setting: WeworkAgentSetting,
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

            val textMsgMap: MutableMap<String, Any> = mutableMapOf()
            textMsgMap["touser"] = setting.toUser
            textMsgMap["toparty"] = setting.toParty
            textMsgMap["totag"] = setting.toTag
            textMsgMap["msgtype"] = "text"
            textMsgMap["agentid"] = setting.agentID
            val textText: MutableMap<String, Any> = mutableMapOf()
            textText["content"] = content
            textMsgMap["text"] = textText
            val accessToken: String by SharedPreference("access_token_" + setting.agentID, "")
            val customApi = if (TextUtils.isEmpty(setting.customizeAPI)) "https://qyapi.weixin.qq.com" else setting.customizeAPI
            val requestUrl = "$customApi/cgi-bin/message/send?access_token=$accessToken"
            Log.i(TAG, "requestUrl:$requestUrl")
            val requestMsg: String = Gson().toJson(textMsgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            val request = XHttp.post(requestUrl)

            //设置代理
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS) && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)) {
                //代理服务器的IP和端口号
                Log.d(TAG, "proxyHost = ${setting.proxyHost}, proxyPort = ${setting.proxyPort}")
                val proxyHost = if (NetworkUtils.isIP(setting.proxyHost)) setting.proxyHost else NetworkUtils.getDomainAddress(setting.proxyHost)
                if (!NetworkUtils.isIP(proxyHost)) {
                    throw Exception(String.format(getString(R.string.invalid_proxy_host), proxyHost))
                }
                val proxyPort: Int = setting.proxyPort.toInt()

                Log.d(TAG, "proxyHost = $proxyHost, proxyPort = $proxyPort")
                request.okproxy(Proxy(setting.proxyType, InetSocketAddress(proxyHost, proxyPort)))

                //代理的鉴权账号密码
                if (setting.proxyAuthenticator && (!TextUtils.isEmpty(setting.proxyUsername) || !TextUtils.isEmpty(setting.proxyPassword))) {
                    Log.i(TAG, "proxyUsername = ${setting.proxyUsername}, proxyPassword = ${setting.proxyPassword}")

                    if (setting.proxyType == Proxy.Type.HTTP) {
                        request.okproxyAuthenticator { _: Route?, response: Response ->
                            //设置代理服务器账号密码
                            val credential = Credentials.basic(setting.proxyUsername, setting.proxyPassword)
                            response.request().newBuilder().header("Proxy-Authorization", credential).build()
                        }
                    } else {
                        Authenticator.setDefault(object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(setting.proxyUsername, setting.proxyPassword.toCharArray())
                            }
                        })
                    }
                }
            }

            request.upJson(requestMsg)
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

                        val resp = Gson().fromJson(response, WeworkAgentResult::class.java)
                        val status = if (resp?.errcode == 0L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })
        }

    }
}