package com.idormy.sms.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.condition.CronSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import com.idormy.sms.forwarder.utils.task.CronJobScheduler
import gatewayapps.crondroid.CronExpression
import java.util.Date

@Suppress("PrivatePropertyName", "DEPRECATION")
class CronWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = CronWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        try {
            val taskId = inputData.getLong(TaskWorker.TASK_ID, -1L)
            if (taskId == -1L) {
                Log.d(TAG, "taskId is -1L")
                return Result.failure()
            }

            val task = Core.task.getOne(taskId)
            if (task == null || task.status == 0) {
                Log.d(TAG, "TASK-$taskId：task is disabled")
                return Result.success()
            }

            // 根据任务信息执行相应操作
            val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
            if (conditionList.isEmpty()) {
                Log.d(TAG, "TASK-${task.id}：conditionList is empty")
                return Result.failure()
            }
            val firstCondition = conditionList.firstOrNull()
            if (firstCondition == null) {
                Log.d(TAG, "TASK-${task.id}：firstCondition is null")
                return Result.failure()
            }
            val cronSetting = Gson().fromJson(firstCondition.setting, CronSetting::class.java)
            if (cronSetting == null) {
                Log.d(TAG, "TASK-${task.id}：cronSetting is null")
                return Result.failure()
            }

            // TODO: 判断其他条件是否满足
            if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
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
            Log.d(TAG, "TASK-${task.id}：lastExecTime = ${task.lastExecTime}, nextExecTime = ${task.nextExecTime}")

            // 自动禁用任务
            if (task.nextExecTime.time / 1000 < now.time / 1000) {
                task.status = 0
            }

            // 更新任务信息
            Core.task.updateExecTime(task.id, task.lastExecTime, task.nextExecTime, task.status)

            if (task.status == 0) {
                Log.d(TAG, "TASK-${task.id}：task is disabled")
                return Result.success()
            }

            //TODO: 组装消息体 && 执行具体任务
            val msgInfo = MsgInfo("task", task.name, task.description, Date(), task.name)
            val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, task.id).putString(TaskWorker.TASK_ACTIONS, task.actions).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
            val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
            WorkManager.getInstance().enqueue(actionRequest)

            // 为新的 nextExecTime 调度下一次任务执行
            CronJobScheduler.cancelTask(task.id)
            CronJobScheduler.scheduleTask(task)
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork error", e)
            return Result.failure()
        }
    }

}
