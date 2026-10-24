package cn.ppps.forwarder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import cn.ppps.forwarder.utils.HTTP_SERVER_TIME_OUT
import cn.ppps.forwarder.utils.HttpServerUtils
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SettingUtils
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.xuexiang.xutil.net.NetworkUtils
import java.net.InetAddress
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName")
class HttpServerService : Service(), Server.ServerListener {

    private val TAG: String = HttpServerService::class.java.simpleName

    //是否已成功 startup，用于避免未启动时在 onDestroy 触发 server 的惰性初始化
    private var serverStarted: Boolean = false

    private val server by lazy {
        AndServer.webServer(this)
            .inetAddress(resolveListenAddress())
            .port(HttpServerUtils.serverPort)
            .listener(this)
            .timeout(HTTP_SERVER_TIME_OUT, TimeUnit.SECONDS)
            .build()
    }

    private fun resolveListenAddress(): InetAddress {
        if (HttpServerUtils.serverLocalIpOnly) return InetAddress.getByName("127.0.0.1")

        return try {
            NetworkUtils.getLocalInetAddress()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get local IP address: ${e.message}")
            InetAddress.getByName("127.0.0.1")
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        //统一守卫：界面开关、开机自启、定时任务等任何入口都必须满足，
        //措施要求的密钥缺失时直接不监听端口，避免服务在无法鉴权的状态下对外提供能力
        if (!HttpServerUtils.isServerSecretsConfigured()) {
            Log.e(TAG, "安全密钥未配置(safetyMeasures=${HttpServerUtils.safetyMeasures})，拒绝启动HTTP服务")
            stopSelf()
            return
        }

        Log.i(TAG, "onCreate: ")
        server.startup()
        serverStarted = true
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
        //未成功启动过就不要触碰 server，否则会先惰性构造再关闭
        if (serverStarted) {
            server.shutdown()
            serverStarted = false
        }
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