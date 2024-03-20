@file:Suppress("DEPRECATION")

package com.idormy.sms.forwarder.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idormy.sms.forwarder.utils.DELAY_TIME_AFTER_SIM_READY
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_NETWORK
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.idormy.sms.forwarder.workers.NetworkWorker
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName", "DEPRECATION", "UNUSED_PARAMETER")
class NetworkChangeReceiver : BroadcastReceiver() {

    private val TAG: String = NetworkChangeReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                handleConnectivityChange(context)
            }

            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                handleWifiStateChanged(context, intent)
            }

            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                handleNetworkStateChanged(context, intent)
            }

            //"android.intent.action.DATA_CONNECTION_STATE_CHANGED" -> {
            //    handleDataConnectionStateChanged(context, intent)
            //}
        }
    }

    private fun handleConnectivityChange(context: Context) {
        val networkStateOld = TaskUtils.networkState
        val dataSimSlotOld = TaskUtils.dataSimSlot
        val wifiSsidOld = TaskUtils.wifiSsid
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            Log.d(TAG, "Network Connected")
            if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                //移动网络
                TaskUtils.networkState = 1
                //获取当前使用的 SIM index
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    TaskUtils.dataSimSlot = getSlotIndex(context) + 1
                }
                TaskUtils.wifiSsid = ""
            } else if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                //WiFi网络
                TaskUtils.networkState = 2
            }
        } else {
            Log.d(TAG, "Network Disconnected")
            TaskUtils.networkState = 0
            TaskUtils.dataSimSlot = 0
            TaskUtils.wifiSsid = ""
        }

        //网络状态未改变，不执行任务，避免重复通知
        if (networkStateOld == TaskUtils.networkState && dataSimSlotOld == TaskUtils.dataSimSlot && wifiSsidOld == TaskUtils.wifiSsid) {
            Log.d(TAG, "Network State Not Changed")
            return
        }

        //【注意】延迟5秒（给够搜索信号时间）才执行任务
        val request = OneTimeWorkRequestBuilder<NetworkWorker>()
            .setInitialDelay(DELAY_TIME_AFTER_SIM_READY, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    TaskWorker.CONDITION_TYPE to TASK_CONDITION_NETWORK,
                )
            ).build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun handleWifiStateChanged(context: Context, intent: Intent) {
        val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        Log.d(TAG, "WiFi State Changed: $wifiState")

        when (wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> {
                Log.d(TAG, "WiFi Enabled")
            }

            WifiManager.WIFI_STATE_DISABLED -> {
                Log.d(TAG, "WiFi Disabled")
            }
        }
    }

    private fun handleNetworkStateChanged(context: Context, intent: Intent) {
        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
        if (networkInfo != null && networkInfo.isConnected) {
            Log.d(TAG, "Network State Changed: Connected")
        } else {
            Log.d(TAG, "Network State Changed: Disconnected")
        }
    }

    //private fun handleDataConnectionStateChanged(context: Context, intent: Intent) {
    //    val extraData = intent.extras
    //    val state = extraData?.getString("state")
    //    val reason = extraData?.getString("reason")
    //
    //    if (state != null && reason != null) {
    //        Log.d(TAG, "Data Connection State Changed: $state, Reason: $reason")
    //    }
    //}

    // 获取当前数据连接的卡槽ID，不需要判断手机数据流量是否打开（上层已判断）
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getSlotIndex(context: Context): Int {
        return try {
            val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } else {
                getDataSubId(context)
            }
            SubscriptionManager.getSlotIndex(subscriptionId)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "getSlotIndex: $e")
            -1
        }
    }

    // 获取数据连接的订阅ID
    @SuppressLint("DiscouragedPrivateApi")
    private fun getDataSubId(context: Context): Int {
        val defaultDataSlotId = getDefaultDataSlotId(context)

        return try {
            val obj = Class.forName("android.telephony.SubscriptionManager")
                .getDeclaredMethod("getSubId", Int::class.javaPrimitiveType)
                .invoke(null, defaultDataSlotId)
            obj?.let {
                when (Build.VERSION.SDK_INT) {
                    Build.VERSION_CODES.LOLLIPOP -> (it as? LongArray)?.get(0)?.toInt()
                    else -> (it as? IntArray)?.get(0)
                }
            } ?: defaultDataSlotId
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "getDataSubId: $e")
            defaultDataSlotId
        }
    }

    // 获取默认数据卡的卡槽ID
    private fun getDefaultDataSlotId(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionManager = SubscriptionManager.from(context.applicationContext)
            subscriptionManager?.let {
                try {
                    val subClass = Class.forName(it.javaClass.name)
                    val getSubID = subClass.getMethod("getDefaultDataSubscriptionInfo")
                    val subInfo = getSubID.invoke(it) as? SubscriptionInfo
                    return subInfo?.simSlotIndex ?: -1
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "getDefaultDataSlotId: $e")
                }
            }
        } else {
            try {
                val cls = Class.forName("android.telephony.SubscriptionManager")
                val methodName = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) "getSlotId" else "getSlotIndex"
                val getSubId = cls.getDeclaredMethod("getDefaultDataSubId") ?: cls.getDeclaredMethod("getDefaultDataSubscriptionId")
                val subId = getSubId.invoke(null) as? Int ?: return -1
                val getSlotId = cls.getDeclaredMethod(methodName, Int::class.javaPrimitiveType)
                return getSlotId.invoke(null, subId) as? Int ?: -1
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "getDefaultDataSlotId: $e")
            }
        }
        return -1
    }

}
