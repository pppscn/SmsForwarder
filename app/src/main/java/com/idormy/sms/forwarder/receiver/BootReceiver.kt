package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.idormy.sms.forwarder.activity.SplashActivity
import com.idormy.sms.forwarder.service.ForegroundService
import com.idormy.sms.forwarder.utils.SettingUtils

@Suppress("PropertyName")
class BootReceiver : BroadcastReceiver() {

    val TAG: String = BootReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent?) {
        val receiveAction: String? = intent?.action
        Log.d(TAG, "onReceive intent $receiveAction")
        if (receiveAction == "android.intent.action.BOOT_COMPLETED") {
            try {
                val i = Intent(context, SplashActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)

                //纯客户端模式
                if (SettingUtils.enablePureClientMode) return

                //前台服务
                val frontServiceIntent = Intent(context, ForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(frontServiceIntent)
                } else {
                    context.startService(frontServiceIntent)
                }

                /*InitUtils.init(context)
                //电池状态监听
                val batteryServiceIntent = Intent(context, BatteryService::class.java)
                context.startService(batteryServiceIntent)

                //后台播放无声音乐
                if (SettingUtils.getPlaySilenceMusic()) {
                    context.startService(Intent(context, MusicService::class.java))
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}