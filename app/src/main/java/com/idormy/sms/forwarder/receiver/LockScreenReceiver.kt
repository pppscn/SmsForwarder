package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.idormy.sms.forwarder.utils.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LOCK_SCREEN
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.idormy.sms.forwarder.workers.LockScreenWorker

@Suppress("PrivatePropertyName")
class LockScreenReceiver : BroadcastReceiver() {

    private val TAG: String = LockScreenReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || (intent?.action != Intent.ACTION_SCREEN_OFF && intent?.action != Intent.ACTION_SCREEN_ON)) return

        Log.d(TAG, "onReceive: ${intent.action}")
        TaskUtils.lockScreenAction = intent.action.toString()
        val request = OneTimeWorkRequestBuilder<LockScreenWorker>().setInputData(
            workDataOf(
                TaskWorker.conditionType to TASK_CONDITION_LOCK_SCREEN,
                TaskWorker.action to intent.action,
            )
        ).build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
