package com.idormy.sms.forwarder.workers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.CleanerSetting
import com.idormy.sms.forwarder.entity.action.FrpcSetting
import com.idormy.sms.forwarder.entity.action.HttpServerSetting
import com.idormy.sms.forwarder.entity.action.SmsSetting
import com.idormy.sms.forwarder.service.HttpServerService
import com.idormy.sms.forwarder.utils.CacheUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.EVENT_TOAST_INFO
import com.idormy.sms.forwarder.utils.EVENT_TOAST_SUCCESS
import com.idormy.sms.forwarder.utils.EVENT_TOAST_WARNING
import com.idormy.sms.forwarder.utils.HistoryUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.TASK_ACTION_CLEANER
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.TASK_ACTION_HTTPSERVER
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TaskWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.file.FileUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import frpclib.Frpclib
import java.util.Calendar

//执行每个task具体动作任务
@Suppress("PrivatePropertyName")
class ActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = ActionWorker::class.java.simpleName
    private var taskId = -1L

    override suspend fun doWork(): Result {
        taskId = inputData.getLong(TaskWorker.taskId, -1L)
        val taskActionsJson = inputData.getString(TaskWorker.taskActions)
        val msgInfoJson = inputData.getString(TaskWorker.msgInfo)
        Log.d(TAG, "taskId: $taskId, taskActionsJson: $taskActionsJson, msgInfoJson: $msgInfoJson")
        if (taskId == -1L || taskActionsJson.isNullOrEmpty() || msgInfoJson.isNullOrEmpty()) {
            Log.d(TAG, "taskId is -1L or actionSetting is null")
            return Result.failure()
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

                        if (ActivityCompat.checkSelfPermission(App.context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            val msg = getString(R.string.no_sms_sending_permission)
                            writeLog(msg, "ERROR")
                        } else {
                            val msg = PhoneUtils.sendSms(mSubscriptionId, smsSetting.phoneNumbers, smsSetting.msgContent)
                            successNum++
                            writeLog("send sms result: $msg", "SUCCESS")
                        }
                    }

                    TASK_ACTION_NOTIFICATION -> {
                        val settingVo = Gson().fromJson(action.setting, Rule::class.java)
                        //自动任务的不需要吐司或者更新日志，特殊处理 logId = -1，msgId = -1
                        SendUtils.sendMsgSender(msgInfo, settingVo, 0, -1L, -1L)

                        successNum++
                        writeLog("send notification success", "SUCCESS")
                    }

                    TASK_ACTION_FRPC -> {
                        if (!FileUtils.isFileExists(App.context.filesDir?.absolutePath + "/libs/libgojni.so")) {
                            writeLog("还未下载Frpc库")
                            continue
                        }
                        val frpcSetting = Gson().fromJson(action.setting, FrpcSetting::class.java)
                        if (frpcSetting == null) {
                            writeLog("frpcSetting is null")
                            continue
                        }

                        val frpcList = if (frpcSetting.uids.isEmpty()) {
                            AppDatabase.getInstance(App.context).frpcDao().getAutorun()
                        } else {
                            val uids = frpcSetting.uids.split(",")
                            AppDatabase.getInstance(App.context).frpcDao().getByUids(uids)
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
                        writeLog("frpc success", "SUCCESS")
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
                        writeLog("httpServer success", "SUCCESS")
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
                        writeLog("cleaner success", "SUCCESS")
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