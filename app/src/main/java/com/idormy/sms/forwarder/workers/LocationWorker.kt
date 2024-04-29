package com.idormy.sms.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.condition.LocationSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LEAVE_ADDRESS
import com.idormy.sms.forwarder.utils.TASK_CONDITION_TO_ADDRESS
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import com.idormy.sms.forwarder.utils.task.ConditionUtils.Companion.calculateDistance
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.Date

@Suppress("PrivatePropertyName", "DEPRECATION")
class LocationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = LocationWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        try {
            val conditionType = inputData.getInt(TaskWorker.CONDITION_TYPE, -1)
            val locationJsonOld = inputData.getString("locationJsonOld")
            val locationJsonNew = inputData.getString("locationJsonNew")
            Log.d(TAG, "conditionType = $conditionType, locationJsonOld = $locationJsonOld, locationJsonNew = $locationJsonNew")

            if (locationJsonOld == null || locationJsonNew == null) {
                Log.d(TAG, "locationInfoOld or locationInfoNew is null")
                return Result.failure()
            }

            val locationOld = Gson().fromJson(locationJsonOld, LocationInfo::class.java)
            val locationNew = Gson().fromJson(locationJsonNew, LocationInfo::class.java)
            if (locationOld == null || locationNew == null) {
                Log.d(TAG, "locationInfoOld or locationInfoNew is null")
                return Result.failure()
            }

            when (conditionType) {

                //到达地点
                TASK_CONDITION_TO_ADDRESS -> {
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

                        val locationSetting = Gson().fromJson(firstCondition.setting, LocationSetting::class.java)
                        if (locationSetting == null) {
                            Log.d(TAG, "TASK-${task.id}：locationSetting is null")
                            continue
                        }

                        //TODO：判断条件是否满足
                        var description = locationSetting.description
                        val isMatchCondition = when (locationSetting.calcType) {
                            "distance" -> {
                                val distanceOld = calculateDistance(locationOld.latitude, locationOld.longitude, locationSetting.latitude, locationSetting.longitude)
                                val distanceNew = calculateDistance(locationNew.latitude, locationNew.longitude, locationSetting.latitude, locationSetting.longitude)
                                Log.d(TAG, "TASK-${task.id}：distanceOld = $distanceOld, distanceNew = $distanceNew")
                                description += String.format(getString(R.string.current_distance_from_center), String.format("%.2f", distanceNew))
                                distanceOld > locationSetting.distance && distanceNew <= locationSetting.distance
                            }

                            "address" -> {
                                !locationOld.address.contains(locationSetting.address) && locationNew.address.contains(locationSetting.address)
                            }

                            else -> false
                        }

                        if (!isMatchCondition) {
                            Log.d(TAG, "TASK-${task.id}：isMatchCondition = false")
                            continue
                        }

                        //TODO：判断其他条件是否满足
                        if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                            Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
                            continue
                        }

                        //TODO: 组装消息体 && 执行具体任务
                        val msgInfo = MsgInfo("task", task.name, locationNew.toString(), Date(), description)
                        val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, task.id).putString(TaskWorker.TASK_ACTIONS, task.actions).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
                        val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                        WorkManager.getInstance().enqueue(actionRequest)
                    }

                    return Result.success()
                }

                //离开地点
                TASK_CONDITION_LEAVE_ADDRESS -> {
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

                        val locationSetting = Gson().fromJson(firstCondition.setting, LocationSetting::class.java)
                        if (locationSetting == null) {
                            Log.d(TAG, "TASK-${task.id}：locationSetting is null")
                            continue
                        }

                        //TODO：判断条件是否满足
                        var description = locationSetting.description
                        val isMatchCondition = when (locationSetting.calcType) {
                            "distance" -> {
                                val distanceOld = calculateDistance(locationOld.latitude, locationOld.longitude, locationSetting.latitude, locationSetting.longitude)
                                val distanceNew = calculateDistance(locationNew.latitude, locationNew.longitude, locationSetting.latitude, locationSetting.longitude)
                                Log.d(TAG, "TASK-${task.id}：distanceOld = $distanceOld, distanceNew = $distanceNew")
                                description += String.format(getString(R.string.current_distance_from_center), String.format("%.2f", distanceNew))
                                distanceOld <= locationSetting.distance && distanceNew > locationSetting.distance
                            }

                            "address" -> {
                                locationOld.address.contains(locationSetting.address) && !locationNew.address.contains(locationSetting.address)
                            }

                            else -> false
                        }

                        if (!isMatchCondition) {
                            Log.d(TAG, "TASK-${task.id}：isMatchCondition = false")
                            continue
                        }

                        //TODO：判断其他条件是否满足
                        if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                            Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
                            continue
                        }

                        //TODO: 组装消息体 && 执行具体任务
                        val msgInfo = MsgInfo("task", task.name, locationNew.toString(), Date(), description)
                        val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, task.id).putString(TaskWorker.TASK_ACTIONS, task.actions).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
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
        } catch (e: Exception) {
            Log.e(TAG, "Error running worker: ${e.message}", e)
            return Result.failure()
        }
    }

}