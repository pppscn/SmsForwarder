package com.idormy.sms.forwarder.utils.sender

import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.BarkResult
import com.idormy.sms.forwarder.entity.setting.BarkSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.interceptor.BasicAuthInterceptor
import com.idormy.sms.forwarder.utils.interceptor.LoggingInterceptor
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("RegExpRedundantEscape", "UselessCallOnNotNull")
class BarkUtils {
    companion object {

        private val TAG: String = BarkUtils::class.java.simpleName

        fun sendMsg(
            setting: BarkSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            //Log.i(TAG, "sendMsg setting:$setting msgInfo:$msgInfo rule:$rule senderIndex:$senderIndex logId:$logId msgId:$msgId")
            val title: String = if (rule != null) {
                msgInfo.getTitleForSend(setting.title, rule.regexReplace)
            } else {
                msgInfo.getTitleForSend(setting.title)
            }
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
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

            val msgMap: MutableMap<String, Any> = mutableMapOf()
            msgMap["title"] = title
            msgMap["body"] = content
            msgMap["isArchive"] = 1
            if (!TextUtils.isEmpty(setting.group)) msgMap["group"] = setting.group
            if (!TextUtils.isEmpty(setting.icon)) msgMap["icon"] = setting.icon
            if (!TextUtils.isEmpty(setting.level)) msgMap["level"] = setting.level
            if (!TextUtils.isEmpty(setting.sound)) msgMap["sound"] = setting.sound
            if (!TextUtils.isEmpty(setting.badge)) msgMap["badge"] = setting.badge
            if (!TextUtils.isEmpty(setting.url)) msgMap["url"] = setting.url
            if (!TextUtils.isEmpty(setting.call)) msgMap["call"] = setting.call

            //自动复制
            if (TextUtils.isEmpty(setting.autoCopy)) {
                val pattern = Regex("(?<!回复)(验证码|授权码|校验码|检验码|确认码|激活码|动态码|安全码|(验证)?代码|校验代码|检验代码|激活代码|确认代码|动态代码|安全代码|登入码|认证码|识别码|短信口令|动态密码|交易码|上网密码|动态口令|随机码|驗證碼|授權碼|校驗碼|檢驗碼|確認碼|激活碼|動態碼|(驗證)?代碼|校驗代碼|檢驗代碼|確認代碼|激活代碼|動態代碼|登入碼|認證碼|識別碼|一次性密码|[Cc][Oo][Dd][Ee]|[Vv]erification)")
                if (pattern.containsMatchIn(content)) {
                    var code = content.replace("(.*)((代|授权|验证|动态|校验)码|[【\\[].*[】\\]]|[Cc][Oo][Dd][Ee]|[Vv]erification\\s?([Cc]ode)?)\\s?(G-|<#>)?([:：\\s是为]|[Ii][Ss]){0,3}[\\(（\\[【{「]?(([0-9\\s]{4,7})|([\\dA-Za-z]{5,6})(?!([Vv]erification)?([Cc][Oo][Dd][Ee])|:))[」}】\\]）\\)]?(?=([^0-9a-zA-Z]|\$))(.*)".toRegex(), "$7").trim()
                    code = code.replace("\\D*[\\(（\\[【{「]?([0-9]{3}\\s?[0-9]{1,3})[」}】\\]）\\)]?(?=.*((代|授权|验证|动态|校验)码|[【\\[].*[】\\]]|[Cc][Oo][Dd][Ee]|[Vv]erification\\s?([Cc]ode)?))(.*)".toRegex(), "$1").trim()
                    if (code.isNotEmpty()) {
                        msgMap["copy"] = code
                        msgMap["autoCopy"] = 1
                    }
                }
            } else {
                msgMap["copy"] = msgInfo.getContentForSend(setting.autoCopy)
                msgMap["autoCopy"] = 1
            }

            val requestMsg: String = Gson().toJson(msgMap)
            Log.i(TAG, "requestMsg:$requestMsg")
            //推送加密
            if (setting.transformation.isNullOrBlank() || "none" == setting.transformation || setting.key.isNullOrBlank() || setting.iv.isNullOrBlank()) {
                request.upJson(requestMsg)
            } else {
                val transformation = setting.transformation.replace("AES128", "AES").replace("AES192", "AES").replace("AES256", "AES")
                val ciphertext = encrypt(requestMsg, transformation, setting.key, setting.iv)
                //Log.d(TAG, "ciphertext: $ciphertext")
                //val plainText = decrypt(ciphertext, transformation, setting.key, setting.iv)
                //Log.d(TAG, "plainText: $plainText")
                //request.params("ciphertext", URLEncoder.encode(ciphertext, "UTF-8"))
                //request.params("iv", URLEncoder.encode(setting.iv, "UTF-8"))
                request.params("ciphertext", ciphertext)
                request.headers("Content-Type", "application/x-www-form-urlencoded")
            }

            request.ignoreHttpsCert() //忽略https证书
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

                        val resp = Gson().fromJson(response, BarkResult::class.java)
                        val status = if (resp?.code == 200L) 2 else 0
                        SendUtils.updateLogs(logId, status, response)
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)

                    }

                })

        }

        fun encrypt(plainText: String, transformation: String, key: String, iv: String): String {
            //Log.d(TAG, "plainText: $plainText, transformation: $transformation, key: $key, iv: $iv")
            val cipher = Cipher.getInstance(transformation)
            val keySpec = SecretKeySpec(key.toByteArray(), "AES")
            if (transformation.contains("ECB")) {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            } else if (transformation.contains("CBC")) {
                val ivSpec = IvParameterSpec(iv.toByteArray())
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            } else {
                throw IllegalArgumentException("Unsupported transformation: $transformation")
            }
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        }

        fun decrypt(encryptedText: String, transformation: String, key: String, iv: String): String {
            //Log.d(TAG, "encryptedText: $encryptedText, transformation: $transformation, key: $key, iv: $iv")
            val cipher = Cipher.getInstance(transformation)
            val keySpec = SecretKeySpec(key.toByteArray(), "AES")
            if (transformation.contains("ECB")) {
                cipher.init(Cipher.DECRYPT_MODE, keySpec)
            } else if (transformation.contains("CBC")) {
                val ivSpec = IvParameterSpec(iv.toByteArray())
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            } else {
                throw IllegalArgumentException("Unsupported transformation: $transformation")
            }
            val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }

    }
}
