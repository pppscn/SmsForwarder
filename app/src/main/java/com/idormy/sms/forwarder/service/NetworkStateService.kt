package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xutil.app.ServiceUtils
import com.xuexiang.xutil.net.NetworkUtils
import java.util.*

@Suppress("DEPRECATION", "DeferredResultUnused")
class NetworkStateService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate--------------")

        //纯客户端模式
        //if (SettingUtils.enablePureClientMode) return

        val networkStateFilter = IntentFilter()
        networkStateFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkStateReceiver, networkStateFilter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand--------------")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy--------------")
        super.onDestroy()

        //纯客户端模式
        //if (SettingUtils.enablePureClientMode) return

        unregisterReceiver(networkStateReceiver)
    }

    // 接收电池信息更新的广播
    private val networkStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action != ConnectivityManager.CONNECTIVITY_ACTION) return

            if (!SettingUtils.enableNetworkStateReceiver) return

            Log.i(TAG, "网络状态已经改变")

            val msg = StringBuilder()

            //枚举网络状态 NET_NO：没有网络 , NET_2G:2g网络 , NET_3G：3g网络, NET_4G：4g网络, NET_5G：5g网络, NET_WIFI：wifi, NET_ETHERNET：有线网络, NET_UNKNOWN：未知网络
            val netStateType = NetworkUtils.getNetStateType()
            Log.d(TAG, "netStateType: $netStateType")
            val netStateTypeName = when (netStateType) {
                NetworkUtils.NetState.NET_NO -> "没有网络"
                NetworkUtils.NetState.NET_2G -> "2G网络"
                NetworkUtils.NetState.NET_3G -> "3G网络"
                NetworkUtils.NetState.NET_4G -> "4G网络"
                NetworkUtils.NetState.NET_5G -> "5G网络"
                NetworkUtils.NetState.NET_WIFI -> "Wifi"
                NetworkUtils.NetState.NET_ETHERNET -> "有线网络"
                NetworkUtils.NetState.NET_UNKNOWN -> "未知网络"
                else -> "未知网络"
            }
            msg.append(getString(R.string.network_type)).append(": ").append(netStateTypeName).append("\n")

            //获取网络运营商名称：中国移动、中国联通、中国电信
            if (netStateType == NetworkUtils.NetState.NET_2G || netStateType == NetworkUtils.NetState.NET_3G || netStateType == NetworkUtils.NetState.NET_4G || netStateType == NetworkUtils.NetState.NET_5G) {
                val operatorName = NetworkUtils.getNetworkOperatorName()
                msg.append(getString(R.string.operator_name)).append(": ").append(operatorName).append("\n")
            }

            val inetAddress = NetworkUtils.getLocalInetAddress()
            var hostAddress: String = inetAddress?.hostAddress?.toString() ?: "127.0.0.1"
            msg.append(getString(R.string.host_address)).append(": ").append(hostAddress).append("\n")

            if (ServiceUtils.isServiceRunning("com.idormy.sms.forwarder.service.HttpService")) {
                hostAddress = if (hostAddress.indexOf(':', 0, false) > 0) "[${hostAddress}]" else hostAddress
                msg.append(getString(R.string.http_server)).append(": ").append("http://${hostAddress}:5000")
            }

            sendMessage(context, msg.toString())
        }
    }

    //发送信息
    private fun sendMessage(context: Context, msg: String) {
        Log.i(TAG, msg)
        try {
            val msgInfo = MsgInfo("app", "77777777", msg, Date(), getString(R.string.network_state_monitor), -1)
            val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
                workDataOf(
                    Worker.sendMsgInfo to Gson().toJson(msgInfo),
                )
            ).build()
            WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "getLog e:" + e.message)
        }
    }

    companion object {
        private const val TAG = "NetworkStateReceiver"
    }
}