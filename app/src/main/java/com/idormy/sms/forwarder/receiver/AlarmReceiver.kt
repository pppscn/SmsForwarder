package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.idormy.sms.forwarder.database.entity.Task

@Suppress("PropertyName")
class AlarmReceiver : BroadcastReceiver() {

    val TAG: String = AlarmReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getParcelableExtra<Task>("task")

        // 根据任务信息执行相应操作
        if (task != null) {
            Log.d(TAG, "onReceive task $task")
            // 处理任务逻辑，例如执行特定操作或者更新界面
        }
    }
}
