package cn.ppps.forwarder.utils


import android.text.TextUtils
import android.util.Base64
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.Core
import cn.ppps.forwarder.entity.CloneInfo
import cn.ppps.forwarder.entity.LocationInfo
import cn.ppps.forwarder.server.model.BaseRequest
import com.google.gson.Gson
import com.xuexiang.xutil.resource.ResUtils.getString
import com.yanzhenjie.andserver.error.HttpException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HttpServer工具类
 */
@Suppress("UselessCallOnNotNull")
class HttpServerUtils private constructor() {

    companion object {

        // 本地版本 -> 允许的请求版本范围
        private val VERSION_COMPAT_MAP: Map<Int, IntRange> = mapOf(
            55 to (54..55)
        )

        //是否启用HttpServer开机自启
        var enableServerAutorun: Boolean by SharedPreference(SP_ENABLE_SERVER_AUTORUN, false)

        //服务端签名密钥
        var serverSignKey: String by SharedPreference(SP_SERVER_SIGN_KEY, "")

        //服务端安全设置，默认启用"校验签名"（SecurityDefaults 迁移保证签名密钥非空）
        var safetyMeasures: Int by SharedPreference(SP_SERVER_SAFETY_MEASURES, 1)

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

        //服务端监听端口
        var serverPort: Int by SharedPreference(SP_SERVER_PORT, HTTP_SERVER_PORT)

        //仅监听本机回环地址，默认开启以避免未认证服务暴露到网络。
        var serverLocalIpOnly: Boolean by SharedPreference(SP_SERVER_LOCAL_IP_ONLY, true)

        //服务地址
        var serverAddress: String by SharedPreference(SP_SERVER_ADDRESS, "http://127.0.0.1:$serverPort")

        //服务地址历史记录
        var serverHistory: String by SharedPreference(SP_SERVER_HISTORY, "")

        //服务端配置
        var serverConfig: String by SharedPreference(SP_SERVER_CONFIG, "")

        //客户端签名密钥/RSA公钥
        var clientSignKey: String by SharedPreference(SP_CLIENT_SIGN_KEY, "")

        //服务端安全设置
        var clientSafetyMeasures: Int by SharedPreference(SP_CLIENT_SAFETY_MEASURES, if (TextUtils.isEmpty(clientSignKey)) 0 else 1)

        //是否启用一键克隆
        var enableApiClone: Boolean by SharedPreference(SP_ENABLE_API_CLONE, false)

        //是否启用远程发短信
        var enableApiSmsSend: Boolean by SharedPreference(SP_ENABLE_API_SMS_SEND, false)

        //是否启用远程查短信
        var enableApiSmsQuery: Boolean by SharedPreference(SP_ENABLE_API_SMS_QUERY, false)

        //是否启用远程查通话
        var enableApiCallQuery: Boolean by SharedPreference(SP_ENABLE_API_CALL_QUERY, false)

        //是否启用远程查话簿
        var enableApiContactQuery: Boolean by SharedPreference(SP_ENABLE_API_CONTACT_QUERY, false)

        //是否启用远程加话簿
        var enableApiContactAdd: Boolean by SharedPreference(SP_ENABLE_API_CONTACT_ADD, false)

        //是否启用远程查电量
        var enableApiBatteryQuery: Boolean by SharedPreference(SP_ENABLE_API_BATTERY_QUERY, false)

        //是否启用远程WOL
        var enableApiWol: Boolean by SharedPreference(SP_ENABLE_API_WOL, false)

        //是否启用远程查配置
        var enableApiConfigQuery: Boolean by SharedPreference(SP_ENABLE_API_CONFIG_QUERY, false)

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

        fun generateServerSignKey(): String {
            val bytes = ByteArray(24)
            SecureRandom().nextBytes(bytes)
            return String(Base64.encode(bytes, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.US_ASCII)
        }

        fun isCloneEncryptionEnabled(): Boolean = safetyMeasures == 2 || safetyMeasures == 3

        //"校验签名"模式下密钥是否已配置，密钥为空时不允许启动服务端
        fun isSignKeyConfigured(): Boolean = !TextUtils.isEmpty(serverSignKey)

        //当前安全措施所需的密钥是否完整，任何入口启动服务端前的统一守卫条件。
        //措施=0 无额外要求（界面已强制回环监听）；1 需要签名密钥，2 需要RSA私钥，3 需要SM4密钥
        fun isServerSecretsConfigured(): Boolean = when (safetyMeasures) {
            1 -> isSignKeyConfigured()
            2 -> !TextUtils.isEmpty(serverPrivateKey)
            3 -> !TextUtils.isEmpty(serverSm4Key)
            else -> true
        }

        //客户端密钥是否已配置，连接前必须填写与服务端一致的密钥
        fun isClientSignKeyConfigured(): Boolean = !TextUtils.isEmpty(clientSignKey)

        //校验签名
        @Throws(HttpException::class)
        fun checkSign(req: BaseRequest<*>) {
            //密钥缺失时直接拒绝，避免"配置了校验签名但没有密钥"时放行所有请求
            if (!isSignKeyConfigured()) throw HttpException(500, getString(R.string.sign_key_required))

            val signSecret = serverSignKey
            if (TextUtils.isEmpty(req.sign)) throw HttpException(HTTP_UNAUTHORIZED_CODE, getString(R.string.sign_required))
            if (req.timestamp == 0L) throw HttpException(HTTP_UNAUTHORIZED_CODE, getString(R.string.timestamp_required))

            val timestamp = System.currentTimeMillis()
            val diffTime = kotlin.math.abs(timestamp - req.timestamp)
            val tolerance = timeTolerance * 1000L
            if (diffTime > tolerance) {
                throw HttpException(HTTP_UNAUTHORIZED_CODE, String.format(getString(R.string.timestamp_verify_failed), timestamp, timeTolerance, diffTime))
            }

            val sign = calcSign(req.timestamp.toString(), signSecret)
            if (sign != req.sign) {
                Log.e("calcSign", sign)
                Log.e("reqSign", req.sign.toString())
                throw HttpException(HTTP_UNAUTHORIZED_CODE, getString(R.string.sign_verify_failed))
            }
        }

        //判断版本是否一致
        @Throws(HttpException::class)
        fun compareVersion(cloneInfo: CloneInfo) {
            val versionCode = cloneInfo.versionCode
            if (versionCode == 0) throw HttpException(500, getString(R.string.version_code_required))

            val requestVersion = versionCode.toString().substring(1).toInt()
            val localVersion = AppUtils.getAppVersionCode().toString().substring(1).toInt()
            val compatibleRange = VERSION_COMPAT_MAP[localVersion]
            Log.d("HttpServerUtils", "compareVersion: localVersion=$localVersion, requestVersion=$requestVersion, compatibleRange=$compatibleRange")
            val isCompatible = if (compatibleRange != null) {
                requestVersion in compatibleRange
            } else {
                requestVersion == localVersion
            }
            if (!isCompatible) {
                throw HttpException(500, getString(R.string.inconsistent_version))
            }
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
                        if (rule.title.isNullOrEmpty()) rule.title = "" //兼容旧版本
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

        //返回统一结构报文，failureCode 允许异常解析器带上 401/403 等业务之外的错误码
        fun response(output: Any?, failureCode: Int = HTTP_FAILURE_CODE): String {
            val resp: MutableMap<String, Any> = mutableMapOf()
            val timestamp = System.currentTimeMillis()
            resp["timestamp"] = timestamp
            if (output is String && output != "success") {
                resp["code"] = failureCode
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