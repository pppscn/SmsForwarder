package com.idormy.sms.forwarder.workers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import com.idormy.sms.forwarder.entity.condition.BluetoothSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import java.util.Date

@Suppress("PrivatePropertyName", "DEPRECATION")
class BluetoothWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = BluetoothWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        try {
            val conditionType = inputData.getInt(TaskWorker.CONDITION_TYPE, -1)
            val action = inputData.getString(TaskWorker.ACTION) ?: BluetoothAdapter.ACTION_STATE_CHANGED
            val msg = inputData.getString(TaskWorker.MSG) ?: "1"
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

                val bluetoothSetting = Gson().fromJson(firstCondition.setting, BluetoothSetting::class.java)
                if (bluetoothSetting == null) {
                    Log.d(TAG, "TASK-${task.id}：bluetoothSetting is null")
                    continue
                }

                if (action != bluetoothSetting.action) {
                    Log.d(TAG, "TASK-${task.id}：action is not match, bluetoothSetting = $bluetoothSetting")
                    continue
                }

                var content = ""
                when (action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        if (msg != bluetoothSetting.state.toString()) {
                            Log.d(TAG, "TASK-${task.id}：bluetoothState is not match, bluetoothSetting = $bluetoothSetting")
                            continue
                        }
                    }

                    BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val devices = Gson().fromJson(msg, MutableMap::class.java)
                        Log.d(TAG, "TASK-${task.id}：devices = $devices")
                        if (devices.isEmpty() || !devices.containsKey(bluetoothSetting.device)) {
                            Log.d(TAG, "TASK-${task.id}：device is not match, bluetoothSetting = $bluetoothSetting")
                            continue
                        }
                        for ((k, v) in devices) {
                            content += "$k ($v)\n"
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        val devices = Gson().fromJson(msg, MutableMap::class.java)
                        Log.d(TAG, "TASK-${task.id}：devices = $devices")
                        if (bluetoothSetting.result == 1 && !devices.containsKey(bluetoothSetting.device)) {
                            Log.d(TAG, "TASK-${task.id}：device is not discovered, bluetoothSetting = $bluetoothSetting")
                            continue
                        } else if (bluetoothSetting.result == 0 && devices.containsKey(bluetoothSetting.device)) {
                            Log.d(TAG, "TASK-${task.id}：device is discovered, bluetoothSetting = $bluetoothSetting")
                            continue
                        }
                        for ((k, v) in devices) {
                            content += "$k ($v)\n"
                        }
                    }
                }

                //TODO：判断其他条件是否满足
                if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                    Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
                    continue
                }

                //TODO: 组装消息体 && 执行具体任务
                val msgInfo = MsgInfo("task", task.name, content.trim(), Date(), task.description)
                val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, task.id).putString(TaskWorker.TASK_ACTIONS, task.actions).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
                val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                WorkManager.getInstance().enqueue(actionRequest)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running worker: ${e.message}", e)
            return Result.failure()
        }
    }

}