package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.BarkResult
import com.idormy.sms.forwarder.entity.setting.BarkSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.util.regex.Pattern

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER", "unused")
class BarkUtils {
    companion object {

        private val TAG: String = BarkUtils::class.java.simpleName

        fun sendMsg(
            setting: BarkSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.title.toString(), rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.title.toString())
            }
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val requestUrl: String = setting.server //推送地址
            Log.i(TAG, "requestUrl:$requestUrl")

            //支持HTTP基本认证(Basic Authentication)
            val regex = "^(https?://)([^:]+):([^@]+)@(.+)"
            val matches = Regex(regex, RegexOption.IGNORE_CASE).findAll(requestUrl).toList().flatMap(MatchResult::groupValues)
            Log.i(TAG, "matches = $matches")
            val request = if (matches.isNotEmpty()) {
                XHttp.post(matches[1] + matches[4]).addInterceptor(BasicAuthInterceptor(matches[2], matches[3]))
            } else {
                XHttp.post(requestUrl)
            }

            request.params("title", title)
                .params("body", content)
                .params("isArchive", 1)
            if (!TextUtils.isEmpty(setting.group)) request.params("group", setting.group)
            if (!TextUtils.isEmpty(setting.icon)) request.params("icon", setting.icon)
            if (!TextUtils.isEmpty(setting.level)) request.params("level", setting.level)
            if (!TextUtils.isEmpty(setting.sound)) request.params("sound", setting.sound)
            if (!TextUtils.isEmpty(setting.badge)) request.params("badge", setting.badge)
            if (!TextUtils.isEmpty(setting.url)) request.params("url", setting.url)

            val isCode: Int = content.indexOf("验证码")
            val isPassword: Int = content.indexOf("动态密码")
            val isPassword2: Int = content.indexOf("短信密码")
            if (isCode != -1 || isPassword != -1 || isPassword2 != -1) {
                val p = Pattern.compile("(\\d{4,6})")
                val m = p.matcher(content)
                if (m.find()) {
                    println(m.group())
                    request.params("automaticallyCopy", "1")
                    request.params("copy", m.group())
                }
            }

            request.ignoreHttpsCert() //忽略https证书
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

                        val resp = Gson().fromJson(response, BarkResult::class.java)
                        if (resp.code == 200L) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })

        }

        fun sendMsg(setting: BarkSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}