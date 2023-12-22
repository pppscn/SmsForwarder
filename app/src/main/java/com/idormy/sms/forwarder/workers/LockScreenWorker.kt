package com.idormy.sms.forwarder.workers

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.condition.LockScreenSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import java.util.Date
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName", "DEPRECATION")
class LockScreenWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = LockScreenWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        try {
            val conditionType = inputData.getInt(TaskWorker.conditionType, -1)
            val action = inputData.getString(TaskWorker.action)

            val taskList = Core.task.getByType(conditionType)
            for (task in taskList) {
                Log.d(TAG, "task = $task")

                // 根据任务信息执行相应操作
                val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
                if (conditionList.isEmpty()) {
                    Log.d(TAG, "TASK-${task.id}：conditionList is empty")
                    continue
                }
                val firstCondition = conditionList.firstOrNull()
                if (firstCondition == null) {
                    Log.d(TAG, "TASK-${task.id}：firstCondition is null")
                    continue
                }

                val lockScreenSetting = Gson().fromJson(firstCondition.setting, LockScreenSetting::class.java)
                if (lockScreenSetting == null) {
                    Log.d(TAG, "TASK-${task.id}：lockScreenSetting is null")
                    continue
                }

                if (action != lockScreenSetting.action) {
                    Log.d(TAG, "TASK-${task.id}：action is not match, action = $action, lockScreenSetting = $lockScreenSetting")
                    continue
                }

                //TODO：判断其他条件是否满足
                if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                    Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
                    continue
                }

                //TODO: 组装消息体 && 执行具体任务
                val msgInfo = MsgInfo("task", task.name, lockScreenSetting.description, Date(), task.description)
                val actionData = Data.Builder()
                    .putLong(TaskWorker.taskId, task.id)
                    .putString(TaskWorker.taskActions, task.actions)
                    .putString(TaskWorker.msgInfo, Gson().toJson(msgInfo))
                    .build()
                val duration = if (action == Intent.ACTION_SCREEN_ON) lockScreenSetting.timeAfterScreenOn else lockScreenSetting.timeAfterScreenOff
                val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>()
                    .setInitialDelay(duration.toLong(), TimeUnit.MINUTES)
                    .setInputData(actionData).build()
                WorkManager.getInstance().enqueue(actionRequest)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork error", e)
            return Result.failure()
        }
    }

}