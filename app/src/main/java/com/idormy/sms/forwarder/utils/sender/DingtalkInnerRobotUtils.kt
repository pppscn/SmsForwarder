package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.DingtalkInnerRobotResult
import com.idormy.sms.forwarder.entity.setting.DingtalkInnerRobotSetting
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

//钉钉企业内机器人
class DingtalkInnerRobotUtils private constructor() {
    companion object {

        private val TAG: String = DingtalkInnerRobotUtils::class.java.simpleName

        fun sendMsg(
            setting: DingtalkInnerRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            var accessToken: String by SharedPreference("accessToken_" + setting.agentID, "")
            var expiresIn: Long by SharedPreference("expiresIn_" + setting.agentID, 0L)
            if (!TextUtils.isEmpty(accessToken) && expiresIn > System.currentTimeMillis()) {
                return sendTextMsg(setting, msgInfo, rule, senderIndex, logId, msgId)
            }

            val requestUrl = "https://api.dingtalk.com/v1.0/oauth2/accessToken"
            Log.d(TAG, "requestUrl：$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["appKey"] = setting.appKey
            msgMap["appSecret"] = setting.appSecret
            val requestMsg: String = Gson().toJson(msgMap)
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

                        val resp = Gson().fromJson(response, DingtalkInnerRobotResult::class.java)
                        if (!TextUtils.isEmpty(resp?.accessToken)) {
                            accessToken = resp.accessToken.toString()
                            expiresIn = System.currentTimeMillis() + ((resp.expireIn ?: 7200) - 120) * 1000L //提前2分钟过期
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
            setting: DingtalkInnerRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val requestUrl = "https://api.dingtalk.com/v1.0/robot/oToMessages/batchSend"
            Log.d(TAG, "requestUrl：$requestUrl")

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            val msgParam: MutableMap<String, Any> = mutableMapOf()
            if ("sampleMarkdown" == setting.msgKey) {
                msgParam["title"] = if (rule != null) {
                    msgInfo.getTitleForSend(setting.titleTemplate, rule.regexReplace)
                } else {
                    msgInfo.getTitleForSend(setting.titleTemplate)
                }
                msgParam["text"] = content
            } else {
                msgParam["content"] = content
            }

            val textMsgMap: MutableMap<String, Any> = mutableMapOf()
            textMsgMap["robotCode"] = setting.appKey
            textMsgMap["userIds"] = setting.userIds.replace("[,，;；|]".toRegex(), "|").trim('|').split('|').toTypedArray()
            textMsgMap["msgKey"] = setting.msgKey
            textMsgMap["msgParam"] = Gson().toJson(msgParam)

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

            val accessToken: String by SharedPreference("accessToken_" + setting.agentID, "")
            request.upJson(requestMsg).headers("x-acs-dingtalk-access-token", accessToken)
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

                        val resp = Gson().fromJson(response, DingtalkInnerRobotResult::class.java)
                        val status = if (!TextUtils.isEmpty(resp?.processQueryKey)) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                    }

                })
        }

    }
}