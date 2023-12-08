package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.Date

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class SimStateReceiver : BroadcastReceiver() {

    private var TAG = "SimStateReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        //SIM卡槽状态监控开关
        if (!SettingUtils.enableSimStateReceiver) return

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // 在这里处理开机启动时的逻辑
            // 例如：注册监听 SIM 变化
            registerSimStateListener(context)
        } else if (action == "android.intent.action.SIM_STATE_CHANGED") {
            // 处理 SIM 卡状态变化的逻辑
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // 获取当前 SIM 卡状态
            when (telephonyManager.simState) {
                TelephonyManager.SIM_STATE_ABSENT -> {
                    Log.d(TAG, "SIM 卡被移除")
                    // 处理 SIM 卡被移除的情况
                }

                TelephonyManager.SIM_STATE_READY -> {
                    Log.d(TAG, "SIM 卡已准备就绪，延迟2秒再获取信息")
                    Thread.sleep(2000)

                    // 获取 SIM 卡信息
                    App.SimInfoList = PhoneUtils.getSimMultiInfo()
                    Log.d(TAG, App.SimInfoList.toString())

                    val msg = StringBuilder()
                    App.SimInfoList.forEach {
                        msg.append("[SIM-").append(it.key + 1).append("]\n")
                        msg.append(getString(R.string.carrier_name)).append(": ").append(it.value.mCarrierName).append("\n")
                        //msg.append(getString(R.string.icc_id)).append(": ").append(it.value.mIccId).append("\n")
                        msg.append(getString(R.string.sim_slot_index)).append(": ").append(it.value.mSimSlotIndex).append("\n")
                        msg.append(getString(R.string.number)).append(": ").append(it.value.mNumber).append("\n")
                        msg.append(getString(R.string.country_iso)).append(": ").append(it.value.mCountryIso).append("\n")
                        msg.append(getString(R.string.subscription_id)).append(": ").append(it.value.mSubscriptionId).append("\n")
                    }

                    sendMessage(context, msg.toString().trimEnd())
                }

                TelephonyManager.SIM_STATE_CARD_IO_ERROR -> {
                    Log.d(TAG, "SIM 卡读取失败")
                }

                TelephonyManager.SIM_STATE_CARD_RESTRICTED -> {
                    Log.d(TAG, "SIM 卡受限")
                }

                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> {
                    Log.d(TAG, "SIM 卡网络锁定")
                }

                TelephonyManager.SIM_STATE_NOT_READY -> {
                    Log.d(TAG, "SIM 卡未准备好")
                }

                TelephonyManager.SIM_STATE_PERM_DISABLED -> {
                    Log.d(TAG, "SIM 卡被禁用")
                }

                TelephonyManager.SIM_STATE_PIN_REQUIRED -> {
                    Log.d(TAG, "SIM 卡需要 PIN 解锁")
                }

                TelephonyManager.SIM_STATE_PUK_REQUIRED -> {
                    Log.d(TAG, "SIM 卡需要 PUK 解锁")
                }

                TelephonyManager.SIM_STATE_UNKNOWN -> {
                    Log.d(TAG, "SIM 卡状态未知")
                }
            }
        }
    }

    private fun registerSimStateListener(context: Context) {
        // 在此处注册 SIM 变化监听器
        // 可以使用 TelephonyManager 或 SubscriptionManager 进行注册监听
    }

    //发送信息
    private fun sendMessage(context: Context, msg: String) {
        Log.i(TAG, msg)
        try {
            val msgInfo = MsgInfo("app", "66666666", msg, Date(), getString(R.string.sim_state_monitor), -1)
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
}