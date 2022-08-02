package com.idormy.sms.forwarder.service

import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.PACKAGE_NAME
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.display.ScreenUtils
import java.util.*


@Suppress("PrivatePropertyName", "DEPRECATION")
class NotifyService : NotificationListenerService()/*, LifecycleOwner*/ {

    /*private val mRegistry = LifecycleRegistry(this)*/
    private val TAG: String = "NotifyService"

    override fun onListenerConnected() {
        //super.onListenerConnected()
        // Check if Notification Listener Permission is allowed
        Log.d(TAG, "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        //总开关
        if (!SettingUtils.enableAppNotify) return

        Log.d(TAG, "通知侦听器断开连接 - 请求重新绑定")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, NotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //总开关
            if (!SettingUtils.enableAppNotify) return

            //异常通知跳过
            if (sbn!!.notification == null) return
            if (sbn.notification.extras == null) return

            //仅锁屏状态转发APP通知
            if (SettingUtils.enableNotUserPresent && !ScreenUtils.isScreenLock()) return

            val from = sbn.packageName
            //自身通知跳过
            if (PACKAGE_NAME == sbn.packageName) return

            //通知标题
            var title = ""
            if (sbn.notification.extras["android.title"] != null) {
                title = sbn.notification.extras["android.title"].toString()
            }
            //通知内容
            var text = ""
            if (sbn.notification.extras["android.text"] != null) {
                text = sbn.notification.extras["android.text"].toString()
            }
            if (text.isEmpty() && sbn.notification.tickerText != null) {
                text = sbn.notification.tickerText.toString()
            }

            //不处理空消息（标题跟内容都为空）
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text)) return

            val msgInfo = MsgInfo("app", from, text, Date(), title, -1)
            val request = OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(
                    workDataOf(
                        Worker.sendMsgInfo to Gson().toJson(msgInfo),
                        //Worker.sendSbnId to sbn.id
                    )
                )
                .build()
            WorkManager.getInstance(applicationContext).enqueue(request)

            //TODO：收不到返回信息，自动消除待解决
            /*WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id)
                .observe(this) { info: WorkInfo? ->
                    Log.e(TAG, info.toString())
                    if (info != null && info.state == WorkInfo.State.SUCCEEDED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cancelNotification(sbn.key)
                        } else {
                            cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                        }
                    }
                }*/
            //自动消除全部通知（临时方案）
            if (SettingUtils.enableCancelAppNotify) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cancelNotification(sbn.key)
                } else {
                    cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Parsing Notification failed: " + e.message.toString())
        }

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        //super.onNotificationRemoved(sbn)
        Log.d(TAG, "Removed Package Name : ${sbn?.packageName}")
    }

    /*override fun getLifecycle(): Lifecycle {
        return mRegistry
    }*/
}