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
import com.idormy.sms.forwarder.utils.SmsCommandUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xrouter.utils.TextUtils
import java.util.*

//短信广播
@Suppress("PrivatePropertyName")
class SmsReceiver : BroadcastReceiver() {

    private var TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //过滤广播
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
                && intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
                && intent.action != Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
                && intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION
            ) return

            var from = ""
            var message = ""
            if (intent.action == Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION || intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
                from = intent.getStringExtra("address") ?: ""
                message = intent.getStringExtra("body") ?: ""
                Log.d(TAG, "from = $from, message = $message")

                val bundle = intent.extras
                bundle?.let {
                    for (key in bundle.keySet()) {
                        val obj = bundle.get(key)
                        if (obj is ByteArray) {
                            val data = String(obj)
                            // 解析彩信内容
                            parseMMSContent(data, from)
                        }
                    }
                }
            } else {
                for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    from = smsMessage.displayOriginatingAddress
                    message += smsMessage.messageBody
                }
            }
            Log.d(TAG, "from = $from, message = $message")

            //短信指令
            if (SettingUtils.enableSmsCommand && message.startsWith("smsf#")) {
                doSmsCommand(context, from, message)
                return
            }

            //总开关
            if (!SettingUtils.enableSms) return

            //TODO：准确获取卡槽信息，目前测试结果只有 subscription 相对靠谱
            val slot = intent.extras?.getInt("slot") ?: -1
            val simId = intent.extras?.getInt("simId") ?: slot
            val subscription = intent.extras?.getInt("subscription") ?: simId
            Log.d(TAG, "slot = $slot, simId = $simId, subscription = $subscription")

            //卡槽id：-1=获取失败、0=卡槽1、1=卡槽2
            var simSlot = -1
            //以自定义卡槽信息优先
            if (SettingUtils.subidSim1 > 0 || SettingUtils.subidSim2 > 0) {
                simSlot = if (subscription == SettingUtils.subidSim1) 0 else 1
            } else {
                //获取卡槽信息
                if (App.SimInfoList.isEmpty()) {
                    App.SimInfoList = PhoneUtils.getSimMultiInfo()
                }
                Log.d(TAG, "SimInfoList = " + App.SimInfoList.toString())

                if (App.SimInfoList.isNotEmpty()) {
                    for (simInfo in App.SimInfoList.values) {
                        if (simInfo.mSubscriptionId == subscription) {
                            simSlot = simInfo.mSimSlotIndex
                            break
                        }
                    }
                }
            }

            //获取卡槽信息
            val simInfo = when (simSlot) {
                0 -> "SIM1_" + SettingUtils.extraSim1
                1 -> "SIM2_" + SettingUtils.extraSim2
                else -> ""
            }

            val msgInfo = MsgInfo("sms", from, message, Date(), simInfo, simSlot, subscription)
            Log.d(TAG, "msgInfo = $msgInfo")

            val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
                workDataOf(
                    Worker.sendMsgInfo to Gson().toJson(msgInfo)
                )
            ).build()
            WorkManager.getInstance(context).enqueue(request)

        } catch (e: Exception) {
            Log.e(TAG, "Parsing SMS failed: " + e.message.toString())
        }
    }

    //处理短信指令
    private fun doSmsCommand(context: Context, from: String, message: String) {
        var safePhone = SettingUtils.smsCommandSafePhone
        Log.d(TAG, "safePhone = $safePhone")

        if (!TextUtils.isEmpty(safePhone)) {
            var isSafePhone = false
            safePhone = safePhone.replace(";", ",").replace("；", ",").replace("，", ",").trim()
            for (phone in safePhone.split(",")) {
                if (!TextUtils.isEmpty(phone.trim()) && from.endsWith(phone.trim())) {
                    isSafePhone = true
                    break
                }
            }
            if (!isSafePhone) {
                Log.d(TAG, "from = $from is not safePhone = $safePhone")
                return
            }
        }

        val smsCommand = message.substring(5)
        SmsCommandUtils.execute(context, smsCommand)
    }

    private fun parseMMSContent(data: String, sender: String?) {
        // 在这里实现解析彩信内容的逻辑
        Log.d("MMSReceiver", "Received MMS from: $sender, Data: $data")

        val parts = data.split("\n\n") // 假设彩信内容以两个换行符 "\n\n" 分隔不同部分
        parts.forEach { part ->
            val lines = part.split("\n")
            var contentType: String? = null
            var content: String? = null
            var contentTransferEncoding: String? = null

            lines.forEach { line ->
                Log.d(TAG, "Line: $line")
                val keyValue = line.split(":")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim()
                    val value = keyValue[1].trim()

                    when (key.toLowerCase()) {
                        "content-type" -> contentType = value
                        "content-transfer-encoding" -> contentTransferEncoding = value
                    }
                }
            }

            // 处理不同 MIME 类型的内容
            contentType?.let {
                when {
                    it.startsWith("text") -> {
                        // 文本内容
                        content = lines.last() // 假设文本内容在当前部分的最后一行
                        // 处理文本内容，例如展示在 TextView 中
                        Log.d(TAG, "Text data: $content")
                    }

                    it.startsWith("image") -> {
                        // 图片内容
                        // 如果 contentTransferEncoding 表示 base64 编码
                        if (contentTransferEncoding.equals("base64", true)) {
                            content = lines.last() // 假设图片数据在当前部分的最后一行
                            // 解码图片数据并展示或保存图片
                            val decodedImage = android.util.Base64.decode(content, android.util.Base64.DEFAULT)
                            // 处理解码后的图片数据
                            Log.d(TAG, "Image data: $decodedImage")
                        }
                    }
                    // 可以根据其他 MIME 类型继续添加处理逻辑，比如视频、音频等
                }
            }
        }
    }

}