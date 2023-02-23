package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.idormy.sms.forwarder.activity.SplashActivity

@Suppress("PropertyName")
class BootReceiver : BroadcastReceiver() {

    val TAG: String = BootReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent?) {
        val receiveAction: String? = intent?.action
        Log.d(TAG, "onReceive intent $receiveAction")
        if (receiveAction == "android.intent.action.BOOT_COMPLETED" || receiveAction == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            try {
                Log.d(TAG, "强制重启APP一次")
                val intent1 = Intent(context, SplashActivity::class.java)
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}