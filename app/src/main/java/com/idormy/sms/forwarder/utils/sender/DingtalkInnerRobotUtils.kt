package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.DingtalkInnerRobotResult
import com.idormy.sms.forwarder.entity.setting.DingtalkInnerRobotSetting
import com.idormy.sms.forwarder.utils.MMKVUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xui.utils.ResUtils.getString
import com.xuexiang.xutil.net.NetworkUtils
import okhttp3.Credentials
import okhttp3.Response
import okhttp3.Route
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

//钉钉企业内机器人
@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class DingtalkInnerRobotUtils private constructor() {
    companion object {

        private val TAG: String = DingtalkInnerRobotUtils::class.java.simpleName

        fun sendMsg(
            setting: DingtalkInnerRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {

            val accessToken: String? = MMKVUtils.getString("accessToken_" + setting.agentID, "")
            val expiresIn: Long = MMKVUtils.getLong("expiresIn_" + setting.agentID, 0L)
            if (!TextUtils.isEmpty(accessToken) && expiresIn > System.currentTimeMillis()) {
                return sendTextMsg(setting, msgInfo, rule, logId)
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
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS)
                && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)
            ) {
                //代理服务器的IP和端口号
                Log.d(TAG, "proxyHost = ${setting.proxyHost}, proxyPort = ${setting.proxyPort}")
                val proxyHost = if (NetworkUtils.isIP(setting.proxyHost)) setting.proxyHost else NetworkUtils.getDomainAddress(setting.proxyHost)
                if (!NetworkUtils.isIP(proxyHost)) {
                    throw Exception("代理服务器主机名解析失败：proxyHost=$proxyHost")
                }
                val proxyPort: Int = setting.proxyPort?.toInt() ?: 7890

                Log.d(TAG, "proxyHost = $proxyHost, proxyPort = $proxyPort")
                request.okproxy(Proxy(setting.proxyType, InetSocketAddress(proxyHost, proxyPort)))

                //代理的鉴权账号密码
                if (setting.proxyAuthenticator == true
                    && (!TextUtils.isEmpty(setting.proxyUsername) || !TextUtils.isEmpty(setting.proxyPassword))
                ) {
                    Log.i(TAG, "proxyUsername = ${setting.proxyUsername}, proxyPassword = ${setting.proxyPassword}")

                    if (setting.proxyType == Proxy.Type.HTTP) {
                        request.okproxyAuthenticator { _: Route?, response: Response ->
                            //设置代理服务器账号密码
                            val credential = Credentials.basic(setting.proxyUsername.toString(), setting.proxyPassword.toString())
                            response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        }
                    } else {
                        Authenticator.setDefault(object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(setting.proxyUsername.toString(), setting.proxyPassword?.toCharArray())
                            }
                        })
                    }
                }
            }

            request.upJson(requestMsg)
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

                        val resp = Gson().fromJson(response, DingtalkInnerRobotResult::class.java)
                        if (!TextUtils.isEmpty(resp.accessToken)) {
                            MMKVUtils.put("accessToken_" + setting.agentID, resp.accessToken)
                            MMKVUtils.put("expiresIn_" + setting.agentID, System.currentTimeMillis() + ((resp.expireIn ?: 7200) - 120) * 1000L) //提前2分钟过期
                            sendTextMsg(setting, msgInfo, rule, logId)
                        } else {
                            SendUtils.updateLogs(logId, 0, String.format(getString(R.string.request_failed_tips), response))
                        }
                    }

                })

        }

        //发送文本消息
        private fun sendTextMsg(
            setting: DingtalkInnerRobotSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val requestUrl = "https://api.dingtalk.com/v1.0/robot/oToMessages/batchSend"
            Log.d(TAG, "requestUrl：$requestUrl")

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val msgParam: MutableMap<String, Any> = mutableMapOf()
            if ("sampleMarkdown" == setting.msgKey) {
                msgParam["title"] = if (rule != null) {
                    msgInfo.getTitleForSend(setting.titleTemplate.toString(), rule.regexReplace)
                } else {
                    msgInfo.getTitleForSend(setting.titleTemplate.toString())
                }
                msgParam["text"] = content
            } else {
                msgParam["content"] = content
            }

            val textMsgMap: MutableMap<String, Any> = mutableMapOf()
            textMsgMap["robotCode"] = setting.appKey
            textMsgMap["userIds"] = setting.userIds.split('|').toTypedArray()
            textMsgMap["msgKey"] = setting.msgKey
            textMsgMap["msgParam"] = Gson().toJson(msgParam)

            val requestMsg: String = Gson().toJson(textMsgMap)
            Log.i(TAG, "requestMsg:$requestMsg")

            val request = XHttp.post(requestUrl)

            //设置代理
            if ((setting.proxyType == Proxy.Type.HTTP || setting.proxyType == Proxy.Type.SOCKS)
                && !TextUtils.isEmpty(setting.proxyHost) && !TextUtils.isEmpty(setting.proxyPort)
            ) {
                //代理服务器的IP和端口号
                Log.d(TAG, "proxyHost = ${setting.proxyHost}, proxyPort = ${setting.proxyPort}")
                val proxyHost = if (NetworkUtils.isIP(setting.proxyHost)) setting.proxyHost else NetworkUtils.getDomainAddress(setting.proxyHost)
                if (!NetworkUtils.isIP(proxyHost)) {
                    throw Exception("代理服务器主机名解析失败：proxyHost=$proxyHost")
                }
                val proxyPort: Int = setting.proxyPort?.toInt() ?: 7890

                Log.d(TAG, "proxyHost = $proxyHost, proxyPort = $proxyPort")
                request.okproxy(Proxy(setting.proxyType, InetSocketAddress(proxyHost, proxyPort)))

                //代理的鉴权账号密码
                if (setting.proxyAuthenticator == true
                    && (!TextUtils.isEmpty(setting.proxyUsername) || !TextUtils.isEmpty(setting.proxyPassword))
                ) {
                    Log.i(TAG, "proxyUsername = ${setting.proxyUsername}, proxyPassword = ${setting.proxyPassword}")

                    if (setting.proxyType == Proxy.Type.HTTP) {
                        request.okproxyAuthenticator { _: Route?, response: Response ->
                            //设置代理服务器账号密码
                            val credential = Credentials.basic(setting.proxyUsername.toString(), setting.proxyPassword.toString())
                            response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        }
                    } else {
                        Authenticator.setDefault(object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(setting.proxyUsername.toString(), setting.proxyPassword?.toCharArray())
                            }
                        })
                    }
                }
            }

            request.upJson(requestMsg)
                .headers("x-acs-dingtalk-access-token", MMKVUtils.getString("accessToken_" + setting.agentID, ""))
                .keepJson(true)
                .ignoreHttpsCert()
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

                        val resp = Gson().fromJson(response, DingtalkInnerRobotResult::class.java)
                        if (!TextUtils.isEmpty(resp.processQueryKey)) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })
        }

        fun sendMsg(setting: DingtalkInnerRobotSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }

    }
}