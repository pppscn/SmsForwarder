package com.idormy.sms.forwarder.utils


import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.CloneInfo
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.xuexiang.xutil.resource.ResUtils.getString
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
        var enableServerAutorun: Boolean by SharedPreference(SP_ENABLE_SERVER_AUTORUN, false)

        //服务端签名密钥
        var serverSignKey: String by SharedPreference(SP_SERVER_SIGN_KEY, "")

        //服务端安全设置
        var safetyMeasures: Int by SharedPreference(SP_SERVER_SAFETY_MEASURES, if (TextUtils.isEmpty(serverSignKey)) 0 else 1)

        //服务端SM4密钥
        var serverSm4Key: String by SharedPreference(SP_SERVER_SM4_KEY, "")

        //服务端RSA公钥
        var serverPublicKey: String by SharedPreference(SP_SERVER_PUBLIC_KEY, "")

        //服务端RSA私钥
        var serverPrivateKey: String by SharedPreference(SP_SERVER_PRIVATE_KEY, "")

        //时间容差
        var timeTolerance: Int by SharedPreference(SP_SERVER_TIME_TOLERANCE, 600)

        //自定义web客户端目录
        var serverWebPath: String by SharedPreference(SP_SERVER_WEB_PATH, "")

        //服务地址
        var serverAddress: String by SharedPreference(SP_SERVER_ADDRESS, "http://127.0.0.1:5000")

        //服务地址历史记录
        var serverHistory: String by SharedPreference(SP_SERVER_HISTORY, "")

        //服务端配置
        var serverConfig: String by SharedPreference(SP_SERVER_CONFIG, "")

        //客户端签名密钥/RSA公钥
        var clientSignKey: String by SharedPreference(SP_CLIENT_SIGN_KEY, "")

        //服务端安全设置
        var clientSafetyMeasures: Int by SharedPreference(SP_CLIENT_SAFETY_MEASURES, if (TextUtils.isEmpty(clientSignKey)) 0 else 1)

        //是否启用一键克隆
        var enableApiClone: Boolean by SharedPreference(SP_ENABLE_API_CLONE, true)

        //是否启用远程发短信
        var enableApiSmsSend: Boolean by SharedPreference(SP_ENABLE_API_SMS_SEND, true)

        //是否启用远程查短信
        var enableApiSmsQuery: Boolean by SharedPreference(SP_ENABLE_API_SMS_QUERY, true)

        //是否启用远程查通话
        var enableApiCallQuery: Boolean by SharedPreference(SP_ENABLE_API_CALL_QUERY, true)

        //是否启用远程查话簿
        var enableApiContactQuery: Boolean by SharedPreference(SP_ENABLE_API_CONTACT_QUERY, true)

        //是否启用远程加话簿
        var enableApiContactAdd: Boolean by SharedPreference(SP_ENABLE_API_CONTACT_ADD, true)

        //是否启用远程查电量
        var enableApiBatteryQuery: Boolean by SharedPreference(SP_ENABLE_API_BATTERY_QUERY, true)

        //是否启用远程WOL
        var enableApiWol: Boolean by SharedPreference(SP_ENABLE_API_WOL, true)

        //是否启用远程找手机
        var enableApiLocation: Boolean by SharedPreference(SP_ENABLE_API_LOCATION, false)

        //远程找手机定位缓存
        var apiLocationCache: LocationInfo by SharedPreference(SP_API_LOCATION_CACHE, LocationInfo())

        //WOL历史记录
        var wolHistory: String by SharedPreference(SP_WOL_HISTORY, "")

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
            val tolerance = timeTolerance * 1000L
            if (diffTime > tolerance) {
                throw HttpException(500, String.format(getString(R.string.timestamp_verify_failed), timestamp, timeTolerance, diffTime))
            }

            val sign = calcSign(req.timestamp.toString(), signSecret)
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
            cloneInfo.settings = SharedPreference.exportPreference()
            cloneInfo.senderList = Core.sender.getAllNonCache()
            cloneInfo.ruleList = Core.rule.getAllNonCache()
            cloneInfo.frpcList = Core.frpc.getAllNonCache()
            cloneInfo.taskList = Core.task.getAllNonCache()
            return cloneInfo
        }

        //还原设置
        fun restoreSettings(cloneInfo: CloneInfo): Boolean {
            return try {
                //保留设备名称、SIM卡主键/备注
                val extraDeviceMark = SettingUtils.extraDeviceMark
                val subidSim1 = SettingUtils.subidSim1
                val extraSim1 = SettingUtils.extraSim1
                val subidSim2 = SettingUtils.subidSim2
                val extraSim2 = SettingUtils.extraSim2
                //应用配置
                SharedPreference.clearPreference()
                SharedPreference.importPreference(cloneInfo.settings)
                //需要排除的配置
                SettingUtils.extraDeviceMark = extraDeviceMark
                SettingUtils.subidSim1 = subidSim1
                SettingUtils.extraSim1 = extraSim1
                SettingUtils.subidSim2 = subidSim2
                SettingUtils.extraSim2 = extraSim2
                //删除消息与转发日志
                Core.logs.deleteAll()
                Core.msg.deleteAll()
                //发送通道
                Core.sender.deleteAll()
                if (!cloneInfo.senderList.isNullOrEmpty()) {
                    for (sender in cloneInfo.senderList!!) {
                        Core.sender.insert(sender)
                    }
                }
                //转发规则
                Core.rule.deleteAll()
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
                //Task配置
                Core.task.deleteAll()
                if (!cloneInfo.taskList.isNullOrEmpty()) {
                    for (task in cloneInfo.taskList!!) {
                        Core.task.insert(task)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("restoreSettings", e.message.toString())
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
                if (safetyMeasures == 1) {
                    resp["sign"] = calcSign(timestamp.toString(), serverSignKey)
                }
            }

            return Gson().toJson(resp)
        }
    }
}