package com.idormy.sms.forwarder.receiver

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LOCK_SCREEN
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.idormy.sms.forwarder.workers.LockScreenWorker

@Suppress("PrivatePropertyName")
class LockScreenReceiver : BroadcastReceiver() {

    private val TAG: String = LockScreenReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || (intent?.action != Intent.ACTION_SCREEN_OFF && intent?.action != Intent.ACTION_SCREEN_ON && intent?.action != Intent.ACTION_USER_PRESENT)) return

        var action = intent.action.toString()
        if (action == Intent.ACTION_SCREEN_OFF && isDeviceLocked(context)) {
            action += "_LOCKED"
        }

        Log.d(TAG, "onReceive: action=$action")
        TaskUtils.lockScreenAction = action
        val request = OneTimeWorkRequestBuilder<LockScreenWorker>().setInputData(
            workDataOf(
                TaskWorker.CONDITION_TYPE to TASK_CONDITION_LOCK_SCREEN,
                TaskWorker.ACTION to action,
            )
        ).build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun isDeviceLocked(context: Context?): Boolean {
        val keyguardManager = context?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager?.isDeviceLocked ?: false
        } else {
            // 对于较早版本的 Android，无法直接检查设备锁定状态
            false
        }
    }
}
