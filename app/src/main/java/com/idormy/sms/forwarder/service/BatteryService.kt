package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.BatteryUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import java.util.*

@Suppress("DEPRECATION")
class BatteryService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate--------------")

        //是否同意隐私协议
        //if (!MyApplication.allowPrivacyPolicy) return

        val batteryFilter = IntentFilter()
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, batteryFilter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand--------------")
        return START_STICKY //
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy--------------")
        super.onDestroy()

        //是否同意隐私协议
        //if (!MyApplication.allowPrivacyPolicy) return
        unregisterReceiver(batteryReceiver)
    }

    // 接收电池信息更新的广播
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        override fun onReceive(context: Context, intent: Intent) {

            //电量发生变化
            val levelCur: Int = intent.getIntExtra("level", 0)
            val levelPre: Int = SettingUtils.batteryLevelCurrent
            if (levelCur != levelPre) {
                var msg: String = BatteryUtils.getBatteryInfo(intent).toString()
                SettingUtils.batteryLevelCurrent = levelCur
                val levelMin: Int = SettingUtils.batteryLevelMin
                val levelMax: Int = SettingUtils.batteryLevelMax
                if (SettingUtils.batteryLevelOnce && levelMin > 0 && levelPre > levelCur && levelCur <= levelMin) { //电量下降到下限
                    msg = "【电量预警】已低于电量预警下限，请及时充电！$msg"
                    sendMessage(context, msg)
                    return
                } else if (SettingUtils.batteryLevelOnce && levelMax > 0 && levelPre < levelCur && levelCur >= levelMax) { //电量上升到上限
                    msg = "【电量预警】已高于电量预警上限，请拔掉充电器！$msg"
                    sendMessage(context, msg)
                    return
                } else if (!SettingUtils.batteryLevelOnce && levelMin > 0 && levelPre > levelCur && levelCur == levelMin) { //电量下降到下限
                    msg = "【电量预警】已到达电量预警下限，请及时充电！$msg"
                    sendMessage(context, msg)
                    return
                } else if (!SettingUtils.batteryLevelOnce && levelMax > 0 && levelPre < levelCur && levelCur == levelMax) { //电量上升到上限
                    msg = "【电量预警】已到达电量预警上限，请拔掉充电器！$msg"
                    sendMessage(context, msg)
                    return
                }
            }

            //充电状态改变
            val status: Int = intent.getIntExtra("status", 0)
            if (SettingUtils.enableBatteryReceiver) {
                val oldStatus: Int = SettingUtils.batteryStatus
                if (status != oldStatus) {
                    var msg: String = BatteryUtils.getBatteryInfo(intent).toString()
                    SettingUtils.batteryStatus = status
                    msg = "【充电状态】发生变化：" + BatteryUtils.getStatus(oldStatus) + " → " + BatteryUtils.getStatus(status) + msg
                    sendMessage(context, msg)
                }
            }
        }
    }

    //发送信息
    private fun sendMessage(context: Context, msg: String) {
        Log.i(TAG, msg)
        try {
            val msgInfo = MsgInfo("app", "88888888", msg, Date(), "电池状态监听", -1)
            val request = OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(
                    workDataOf(
                        Worker.sendMsgInfo to Gson().toJson(msgInfo),
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "getLog e:" + e.message)
        }
    }

    companion object {
        private const val TAG = "BatteryReceiver"
    }
}