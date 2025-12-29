package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import java.util.Date

class SmsReceiver : BroadcastReceiver() {
    private var TAG = SmsReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
                && intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION
            ) return

            var from = ""
            var msg = ""
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                from = smsMessage.displayOriginatingAddress ?: ""
                msg += smsMessage.messageBody ?: ""
            }
            
            val subscription = intent.extras?.getInt("subscription") ?: -1
            if (App.SimInfoList.isEmpty()) {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
            }
            
            var simInfo = ""
            var simSlot = -1
            val sim = App.SimInfoList[subscription]
            if (sim != null) {
                simSlot = sim.mSimSlotIndex
                simInfo = "SIM${simSlot + 1}"
                if (!sim.mNumber.isNullOrEmpty()) {
                    simInfo += "(${sim.mNumber})"
                }
            }

            val msgInfo = MsgInfo("sms", from, msg, Date(), simInfo, simSlot, subscription)
            Thread { SendUtils.sendMsg(msgInfo) }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Parsing SMS failed: " + e.message.toString())
        }
    }
}
