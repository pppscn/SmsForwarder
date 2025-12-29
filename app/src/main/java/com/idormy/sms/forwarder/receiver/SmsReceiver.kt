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
import com.idormy.sms.forwarder.utils.SettingUtils
import java.util.Date

class SmsReceiver : BroadcastReceiver() {
    private var TAG = SmsReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=" + intent.action)
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
            Log.d(TAG, "Received SMS from: $from, content: $msg")
            
            val subscription = intent.extras?.getInt("subscription") ?: -1
            Log.d(TAG, "Subscription ID: $subscription")
            if (App.SimInfoList.isEmpty()) {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
            }
            
            var simInfo = ""
            var simSlot = -1
            val sim = App.SimInfoList[subscription]
            if (sim != null) {
                simSlot = sim.mSimSlotIndex
                val manualNumber = if (simSlot == 0) SettingUtils.sim1Number else SettingUtils.sim2Number
                simInfo = when {
                    !sim.mNumber.isNullOrEmpty() -> sim.mNumber!!
                    manualNumber.isNotEmpty() -> manualNumber
                    else -> "SIM${simSlot + 1}"
                }
            } else {
                Log.d(TAG, "SIM info not found for subscription ID: $subscription")
            }

            val otp = extractOtp(msg)
            val msgInfo = MsgInfo("sms", from, msg, Date(), simInfo, simSlot, subscription, otp)
            Log.d(TAG, "Starting thread to send message: $msgInfo")
            Thread { 
                try {
                    SendUtils.sendMsg(msgInfo) 
                } catch (e: Exception) {
                    Log.e(TAG, "Error in send thread: " + e.message, e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Parsing SMS failed: " + e.message.toString())
        }
    }

    private fun extractOtp(content: String): String? {
        // Try to find 4-8 digit OTP
        val digitOtpRegex = Regex("""\b\d{4,8}\b""")
        val digitMatch = digitOtpRegex.find(content)
        if (digitMatch != null) {
            return digitMatch.value
        }

        // Try to find alphanumeric OTP (4-8 chars, must contain at least one digit)
        val alphaNumericOtpRegex = Regex("""\b(?=.*[0-9])[a-zA-Z0-9]{4,8}\b""")
        val alphaNumericMatch = alphaNumericOtpRegex.find(content)
        if (alphaNumericMatch != null) {
            return alphaNumericMatch.value
        }

        return null
    }
}
