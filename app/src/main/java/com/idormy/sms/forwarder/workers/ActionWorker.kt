package com.idormy.sms.forwarder.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.task.SmsSetting
import com.idormy.sms.forwarder.entity.task.TaskSetting
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TaskWorker
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xutil.XUtil

//执行每个task具体动作任务
@Suppress("PrivatePropertyName", "DEPRECATION")
class ActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = ActionWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(TaskWorker.taskId, -1L)
        val taskActionsJson = inputData.getString(TaskWorker.taskActions)
        val msgInfoJson = inputData.getString(TaskWorker.msgInfo)
        Log.d(TAG, "taskId: $taskId, taskActionsJson: $taskActionsJson, msgInfoJson: $msgInfoJson")
        if (taskId == -1L || taskActionsJson.isNullOrEmpty() || msgInfoJson.isNullOrEmpty()) {
            Log.d(TAG, "taskId is -1L or actionSetting is null")
            return Result.failure()
        }

        val actionList = Gson().fromJson(taskActionsJson, Array<TaskSetting>::class.java).toMutableList()
        if (actionList.isEmpty()) {
            Log.d(TAG, "任务$taskId：actionList is empty")
            return Result.failure()
        }

        val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java)
        if (msgInfo == null) {
            Log.d(TAG, "任务$taskId：msgInfo is null")
            return Result.failure()
        }

        var successNum = 0
        for (action in actionList) {
            when (action.type) {
                TASK_ACTION_SENDSMS -> {
                    val smsSetting = Gson().fromJson(action.setting, SmsSetting::class.java)
                    if (smsSetting == null) {
                        Log.d(TAG, "任务$taskId：smsSetting is null")
                        continue
                    }
                    //获取卡槽信息
                    if (App.SimInfoList.isEmpty()) {
                        App.SimInfoList = PhoneUtils.getSimMultiInfo()
                    }
                    Log.d(TAG, App.SimInfoList.toString())

                    //发送卡槽: 1=SIM1, 2=SIM2
                    val simSlotIndex = smsSetting.simSlot - 1
                    //TODO：取不到卡槽信息时，采用默认卡槽发送
                    val mSubscriptionId: Int = App.SimInfoList[simSlotIndex]?.mSubscriptionId ?: -1

                    val msg = if (ActivityCompat.checkSelfPermission(XUtil.getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        ResUtils.getString(R.string.no_sms_sending_permission)
                    } else {
                        PhoneUtils.sendSms(mSubscriptionId, smsSetting.phoneNumbers, smsSetting.msgContent)
                        successNum++
                    }

                    Log.d(TAG, "任务$taskId：send sms result: $msg")
                    continue
                }

                TASK_ACTION_NOTIFICATION -> {
                    try {
                        val settingVo = Gson().fromJson(action.setting, Rule::class.java)
                        //自动任务的不需要吐司或者更新日志，特殊处理 logId = -1，msgId = -1
                        SendUtils.sendMsgSender(msgInfo, settingVo, 0, -1L, -1L)
                        successNum++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    continue
                }

                else -> {
                    Log.d(TAG, "任务$taskId：action.type is ${action.type}")
                    continue
                }
            }
        }

        return if (successNum == actionList.size) Result.success() else Result.failure()
    }

}