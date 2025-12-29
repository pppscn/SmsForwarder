package com.idormy.sms.forwarder.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.SimInfo

object PhoneUtils {

    @SuppressLint("Range")
    fun getSimMultiInfo(): MutableMap<Int, SimInfo> {
        val simInfoList = mutableMapOf<Int, SimInfo>()
        try {
            val subscriptionManager = App.context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
            if (activeSubscriptionInfoList != null) {
                for (subscriptionInfo in activeSubscriptionInfoList) {
                    val simInfo = SimInfo(
                        subscriptionInfo.subscriptionId,
                        subscriptionInfo.simSlotIndex,
                        subscriptionInfo.carrierName?.toString(),
                        subscriptionInfo.number
                    )
                    simInfoList[subscriptionInfo.subscriptionId] = simInfo
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneUtils", "getSimMultiInfo error: \${e.message}")
        }
        return simInfoList
    }

    fun sendSms(subId: Int, mobile: String, content: String): String? {
        return try {
            val smsManager = if (subId == -1) {
                SmsManager.getDefault()
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subId)
            }
            val sentIntent = PendingIntent.getBroadcast(App.context, 0, android.content.Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
            val deliveryIntent = PendingIntent.getBroadcast(App.context, 0, android.content.Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)

            val messages = smsManager.divideMessage(content)
            for (message in messages) {
                smsManager.sendTextMessage(mobile, null, message, sentIntent, deliveryIntent)
            }
            null
        } catch (e: Exception) {
            e.message
        }
    }
}
