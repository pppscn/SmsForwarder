package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.SmsCommandUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xrouter.utils.TextUtils
import java.util.Date

//短信广播
@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class SmsReceiver : BroadcastReceiver() {

    private var TAG = SmsReceiver::class.java.simpleName
    private var from = ""
    private var msg = ""

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

            if (intent.action == Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION || intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
                val contentType = intent.type
                if (contentType == "application/vnd.wap.mms-message") {
                    val pduType = intent.getStringExtra("transactionId")
                    if ("mms" == pduType) {
                        val data = intent.getByteArrayExtra("data")
                        if (data != null) {
                            // 处理收到的 MMS 数据
                            handleMmsData(context, data)
                        }
                    }
                }

                from = intent.getStringExtra("address") ?: ""
                Log.d(TAG, "from = $from, msg = $msg")
            } else {
                for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    from = smsMessage.displayOriginatingAddress
                    msg += smsMessage.messageBody
                }
            }
            Log.d(TAG, "from = $from, msg = $msg")

            //短信指令
            if (SettingUtils.enableSmsCommand && msg.startsWith("smsf#")) {
                doSmsCommand(context, from, msg)
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

            val msgInfo = MsgInfo("sms", from, msg, Date(), simInfo, simSlot, subscription)
            Log.d(TAG, "msgInfo = $msgInfo")

            val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
                workDataOf(
                    Worker.SEND_MSG_INFO to Gson().toJson(msgInfo)
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

    private fun handleMmsData(context: Context, data: ByteArray) {
        try {
            val mmsClass = Class.forName("android.telephony.gsm.SmsMessage")
            val method = mmsClass.getDeclaredMethod("createFromPdu", ByteArray::class.java)
            val pdus = arrayOf(data)
            val messages = mutableListOf<Any>()

            for (pdu in pdus) {
                val message = method.invoke(null, pdu)
                message?.let { messages.add(it) }
            }

            // 处理 MMS 中的各个部分
            for (message in messages) {
                // 获取 MMS 的各个部分
                val parts = message.javaClass.getMethod("getParts").invoke(message) as? Array<*>

                // 遍历 MMS 的各个部分
                parts?.forEach { part ->
                    // 获取部分的内容类型
                    val contentType = part?.javaClass?.getMethod("getContentType")?.invoke(part) as? String

                    // 处理文本部分
                    if (contentType?.startsWith("text/plain") == true) {
                        val text = part.javaClass.getMethod("getData").invoke(part) as? String
                        // 处理文本信息
                        if (text != null) {
                            Log.d(TAG, "Text: $text")
                            msg += text
                        }
                    }

                    // 处理图像部分
                    if (contentType?.startsWith("image/") == true) {
                        val imageData = part.javaClass.getMethod("getData").invoke(part) as? ByteArray
                        // 处理图像信息
                        if (imageData != null) {
                            // 在这里你可以保存图像数据或进行其他处理
                            Log.d(TAG, "Image data received")
                        }
                    }

                    // 其他部分的处理可以根据需要继续扩展
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "handleMmsData: $e")
        }
    }

}