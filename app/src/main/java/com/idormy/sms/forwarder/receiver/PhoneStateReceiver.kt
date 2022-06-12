package com.idormy.sms.forwarder.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.MMKVUtils
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.*

@Suppress("DEPRECATION")
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //总开关
            if (!SettingUtils.enablePhone) return

            //过滤广播
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED != intent.action) return

            //权限判断
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return

            //获取来电号码
            /*val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Core.telephonyManager.callStateForSubscription
            } else {
                Core.telephonyManager.callState
            }
            Log.d(TAG, "来电信息：state=$state phoneNumber = $phoneNumber")
            if (TextUtils.isEmpty(phoneNumber)) return

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {}
                TelephonyManager.CALL_STATE_IDLE -> {
                    *//*(Core.app as App).applicationScope.launch {
                        delay(500L) //延时0.5秒获取通话记录
                        sendReceiveCallMsg(context, phoneNumber)
                    }*//*
                    sendReceiveCallMsg(context, phoneNumber)
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {}
            }*/

            val number = intent.extras!!.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
            val stateStr = intent.extras!!.getString(TelephonyManager.EXTRA_STATE)
            var state = 0
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_IDLE -> state = TelephonyManager.CALL_STATE_IDLE
                TelephonyManager.EXTRA_STATE_OFFHOOK -> state = TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_RINGING -> state = TelephonyManager.CALL_STATE_RINGING
            }
            onCallStateChanged(context, state, number)

        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        val lastState = MMKVUtils.getInt("CALL_LAST_STATE", TelephonyManager.CALL_STATE_IDLE)
        if (lastState == state) {
            //No change, debounce extras
            return
        }

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "来电响铃")
                MMKVUtils.put("CALL_IS_INCOMING", true)
                //MMKVUtils.put("CALL_START_TIME", Date())
                MMKVUtils.put("CALL_SAVED_NUMBER", number)
            }
            TelephonyManager.CALL_STATE_OFFHOOK ->
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                when {
                    lastState != TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "去电接通")
                        MMKVUtils.put("CALL_IS_INCOMING", false)
                        //MMKVUtils.put("CALL_START_TIME", Date())
                    }
                    else -> {
                        Log.d(TAG, "来电接通")
                        MMKVUtils.put("CALL_IS_INCOMING", true)
                        //MMKVUtils.put("CALL_START_TIME", Date())
                    }
                }
            TelephonyManager.CALL_STATE_IDLE ->
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                when {
                    lastState == TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "来电未接")
                        sendReceiveCallMsg(context, 3, MMKVUtils.getString("CALL_SAVED_NUMBER", null))
                    }
                    MMKVUtils.getBoolean("CALL_IS_INCOMING", false) -> {
                        Log.d(TAG, "来电挂机")
                        sendReceiveCallMsg(context, 1, MMKVUtils.getString("CALL_SAVED_NUMBER", null))
                    }
                    else -> {
                        Log.d(TAG, "去电挂机")
                        sendReceiveCallMsg(context, 2, MMKVUtils.getString("CALL_SAVED_NUMBER", null))
                    }
                }
        }
        MMKVUtils.put("CALL_LAST_STATE", state)
    }

    private fun sendReceiveCallMsg(context: Context, callType: Int, phoneNumber: String?) {
        //必须休眠才能获取来电记录，否则可能获取到上一次通话的
        Thread.sleep(500)
        //获取后一条通话记录
        Log.d(TAG, "callType = $callType, phoneNumber = $phoneNumber")
        val callInfo: CallInfo? = PhoneUtils.getLastCallInfo(callType, phoneNumber)
        Log.d(TAG, "callInfo = $callInfo")
        if (callInfo?.number == null) return

        //判断是否开启该类型转发
        if ((callInfo.type == 1 && !SettingUtils.enableCallType1)
            || (callInfo.type == 2 && !SettingUtils.enableCallType2)
            || (callInfo.type == 3 && !SettingUtils.enableCallType3)
        ) {
            Log.w(TAG, "未开启该类型转发，type=" + callInfo.type)
            return
        }

        //卡槽id：-1=获取失败、0=卡槽1、1=卡槽2
        val simSlot = callInfo.simId
        //获取卡槽信息
        val simInfo = when (simSlot) {
            0 -> "SIM1_" + SettingUtils.extraSim1
            1 -> "SIM2_" + SettingUtils.extraSim2
            else -> ""
        }

        //获取联系人姓名
        if (TextUtils.isEmpty(callInfo.name)) {
            val contacts = PhoneUtils.getContactByNumber(phoneNumber)
            callInfo.name = if (contacts.isNotEmpty()) contacts[0].name else getString(R.string.unknown_number)
        }

        val msgInfo = MsgInfo("call", callInfo.number, PhoneUtils.getCallMsg(callInfo), Date(), simInfo, simSlot)
        val request = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(
                workDataOf(
                    Worker.sendMsgInfo to Gson().toJson(msgInfo)
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)

    }

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }
}