package cn.ppps.forwarder.utils

import android.text.TextUtils
import android.util.Log as AndroidLog

/**
 * 服务端安全默认值迁移
 *
 * 历史版本的服务端处于 fail-open 状态：签名密钥为空时安全措施为 0，请求不做任何身份验证，
 * 且远程接口默认全部开启。这里在应用启动时做一次性迁移，把默认状态改为 fail-closed。
 * 迁移只处理"用户没有显式配置过"或"沿用旧默认值"的情况，不覆盖用户显式做出的选择。
 */
object SecurityDefaults {

    //迁移版本，新增迁移项时自增，并同步 Constants.SP_SECURITY_DEFAULTS_VERSION 注释
    private const val CURRENT_VERSION = 1

    private const val SAFETY_MEASURES_SIGN = 1

    fun migrate() {
        if (readVersion() >= CURRENT_VERSION) return

        //1. 签名密钥为空时自动生成，避免"校验签名"模式下缺失密钥
        if (TextUtils.isEmpty(HttpServerUtils.serverSignKey)) {
            HttpServerUtils.serverSignKey = HttpServerUtils.generateServerSignKey()
            AndroidLog.i(TAG, "迁移：已自动生成服务端签名密钥")
        }

        //2. 用户没有显式设置过安全措施时，默认启用"校验签名"
        if (!SharedPreference.contains(SP_SERVER_SAFETY_MEASURES)) {
            HttpServerUtils.safetyMeasures = SAFETY_MEASURES_SIGN
            AndroidLog.i(TAG, "迁移：服务端安全措施默认设为校验签名")
        }

        //3. 老版本默认开启的远程接口全部关闭，避免升级后仍处于全开状态
        disableApiIfEnabled(SP_ENABLE_API_CLONE) { HttpServerUtils.enableApiClone = false }
        disableApiIfEnabled(SP_ENABLE_API_SMS_SEND) { HttpServerUtils.enableApiSmsSend = false }
        disableApiIfEnabled(SP_ENABLE_API_SMS_QUERY) { HttpServerUtils.enableApiSmsQuery = false }
        disableApiIfEnabled(SP_ENABLE_API_CALL_QUERY) { HttpServerUtils.enableApiCallQuery = false }
        disableApiIfEnabled(SP_ENABLE_API_CONTACT_QUERY) { HttpServerUtils.enableApiContactQuery = false }
        disableApiIfEnabled(SP_ENABLE_API_CONTACT_ADD) { HttpServerUtils.enableApiContactAdd = false }
        disableApiIfEnabled(SP_ENABLE_API_BATTERY_QUERY) { HttpServerUtils.enableApiBatteryQuery = false }
        disableApiIfEnabled(SP_ENABLE_API_WOL) { HttpServerUtils.enableApiWol = false }

        //4. 仅监听本机回环地址，避免未认证服务暴露到网络
        if (!SharedPreference.contains(SP_SERVER_LOCAL_IP_ONLY)) {
            HttpServerUtils.serverLocalIpOnly = true
        }

        writeVersion(CURRENT_VERSION)
    }

    /**
     * 只处理老版本遗留的"开启"状态，已关闭或从未设置过的保持不变
     */
    private fun disableApiIfEnabled(key: String, disable: () -> Unit) {
        if (!SharedPreference.preference.getBoolean(key, false)) return
        disable()
        AndroidLog.i(TAG, "迁移：已关闭远程接口 $key")
    }

    private fun readVersion(): Int = SharedPreference.preference.getInt(SP_SECURITY_DEFAULTS_VERSION, 0)

    private fun writeVersion(version: Int) {
        SharedPreference.preference.edit().putInt(SP_SECURITY_DEFAULTS_VERSION, version).apply()
    }

    private const val TAG: String = "SecurityDefaults"
}
