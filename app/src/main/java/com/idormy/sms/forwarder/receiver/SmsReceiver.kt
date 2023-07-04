package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.service.HttpService
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.file.FileUtils
import com.xuexiang.xutil.system.DeviceUtils
import frpclib.Frpclib
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.*

//短信广播
@OptIn(DelicateCoroutinesApi::class)
@Suppress("PrivatePropertyName", "DeferredResultUnused", "SENSELESS_COMPARISON", "DEPRECATION")
class SmsReceiver : BroadcastReceiver() {

    private var TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //过滤广播
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION && intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

            var from = ""
            var message = ""
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                from = smsMessage.displayOriginatingAddress
                message += smsMessage.messageBody
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
        val cmdList = smsCommand.split("#")
        Log.d(TAG, "smsCommand = $smsCommand, cmdList = $cmdList")
        if (cmdList.count() < 2) return

        val function = cmdList[0]
        val action = cmdList[1]
        val param = if (cmdList.count() > 2) cmdList[2] else ""
        when (function) {
            "frpc" -> {
                if (!FileUtils.isFileExists(context.filesDir?.absolutePath + "/libs/libgojni.so")) {
                    Log.d(TAG, "还未下载Frpc库")
                    return
                }

                if (TextUtils.isEmpty(param)) {
                    GlobalScope.async(Dispatchers.IO) {
                        val frpcList = AppDatabase.getInstance(App.context).frpcDao().getAutorun()

                        if (frpcList.isEmpty()) {
                            Log.d(TAG, "没有自启动的Frpc")
                            return@async
                        }

                        for (frpc in frpcList) {
                            if (action == "start") {
                                if (!Frpclib.isRunning(frpc.uid)) {
                                    val error = Frpclib.runContent(frpc.uid, frpc.config)
                                    if (!TextUtils.isEmpty(error)) {
                                        Log.e(TAG, error)
                                    }
                                }
                            } else if (action == "stop") {
                                if (Frpclib.isRunning(frpc.uid)) {
                                    Frpclib.close(frpc.uid)
                                }
                            }
                        }
                    }
                } else {
                    GlobalScope.async(Dispatchers.IO) {
                        val frpc = AppDatabase.getInstance(App.context).frpcDao().getOne(param)

                        if (frpc == null) {
                            Log.d(TAG, "没有找到指定的Frpc")
                            return@async
                        }

                        if (action == "start") {
                            if (!Frpclib.isRunning(frpc.uid)) {
                                val error = Frpclib.runContent(frpc.uid, frpc.config)
                                if (!TextUtils.isEmpty(error)) {
                                    Log.e(TAG, error)
                                }
                            }
                        } else if (action == "stop") {
                            if (Frpclib.isRunning(frpc.uid)) {
                                Frpclib.close(frpc.uid)
                            }
                        }
                    }
                }
            }
            "httpserver" -> {
                Intent(context, HttpService::class.java).also {
                    if (action == "start") {
                        context.startService(it)
                    } else if (action == "stop") {
                        context.stopService(it)
                    }
                }
            }
            "system" -> {
                //判断是否已root
                if (!DeviceUtils.isDeviceRooted()) return

                if (action == "reboot") {
                    DeviceUtils.reboot()
                } else if (action == "shutdown") {
                    DeviceUtils.shutdown()
                }
            }
            "wifi" -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                if (action == "on") {
                    wifiManager.isWifiEnabled = true
                } else if (action == "off") {
                    wifiManager.isWifiEnabled = false
                }
            }
        }
    }

}