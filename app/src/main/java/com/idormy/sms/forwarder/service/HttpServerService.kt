package com.idormy.sms.forwarder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.HTTP_SERVER_PORT
import com.idormy.sms.forwarder.utils.HTTP_SERVER_TIME_OUT
import com.idormy.sms.forwarder.utils.SettingUtils
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName")
class HttpServerService : Service(), Server.ServerListener {

    private val TAG: String = HttpServerService::class.java.simpleName
    private val server by lazy {
        AndServer.webServer(this).port(HTTP_SERVER_PORT).listener(this).timeout(HTTP_SERVER_TIME_OUT, TimeUnit.SECONDS).build()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        Log.i(TAG, "onCreate: ")
        server.startup()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        Log.i(TAG, "onDestroy: ")
        server.shutdown()
    }

    override fun onException(e: Exception?) {
        Log.i(TAG, "onException: ")
    }

    override fun onStarted() {
        Log.i(TAG, "onStarted: ")
    }

    override fun onStopped() {
        Log.i(TAG, "onStopped: ")
    }
}