package com.idormy.sms.forwarder

import android.app.Application
import android.content.Context
import com.idormy.sms.forwarder.entity.SimInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.SharedPreference
import com.xuexiang.xhttp2.XHttpSDK
import com.xuexiang.xutil.XUtil

class App : Application() {

    companion object {
        const val TAG: String = "SmsForwarder"
        lateinit var context: Context
        var SimInfoList: MutableMap<Int, SimInfo> = mutableMapOf()
        val isDebug: Boolean = BuildConfig.DEBUG
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        // Initialize Log first so we can see what's happening
        Log.init(applicationContext)
        Log.i(TAG, "Application starting...")

        SharedPreference.init(applicationContext)
        XUtil.init(this)
        XHttpSDK.init(this)
        XHttpSDK.setBaseUrl(SettingUtils.WEBHOOK_BASE_URL)
        if (isDebug) {
            XHttpSDK.debug("XHttp")
        }

        Log.i(TAG, "Application started")
    }
}
