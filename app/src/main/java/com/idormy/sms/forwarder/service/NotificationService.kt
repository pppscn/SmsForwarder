package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PACKAGE_NAME
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.SendWorker
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.display.ScreenUtils
import java.util.Date


@Suppress("PrivatePropertyName", "DEPRECATION")
class NotificationService : NotificationListenerService() {

    private val TAG: String = NotificationService::class.java.simpleName

    override fun onListenerConnected() {
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

    @SuppressLint("DiscouragedPrivateApi")
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //异常通知跳过
            val notification = sbn?.notification ?: return
            val extras = notification.extras ?: return

            //自动消除额外APP通知
            SettingUtils.cancelExtraAppNotify
                .takeIf { it.isNotEmpty() }
                ?.split("\n")
                ?.forEach { app ->
                    if (sbn.packageName == app.trim()) {
                        Log.d(TAG, "自动消除额外APP通知：$app")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cancelNotification(sbn.key)
                        } else {
                            cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                        }
                        return@forEach
                    }
                }


            //总开关
            if (!SettingUtils.enableAppNotify) return

            //仅锁屏状态转发APP通知
            if (SettingUtils.enableNotUserPresent && !ScreenUtils.isScreenLock()) return

            val from = sbn.packageName
            //自身通知跳过
            if (PACKAGE_NAME == sbn.packageName) return
            // 标题
            val title = extras["android.title"]?.toString() ?: ""
            // 通知内容
            var text = extras["android.text"]?.toString() ?: ""
            if (text.isEmpty() && notification.tickerText != null) {
                text = notification.tickerText.toString()
            }

            //不处理空消息（标题跟内容都为空）
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text)) return

            val msgInfo = MsgInfo("app", from, text, Date(), title, -1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "消息的UID====>" + sbn.uid)
                msgInfo.uid = sbn.uid
            }
            //TODO：自动消除通知（临时方案，重复查询换取准确性）
            if (SettingUtils.enableCancelAppNotify) {
                val ruleList: List<Rule> = Core.rule.getRuleList(msgInfo.type, 1, "SIM0")
                for (rule in ruleList) {
                    if (rule.checkMsg(msgInfo)) {
                        Log.d(TAG, "自动消除通知")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cancelNotification(sbn.key)
                        } else {
                            cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                        }
                        break
                    }
                }
            }

            val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
                workDataOf(
                    Worker.SEND_MSG_INFO to Gson().toJson(msgInfo),
                )
            ).build()
            WorkManager.getInstance(applicationContext).enqueue(request)

        } catch (e: Exception) {
            Log.e(TAG, "Parsing Notification failed: " + e.message.toString())
        }

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "Removed Package Name : ${sbn?.packageName}")
    }

}