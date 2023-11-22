package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xutil.app.ServiceUtils
import com.xuexiang.xutil.net.NetworkUtils
import java.util.*

import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.utils.PhoneUtils
import java.lang.reflect.Method

@Suppress("DEPRECATION")
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

    // 接收网络状态更新的广播
    private val networkStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("DefaultLocale")
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action != ConnectivityManager.CONNECTIVITY_ACTION) return

            if (!SettingUtils.enableNetworkStateReceiver) return

            Log.i(TAG, "网络状态已经改变，延时2秒后获取信息")
            Thread.sleep(2000)

            val msg = StringBuilder()

            //枚举网络状态 NET_NO：没有网络 , NET_2G:2g网络 , NET_3G：3g网络, NET_4G：4g网络, NET_5G：5g网络, NET_WIFI：wifi, NET_ETHERNET：有线网络, NET_UNKNOWN：未知网络
            val netStateType = NetworkUtils.getNetStateType()
            Log.d(TAG, "netStateType: $netStateType")
            val netStateTypeName = when (netStateType) {
                NetworkUtils.NetState.NET_NO -> getString(R.string.no_network)
                NetworkUtils.NetState.NET_2G -> getString(R.string.net_2g)
                NetworkUtils.NetState.NET_3G -> getString(R.string.net_3g)
                NetworkUtils.NetState.NET_4G -> getString(R.string.net_4g)
                NetworkUtils.NetState.NET_5G -> getString(R.string.net_5g)
                NetworkUtils.NetState.NET_WIFI -> getString(R.string.net_wifi)
                NetworkUtils.NetState.NET_ETHERNET -> getString(R.string.net_ethernet)
                NetworkUtils.NetState.NET_UNKNOWN -> getString(R.string.net_unknown)
                else -> getString(R.string.net_unknown)
            }
            msg.append(getString(R.string.network_type)).append(": ").append(netStateTypeName).append("\n")

            if (netStateType == NetworkUtils.NetState.NET_2G || netStateType == NetworkUtils.NetState.NET_3G || netStateType == NetworkUtils.NetState.NET_4G || netStateType == NetworkUtils.NetState.NET_5G) {
                // 获取当前使用的 SIM index
                val simIndex = getSlotId(context)
                if (simIndex != -1) {
                    // 获取 SIM 卡信息
                    App.SimInfoList = PhoneUtils.getSimMultiInfo()
                    Log.d(TAG, App.SimInfoList.toString())
                    if (App.SimInfoList[simIndex]?.mCarrierName != null) {
                        //获取网络运营商名称：中国移动、中国联通、中国电信
                        msg.append(getString(R.string.carrier_name)).append(": ").append(App.SimInfoList[simIndex]?.mCarrierName).append("\n")
                    }
                    msg.append(getString(R.string.data_sim_index)).append(": SIM-").append(simIndex + 1).append("\n")
                }
            } else if (netStateType == NetworkUtils.NetState.NET_WIFI) {
                //获取当前连接的WiFi名称
                val wifiSSID = getWifiSSID(context)
                msg.append(getString(R.string.wifi_ssid)).append(": ").append(wifiSSID).append("\n")
            }

            //获取IP地址
            val ipList = CommonUtils.getIPAddresses().filter { !isLocalAddress(it) }
            if (ServiceUtils.isServiceRunning("com.idormy.sms.forwarder.service.HttpService")) {
                ipList.forEach {
                    msg.append(getString(R.string.host_address)).append(": ").append(it).append("\n")
                    val hostAddress = if (it.indexOf(':', 0, false) > 0) "[${CommonUtils.removeInterfaceFromIP(it)}]" else it
                    msg.append(getString(R.string.http_server)).append(": ").append("http://${hostAddress}:5000").append("\n")
                }
            } else {
                ipList.forEach {
                    msg.append(getString(R.string.host_address)).append(": ").append(it).append("\n")
                }
            }

            sendMessage(context, msg.toString().trimEnd())
        }
    }

    // 判断手机数据流量是否打开
    @Suppress("rawtypes", "unchecked")
    private fun isMobileDataOpen(context: Context): Boolean {
        return try {
            val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ownerClass = mConnectivityManager.javaClass
            val method = ownerClass.getMethod("getMobileDataEnabled")
            method.invoke(mConnectivityManager) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    // 获取当前数据连接的卡槽ID
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getSlotId(context: Context): Int {
        if (!isMobileDataOpen(context)) {
            return -1
        }
        var dataSubId = 0
        try {
            dataSubId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } else {
                getDataSubId(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return SubscriptionManager.getSlotIndex(dataSubId)
    }

    // 获取数据连接的订阅ID
    @SuppressLint("DiscouragedPrivateApi")
    private fun getDataSubId(context: Context): Int {
        val defaultDataSlotId = getDefaultDataSlotId(context)
        try {
            val obj = Class.forName("android.telephony.SubscriptionManager")
                .getDeclaredMethod("getSubId", Int::class.javaPrimitiveType)
                .invoke(null, defaultDataSlotId)
            if (obj != null) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    return (obj as LongArray)[0].toInt()
                }
                return (obj as IntArray)[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultDataSlotId
    }

    // 获取默认数据卡的卡槽ID
    private fun getDefaultDataSlotId(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionManager = SubscriptionManager.from(context.applicationContext)
            if (subscriptionManager != null) {
                try {
                    val subClass = Class.forName(subscriptionManager.javaClass.name)
                    val getSubID = subClass.getMethod("getDefaultDataSubscriptionInfo")
                    val subInfo = getSubID.invoke(subscriptionManager) as SubscriptionInfo
                    return subInfo.simSlotIndex
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            try {
                val cls = Class.forName("android.telephony.SubscriptionManager")
                val getSubId: Method = try {
                    cls.getDeclaredMethod("getDefaultDataSubId")
                } catch (e: NoSuchMethodException) {
                    cls.getDeclaredMethod("getDefaultDataSubscriptionId")
                }
                val subId = getSubId.invoke(null) as Int
                val slotId: Int = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    val getSlotId = cls.getDeclaredMethod("getSlotId", Long::class.javaPrimitiveType)
                    getSlotId.invoke(null, subId.toLong()) as Int
                } else {
                    val getSlotId = cls.getDeclaredMethod("getSlotId", Int::class.javaPrimitiveType)
                    getSlotId.invoke(null, subId) as Int
                }
                return slotId
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1
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

    //获取当前连接的WiFi名称
    @SuppressLint("WifiManagerPotentialLeak")
    private fun getWifiSSID(context: Context): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo

        if (wifiInfo != null && wifiInfo.networkId != -1) {
            return wifiInfo.ssid.replace("\"", "")
        }

        return "Not connected to WiFi"
    }

    //检查IP地址是否为本地地址
    private fun isLocalAddress(ip: String): Boolean {
        return ip == "127.0.0.1" || ip == "::1" || ip.startsWith("fe80:") || ip.startsWith("fec0:")
    }

    companion object {
        private const val TAG = "NetworkStateReceiver"
    }
}