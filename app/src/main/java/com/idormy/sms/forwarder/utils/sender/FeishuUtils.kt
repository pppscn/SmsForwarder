package com.idormy.sms.forwarder.utils.sender

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.FeishuResult
import com.idormy.sms.forwarder.entity.setting.FeishuSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class FeishuUtils private constructor() {
    companion object {

        private val TAG: String = FeishuUtils::class.java.simpleName
        private val MSG_TEMPLATE = """
{
    "config": {
        "wide_screen_mode": true
    },
    "elements": [
        {
            "fields": [
                {
                    "is_short": true,
                    "text": {
                        "content": "**时间**\n{{MSG_TIME}}",
                        "tag": "lark_md"
                    }
                },
                {
                    "is_short": true,
                    "text": {
                        "content": "**来源**\n{{MSG_FROM}}",
                        "tag": "lark_md"
                    }
                }
            ],
            "tag": "div"
        },
        {
            "tag": "div",
            "text": {
                "content": "{{MSG_CONTENT}}",
                "tag": "lark_md"
            }
        },
        {
            "tag": "hr"
        },
        {
            "elements": [
                {
                    "content": "[SmsForwarder](https://github.com/pppscn/SmsForwarder)",
                    "tag": "lark_md"
                }
            ],
            "tag": "note"
        }
    ],
    "header": {
        "template": "turquoise",
        "title": {
            "content": "{{MSG_TITLE}}",
            "tag": "plain_text"
        }
    }
}
        """.trimIndent()

        fun sendMsg(
            setting: FeishuSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            val from: String = msgInfo.from
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.titleTemplate.toString(), rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.titleTemplate.toString())
            }
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            val requestUrl = setting.webhook
            Log.i(TAG, "requestUrl:$requestUrl")

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            if (setting.secret != null) {
                val timestamp = System.currentTimeMillis() / 1000
                val stringToSign = "$timestamp\n" + setting.secret
                Log.i(TAG, "stringToSign = $stringToSign")

                //使用HmacSHA256算法计算签名
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(stringToSign.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
                val signData = mac.doFinal(byteArrayOf())
                val sign = String(Base64.encode(signData, Base64.NO_WRAP))

                msgMap["timestamp"] = timestamp
                msgMap["sign"] = sign
            }

            //组装报文
            val requestMsg: String
            if (setting.msgType == null || setting.msgType == "interactive") {
                msgMap["msg_type"] = "interactive"
                msgMap["card"] = "{{CARD_BODY}}"
                requestMsg = Gson().toJson(msgMap).replace("\"{{CARD_BODY}}\"", buildMsg(title, content, from, msgInfo.date))
            } else {
                msgMap["msg_type"] = "text"
                val contentMap: MutableMap<String, Any> = mutableMapOf()
                contentMap["text"] = content
                msgMap["content"] = contentMap
                requestMsg = Gson().toJson(msgMap)
            }
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

                        val resp = Gson().fromJson(response, FeishuResult::class.java)
                        if (resp.code == 0L) {
                            SendUtils.updateLogs(logId, 2, response)
                        } else {
                            SendUtils.updateLogs(logId, 0, response)
                        }
                    }

                })

        }

        private fun buildMsg(title: String, content: String, from: String, date: Date): String {
            val msgTitle = jsonInnerStr(title)
            val msgContent = jsonInnerStr(content)
            val msgFrom = jsonInnerStr(from)
            val msgTime = jsonInnerStr(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date))
            return MSG_TEMPLATE.replace("{{MSG_TITLE}}", msgTitle)
                .replace("{{MSG_TIME}}", msgTime)
                .replace("{{MSG_FROM}}", msgFrom)
                .replace("{{MSG_CONTENT}}", msgContent)
        }

        private fun jsonInnerStr(string: String?): String {
            if (string == null) return "null"

            val jsonStr: String = Gson().toJson(string)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

        fun sendMsg(setting: FeishuSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}