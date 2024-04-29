package com.idormy.sms.forwarder.workers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.AlarmSetting
import com.idormy.sms.forwarder.entity.action.CleanerSetting
import com.idormy.sms.forwarder.entity.action.FrpcSetting
import com.idormy.sms.forwarder.entity.action.HttpServerSetting
import com.idormy.sms.forwarder.entity.action.ResendSetting
import com.idormy.sms.forwarder.entity.action.RuleSetting
import com.idormy.sms.forwarder.entity.action.SenderSetting
import com.idormy.sms.forwarder.entity.action.SettingsSetting
import com.idormy.sms.forwarder.entity.action.SmsSetting
import com.idormy.sms.forwarder.entity.action.TaskActionSetting
import com.idormy.sms.forwarder.service.HttpServerService
import com.idormy.sms.forwarder.service.LocationService
import com.idormy.sms.forwarder.utils.ACTION_RESTART
import com.idormy.sms.forwarder.utils.CacheUtils
import com.idormy.sms.forwarder.utils.EVENT_ALARM_ACTION
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.EVENT_TOAST_INFO
import com.idormy.sms.forwarder.utils.EVENT_TOAST_SUCCESS
import com.idormy.sms.forwarder.utils.EVENT_TOAST_WARNING
import com.idormy.sms.forwarder.utils.HistoryUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_ACTION_ALARM
import com.idormy.sms.forwarder.utils.TASK_ACTION_CLEANER
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.TASK_ACTION_HTTPSERVER
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_RESEND
import com.idormy.sms.forwarder.utils.TASK_ACTION_RULE
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDER
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TASK_ACTION_SETTINGS
import com.idormy.sms.forwarder.utils.TASK_ACTION_TASK
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.resource.ResUtils.getString
import frpclib.Frpclib
import java.util.Calendar

//执行每个task具体动作任务
@Suppress("PrivatePropertyName")
class ActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = ActionWorker::class.java.simpleName
    private var taskId = -1L

    override suspend fun doWork(): Result {
        taskId = inputData.getLong(TaskWorker.TASK_ID, -1L)
        val taskConditionsJson = inputData.getString(TaskWorker.TASK_CONDITIONS)
        val taskActionsJson = inputData.getString(TaskWorker.TASK_ACTIONS)
        val msgInfoJson = inputData.getString(TaskWorker.MSG_INFO)
        Log.d(TAG, "taskId: $taskId, taskActionsJson: $taskActionsJson, msgInfoJson: $msgInfoJson")
        if (taskId == -1L || taskActionsJson.isNullOrEmpty() || msgInfoJson.isNullOrEmpty()) {
            Log.d(TAG, "taskId is -1L or actionSetting is null")
            return Result.failure()
        }

        //TODO: 如果传入的taskConditionsJson不为空，需要再次判断触发条件是否满足
        if (!taskConditionsJson.isNullOrEmpty()) {
            val conditionList = Gson().fromJson(taskConditionsJson, Array<TaskSetting>::class.java).toMutableList()
            if (conditionList.isEmpty()) {
                writeLog("conditionList is empty")
                return Result.failure()
            }
            if (!ConditionUtils.checkCondition(taskId, conditionList, 0, 0)) {
                writeLog("recheck condition is not pass", "WARN")
                return Result.failure()
            }
        }

        val actionList = Gson().fromJson(taskActionsJson, Array<TaskSetting>::class.java).toMutableList()
        if (actionList.isEmpty()) {
            writeLog("actionList is empty")
            return Result.failure()
        }

        val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java)
        if (msgInfo == null) {
            writeLog("msgInfo is null")
            return Result.failure()
        }

        var successNum = 0
        for (action in actionList) {
            try {
                when (action.type) {
                    TASK_ACTION_SENDSMS -> {
                        val smsSetting = Gson().fromJson(action.setting, SmsSetting::class.java)
                        if (smsSetting == null) {
                            writeLog("smsSetting is null")
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

                        val msg = if (ActivityCompat.checkSelfPermission(App.context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            getString(R.string.no_sms_sending_permission)
                        } else {
                            val mobileList = msgInfo.replaceTemplate(smsSetting.phoneNumbers)
                            val message = msgInfo.replaceTemplate(smsSetting.msgContent)
                            PhoneUtils.sendSms(mSubscriptionId, mobileList, message)
                        }
                        if (msg == null || msg == "") {
                            successNum++
                            writeLog(String.format(getString(R.string.successful_execution), smsSetting.description), "SUCCESS")
                        } else {
                            writeLog(msg, "ERROR")
                        }
                    }

                    TASK_ACTION_NOTIFICATION -> {
                        val ruleSetting = Gson().fromJson(action.setting, Rule::class.java)
                        //重新查询发送通道最新设置
                        val ids = ruleSetting.senderList.joinToString(",") { it.id.toString() }
                        ruleSetting.senderList = Core.sender.getByIds(ids.split(",").map { it.trim().toLong() }, ids)
                        //自动任务的不需要吐司或者更新日志，特殊处理 logId = -1，msgId = -1
                        SendUtils.sendMsgSender(msgInfo, ruleSetting, 0, -1L, -1L)

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), ruleSetting.getName()), "SUCCESS")
                    }

                    TASK_ACTION_CLEANER -> {
                        val cleanerSetting = Gson().fromJson(action.setting, CleanerSetting::class.java)
                        if (cleanerSetting == null) {
                            writeLog("cleanerSetting is null")
                            continue
                        }
                        if (cleanerSetting.days > 0) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_MONTH, 0 - cleanerSetting.days)
                            Core.msg.deleteTimeAgo(cal.timeInMillis)
                        } else {
                            Core.msg.deleteAll()
                        }
                        //清理缓存
                        HistoryUtils.clearPreference()
                        CacheUtils.clearAllCache(App.context)

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), cleanerSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_SETTINGS -> {
                        val settingsSetting = Gson().fromJson(action.setting, SettingsSetting::class.java)
                        if (settingsSetting == null) {
                            writeLog("settingsSetting is null")
                            continue
                        }

                        SettingUtils.enableSms = settingsSetting.enableSms
                        SettingUtils.enablePhone = settingsSetting.enablePhone
                        SettingUtils.enableCallType1 = settingsSetting.enableCallType1
                        SettingUtils.enableCallType2 = settingsSetting.enableCallType2
                        SettingUtils.enableCallType3 = settingsSetting.enableCallType3
                        SettingUtils.enableCallType4 = settingsSetting.enableCallType4
                        SettingUtils.enableCallType5 = settingsSetting.enableCallType5
                        SettingUtils.enableCallType6 = settingsSetting.enableCallType6
                        SettingUtils.enableAppNotify = settingsSetting.enableAppNotify
                        SettingUtils.enableCancelAppNotify = settingsSetting.enableCancelAppNotify
                        SettingUtils.enableNotUserPresent = settingsSetting.enableNotUserPresent
                        SettingUtils.enableLocation = settingsSetting.enableLocation
                        SettingUtils.locationAccuracy = settingsSetting.locationAccuracy
                        SettingUtils.locationPowerRequirement = settingsSetting.locationPowerRequirement
                        SettingUtils.locationMinInterval = settingsSetting.locationMinInterval
                        SettingUtils.locationMinDistance = settingsSetting.locationMinDistance
                        SettingUtils.enableSmsCommand = settingsSetting.enableSmsCommand
                        SettingUtils.smsCommandSafePhone = settingsSetting.smsCommandSafePhone
                        SettingUtils.enableLoadAppList = settingsSetting.enableLoadAppList
                        SettingUtils.enableLoadUserAppList = settingsSetting.enableLoadUserAppList
                        SettingUtils.enableLoadSystemAppList = settingsSetting.enableLoadSystemAppList
                        SettingUtils.cancelExtraAppNotify = settingsSetting.cancelExtraAppNotify
                        SettingUtils.duplicateMessagesLimits = settingsSetting.duplicateMessagesLimits

                        if (settingsSetting.enableLocation) {
                            val serviceIntent = Intent(App.context, LocationService::class.java)
                            serviceIntent.action = ACTION_RESTART
                            App.context.startService(serviceIntent)
                        }

                        if (settingsSetting.enableLoadAppList) {
                            val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                            WorkManager.getInstance(XUtil.getContext()).enqueue(request)
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), settingsSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_FRPC -> {
                        if (!App.FrpclibInited) {
                            writeLog("还未下载Frpc库")
                            continue
                        }
                        val frpcSetting = Gson().fromJson(action.setting, FrpcSetting::class.java)
                        if (frpcSetting == null) {
                            writeLog("frpcSetting is null")
                            continue
                        }

                        val frpcList = frpcSetting.frpcList.ifEmpty {
                            Core.frpc.getAutorun()
                        }

                        if (frpcList.isEmpty()) {
                            writeLog("没有需要操作的Frpc")
                            continue
                        }

                        for (frpc in frpcList) {
                            if (frpcSetting.action == "start") {
                                if (!Frpclib.isRunning(frpc.uid)) {
                                    val error = Frpclib.runContent(frpc.uid, frpc.config)
                                    if (!TextUtils.isEmpty(error)) {
                                        Log.e(TAG, error)
                                    }
                                }
                            } else if (frpcSetting.action == "stop") {
                                if (Frpclib.isRunning(frpc.uid)) {
                                    Frpclib.close(frpc.uid)
                                }
                            }
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), frpcSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_HTTPSERVER -> {
                        val httpServerSetting = Gson().fromJson(action.setting, HttpServerSetting::class.java)
                        if (httpServerSetting == null) {
                            writeLog("httpServerSetting is null")
                            continue
                        }

                        HttpServerUtils.enableApiClone = httpServerSetting.enableApiClone
                        HttpServerUtils.enableApiSmsQuery = httpServerSetting.enableApiSmsQuery
                        HttpServerUtils.enableApiSmsSend = httpServerSetting.enableApiSmsSend
                        HttpServerUtils.enableApiCallQuery = httpServerSetting.enableApiCallQuery
                        HttpServerUtils.enableApiContactQuery = httpServerSetting.enableApiContactQuery
                        HttpServerUtils.enableApiContactAdd = httpServerSetting.enableApiContactAdd
                        HttpServerUtils.enableApiWol = httpServerSetting.enableApiWol
                        HttpServerUtils.enableApiLocation = httpServerSetting.enableApiLocation
                        HttpServerUtils.enableApiBatteryQuery = httpServerSetting.enableApiBatteryQuery
                        Intent(App.context, HttpServerService::class.java).also {
                            if (httpServerSetting.action == "start") {
                                App.context.startService(it)
                            } else if (httpServerSetting.action == "stop") {
                                App.context.stopService(it)
                            }
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), httpServerSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_RULE -> {
                        val ruleSetting = Gson().fromJson(action.setting, RuleSetting::class.java)
                        if (ruleSetting == null) {
                            writeLog("httpServerSetting is null")
                            continue
                        }

                        val ids = ruleSetting.ruleList.map { it.id }
                        if (ids.isNotEmpty()) {
                            Core.rule.updateStatusByIds(ids, ruleSetting.status)
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), ruleSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_SENDER -> {
                        val senderSetting = Gson().fromJson(action.setting, SenderSetting::class.java)
                        if (senderSetting == null) {
                            writeLog("senderSetting is null")
                            continue
                        }

                        val ids = senderSetting.senderList.map { it.id }
                        if (ids.isNotEmpty()) {
                            Core.sender.updateStatusByIds(ids, senderSetting.status)
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), senderSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_TASK -> {
                        val taskActionSetting = Gson().fromJson(action.setting, TaskActionSetting::class.java)
                        if (taskActionSetting == null) {
                            writeLog("taskActionSetting is null")
                            continue
                        }

                        val ids = taskActionSetting.taskList.map { it.id }
                        if (ids.isNotEmpty()) {
                            Core.task.updateStatusByIds(ids, taskActionSetting.status)
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), taskActionSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_ALARM -> {
                        val alarmSetting = Gson().fromJson(action.setting, AlarmSetting::class.java)
                        if (alarmSetting == null) {
                            writeLog("alarmSetting is null")
                            continue
                        }

                        // 发送开始播放指令
                        LiveEventBus.get<AlarmSetting>(EVENT_ALARM_ACTION).post(alarmSetting)

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), alarmSetting.description), "SUCCESS")
                    }

                    TASK_ACTION_RESEND -> {
                        val resendSetting = Gson().fromJson(action.setting, ResendSetting::class.java)
                        if (resendSetting == null) {
                            writeLog("resendSetting is null")
                            continue
                        }

                        val logsList = Core.logs.getIdsByTimeAndStatus(resendSetting.hours, resendSetting.statusList)
                        logsList.forEach { item ->
                            Log.d(TAG, "resend logsList item: $item")
                            SendUtils.retrySendMsg(item.id)
                        }

                        successNum++
                        writeLog(String.format(getString(R.string.successful_execution), resendSetting.description), "SUCCESS")
                    }

                    else -> {
                        writeLog("action.type is ${action.type}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "action.type is ${action.type}, exception: ${e.message}")
                writeLog("action.type is ${action.type}, exception: ${e.message}")
            }
        }

        return if (successNum == actionList.size) Result.success() else Result.failure()
    }

    private fun writeLog(msg: String, level: String = "DEBUG") {
        val key = when (level) {
            "INFO" -> {
                Log.i(TAG, "TASK-$taskId：$msg")
                EVENT_TOAST_INFO
            }

            "WARN" -> {
                Log.w(TAG, "TASK-$taskId：$msg")
                EVENT_TOAST_WARNING
            }

            "ERROR" -> {
                Log.e(TAG, "TASK-$taskId：$msg")
                EVENT_TOAST_ERROR
            }

            "SUCCESS" -> {
                Log.d(TAG, "TASK-$taskId：$msg")
                EVENT_TOAST_SUCCESS
            }

            else -> {
                Log.d(TAG, "TASK-$taskId：$msg")
                ""
            }
        }

        if (taskId == 0L && key.isNotEmpty()) {
            LiveEventBus.get(key, String::class.java).post(msg)
            return
        }

        //TODO: 写入日志
    }

}