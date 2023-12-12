package com.idormy.sms.forwarder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.task.CronSetting
import com.idormy.sms.forwarder.entity.task.TaskSetting
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.utils.task.CronJobScheduler
import gatewayapps.crondroid.CronExpression
import java.util.Date

@Suppress("PrivatePropertyName", "DEPRECATION")
class CronWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = CronWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L)
        if (taskId == -1L) {
            Log.d(TAG, "taskId is -1L")
            return Result.failure()
        }

        val task = AppDatabase.getInstance(App.context).taskDao().getOne(taskId)
        if (task.status == 0) {
            Log.d(TAG, "任务${task.id}：task is disabled")
            return Result.success()
        }

        // 根据任务信息执行相应操作
        val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
        if (conditionList.isEmpty()) {
            Log.d(TAG, "任务${task.id}：conditionList is empty")
            return Result.failure()
        }
        val firstCondition = conditionList.firstOrNull()
        if (firstCondition == null) {
            Log.d(TAG, "任务${task.id}：firstCondition is null")
            return Result.failure()
        }
        val cronSetting = Gson().fromJson(firstCondition.setting, CronSetting::class.java)
        if (cronSetting == null) {
            Log.d(TAG, "任务${task.id}：cronSetting is null")
            return Result.failure()
        }

        // TODO: 判断其他条件是否满足
        if (false) {
            Log.d(TAG, "任务${task.id}：其他条件不满足")
            return Result.failure()
        }

        // 更新任务的上次执行时间和下次执行时间
        val now = Date()
        task.lastExecTime = task.nextExecTime
        val cronExpression = CronExpression(cronSetting.expression)
        val nextExecTime = cronExpression.getNextValidTimeAfter(now)
        // 将 nextExecTime 的毫秒部分设置为 0，避免因为毫秒部分不同导致的任务重复执行
        nextExecTime.time = nextExecTime.time / 1000 * 1000
        task.nextExecTime = nextExecTime
        Log.d(TAG, "任务${task.id}：lastExecTime = ${task.lastExecTime}, nextExecTime = ${task.nextExecTime}")

        // 自动禁用任务
        if (task.nextExecTime.time / 1000 < now.time / 1000) {
            task.status = 0
        }

        // 更新任务信息
        AppDatabase.getInstance(App.context).taskDao().updateExecTime(task.id, task.lastExecTime, task.nextExecTime, task.status)

        if (task.status == 0) {
            Log.d(TAG, "任务${task.id}：task is disabled")
            return Result.success()
        }

        //组装消息体
        val msgInfo = MsgInfo("task", task.name, task.description, Date(), task.name)

        // TODO: 执行具体任务
        val actionList = Gson().fromJson(task.actions, Array<TaskSetting>::class.java).toMutableList()
        if (actionList.isEmpty()) {
            Log.d(TAG, "任务${task.id}：actionsList is empty")
            return Result.failure()
        }
        for (action in actionList) {
            val actionData = Data.Builder()
                .putLong(Worker.taskId, task.id)
                .putInt(Worker.actionType, action.type)
                .putString(Worker.actionSetting, action.setting)
                .putString(Worker.sendMsgInfo, Gson().toJson(msgInfo))
                .build()
            val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
            WorkManager.getInstance().enqueue(actionRequest)
        }

        // 为新的 nextExecTime 调度下一次任务执行
        CronJobScheduler.cancelTask(task.id)
        CronJobScheduler.scheduleTask(task)
        return Result.success()
    }

}
