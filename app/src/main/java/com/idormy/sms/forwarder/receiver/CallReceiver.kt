package com.idormy.sms.forwarder.receiver

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.App.Companion.CALL_TYPE_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.Date

open class CallReceiver : PhoneStateReceiver() {

    companion object {
        private val TAG = CallReceiver::class.java.simpleName

        //const val ACTION_IN = "android.intent.action.PHONE_STATE"
        const val ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL"
        const val EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER"
    }

    //来电提醒
    override fun onIncomingCallReceived(context: Context, number: String?, start: Date) {
        Log.d(TAG, "onIncomingCallReceived：$number")
        sendNotice(context, 4, number)
    }

    //来电接通
    override fun onIncomingCallAnswered(context: Context, number: String?, start: Date) {
        Log.d(TAG, "onIncomingCallAnswered：$number")
        sendNotice(context, 5, number)
    }

    //来电挂机
    override fun onIncomingCallEnded(context: Context, number: String?, start: Date, end: Date) {
        Log.d(TAG, "onIncomingCallEnded：$number")
        sendCallMsg(context, 1, number)
    }

    //去电拨出
    override fun onOutgoingCallStarted(context: Context, number: String?, start: Date) {
        Log.d(TAG, "onOutgoingCallStarted：$number")
        sendNotice(context, 6, number)
    }

    //去电挂机
    override fun onOutgoingCallEnded(context: Context, number: String?, start: Date, end: Date) {
        Log.d(TAG, "onOutgoingCallEnded：$number")
        sendCallMsg(context, 2, number)
    }

    //未接来电
    override fun onMissedCall(context: Context, number: String?, start: Date) {
        Log.d(TAG, "onMissedCall：$number")
        sendCallMsg(context, 3, number)
    }

    //转发通话提醒
    private fun sendNotice(context: Context, callType: Int, phoneNumber: String?) {
        if (TextUtils.isEmpty(phoneNumber)) return

        //判断是否开启该类型转发
        if ((callType == 4 && !SettingUtils.enableCallType4) || (callType == 5 && !SettingUtils.enableCallType5) || (callType == 6 && !SettingUtils.enableCallType6)) {
            Log.w(TAG, "未开启该类型转发，type=$callType")
            return
        }

        val contacts = PhoneUtils.getContactByNumber(phoneNumber)
        val contactName = if (contacts.isNotEmpty()) contacts[0].name else getString(R.string.unknown_number)

        val msg = StringBuilder()
        msg.append(getString(R.string.contact)).append(contactName).append("\n")
        msg.append(getString(R.string.mandatory_type))
        msg.append(CALL_TYPE_MAP[callType.toString()] ?: getString(R.string.unknown_call))

        val msgInfo = MsgInfo("call", phoneNumber.toString(), msg.toString(), Date(), "", -1, 0, callType)
        val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
            workDataOf(
                Worker.SEND_MSG_INFO to Gson().toJson(msgInfo)
            )
        ).build()
        WorkManager.getInstance(context).enqueue(request)
    }

    //转发通话记录
    private fun sendCallMsg(context: Context, callType: Int, phoneNumber: String?) {
        //必须休眠才能获取来电记录，否则可能获取到上一次通话的
        Thread.sleep(1000)

        //获取后一条通话记录
        Log.d(TAG, "callType = $callType, phoneNumber = $phoneNumber")
        val callInfo: CallInfo? = PhoneUtils.getLastCallInfo(callType, phoneNumber)
        Log.d(TAG, "callInfo = $callInfo")
        if (callInfo?.number == null) {
            Log.w(TAG, "查不到通话记录直接发通知")
            sendNotice(context, callType, phoneNumber)
            return
        }

        //判断是否开启该类型转发
        if ((callInfo.type == 1 && !SettingUtils.enableCallType1) || (callInfo.type == 2 && !SettingUtils.enableCallType2) || (callInfo.type == 3 && !SettingUtils.enableCallType3)) {
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

        val msgInfo = MsgInfo("call", callInfo.number, PhoneUtils.getCallMsg(callInfo), Date(), simInfo, simSlot, callInfo.subId, callType)
        val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
            workDataOf(
                Worker.SEND_MSG_INFO to Gson().toJson(msgInfo)
            )
        ).build()
        WorkManager.getInstance(context).enqueue(request)

    }

}