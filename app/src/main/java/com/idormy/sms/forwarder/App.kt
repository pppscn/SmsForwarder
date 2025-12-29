package com.idormy.sms.forwarder

import android.app.Application
import android.content.Context
import com.idormy.sms.forwarder.entity.SimInfo
import com.idormy.sms.forwarder.utils.Log
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

        SharedPreference.init(applicationContext)
        XUtil.init(this)
        XHttpSDK.init(this)
        Log.init(applicationContext)
    }
}
