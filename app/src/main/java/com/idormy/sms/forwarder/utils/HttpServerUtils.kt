package com.idormy.sms.forwarder.utils


import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.CloneInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.xuexiang.xui.utils.ResUtils.getString
import com.xuexiang.xutil.app.AppUtils
import com.yanzhenjie.andserver.error.HttpException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HttpServer工具类
 */
class HttpServerUtils private constructor() {

    companion object {

        //是否启用HttpServer开机自启
        @JvmStatic
        var enableServerAutorun: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_SERVER_AUTORUN, false)
            set(enableServerAutorun) {
                MMKVUtils.put(SP_ENABLE_SERVER_AUTORUN, enableServerAutorun)
            }

        //服务端签名密钥
        @JvmStatic
        var serverSignKey: String?
            get() = MMKVUtils.getString(SP_SERVER_SIGN_KEY, "")
            set(serverSignKey) {
                MMKVUtils.put(SP_SERVER_SIGN_KEY, serverSignKey)
            }

        //自定义web客户端目录
        @JvmStatic
        var serverWebPath: String?
            get() = MMKVUtils.getString(SP_SERVER_WEB_PATH, "")
            set(serverWebPath) {
                MMKVUtils.put(SP_SERVER_WEB_PATH, serverWebPath)
            }

        //服务地址
        @JvmStatic
        var serverAddress: String?
            get() = MMKVUtils.getString(SP_SERVER_ADDRESS, "")
            set(clientSignKey) {
                MMKVUtils.put(SP_SERVER_ADDRESS, clientSignKey)
            }

        //服务地址历史记录
        @JvmStatic
        var serverHistory: String?
            get() = MMKVUtils.getString(SP_SERVER_HISTORY, "")
            set(serverHistory) {
                MMKVUtils.put(SP_SERVER_HISTORY, serverHistory)
            }

        //服务端配置
        @JvmStatic
        var serverConfig: String?
            get() = MMKVUtils.getString(SP_SERVER_CONFIG, "")
            set(serverConfig) {
                MMKVUtils.put(SP_SERVER_CONFIG, serverConfig)
            }

        //客户端签名密钥
        @JvmStatic
        var clientSignKey: String?
            get() = MMKVUtils.getString(SP_CLIENT_SIGN_KEY, "")
            set(clientSignKey) {
                MMKVUtils.put(SP_CLIENT_SIGN_KEY, clientSignKey)
            }

        //是否启用一键克隆
        @JvmStatic
        var enableApiClone: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_CLONE, false)
            set(enableApiClone) {
                MMKVUtils.put(SP_ENABLE_API_CLONE, enableApiClone)
            }

        //是否启用远程发短信
        @JvmStatic
        var enableApiSmsSend: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_SMS_SEND, false)
            set(enableApiSendSms) {
                MMKVUtils.put(SP_ENABLE_API_SMS_SEND, enableApiSendSms)
            }

        //是否启用远程查短信
        @JvmStatic
        var enableApiSmsQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_SMS_QUERY, false)
            set(enableApiQuerySms) {
                MMKVUtils.put(SP_ENABLE_API_SMS_QUERY, enableApiQuerySms)
            }

        //是否启用远程查通话
        @JvmStatic
        var enableApiCallQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_CALL_QUERY, false)
            set(enableApiQueryCall) {
                MMKVUtils.put(SP_ENABLE_API_CALL_QUERY, enableApiQueryCall)
            }

        //是否启用远程查话簿
        @JvmStatic
        var enableApiContactQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_CONTACT_QUERY, false)
            set(enableApiQueryLinkman) {
                MMKVUtils.put(SP_ENABLE_API_CONTACT_QUERY, enableApiQueryLinkman)
            }

        //是否启用远程查电量
        @JvmStatic
        var enableApiBatteryQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_BATTERY_QUERY, false)
            set(enableApiQueryBattery) {
                MMKVUtils.put(SP_ENABLE_API_BATTERY_QUERY, enableApiQueryBattery)
            }

        //是否启用远程WOL
        @JvmStatic
        var enableApiWol: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_WOL, false)
            set(enableApiWol) {
                MMKVUtils.put(SP_ENABLE_API_WOL, enableApiWol)
            }

        //WOL历史记录
        @JvmStatic
        var wolHistory: String?
            get() = MMKVUtils.getString(SP_WOL_HISTORY, "")
            set(wolHistory) {
                MMKVUtils.put(SP_WOL_HISTORY, wolHistory)
            }

        //计算签名
        fun calcSign(timestamp: String, signSecret: String): String {
            val stringToSign = "$timestamp\n" + signSecret
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(signSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
            return URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
        }

        //校验签名
        @Throws(HttpException::class)
        fun checkSign(req: BaseRequest<*>) {
            val signSecret = serverSignKey
            if (TextUtils.isEmpty(signSecret)) return

            if (TextUtils.isEmpty(req.sign)) throw HttpException(500, getString(R.string.sign_required))
            if (req.timestamp == 0L) throw HttpException(500, getString(R.string.timestamp_required))

            val timestamp = System.currentTimeMillis()
            val diffTime = kotlin.math.abs(timestamp - req.timestamp)
            if (diffTime > 3600000L) {
                throw HttpException(500, String.format(getString(R.string.timestamp_verify_failed), timestamp, diffTime))
            }

            val sign = calcSign(req.timestamp.toString(), signSecret.toString())
            if (sign != req.sign) {
                Log.e("calcSign", sign)
                Log.e("reqSign", req.sign.toString())
                throw HttpException(500, getString(R.string.sign_verify_failed))
            }
        }

        //判断版本是否一致
        @Throws(HttpException::class)
        fun compareVersion(cloneInfo: CloneInfo) {
            val versionCodeRequest = cloneInfo.versionCode
            if (versionCodeRequest == 0) throw HttpException(500, getString(R.string.version_code_required))
            val versionCodeLocal = AppUtils.getAppVersionCode().toString().substring(1)
            if (!versionCodeRequest.toString().endsWith(versionCodeLocal)) throw HttpException(500, getString(R.string.inconsistent_version))
        }

        //导出设置
        fun exportSettings(): CloneInfo {
            val cloneInfo = CloneInfo()
            cloneInfo.versionCode = AppUtils.getAppVersionCode()
            cloneInfo.versionName = AppUtils.getAppVersionName()
            cloneInfo.enableSms = SettingUtils.enableSms
            cloneInfo.enablePhone = SettingUtils.enablePhone
            cloneInfo.callType1 = SettingUtils.enableCallType1
            cloneInfo.callType2 = SettingUtils.enableCallType2
            cloneInfo.callType3 = SettingUtils.enableCallType3
            cloneInfo.enableAppNotify = SettingUtils.enableAppNotify
            cloneInfo.cancelAppNotify = SettingUtils.enableCancelAppNotify
            cloneInfo.enableNotUserPresent = SettingUtils.enableNotUserPresent
            cloneInfo.enableLoadAppList = SettingUtils.enableLoadAppList
            cloneInfo.enableLoadUserAppList = SettingUtils.enableLoadUserAppList
            cloneInfo.enableLoadSystemAppList = SettingUtils.enableLoadSystemAppList
            cloneInfo.duplicateMessagesLimits = SettingUtils.duplicateMessagesLimits
            cloneInfo.enableBatteryReceiver = SettingUtils.enableBatteryReceiver
            cloneInfo.batteryLevelMin = SettingUtils.batteryLevelMin
            cloneInfo.batteryLevelMax = SettingUtils.batteryLevelMax
            cloneInfo.batteryLevelOnce = SettingUtils.batteryLevelOnce
            cloneInfo.enableBatteryCron = SettingUtils.enableBatteryCron
            cloneInfo.batteryCronStartTime = SettingUtils.batteryCronStartTime
            cloneInfo.batteryCronInterval = SettingUtils.batteryCronInterval
            cloneInfo.enableExcludeFromRecents = SettingUtils.enableExcludeFromRecents
            cloneInfo.enableCactus = SettingUtils.enableCactus
            cloneInfo.enablePlaySilenceMusic = SettingUtils.enablePlaySilenceMusic
            cloneInfo.enableOnePixelActivity = SettingUtils.enableOnePixelActivity
            cloneInfo.requestRetryTimes = SettingUtils.requestRetryTimes
            cloneInfo.requestDelayTime = SettingUtils.requestDelayTime
            cloneInfo.requestTimeout = SettingUtils.requestTimeout
            cloneInfo.notifyContent = SettingUtils.notifyContent
            cloneInfo.enableSmsTemplate = SettingUtils.enableSmsTemplate
            cloneInfo.smsTemplate = SettingUtils.smsTemplate
            cloneInfo.enableHelpTip = SettingUtils.enableHelpTip
            cloneInfo.enablePureClientMode = SettingUtils.enablePureClientMode
            cloneInfo.senderList = Core.sender.all
            cloneInfo.ruleList = Core.rule.all
            cloneInfo.frpcList = Core.frpc.all

            return cloneInfo
        }

        //还原设置
        fun restoreSettings(cloneInfo: CloneInfo): Boolean {
            return try {
                //应用配置
                SettingUtils.enableSms = cloneInfo.enableSms
                SettingUtils.enablePhone = cloneInfo.enablePhone
                SettingUtils.enableCallType1 = cloneInfo.callType1
                SettingUtils.enableCallType2 = cloneInfo.callType2
                SettingUtils.enableCallType3 = cloneInfo.callType3
                SettingUtils.enableAppNotify = cloneInfo.enableAppNotify
                SettingUtils.enableCancelAppNotify = cloneInfo.cancelAppNotify
                SettingUtils.enableNotUserPresent = cloneInfo.enableNotUserPresent
                SettingUtils.enableLoadAppList = cloneInfo.enableLoadAppList
                SettingUtils.enableLoadUserAppList = cloneInfo.enableLoadUserAppList
                SettingUtils.enableLoadSystemAppList = cloneInfo.enableLoadSystemAppList
                SettingUtils.duplicateMessagesLimits = cloneInfo.duplicateMessagesLimits
                SettingUtils.enableBatteryReceiver = cloneInfo.enableBatteryReceiver
                SettingUtils.batteryLevelMin = cloneInfo.batteryLevelMin
                SettingUtils.batteryLevelMax = cloneInfo.batteryLevelMax
                SettingUtils.batteryLevelOnce = cloneInfo.batteryLevelOnce
                SettingUtils.enableBatteryCron = cloneInfo.enableBatteryCron
                SettingUtils.batteryCronStartTime = cloneInfo.batteryCronStartTime
                SettingUtils.batteryCronInterval = cloneInfo.batteryCronInterval
                SettingUtils.enableExcludeFromRecents = cloneInfo.enableExcludeFromRecents
                SettingUtils.enableCactus = cloneInfo.enableCactus
                SettingUtils.enablePlaySilenceMusic = cloneInfo.enablePlaySilenceMusic
                SettingUtils.enableOnePixelActivity = cloneInfo.enableOnePixelActivity
                SettingUtils.requestRetryTimes = cloneInfo.requestRetryTimes
                SettingUtils.requestDelayTime = cloneInfo.requestDelayTime
                SettingUtils.requestTimeout = cloneInfo.requestTimeout
                SettingUtils.notifyContent = cloneInfo.notifyContent
                SettingUtils.enableSmsTemplate = cloneInfo.enableSmsTemplate
                SettingUtils.smsTemplate = cloneInfo.smsTemplate
                SettingUtils.enableHelpTip = cloneInfo.enableHelpTip
                SettingUtils.enablePureClientMode = cloneInfo.enablePureClientMode
                //删除发送通道、转发规则、转发日志
                Core.sender.deleteAll()
                //发送通道
                if (!cloneInfo.senderList.isNullOrEmpty()) {
                    for (sender in cloneInfo.senderList!!) {
                        Core.sender.insert(sender)
                    }
                }
                //转发规则
                if (!cloneInfo.ruleList.isNullOrEmpty()) {
                    for (rule in cloneInfo.ruleList!!) {
                        Core.rule.insert(rule)
                    }
                }
                //Frpc配置
                Core.frpc.deleteAll()
                if (!cloneInfo.frpcList.isNullOrEmpty()) {
                    for (frpc in cloneInfo.frpcList!!) {
                        Core.frpc.insert(frpc)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                throw HttpException(500, e.message)
                //false
            }
        }

        //返回统一结构报文
        fun response(output: Any?): String {
            val resp: MutableMap<String, Any> = mutableMapOf()
            val timestamp = System.currentTimeMillis()
            resp["timestamp"] = timestamp
            if (output is String && output != "success") {
                resp["code"] = HTTP_FAILURE_CODE
                resp["msg"] = output
            } else {
                resp["code"] = HTTP_SUCCESS_CODE
                resp["msg"] = "success"
                if (output != null) {
                    resp["data"] = output
                }
                if (!TextUtils.isEmpty(serverSignKey)) {
                    resp["sign"] = calcSign(timestamp.toString(), serverSignKey.toString())
                }
            }

            return Gson().toJson(resp)
        }
    }
}