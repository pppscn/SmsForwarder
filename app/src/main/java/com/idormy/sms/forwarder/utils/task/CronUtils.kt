package com.idormy.sms.forwarder.utils.task

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.receiver.AlarmReceiver

@Suppress("unused")
class CronUtils {
    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context

        fun initialize(context: Context) {
            this.context = context.applicationContext
        }

        fun updateTaskAndScheduleAlarm(task: Task) {
            val oldTask = getOldTask(task.id) // 获取旧的任务信息
            cancelAlarm(oldTask) // 取消旧任务的定时器

            updateTaskInDatabase(task) // 更新任务信息（例如，更新数据库中的任务信息）
            scheduleAlarm(task) // 设置新的定时器
        }

        private fun cancelAlarm(task: Task?) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val requestCode = task?.id?.toInt() ?: -1

            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, alarmIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        private fun scheduleAlarm(task: Task) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.putExtra("task", task)
            val requestCode = task.id.toInt()
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
            //val now = Calendar.getInstance()
            val nextExecutionTime = task.nextExecTime.time

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP, nextExecutionTime, pendingIntent
            )
        }

        private fun getOldTask(taskId: Long): Task {
            // 实现获取旧任务信息的逻辑
            // 返回旧任务信息（Task对象）
            return Task()
        }

        private fun updateTaskInDatabase(task: Task) {
            // 实现更新数据库中任务信息的逻辑
        }
    }
}
