package com.idormy.sms.forwarder.utils.task

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.receiver.AlarmReceiver

class AlarmUtils {
    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context

        fun initialize(context: Context) {
            this.context = context.applicationContext
        }

        fun cancelAlarm(task: Task?) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val requestCode = task?.id?.toInt() ?: -1

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                alarmIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        @SuppressLint("ScheduleExactAlarm")
        fun scheduleAlarm(task: Task) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val requestCode = task.id.toInt()
            alarmIntent.putExtra("TASK", task)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // TODO：设置闹钟，低电量模式下无法设置精确闹钟
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.nextExecTime.time,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    task.nextExecTime.time,
                    pendingIntent
                )
            }
        }

    }
}
