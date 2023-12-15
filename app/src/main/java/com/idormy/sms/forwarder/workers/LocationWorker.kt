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
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.task.LocationSetting
import com.idormy.sms.forwarder.entity.task.TaskSetting
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LEAVE_ADDRESS
import com.idormy.sms.forwarder.utils.TASK_CONDITION_TO_ADDRESS
import com.idormy.sms.forwarder.utils.TaskWorker
import java.util.Date

@Suppress("PrivatePropertyName", "DEPRECATION")
class LocationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = LocationWorker::class.java.simpleName

    override suspend fun doWork(): Result {

        Log.d(TAG, "doWork")
        val locationInfoJsonOld = inputData.getString("locationInfoJsonOld")
        val locationInfoJsonNew = inputData.getString("locationInfoJsonNew")
        if (locationInfoJsonOld == null || locationInfoJsonNew == null) {
            Log.d(TAG, "locationInfoOld or locationInfoNew is null")
            return Result.failure()
        }

        val locationInfoOld = Gson().fromJson(locationInfoJsonOld, LocationInfo::class.java)
        val locationInfoNew = Gson().fromJson(locationInfoJsonNew, LocationInfo::class.java)
        if (locationInfoOld == null || locationInfoNew == null) {
            Log.d(TAG, "locationInfoOld or locationInfoNew is null")
            return Result.failure()
        }

        when (val conditionType = inputData.getInt(TaskWorker.conditionType, -1)) {

            //到达地点
            TASK_CONDITION_TO_ADDRESS -> {
                val taskList = AppDatabase.getInstance(App.context).taskDao().getByType(conditionType)
                for (task in taskList) {
                    Log.d(TAG, "task = $task")

                    // 根据任务信息执行相应操作
                    val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
                    if (conditionList.isEmpty()) {
                        Log.d(TAG, "任务${task.id}：conditionList is empty")
                        continue
                    }
                    val firstCondition = conditionList.firstOrNull()
                    if (firstCondition == null) {
                        Log.d(TAG, "任务${task.id}：firstCondition is null")
                        continue
                    }

                    val locationSetting = Gson().fromJson(firstCondition.setting, LocationSetting::class.java)
                    if (locationSetting == null) {
                        Log.d(TAG, "任务${task.id}：locationSetting is null")
                        continue
                    }

                    //TODO：判断条件是否满足

                    //TODO: 组装消息体 && 执行具体任务
                    val msg = locationInfoNew.toString()
                    val msgInfo = MsgInfo("task", task.name, msg, Date(), task.name)
                    val actionData = Data.Builder().putLong(TaskWorker.taskId, task.id).putString(TaskWorker.taskActions, task.actions).putString(TaskWorker.msgInfo, Gson().toJson(msgInfo)).build()
                    val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                    WorkManager.getInstance().enqueue(actionRequest)
                }

                return Result.success()
            }

            //离开地点
            TASK_CONDITION_LEAVE_ADDRESS -> {
                val taskList = AppDatabase.getInstance(App.context).taskDao().getByType(conditionType)
                for (task in taskList) {
                    Log.d(TAG, "task = $task")

                    // 根据任务信息执行相应操作
                    val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
                    if (conditionList.isEmpty()) {
                        Log.d(TAG, "任务${task.id}：conditionList is empty")
                        continue
                    }
                    val firstCondition = conditionList.firstOrNull()
                    if (firstCondition == null) {
                        Log.d(TAG, "任务${task.id}：firstCondition is null")
                        continue
                    }

                    val locationSetting = Gson().fromJson(firstCondition.setting, LocationSetting::class.java)
                    if (locationSetting == null) {
                        Log.d(TAG, "任务${task.id}：locationSetting is null")
                        continue
                    }

                    //TODO：判断条件是否满足

                    //TODO: 组装消息体 && 执行具体任务
                    val msg = locationInfoNew.toString()
                    val msgInfo = MsgInfo("task", task.name, msg, Date(), task.description)
                    val actionData = Data.Builder().putLong(TaskWorker.taskId, task.id).putString(TaskWorker.taskActions, task.actions).putString(TaskWorker.msgInfo, Gson().toJson(msgInfo)).build()
                    val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                    WorkManager.getInstance().enqueue(actionRequest)
                }

                return Result.success()
            }

            else -> {
                Log.d(TAG, "conditionType is $conditionType")
                return Result.failure()
            }
        }

    }

}