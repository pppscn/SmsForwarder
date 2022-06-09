package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import java.util.*

//短信广播
@Suppress("PrivatePropertyName", "DEPRECATION")
class SmsReceiver : BroadcastReceiver() {

    private var TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //总开关
            if (!SettingUtils.enableSms) return

            //过滤广播
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION && intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

            //权限判断
            //if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) return

            var from = ""
            var content = ""
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                from = smsMessage.displayOriginatingAddress
                content += smsMessage.messageBody
            }
            Log.d(TAG, "from = $from")
            Log.d(TAG, "content = $content")

            //获取卡槽信息
            if (App.SimInfoList.isEmpty()) {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
            }
            Log.e(TAG, "SimInfoList = " + App.SimInfoList.toString())

            //TODO：准确获取卡槽信息，目前测试结果只有 subscription 相对靠谱
            val slot = intent.extras?.getInt("slot") ?: -1
            val simId = intent.extras?.getInt("simId") ?: slot
            val subscription = intent.extras?.getInt("subscription") ?: simId
            Log.d(TAG, "slot = $slot, simId = $simId, subscription = $subscription")

            //卡槽id：-1=获取失败、0=卡槽1、1=卡槽2
            var simSlot = -1
            if (App.SimInfoList.isNotEmpty()) {
                for (simInfo in App.SimInfoList.values) {
                    if (simInfo.mSubscriptionId == subscription) {
                        simSlot = simInfo.mSimSlotIndex
                        break
                    }
                }
            }
            //获取卡槽信息
            val simInfo = when (simSlot) {
                0 -> "SIM1_" + SettingUtils.extraSim1
                1 -> "SIM2_" + SettingUtils.extraSim2
                else -> ""
            }

            val msgInfo = MsgInfo("sms", from, content, Date(), simInfo, simSlot)
            Log.d(TAG, "msgInfo = $msgInfo")

            val request = OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(
                    workDataOf(
                        Worker.sendMsgInfo to Gson().toJson(msgInfo)
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)

        } catch (e: Exception) {
            Log.e(TAG, "Parsing SMS failed: " + e.message.toString())
        }
    }

}