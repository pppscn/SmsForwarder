package com.idormy.sms.forwarder.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Logs
import com.idormy.sms.forwarder.database.entity.Msg
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_ALL
import com.idormy.sms.forwarder.utils.DataProvider
import com.idormy.sms.forwarder.utils.HistoryUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_CONDITION_APP
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CALL
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SMS
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import com.xuexiang.xutil.resource.ResUtils
import com.xuexiang.xutil.security.CipherUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("PrivatePropertyName", "DEPRECATION")
class SendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = SendWorker::class.java.simpleName

    @SuppressLint("SimpleDateFormat")
    override suspend fun doWork(): Result {

        return withContext(Dispatchers.IO) {
            try {
                val msgInfoJson = inputData.getString(Worker.SEND_MSG_INFO)
                if (msgInfoJson.isNullOrBlank()) {
                    return@withContext Result.failure(workDataOf("send" to "msgInfoJson is null"))
                }

                val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java)
                //【注意】卡槽id：-1=获取失败、0=卡槽1、1=卡槽2，但是 Rule 表里存的是 SIM1/SIM2
                val simSlot = "SIM" + (msgInfo.simSlot + 1)

                //自动任务处理逻辑
                autoTaskProcess(msgInfo, msgInfoJson, simSlot)

                // 免打扰(禁用转发)时间段
                var isSilentPeriod = false
                Log.d(TAG, "silentPeriodStart = ${SettingUtils.silentPeriodStart}, silentPeriodEnd = ${SettingUtils.silentPeriodEnd}")
                if (SettingUtils.silentPeriodStart != SettingUtils.silentPeriodEnd) {
                    isSilentPeriod = DataProvider.isCurrentTimeInPeriod(SettingUtils.silentPeriodStart, SettingUtils.silentPeriodEnd)
                    Log.d(TAG, "isSilentPeriod = $isSilentPeriod, enableSilentPeriodLogs = ${SettingUtils.enableSilentPeriodLogs}")
                    if (isSilentPeriod && !SettingUtils.enableSilentPeriodLogs) {
                        Log.e(TAG, "免打扰(禁用转发)时间段")
                        return@withContext Result.failure(workDataOf("send" to "failed"))
                    }
                }

                // 过滤重复消息机制
                val duplicateMessagesLimits = SettingUtils.duplicateMessagesLimits * 1000L
                if (duplicateMessagesLimits > 0L) {
                    val key = CipherUtils.md5(msgInfo.type + msgInfo.from + msgInfo.content)
                    val timestamp: Long = System.currentTimeMillis()
                    var timestampPrev: Long by HistoryUtils(key, timestamp)
                    Log.d(TAG, "duplicateMessagesLimits=$duplicateMessagesLimits, timestamp=$timestamp, timestampPrev=$timestampPrev, msgInfo=$msgInfo")
                    if (timestampPrev != timestamp && timestamp - timestampPrev <= duplicateMessagesLimits) {
                        Log.e(TAG, "过滤重复消息机制")
                        timestampPrev = timestamp
                        return@withContext Result.failure(workDataOf("send" to "failed"))
                    }
                    timestampPrev = timestamp
                }

                val ruleList: List<Rule> = Core.rule.getRuleList(msgInfo.type, 1, simSlot)
                if (ruleList.isEmpty()) {
                    return@withContext Result.failure(workDataOf("send" to "failed"))
                }

                val ruleListMatched: MutableList<Rule> = mutableListOf()
                for (rule in ruleList) {
                    Log.d(TAG, rule.toString())
                    if (rule.checkMsg(msgInfo)) ruleListMatched.add(rule)
                }
                if (ruleListMatched.isEmpty()) {
                    return@withContext Result.failure(workDataOf("send" to "failed"))
                }

                val msg = Msg(0, msgInfo.type, msgInfo.from, msgInfo.content, msgInfo.simSlot, msgInfo.simInfo, msgInfo.subId, msgInfo.callType)
                val msgId = Core.msg.insert(msg)
                for (rule in ruleListMatched) {
                    val sender = rule.senderList[0]
                    if (isSilentPeriod) {
                        val log = Logs(0, msgInfo.type, msgId, rule.id, sender.id, 0, ResUtils.getString(R.string.silent_time_period))
                        Core.logs.insert(log)
                        continue
                    }
                    val log = Logs(0, msgInfo.type, msgId, rule.id, sender.id)
                    val logId = Core.logs.insert(log)
                    SendUtils.sendMsgSender(msgInfo, rule, 0, logId, msgId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "SendWorker error: ${e.message}")
                return@withContext Result.failure(workDataOf("send" to e.message.toString()))
            }

            return@withContext Result.success()
        }
    }

    private fun autoTaskProcess(msgInfo: MsgInfo, msgInfoJson: String, simSlot: String) {
        val conditionType = when (msgInfo.type) {
            "app" -> TASK_CONDITION_APP
            "call" -> TASK_CONDITION_CALL
            else -> TASK_CONDITION_SMS
        }

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

            val ruleSetting = Gson().fromJson(firstCondition.setting, Rule::class.java)
            if (ruleSetting == null) {
                Log.d(TAG, "TASK-${task.id}：ruleSetting is null")
                continue
            }

            if (ruleSetting.simSlot != CHECK_SIM_SLOT_ALL && simSlot != ruleSetting.simSlot) {
                Log.d(TAG, "TASK-${task.id}：simSlot is not matched, simSlot = $simSlot, ruleSetting = $ruleSetting")
                continue
            }

            if (!ruleSetting.checkMsg(msgInfo)) {
                Log.d(TAG, "TASK-${task.id}：ruleSetting is not matched, msgInfo = $msgInfo, ruleSetting = $ruleSetting")
                continue
            }

            //TODO：判断其他条件是否满足
            if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
                continue
            }

            //TODO: 组装消息体 && 执行具体任务
            val actionData = Data.Builder()
                .putLong(TaskWorker.TASK_ID, task.id)
                .putString(TaskWorker.TASK_ACTIONS, task.actions)
                .putString(TaskWorker.MSG_INFO, msgInfoJson)
                .build()
            val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
            WorkManager.getInstance().enqueue(actionRequest)
        }
    }

}