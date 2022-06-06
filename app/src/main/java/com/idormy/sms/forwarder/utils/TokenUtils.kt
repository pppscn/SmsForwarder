package com.idormy.sms.forwarder.utils

import android.content.Context
import com.idormy.sms.forwarder.R
import com.umeng.analytics.MobclickAgent
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xutil.app.ActivityUtils
import com.xuexiang.xutil.common.StringUtils

/**
 * Token管理工具
 *
 * @author xuexiang
 * @since 2019-11-17 22:37
 */
@Suppress("unused")
class TokenUtils private constructor() {
    companion object {
        private var sToken: String? = null
        private const val KEY_TOKEN = "KEY_TOKEN"
        private const val KEY_PROFILE_CHANNEL = "github"

        /**
         * 初始化Token信息
         */
        @JvmStatic
        fun init(context: Context) {
            MMKVUtils.init(context)
            sToken = MMKVUtils.getString(KEY_TOKEN, "")
        }

        private fun clearToken() {
            sToken = null
            MMKVUtils.remove(KEY_TOKEN)
        }

        var token: String?
            get() = sToken
            set(token) {
                sToken = token
                MMKVUtils.put(KEY_TOKEN, token)
            }

        @JvmStatic
        fun hasToken(): Boolean {
            return MMKVUtils.containsKey(KEY_TOKEN)
        }

        /**
         * 处理登录成功的事件
         *
         * @param token 账户信息
         */
        @JvmStatic
        fun handleLoginSuccess(token: String?): Boolean {
            return if (!StringUtils.isEmpty(token)) {
                XToastUtils.success(ResUtils.getString(R.string.login_succeeded))
                MobclickAgent.onProfileSignIn(KEY_PROFILE_CHANNEL, token)
                Companion.token = token
                true
            } else {
                XToastUtils.error(ResUtils.getString(R.string.login_failed))
                false
            }
        }

        /**
         * 处理登出的事件
         */
        @JvmStatic
        fun handleLogoutSuccess() {
            MobclickAgent.onProfileSignOff()
            //登出时，清除账号信息
            clearToken()
            XToastUtils.success(ResUtils.getString(R.string.logout_succeeded))
            SettingUtils.isAgreePrivacy = false
            //跳转到登录页
            ActivityUtils.startActivity(com.idormy.sms.forwarder.activity.LoginActivity::class.java)
        }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}