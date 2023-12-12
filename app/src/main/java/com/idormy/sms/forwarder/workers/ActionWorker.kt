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
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.Worker
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xutil.XUtil

@Suppress("PrivatePropertyName", "DEPRECATION")
class ActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = ActionWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(Worker.taskId, -1L)
        val actionType = inputData.getInt(Worker.actionType, -1)
        val actionSetting = inputData.getString(Worker.actionSetting)
        val msgInfoJson = inputData.getString(Worker.sendMsgInfo)
        Log.d(TAG, "taskId: $taskId, actionType: $actionType, actionSetting: $actionSetting, msgInfoJson: $msgInfoJson")
        if (taskId == -1L || actionSetting == null) {
            Log.d(TAG, "taskId is -1L or actionSetting is null")
            return Result.failure()
        }
        val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java)

        when (actionType) {
            TASK_ACTION_SENDSMS -> {
                val smsSetting = Gson().fromJson(actionSetting, SmsSetting::class.java)
                if (smsSetting == null) {
                    Log.d(TAG, "任务$taskId：smsSetting is null")
                    return Result.failure()
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
                    PhoneUtils.sendSms(mSubscriptionId, smsSetting.phoneNumbers, smsSetting.msgContent) ?: "success"
                }

                Log.d(TAG, "任务$taskId：send sms result: $msg")
                return Result.success()
            }

            TASK_ACTION_NOTIFICATION -> {
                return try {
                    val settingVo = Gson().fromJson(actionSetting, Rule::class.java)
                    //自动任务的不需要吐司或者更新日志，特殊处理 logId = -1，msgId = -1
                    SendUtils.sendMsgSender(msgInfo, settingVo, 0, -1L, -1L)
                    Result.success()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.failure()
                }
            }

            else -> {
                Log.d(TAG, "任务$taskId：action.type is $actionType")
                return Result.failure()
            }
        }
    }

}