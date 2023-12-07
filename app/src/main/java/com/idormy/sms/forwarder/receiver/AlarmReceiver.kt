package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.entity.task.CronSetting
import com.idormy.sms.forwarder.entity.task.TaskSetting
import com.idormy.sms.forwarder.utils.task.AlarmUtils
import gatewayapps.crondroid.CronExpression
import java.util.Date

@Suppress("PropertyName", "DEPRECATION")
class AlarmReceiver : BroadcastReceiver() {

    val TAG: String = AlarmReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getParcelableExtra<Task>("TASK")
        if (task == null) {
            Log.d(TAG, "onReceive task is null")
            return
        }

        Log.d(TAG, "onReceive task $task")
        Log.d(TAG, "lastExecTime = ${task.lastExecTime}, nextExecTime = ${task.nextExecTime}")
        try {
            //取消旧任务的定时器
            AlarmUtils.cancelAlarm(task)

            // 根据任务信息执行相应操作
            val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
            if (conditionList.isEmpty()) {
                Log.d(TAG, "onReceive conditionList is empty")
                return
            }
            val firstCondition = conditionList.firstOrNull()
            if (firstCondition == null) {
                Log.d(TAG, "onReceive firstCondition is null")
                return
            }
            val cronSetting = Gson().fromJson(firstCondition.setting, CronSetting::class.java)
            if (cronSetting == null) {
                Log.d(TAG, "onReceive cronSetting is null")
                return
            }
            // 更新任务的上次执行时间和下次执行时间
            val cronExpression = CronExpression(cronSetting.expression)
            task.lastExecTime = Date()
            task.nextExecTime = cronExpression.getNextValidTimeAfter(task.lastExecTime)
            Log.d(TAG, "lastExecTime = ${task.lastExecTime}, nextExecTime = ${task.nextExecTime}")
            // 自动禁用任务
            if (task.nextExecTime <= task.lastExecTime) {
                task.status = 0
            }
            // 更新任务信息
            AppDatabase.getInstance(App.context).taskDao().update(task)
            if (task.status == 0) {
                Log.d(TAG, "onReceive task is disabled")
                return
            }
            //设置新的定时器
            AlarmUtils.scheduleAlarm(task)
        } catch (e: Exception) {
            Log.e(TAG, "onReceive error $e")
        }

    }
}
